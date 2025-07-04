from flask import Flask, request, render_template_string, jsonify, redirect, url_for, flash, session
import json
import os
import logging
import requests
from deep_translator import GoogleTranslator
from functools import lru_cache
import re
from gtts import gTTS
import base64
import io
import hashlib
from datetime import datetime

# --- 1. إدارة الإعدادات (Configuration Management) ---
class Config:
    SECRET_KEY = os.environ.get('SECRET_KEY', 'your_highly_secure_and_random_secret_key_for_production_dobbi')

    # !!! هام جداً: استبدل 'YOUR_ACTUAL_GEMINI_API_KEY_HERE' بمفتاحك الفعلي !!!
    GEMINI_API_KEY = os.environ.get('GEMINI_API_KEY', 'AIzaSyAuwr-6B2Mq9B1m_zYOds6cxcWcnmUH5aM')

    BASE_DIR = os.path.abspath(os.path.dirname(__file__))
    DATA_DIR = os.path.join(BASE_DIR, "data")
    TAFSIR_DIR = os.path.join(DATA_DIR, "tafasir_json")
    SURAHS_FILE = os.path.join(DATA_DIR, "surahs.json")
    QURAN_FILE = os.path.join(DATA_DIR, "quran.json")
    FAVORITES_FILE = os.path.join(BASE_DIR, "favorites.json")

    GEMINI_MODEL = "gemini-1.5-flash" # يمكنك تجربة "gemini-1.5-pro" إذا احتجت دقة أكبر (مع استهلاك أكثر للتوكنز)
    GEMINI_API_URL = f"https://generativelanguage.googleapis.com/v1beta/models/{GEMINI_MODEL}:generateContent"

# --- 2. إعداد تطبيق Flask ---
app = Flask(__name__)
app.config.from_object(Config)
app.secret_key = app.config['SECRET_KEY']

# --- 3. تهيئة نظام التسجيل (Logging Configuration) ---
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

# --- 4. الدوال المساعدة (Helper Functions) ---

# لا نستخدم lru_cache هنا لأننا نعدل البيانات لـ get_surah_number
def load_surah_names_data():
    """تحميل أسماء السور من ملف surahs.json. إذا لم يكن موجوداً، يتم إنشاء بيانات افتراضية."""
    if not os.path.exists(app.config['SURAHS_FILE']):
        logger.warning(f"ملف السور غير موجود: {app.config['SURAHS_FILE']}. سيتم استخدام أسماء افتراضية.")
        return {str(i): {"arabic": f"السورة {i}", "english": f"Surah {i}"} for i in range(1, 115)}
    try:
        with open(app.config['SURAHS_FILE'], 'r', encoding='utf-8') as f:
            data = json.load(f)
            return data if data else {}
    except (json.JSONDecodeError, Exception) as e:
        logger.error(f"خطأ في تحميل أو فك ترميز ملف surahs.json: {e}. سيتم استخدام أسماء افتراضية.")
        return {str(i): {"arabic": f"السورة {i}", "english": f"Surah {i}"} for i in range(1, 115)}

_surahs_data_cache = load_surah_names_data() # تحميل البيانات مرة واحدة عند بدء التطبيق

# تم إزالة lru_cache من get_surah_number لنتجنب TypeError: unhashable type: 'dict'
def get_surah_number(input_value):
    """يحدد رقم السورة بناءً على المدخل (إما رقم أو اسم السورة بالعربي/الإنجليزي)."""
    surahs_data = _surahs_data_cache # استخدام البيانات المحملة مرة واحدة
    
    if not input_value: return None
    input_value = input_value.strip().lower()

    def normalize_arabic(text):
        # إزالة التشكيل
        text = re.sub(r'[ًٌٍَُِّّْ]', '', text)
        # توحيد الهمزات والتاء المربوطة
        text = re.sub(r'أ|إ|آ', 'ا', text)
        text = re.sub(r'ة', 'ه', text)
        return text

    normalized_input = normalize_arabic(input_value)

    if input_value.isdigit():
        num = int(input_value)
        return str(num) if 1 <= num <= 114 else None
    
    for num, names in surahs_data.items():
        arabic_name = names.get("arabic", "").strip().lower()
        english_name = names.get("english", "").strip().lower()
        
        if input_value == arabic_name or input_value == english_name: return num
        if normalized_input == normalize_arabic(arabic_name): return num
            
    return None

@lru_cache(maxsize=1)
def load_quran_text():
    """تحميل نص القرآن الكريم من ملف quran.json. إذا لم يكن موجوداً، سيتم الإبلاغ."""
    if not os.path.exists(app.config['QURAN_FILE']):
        logger.warning(f"ملف القرآن غير موجود: {app.config['QURAN_FILE']}. لن يتم عرض نص الآيات من ملف محلي.")
        return {} # سيتم جلب الآية من AI لاحقاً إذا لزم الأمر
    try:
        with open(app.config['QURAN_FILE'], 'r', encoding='utf-8') as f:
            data = json.load(f)
            return data if data else {}
    except (json.JSONDecodeError, Exception) as e:
        logger.error(f"خطأ في تحميل أو فك ترميز ملف quran.json: {e}. لن يتم عرض نص الآيات من ملف محلي.")
        return {}

def get_ayah_text_from_quran_json(surah_number, ayah_number, quran_data):
    """يسترد نص آية محددة من بيانات القرآن المحملة محلياً."""
    surah_key = str(surah_number)
    ayah_key = str(ayah_number)
    return quran_data.get(surah_key, {}).get(ayah_key)

def get_total_ayahs_in_surah(surah_number, quran_data):
    """يعيد إجمالي عدد الآيات في سورة معينة من بيانات quran.json."""
    surah_key = str(surah_number)
    return len(quran_data.get(surah_key, {}))

@lru_cache(maxsize=128)
def get_tafsir_data_local(surah_number):
    """تحميل بيانات التفسير لسورة معينة من ملف JSON الخاص بها. يعيد { } إذا لم يكن موجوداً."""
    path = os.path.join(app.config['TAFSIR_DIR'], f"{surah_number}.json")
    if not os.path.exists(path):
        logger.warning(f"ملف التفسير الميسر غير موجود للسورة {surah_number}: {path}. سيتم استخدام الذكاء الاصطناعي.")
        return {}
    try:
        with open(path, 'r', encoding='utf-8') as f:
            return json.load(f)
    except (json.JSONDecodeError, Exception) as e:
        logger.error(f"خطأ في تحميل أو فك ترميز JSON التفسير الميسر للسورة {surah_number}: {e}. سيتم استخدام الذكاء الاصطناعي.")
        return {}

def ask_gemini(prompt):
    """يرسل طلباً إلى Gemini API ويعيد الاستجابة."""
    if not app.config['GEMINI_API_KEY'] or app.config['GEMINI_API_KEY'] == 'YOUR_ACTUAL_GEMINI_API_KEY_HERE':
        logger.error("خطأ: لم يتم تكوين مفتاح Gemini API. يرجى إضافته إلى app.py أو كمتغير بيئة.")
        flash("عذرًا، لم يتم تكوين مفتاح API الخاص بـ Gemini بشكل صحيح. يرجى إعلام المسؤول.", "error")
        return None

    headers = {
        "Content-Type": "application/json",
        "X-goog-api-key": app.config['GEMINI_API_KEY']
    }
    payload = {
        "contents": [
            {"parts": [{"text": prompt}]}
        ]
    }
    try:
        res = requests.post(app.config['GEMINI_API_URL'], headers=headers, json=payload, timeout=90) # زيادة المهلة
        res.raise_for_status()
        data = res.json()
        if data and 'candidates' in data and data['candidates']:
            return data['candidates'][0]['content']['parts'][0]['text']
        else:
            logger.warning(f"Gemini API لم تُعد أي مرشحات أو استجابة صالحة: {json.dumps(data, ensure_ascii=False)}")
            flash("عذرًا، لم يستجب الذكاء الاصطناعي بشكل مفهوم. يرجى المحاولة مرة أخرى.", "warning")
            return None
    except requests.exceptions.Timeout:
        logger.error("انتهت مهلة طلب Gemini API.")
        flash("انتهت مهلة طلب تفسير الذكاء الاصطناعي. قد تكون الشبكة بطيئة.", "error")
        return None
    except requests.exceptions.RequestException as e:
        logger.error(f"فشل الاتصال بـ Gemini API: {e}")
        flash(f"فشل في الاتصال بخدمة الذكاء الاصطناعي: {e}", "error")
        return None
    except Exception as e:
        logger.error(f"حدث خطأ غير متوقع أثناء الاتصال بـ Gemini API: {e}")
        flash(f"حدث خطأ غير متوقع أثناء المعالجة: {e}", "error")
        return None

@lru_cache(maxsize=1024)
def translate_text(text, to_lang='en'):
    """يترجم النص باستخدام مترجم جوجل (deep_translator)."""
    if not text: return None
    try:
        return GoogleTranslator(source='auto', target=to_lang).translate(text)
    except Exception as e:
        logger.error(f"فشلت الترجمة للنص '{text[:50]}...': {e}")
        return None

def text_to_speech_base64(text, lang='ar'):
    """يحول النص إلى كلام ويعيده كـ Base64 لدمجه في HTML."""
    if not text: return None
    try:
        tts = gTTS(text=text, lang=lang, slow=False)
        audio_buffer = io.BytesIO()
        tts.write_to_fp(audio_buffer)
        audio_buffer.seek(0)
        return base64.b64encode(audio_buffer.read()).decode('utf-8')
    except Exception as e:
        logger.error(f"فشل تحويل النص إلى كلام: {e}")
        return None

def search_in_tafasir_local(query, surahs_data, quran_data):
    """
    يبحث عن الكلمات المفتاحية في جميع التفاسير المحلية المتاحة (التفسير الميسر حالياً).
    يعيد قائمة بالآيات المطابقة مع تفسيرها.
    """
    results = []
    # Loop through surah numbers from 1 to 114
    for surah_num_str in map(str, range(1, 115)): 
        tafsir_data = get_tafsir_data_local(surah_num_str) 
        
        if not tafsir_data: # تخطي السور التي ليس لها ملف تفسير محلي
            continue

        surah_name_arabic = surahs_data.get(surah_num_str, {}).get("arabic", f"السورة {surah_num_str}")
        
        for ayah_num, tafsir_text in tafsir_data.items():
            if query.lower() in tafsir_text.lower():
                ayah_text = get_ayah_text_from_quran_json(surah_num_str, ayah_num, quran_data)
                results.append({
                    "surah_name": surah_name_arabic,
                    "surah_number": surah_num_str,
                    "ayah_number": ayah_num,
                    "ayah_text": ayah_text, 
                    "tafsir": tafsir_text,
                    "hash": hashlib.md5(f"{surah_num_str}-{ayah_num}-{tafsir_text}".encode()).hexdigest()
                })
    return results

def load_favorites():
    """تحميل قائمة المفضلة من ملف JSON."""
    if not os.path.exists(app.config['FAVORITES_FILE']):
        return []
    try:
        with open(app.config['FAVORITES_FILE'], 'r', encoding='utf-8') as f:
            return json.load(f)
    except json.JSONDecodeError:
        logger.error("خطأ في قراءة ملف المفضلة، سيتم إنشاء ملف جديد.")
        flash("حدث خطأ في قراءة المفضلة، سيتم البدء بقائمة جديدة.", "error")
        return [] 
    except Exception as e:
        logger.error(f"خطأ غير متوقع عند تحميل ملف المفضلة: {e}")
        flash(f"حدث خطأ غير متوقع عند تحميل المفضلة: {e}", "error")
        return []

def save_favorites(favorites):
    """حفظ قائمة المفضلة إلى ملف JSON."""
    try:
        with open(app.config['FAVORITES_FILE'], 'w', encoding='utf-8') as f:
            json.dump(favorites, f, ensure_ascii=False, indent=4)
    except Exception as e:
        logger.error(f"خطأ في حفظ المفضلة: {e}")
        flash(f"حدث خطأ في حفظ المفضلة: {e}", "error")

# --- 5. قالب HTML (Improved HTML Template with new features) ---
HTML_TEMPLATE = '''
<!DOCTYPE html>
<html lang="ar" dir="rtl">
<head>
    <meta charset="UTF-8">
    <title>المصباح الوهّاج - تفسير وذكاء</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link href="https://fonts.googleapis.com/css2?family=Cairo:wght@400;700&family=Amiri+Quran&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0-beta3/css/all.min.css">
    <style>
        /* CSS styles */
        :root {
            --bg-gradient-start: rgba(0, 0, 0, 0.6); /* Darker overlay */
            --bg-gradient-end: rgba(0, 0, 0, 0.6);   /* Darker overlay */
            --bg-image: url('https://i.imgur.com/eQ4L2Z2.jpg'); /* Luxurious Islamic Pattern */
            --text-color: #000; /* Black text for light mode */
            --heading-color: #000; /* Black headings for light mode */
            --form-bg: #ffffff;
            --form-shadow: rgba(0,0,0,0.1);
            --border-color-form: #4CAF50; /* Green */
            --input-bg: #f8f8f8;
            --input-focus-bg: #ffffff;
            --input-focus-shadow: rgba(76,175,80,0.3);
            --button-bg: #4CAF50; /* Green */
            --button-hover-bg: #45a049;
            --button-shadow: rgba(76,175,80,0.4);
            --result-bg: #ffffff;
            --result-shadow: rgba(0,0,0,0.1);
            --result-border-left: #0f3460;
            --result-divider: #eee;
            --error-bg: #ffebee;
            --error-border: #f44336;
            --error-shadow: rgba(244,67,54,0.3);
            --footer-color: #777;
            --spinner-border-top: #4CAF50;
            --flash-success-bg: #e8f5e9;
            --flash-success-border: #4CAF50;
            --flash-error-bg: #ffebee;
            --flash-error-border: #f44336;
            --flash-warning-bg: #fffde7;
            --flash-warning-border: #ffeb3b;
            --copy-button-bg: #607d8b; /* Blue Grey */
            --copy-button-hover-bg: #546e7a;
            --nav-button-bg: #7986cb; /* Indigo light */
            --nav-button-hover-bg: #606fc7;
            --nav-button-border: #9fa8da;
        }

        body.dark-mode {
            --bg-gradient-start: rgba(0,0,0,0.85);
            --bg-gradient-end: rgba(0,0,0,0.85);
            --text-color: #f0f0f0; /* Light text for dark mode */
            --heading-color: #ffeb3b; /* Brighter gold for dark mode headings */
            --form-bg: #1a1a2e; /* Darker form background */
            --form-shadow: rgba(0,0,0,0.9);
            --border-color-form: #4caf50; /* A bit lighter green */
            --input-bg: #22223b;
            --input-focus-bg: #2a2a4f;
            --input-focus-shadow: rgba(76,175,80,0.7);
            --button-bg: #4caf50; /* Darker green for button */
            --button-hover-bg: #388e3c;
            --button-shadow: rgba(76,175,80,0.4);
            --result-bg: #1a1a2e;
            --result-shadow: rgba(0,0,0,0.7);
            --result-border-left: #ffeb3b;
            --result-divider: #3a3f5c;
            --error-bg: #5c1b1b;
            --error-border: #ef5350;
            --error-shadow: rgba(239,83,80,0.5);
            --footer-color: #aaa;
            --spinner-border-top: #4caf50;
            --flash-success-bg: #2e5c3e;
            --flash-success-border: #4caf50;
            --flash-error-bg: #5c2e2e;
            --flash-error-border: #ef5350;
            --flash-warning-bg: #5c5c2e;
            --flash-warning-border: #ffeb3b;
            --copy-button-bg: #6a8cb0; 
            --copy-button-hover-bg: #5a7b9f;
            --nav-button-bg: #4a577b;
            --nav-button-hover-bg: #3a476b;
            --nav-button-border: #6a779b;
        }
        
        body { 
            background: linear-gradient(var(--bg-gradient-start), var(--bg-gradient-end)), var(--bg-image) no-repeat center center fixed;
            background-size: cover;
            color: var(--text-color); 
            font-family: 'Cairo', sans-serif; 
            max-width: 900px;
            margin: auto; 
            padding: 2rem; 
            line-height: 1.8;
            box-shadow: 0 0 50px rgba(0, 0, 0, 0.8);
            border-radius: 15px;
        }
        h2 { 
            color: var(--heading-color); 
            text-align: center; 
            margin-bottom: 2.5rem;
            font-size: 3rem;
            letter-spacing: 2px;
            font-family: 'Amiri Quran', serif;
            text-shadow: 0 0 15px rgba(0,0,0,0.3); /* Adjust shadow for black text */
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 15px;
        }
        h2 i {
            color: var(--border-color-form);
            font-size: 2.5rem;
        }
        form { 
            background: var(--form-bg); 
            padding: 3rem;
            border-radius: 20px; 
            box-shadow: 0 10px 30px var(--form-shadow);
            transition: all 0.3s ease;
            border: 1px solid var(--border-color-form);
        }
        form:hover {
            box-shadow: 0 15px 40px var(--form-shadow);
            transform: translateY(-3px);
        }
        label { 
            display: block; 
            margin-top: 1.8rem;
            font-weight: bold; 
            color: var(--text-color);
            font-size: 1.2rem;
            display: flex;
            align-items: center;
            gap: 10px;
        }
        input, select, textarea { 
            width: calc(100% - 24px);
            padding: 1rem 12px;
            margin-top: 0.7rem; 
            border-radius: 12px;
            border: 1px solid var(--form-bg); 
            background: var(--input-bg); 
            color: var(--text-color); 
            font-size: 1.1rem;
            box-sizing: border-box; 
            transition: all 0.3s ease;
        }
        input:focus, select:focus, textarea:focus {
            outline: none;
            border-color: var(--border-color-form);
            box-shadow: 0 0 12px var(--input-focus-shadow);
            background: var(--input-focus-bg);
        }
        textarea {
            resize: vertical;
            min-height: 120px;
        }
        button { 
            margin-top: 2.5rem; 
            width: 100%; 
            padding: 1.4rem;
            background: var(--button-bg); 
            color: white; 
            border: none; 
            border-radius: 12px; 
            font-size: 1.5rem;
            cursor: pointer; 
            transition: background 0.3s ease, transform 0.2s ease, box-shadow 0.3s ease;
            font-weight: bold;
            box-shadow: 0 5px 15px var(--button-shadow);
        }
        button:hover {
            background: var(--button-hover-bg);
            transform: translateY(-4px);
            box-shadow: 0 8px 20px var(--button-shadow);
        }
        button:active {
            transform: translateY(0);
            box-shadow: 0 2px 5px var(--button-shadow);
        }
        .result { 
            background: var(--result-bg); 
            margin-top: 3.5rem;
            padding: 2rem;
            border-radius: 18px; 
            box-shadow: 0 8px 25px var(--result-shadow); 
            border-left: 6px solid var(--result-border-left);
            font-size: 1.1rem;
            animation: fadeIn 1s ease-out; /* Fade-in animation */
        }
        .result p {
            margin-bottom: 1.2rem;
            padding-bottom: 1.2rem;
            border-bottom: 1px dashed var(--result-divider);
        }
        .result p:last-child {
            border-bottom: none;
            margin-bottom: 0;
            padding-bottom: 0;
        }
        .result strong {
            color: var(--border-color-form);
            font-size: 1.2rem;
        }
        .error-message { 
            color: var(--error-border); 
            margin-top: 2rem; 
            text-align: center; 
            font-weight: bold;
            background-color: var(--error-bg);
            padding: 1.2rem;
            border-radius: 12px;
            border: 1px solid var(--error-border);
            box-shadow: 0 0 15px var(--error-shadow);
            animation: shake 0.5s ease-in-out;
        }
        .loading-spinner {
            display: none;
            border: 5px solid rgba(255, 255, 255, 0.3);
            border-radius: 50%;
            border-top: 5px solid var(--spinner-border-top);
            width: 40px;
            height: 40px;
            -webkit-animation: spin 1s linear infinite;
            animation: spin 1s linear infinite;
            margin: 30px auto;
        }

        /* Animations */
        @-webkit-keyframes spin { 0% { -webkit-transform: rotate(0deg); } 100% { -webkit-transform: rotate(360deg); } }
        @keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }
        @keyframes fadeIn { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: translateY(0); } }
        @keyframes shake { 0%, 100% { transform: translateX(0); } 10%, 30%, 50%, 70%, 90% { transform: translateX(-5px); } 20%, 40%, 60%, 80% { transform: translateX(5px); } }
        
        .footer {
            text-align: center;
            margin-top: 4rem;
            padding-top: 1.5rem;
            border-top: 1px dashed var(--result-divider);
            color: var(--footer-color);
            font-size: 0.9rem;
        }
        .footer a {
            color: var(--button-bg);
            text-decoration: none;
            font-weight: bold;
            transition: color 0.3s ease;
        }
        .footer a:hover {
            color: var(--button-hover-bg);
            text-decoration: underline;
        }
        .footer strong {
            font-family: 'Amiri Quran', serif;
            font-size: 1.1rem;
            color: var(--heading-color); /* Use heading color for developer name */
        }

        /* Audio player styling */
        .audio-player {
            display: flex;
            align-items: center;
            gap: 10px;
            margin-top: 15px;
            background: rgba(76, 175, 80, 0.1);
            padding: 10px 15px;
            border-radius: 10px;
            border: 1px solid var(--border-color-form);
            color: var(--text-color);
        }
        .audio-player i {
            color: var(--border-color-form);
            font-size: 1.5rem;
        }
        .audio-player audio {
            flex-grow: 1;
            height: 35px;
            background: var(--input-bg);
            border-radius: 8px;
        }
        .audio-player audio::-webkit-media-controls-panel {
            background-color: var(--input-bg);
            border-radius: 8px;
        }
        .audio-player audio::-webkit-media-controls-play-button,
        .audio-player audio::-webkit-media-controls-timeline,
        .audio-player audio::-webkit-media-controls-volume-slider {
            color: var(--border-color-form);
        }

        /* Favorites button */
        .favorite-actions {
            display: flex;
            gap: 10px;
            margin-top: 15px;
            justify-content: flex-end; /* Align to the right */
        }
        .action-button {
            background: #ffc107; /* Gold color */
            color: #333;
            border: none;
            padding: 8px 15px;
            border-radius: 8px;
            cursor: pointer;
            font-size: 0.9rem;
            display: inline-flex;
            align-items: center;
            gap: 5px;
            transition: background 0.3s ease, transform 0.2s ease;
        }
        .action-button:hover {
            background: #e0a800;
            transform: translateY(-2px);
        }
        .action-button i {
            color: #333;
        }
        .action-button.remove {
            background: #dc3545; /* Red for remove */
            color: white;
        }
        .action-button.remove i {
            color: white;
        }
        .action-button.disabled {
            background: #6c757d; /* Grey for disabled */
            cursor: not-allowed;
            opacity: 0.7;
        }
        .action-button.disabled:hover {
            transform: translateY(0);
        }

        /* Copy button specific style */
        .action-button.copy {
            background: var(--copy-button-bg);
            color: white;
        }
        .action-button.copy:hover {
            background: var(--copy-button-hover-bg);
        }
        .action-button.copy i {
            color: white;
        }

        /* Navigation buttons (prev/next ayah) */
        .navigation-buttons {
            display: flex;
            justify-content: space-between;
            margin-top: 20px;
            gap: 10px;
        }
        .nav-button {
            flex: 1; /* Make buttons take equal width */
            background: var(--nav-button-bg);
            color: white;
            border: 1px solid var(--nav-button-border);
            padding: 10px 15px;
            border-radius: 8px;
            cursor: pointer;
            font-size: 1rem;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            gap: 8px;
            transition: background 0.3s ease, transform 0.2s ease;
        }
        .nav-button:hover {
            background: var(--nav-button-hover-bg);
            transform: translateY(-2px);
        }
        .nav-button:disabled {
            background: #444;
            border-color: #555;
            color: #888;
            cursor: not-allowed;
            transform: translateY(0);
        }
        .nav-button.prev { order: 1; } /* Ensure Previous is on the right in RTL */
        .nav-button.next { order: 2; } /* Ensure Next is on the left in RTL */


        /* Favorites list styling */
        .favorites-list {
            background: var(--result-bg); 
            margin-top: 3.5rem;
            padding: 2rem;
            border-radius: 18px; 
            box-shadow: 0 8px 25px var(--result-shadow); 
            border-left: 6px solid var(--result-border-left);
            font-size: 1.1rem;
        }
        .favorites-list h3 {
            color: var(--heading-color);
            margin-bottom: 1.5rem;
            text-align: center;
            font-size: 2rem;
            text-shadow: 0 0 10px rgba(0,0,0,0.3); /* Adjust shadow for black text */
        }
        .favorites-list .favorite-item {
            padding: 1.5rem 0;
            border-bottom: 1px dashed var(--result-divider);
            margin-bottom: 1.5rem;
            display: flex;
            flex-direction: column;
            gap: 10px;
        }
        .favorites-list .favorite-item:last-child {
            border-bottom: none;
            margin-bottom: 0;
        }
        .favorite-item strong {
            color: var(--border-color-form);
            font-size: 1.2rem;
            display: block;
            margin-bottom: 0.5rem;
        }
        .favorite-item .item-details {
            display: flex;
            flex-direction: column;
            gap: 8px;
        }
        .favorite-item .item-actions {
            align-self: flex-end; /* Align button to the right */
        }
        /* Flash messages */
        .flash-message {
            padding: 1rem;
            margin-bottom: 1.5rem;
            border-radius: 10px;
            font-weight: bold;
            text-align: center;
            animation: fadeIn 0.5s ease-out;
            border: 1px solid;
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 10px;
        }
        .flash-message.success {
            background-color: var(--flash-success-bg);
            border-color: var(--flash-success-border);
            color: var(--flash-success-border);
        }
        .flash-message.error {
            background-color: var(--flash-error-bg);
            border-color: var(--flash-error-border);
            color: var(--flash-error-border);
        }
        .flash-message.warning {
            background-color: var(--flash-warning-bg);
            border-color: var(--flash-warning-border);
            color: var(--flash-warning-border);
        }
        .input-group {
            display: none; /* Hidden by default, JavaScript will show them */
        }
        .input-group.active {
            display: block;
        }
        /* Hide Interpreter selection by default for Dream/Favorites */
        #interpreterLabel, #interpreter {
            display: none;
        }

        /* Dark Mode Toggle Switch CSS */
        .switch {
            position: relative;
            display: inline-block;
            width: 60px;
            height: 34px;
            vertical-align: middle;
            margin-left: 15px; /* Adjust as needed for RTL */
        }

        .switch input {
            opacity: 0;
            width: 0;
            height: 0;
        }

        .slider {
            position: absolute;
            cursor: pointer;
            top: 0;
            left: 0;
            right: 0;
            bottom: 0;
            background-color: #ccc;
            -webkit-transition: .4s;
            transition: .4s;
            border-radius: 34px;
        }

        .slider:before {
            position: absolute;
            content: "";
            height: 26px;
            width: 26px;
            left: 4px;
            bottom: 4px;
            background-color: white;
            -webkit-transition: .4s;
            transition: .4s;
            border-radius: 50%;
        }

        input:checked + .slider {
            background-color: var(--button-bg);
        }

        input:focus + .slider {
            box-shadow: 0 0 1px var(--button-bg);
        }

        input:checked + .slider:before {
            -webkit-transform: translateX(26px);
            -ms-transform: translateX(26px);
            transform: translateX(26px);
        }

        /* Rounded sliders */
        .slider.round {
            border-radius: 34px;
        }

        .slider.round:before {
            border-radius: 50%;
        }

        /* Responsive adjustments */
        @media (max-width: 768px) {
            body { padding: 1rem; border-radius: 0; }
            form { padding: 2rem; }
            h2 { font-size: 2.2rem; gap: 10px; }
            h2 i { font-size: 1.8rem; }
            button { font-size: 1.3rem; padding: 1.1rem; }
            .result { padding: 1.5rem; }
            .action-button, .nav-button { font-size: 0.8rem; padding: 6px 10px; }
            .navigation-buttons { flex-direction: column; }
            .nav-button { width: 100%; }
        }
        @media (max-width: 480px) {
            body { padding: 0.8rem; }
            form { padding: 1.5rem; }
            h2 { font-size: 1.8rem; flex-direction: column; }
            h2 i { font-size: 1.5rem; }
            label { font-size: 1rem; }
            input, select, textarea { font-size: 0.9rem; }
            button { font-size: 1.1rem; padding: 1rem; }
            .favorite-actions { flex-direction: column; gap: 5px; }
            .action-button { width: 100%; justify-content: center; }
        }
    </style>
</head>
<body>
    <h2><i class="fas fa-lightbulb"></i> المصباح الوهّاج <i class="fas fa-quran"></i></h2>

    <div style="text-align: center; margin-top: 2rem; margin-bottom: 2rem;">
        <label class="switch">
            <input type="checkbox" id="darkModeToggle">
            <span class="slider round"></span>
        </label>
        <span style="vertical-align: middle; margin-right: 10px; color: var(--text-color);">
            <i class="fas fa-moon"></i> الوضع الليلي
        </span>
    </div>

    {% with messages = get_flashed_messages(with_categories=true) %}
        {% if messages %}
            {% for category, message in messages %}
                <div class="flash-message {{ category }}">
                    {% if category == 'success' %}<i class="fas fa-check-circle"></i>
                    {% elif category == 'error' %}<i class="fas fa-times-circle"></i>
                    {% elif category == 'warning' %}<i class="fas fa-exclamation-triangle"></i>
                    {% else %}<i class="fas fa-info-circle"></i>
                    {% endif %}
                    {{ message }}
                </div>
            {% endfor %}
        {% endif %}
    {% endwith %}

    <form method="post" onsubmit="showSpinner()">
        <label for="lang"><i class="fas fa-language"></i> اختر لغة العرض:</label>
        <select name="lang" id="lang">
            <option value="arabic" {% if lang=='arabic' %}selected{% endif %}>العربية</option>
            <option value="english" {% if lang=='english' %}selected{% endif %}>الإنجليزية</option>
        </select>

        <label for="mode"><i class="fas fa-compass"></i> اختر نوع التفسير:</label>
        <select name="mode" id="mode">
            <option value="quran" {% if mode=='quran' %}selected{% endif %}>📖 تفسير القرآن الكريم</option>
            <option value="search" {% if mode=='search' %}selected{% endif %}>🔍 بحث في القرآن والتفاسير</option>
            <option value="dream" {% if mode=='dream' %}selected{% endif %}>💤 تفسير الأحلام</option>
            <option value="favorites" {% if mode=='favorites' %}selected{% endif %}>⭐ المفضلة</option>
        </select>
        
        <label for="interpreter" id="interpreterLabel"><i class="fas fa-user-tie"></i> اختر المفسّر / مصدر الذكاء الاصطناعي:</label>
        <select name="interpreter" id="interpreter">
            <option value="gemini_general" {% if interpreter=='gemini_general' %}selected{% endif %}>الذكاء الاصطناعي العام (Gemini)</option>
            <option value="maissar" {% if interpreter=='maissar' %}selected{% endif %}>التفسير الميسر (المتوفر محلياً)</option>
            <option value="ibn_kathir" {% if interpreter=='ibn_kathir' %}selected{% endif %}>ابن كثير (بالذكاء الاصطناعي)</option>
            <option value="qurtubi" {% if interpreter=='qurtubi' %}selected{% endif %}>القرطبي (بالذكاء الاصطناعي)</option>
            <option value="saadi" {% if interpreter=='saadi' %}selected{% endif %}>السعدي (بالذكاء الاصطناعي)</option>
            <option value="all" {% if interpreter=='all' %}selected{% endif %}>مقارنة بين المفسرين (بالذكاء الاصطناعي)</option>
        </select>

        <div id="quran-inputs" class="input-group">
            <label for="surah"><i class="fas fa-book"></i> رقم / اسم السورة:</label>
            <input type="text" name="surah" id="surah" value="{{ surah or '' }}" placeholder="مثال: الفاتحة أو 1">
            <label for="ayah_input"><i class="fas fa-highlighter"></i> رقم الآية أو جزء من نصها:</label>
            <input type="text" name="ayah_input" id="ayah_input" value="{{ ayah_input or '' }}" placeholder="مثال: 1 أو 'بسم الله الرحمن الرحيم'">
        </div>

        <div id="dream-inputs" class="input-group">
            <label for="dream_text"><i class="fas fa-moon"></i> وصف حلمك بالتفصيل:</label>
            <textarea name="dream_text" id="dream_text" placeholder="اكتب حلمك هنا...">{{ dream_text or '' }}</textarea>
            <label for="gender"><i class="fas fa-user"></i> الجنس:</label>
            <select name="gender" id="gender">
                <option value="ذكر" {% if gender=='ذكر' %}selected{% endif %}>ذكر</option>
                <option value="أنثى" {% if gender=='أنثى' %}selected{% endif %}>أنثى</option>
            </select>
        </div>

        <div id="search-inputs" class="input-group">
            <label for="search_query"><i class="fas fa-search"></i> اكتب كلمة مفتاحية أو موضوع للبحث:</label>
            <input type="text" name="search_query" id="search_query" value="{{ search_query or '' }}" placeholder="مثال: الصلاة أو الربا">
        </div>

        <button type="submit">
            <i class="fas fa-arrow-circle-right"></i> عرض النتائج
        </button>
    </form>

    <div class="loading-spinner" id="loadingSpinner"></div>

    {% if error_message %}<p class="error-message"><i class="fas fa-exclamation-triangle"></i> {{ error_message }}</p>{% endif %}

    {% if tafsir %}
        <div class="result">
            <p>
                <strong><i class="fas fa-quran"></i> السورة:</strong> {{ surah_name or '' }} ({{ surah_number or '' }}) - الآية: {{ ayah_number_found or '' }}
                <button class="action-button copy" onclick="copyToClipboard('ayah_info_{{ surah_number }}_{{ ayah_number_found }}')"><i class="far fa-copy"></i></button>
                <span id="ayah_info_{{ surah_number }}_{{ ayah_number_found }}" style="display:none;">
                    السورة: {{ surah_name or '' }} ({{ surah_number or '' }}) - الآية: {{ ayah_number_found or '' }}
                </span>
            </p>
            {% if ayah_text %}
                <p class="quran-text" style="font-family: 'Amiri Quran', serif; font-size: 1.5rem; text-align: center; color: #0f3460; margin: 1rem 0; border-bottom: 1px dashed var(--result-divider); padding-bottom: 1rem;">
                    <i class="fas fa-book-open"></i> <span id="ayah_text_content">{{ ayah_text }}</span> 
                    <button class="action-button copy" onclick="copyToClipboard('ayah_text_content')"><i class="far fa-copy"></i></button>
                </p>
            {% endif %}
            <p><strong><i class="fas fa-arabic-language"></i> [AR]</strong> <span id="tafsir_content">{{ tafsir }}</span> 
                <button class="action-button copy" onclick="copyToClipboard('tafsir_content')"><i class="far fa-copy"></i></button>
            </p>
            {% if audio_base64 %}<div class="audio-player"><i class="fas fa-volume-up"></i> <audio controls><source src="data:audio/mpeg;base64,{{ audio_base64 }}" type="audio/mpeg"></audio></div>{% endif %}
            {% if translated %}<p><strong><i class="fas fa-globe"></i> [EN]</strong> <span id="translated_content">{{ translated }}</span> 
                <button class="action-button copy" onclick="copyToClipboard('translated_content')"><i class="far fa-copy"></i></button>
            </p>{% endif %}
            
            <div class="favorite-actions">
                {% if tafsir_hash and not is_favorited %}
                    <form action="/add_favorite" method="post" style="display:inline;">
                        <input type="hidden" name="surah_name" value="{{ surah_name or '' }}">
                        <input type="hidden" name="surah_number" value="{{ surah_number or '' }}">
                        <input type="hidden" name="ayah_number" value="{{ ayah_number_found or '' }}">
                        <input type="hidden" name="ayah_text" value="{{ ayah_text or '' }}"> 
                        <input type="hidden" name="tafsir" value="{{ tafsir }}">
                        <input type="hidden" name="lang" value="{{ lang }}">
                        <input type="hidden" name="interpreter" value="{{ interpreter }}">
                        <input type="hidden" name="translated" value="{{ translated or '' }}">
                        <input type="hidden" name="ayah_hash" value="{{ tafsir_hash }}">
                        <button type="submit" class="action-button"><i class="fas fa-star"></i> أضف للمفضلة</button>
                    </form>
                {% elif is_favorited %}
                    <button class="action-button disabled"><i class="fas fa-check"></i> في المفضلة</button>
                {% endif %}
            </div>

            {% if surah_number and ayah_number_found %} {# No total_ayahs needed for navigation logic #}
            <div class="navigation-buttons">
                <form action="/" method="post" style="display:inline;">
                    <input type="hidden" name="mode" value="quran">
                    <input type="hidden" name="lang" value="{{ lang }}">
                    <input type="hidden" name="interpreter" value="{{ interpreter }}">
                    <input type="hidden" name="surah" value="{{ surah_number }}">
                    <input type="hidden" name="ayah_input" value="{{ ayah_number_found | int - 1 }}">
                    <button type="submit" class="nav-button prev" {% if ayah_number_found|int == 1 %}disabled{% endif %}>
                        <i class="fas fa-chevron-right"></i> الآية السابقة
                    </button>
                </form>
                <form action="/" method="post" style="display:inline;">
                    <input type="hidden" name="mode" value="quran">
                    <input type="hidden" name="lang" value="{{ lang }}">
                    <input type="hidden" name="interpreter" value="{{ interpreter }}">
                    <input type="hidden" name="surah" value="{{ surah_number }}">
                    <input type="hidden" name="ayah_input" value="{{ ayah_number_found | int + 1 }}">
                    <button type="submit" class="nav-button next"> {# Removed disabled check as AI can generate for any next #}
                        الآية التالية <i class="fas fa-chevron-left"></i>
                    </button>
                </form>
            </div>
            {% endif %}
        </div>
    {% elif search_results %}
        <div class="result">
            <h3><i class="fas fa-search-dollar"></i> نتائج البحث عن "{{ search_query }}"</h3>
            {% for item in search_results %}
                <p>
                    <strong><i class="fas fa-quran"></i> السورة: {{ item.surah_name }} ({{ item.surah_number }}) - الآية: {{ item.ayah_number }}</strong>
                    <button class="action-button copy" onclick="copyToClipboard('search_ayah_info_{{ loop.index }}')"><i class="far fa-copy"></i></button>
                    <span id="search_ayah_info_{{ loop.index }}" style="display:none;">
                        السورة: {{ item.surah_name }} ({{ item.surah_number }}) - الآية: {{ item.ayah_number }}
                    </span>
                    <br>
                    {% if item.ayah_text %}
                        <span class="quran-text" style="font-family: 'Amiri Quran', serif; font-size: 1.3rem; color: #0f3460;">
                            <span id="search_ayah_text_content_{{ loop.index }}">{{ item.ayah_text }}</span> 
                            <button class="action-button copy" onclick="copyToClipboard('search_ayah_text_content_{{ loop.index }}')"><i class="far fa-copy"></i></button>
                        </span><br>
                    {% endif %}
                    <i class="fas fa-arabic-language"></i> <span id="search_tafsir_content_{{ loop.index }}">{{ item.tafsir }}</span>
                    <button class="action-button copy" onclick="copyToClipboard('search_tafsir_content_{{ loop.index }}')"><i class="far fa-copy"></i></button>
                    {% if item.audio_base64 %}<div class="audio-player"><i class="fas fa-volume-up"></i> <audio controls><source src="data:audio/mpeg;base64,{{ item.audio_base64 }}" type="audio/mpeg"></audio></div>{% endif %}
                    <div class="favorite-actions">
                        <form action="/add_favorite" method="post" style="display:inline;">
                            <input type="hidden" name="surah_name" value="{{ item.surah_name }}">
                            <input type="hidden" name="surah_number" value="{{ item.surah_number }}">
                            <input type="hidden" name="ayah_number" value="{{ item.ayah_number }}">
                            <input type="hidden" name="ayah_text" value="{{ item.ayah_text or '' }}"> 
                            <input type="hidden" name="tafsir" value="{{ item.tafsir }}">
                            <input type="hidden" name="lang" value="{{ lang }}"> 
                            <input type="hidden" name="interpreter" value="بحث"> 
                            <input type="hidden" name="translated" value=""> 
                            <input type="hidden" name="ayah_hash" value="{{ item.hash }}">
                            {% if item.is_favorited %}
                                <button class="action-button disabled"><i class="fas fa-check"></i> في المفضلة</button>
                            {% else %}
                                <button type="submit" class="action-button"><i class="fas fa-star"></i> أضف للمفضلة</button>
                            {% endif %}
                        </form>
                    </div>
                </p>
            {% else %}
                <p>عذرًا، لم يتم العثور على نتائج مطابقة لبحثك في التفاسير المحلية. يمكنك محاولة استخدام تفسير القرآن الكريم المباشر أو البحث بكلمة أخرى.</p>
            {% endfor %}
        </div>
    {% endif %}

    {% if favorites_list and mode == 'favorites' %}
        <div class="favorites-list">
            <h3><i class="fas fa-star"></i> المفضلة</h3>
            {% if favorites_list %}
                {% for fav in favorites_list %}
                    <div class="favorite-item">
                        <div class="item-details">
                            <strong>السورة: {{ fav.surah_name }} ({{ fav.surah_number }}) - الآية: {{ fav.ayah_number }}</strong>
                            <button class="action-button copy" onclick="copyToClipboard('fav_ayah_info_{{ loop.index }}')"><i class="far fa-copy"></i></button>
                            <span id="fav_ayah_info_{{ loop.index }}" style="display:none;">
                                السورة: {{ fav.surah_name }} ({{ fav.surah_number }}) - الآية: {{ fav.ayah_number }}
                            </span>
                            {% if fav.ayah_text %}
                                <span class="quran-text" style="font-family: 'Amiri Quran', serif; font-size: 1.3rem; color: #0f3460;">
                                    <span id="fav_ayah_text_content_{{ loop.index }}">{{ fav.ayah_text }}</span> 
                                    <button class="action-button copy" onclick="copyToClipboard('fav_ayah_text_content_{{ loop.index }}')"><i class="far fa-copy"></i></button>
                                </span>
                            {% endif %}
                            <p><span id="fav_tafsir_content_{{ loop.index }}">{{ fav.tafsir }}</span>
                                <button class="action-button copy" onclick="copyToClipboard('fav_tafsir_content_{{ loop.index }}')"><i class="far fa-copy"></i></button>
                            </p>
                            {% if fav.audio_base64 %}<div class="audio-player"><i class="fas fa-volume-up"></i> <audio controls><source src="data:audio/mpeg;base64,{{ fav.audio_base64 }}" type="audio/mpeg"></audio></div>{% endif %}
                            {% if fav.translated %}<p><strong>[EN]</strong> <span id="fav_translated_content_{{ loop.index }}">{{ fav.translated }}</span>
                                <button class="action-button copy" onclick="copyToClipboard('fav_translated_content_{{ loop.index }}')"><i class="far fa-copy"></i></button>
                            </p>{% endif %}
                        </div>
                        <div class="item-actions">
                            <form action="/remove_favorite/{{ fav.hash }}" method="post" style="display:inline;">
                                <button type="submit" class="action-button remove"><i class="fas fa-trash"></i> حذف من المفضلة</button>
                            </form>
                        </div>
                    </div>
                {% endfor %}
            {% else %}
                <p>لا توجد لديك أي مفضلة حتى الآن.</p>
            {% endif %}
        </div>
    {% endif %}

    <div class="footer">
        <p>&copy; {{ year }} المصباح الوهّاج. جميع الحقوق محفوظة.</p>
        <p><a href="{{ url_for('show_favorites') }}"><i class="fas fa-heart"></i> عرض المفضلة</a> | <a href="https://github.com/Dubaie" target="_blank"><i class="fab fa-github"></i> GitHub</a></p>
        <p>تم التطوير بواسطة: <strong style="color: var(--heading-color);">دبي محمد عبد الرزاق</strong></p>
    </div>

    <script>
        function showSpinner() {
            document.getElementById('loadingSpinner').style.display = 'block';
        }

        function copyToClipboard(elementId) {
            var text = document.getElementById(elementId).textContent;
            navigator.clipboard.writeText(text).then(function() {
                flashMessage('تم النسخ إلى الحافظة بنجاح!', 'success');
            }, function(err) {
                flashMessage('فشل النسخ إلى الحافظة.', 'error');
                console.error('Could not copy text: ', err);
            });
        }

        function flashMessage(message, category = 'info') {
            const flashDiv = document.createElement('div');
            flashDiv.className = `flash-message ${category}`;
            flashDiv.innerHTML = (category === 'success' ? '<i class="fas fa-check-circle"></i> ' : 
                                 (category === 'error' ? '<i class="fas fa-times-circle"></i> ' : 
                                 (category === 'warning' ? '<i class="fas fa-exclamation-triangle"></i> ' : '<i class="fas fa-info-circle"></i> '))) + message;
            document.body.insertBefore(flashDiv, document.querySelector('form'));
            setTimeout(() => {
                flashDiv.remove();
            }, 3000);
        }

        document.addEventListener('DOMContentLoaded', (event) => {
            const modeSelect = document.getElementById('mode');
            const quranInputs = document.getElementById('quran-inputs');
            const dreamInputs = document.getElementById('dream-inputs');
            const searchInputs = document.getElementById('search-inputs');
            const interpreterLabel = document.getElementById('interpreterLabel');
            const interpreterSelect = document.getElementById('interpreter');

            function updateInputVisibility() {
                const selectedMode = modeSelect.value;
                
                // Hide all input groups first
                quranInputs.classList.remove('active');
                dreamInputs.classList.remove('active');
                searchInputs.classList.remove('active');
                interpreterLabel.style.display = 'none';
                interpreterSelect.style.display = 'none';

                // Show inputs based on selected mode
                if (selectedMode === 'quran') {
                    quranInputs.classList.add('active');
                    interpreterLabel.style.display = 'block';
                    interpreterSelect.style.display = 'block';
                } else if (selectedMode === 'dream') {
                    dreamInputs.classList.add('active');
                } else if (selectedMode === 'search') {
                    searchInputs.classList.add('active');
                    interpreterLabel.style.display = 'block'; // Search can also use AI or local.
                    interpreterSelect.style.display = 'block';
                } else if (selectedMode === 'favorites') {
                    // Favorites mode doesn't need specific inputs or interpreter selection on the form itself.
                    // It's handled by redirecting to a dedicated favorites view.
                }
            }

            modeSelect.addEventListener('change', updateInputVisibility);
            updateInputVisibility(); // Call on page load to set initial state based on current selection

            const darkModeToggle = document.getElementById('darkModeToggle');
            const body = document.body;

            // Load dark mode preference
            const isDarkMode = localStorage.getItem('darkMode') === 'enabled';
            if (isDarkMode) {
                body.classList.add('dark-mode');
                darkModeToggle.checked = true;
            }

            darkModeToggle.addEventListener('change', () => {
                body.classList.toggle('dark-mode');
                localStorage.setItem('darkMode', body.classList.contains('dark-mode') ? 'enabled' : 'disabled');
            });
        });
    </script>
</body>
</html>
'''

# Map interpreter names for AI prompts
interpreter_names = {
    'gemini_general': 'الذكاء الاصطناعي العام (Gemini)',
    'ibn_kathir': 'تفسير ابن كثير',
    'qurtubi': 'تفسير القرطبي',
    'saadi': 'تفسير السعدي',
    'all': 'مقارنة بين تفسير ابن كثير، القرطبي، والسعدي'
}

# --- 6. مسارات التطبيق (Application Routes) ---

@app.route('/', methods=['GET', 'POST'])
def home():
    quran_data = load_quran_text() # تحميل بيانات القرآن (قد تكون فارغة إذا لم يكن الملف موجوداً)
    surahs_data = _surahs_data_cache # استخدام البيانات المحملة مرة واحدة عند بدء التطبيق
    
    surah_name = None
    surah_number = None
    ayah_number_found = None
    ayah_text = None
    tafsir = None
    translated = None
    audio_base64 = None
    error_message = None
    search_results = None
    favorites_list = None
    is_favorited = False
    tafsir_hash = None
    total_ayahs = None

    # Default values from form or initial load
    lang = request.form.get('lang', 'arabic')
    mode = request.form.get('mode', 'quran')
    interpreter = request.form.get('interpreter', 'gemini_general') # Default to general AI
    dream_text_input = request.form.get('dream_text', '')
    gender_input = request.form.get('gender', 'ذكر')
    search_query_input = request.form.get('search_query', '')

    if request.method == 'POST':
        # Handling different modes
        if mode == 'quran':
            surah_input = request.form.get('surah')
            ayah_input = request.form.get('ayah_input')

            surah_number = get_surah_number(surah_input) # لا تمرر surahs_data هنا
            
            if not surah_number:
                error_message = "يرجى إدخال رقم سورة صحيح (1-114) أو اسمها."
            elif not ayah_input:
                error_message = "يرجى إدخال رقم الآية أو جزء من نصها."
            else:
                surah_name = surahs_data.get(surah_number, {}).get("arabic", f"السورة {surah_number}")
                total_ayahs = get_total_ayahs_in_surah(surah_number, quran_data) 
                
                # Try to get ayah text from local quran.json first
                if str(ayah_input).isdigit():
                    ayah_number_found = int(ayah_input)
                    ayah_text = get_ayah_text_from_quran_json(surah_number, ayah_number_found, quran_data)

                # If ayah text not found locally, use AI
                if not ayah_text:
                    ayah_prompt = f"جلب نص الآية رقم {ayah_input} من سورة {surah_name} (السورة رقم {surah_number}). أعد النص القرآني فقط بدون أي تفسير أو معلومات إضافية."
                    if not str(ayah_input).isdigit(): # If ayah_input is text, try to find it first
                        ayah_prompt = f"جلب نص الآية التي تحتوي على '{ayah_input}' من سورة {surah_name} (السورة رقم {surah_number}). أعد النص القرآني فقط بدون أي تفسير أو معلومات إضافية."

                    logger.info(f"جاري جلب نص الآية من Gemini: {ayah_prompt}")
                    ai_ayah_response = ask_gemini(ayah_prompt)
                    if ai_ayah_response:
                        # Attempt to extract just the ayah text
                        # Remove anything that's not Arabic letters, numbers, spaces, or specific Quranic punctuation (optional for robust parsing)
                        ayah_text = re.sub(r'[^\u0600-\u06FF\s\d\u06D6-\u06ED]+', '', ai_ayah_response).strip()
                        if not ayah_text: # Fallback if cleanup removes everything
                            ayah_text = ai_ayah_response.strip()
                        logger.info(f"نص الآية من AI: {ayah_text[:50]}...")
                        
                        # Try to deduce ayah number from AI response or just use the input if it was a number
                        if str(ayah_input).isdigit():
                            ayah_number_found = int(ayah_input)
                        else: # If ayah_input was text, we might not have the exact number
                            ayah_number_found = "غير محدد (تم البحث بالنص)" 
                    else:
                        error_message = "لم يتمكن الذكاء الاصطناعي من جلب نص الآية. يرجى التأكد من توفر ملفات القرآن محلياً أو مراجعة المدخلات."

                # Get Tafsir
                if ayah_text: # Proceed only if ayah_text is available
                    if interpreter == 'maissar':
                        tafsir_data_maissar = get_tafsir_data_local(surah_number)
                        if str(ayah_input).isdigit() and str(int(ayah_input)) in tafsir_data_maissar:
                            tafsir = tafsir_data_maissar[str(int(ayah_input))]
                            ayah_number_found = int(ayah_input) # Confirm ayah number if found locally
                        
                        if not tafsir: # If still no tafsir from local, use AI for Maissar-like tafsir
                            flash(f"لم يتم العثور على تفسير ميسر محلي للآية {ayah_input} من سورة {surah_name}. سيتم جلب تفسير من الذكاء الاصطناعي.", "warning")
                            prompt = f"قدم تفسيراً موجزاً وواضحاً (على طريقة التفسير الميسر) للآية القرآنية: {ayah_text} من سورة {surah_name} (السورة رقم {surah_number})."
                            tafsir = ask_gemini(prompt)

                    elif interpreter == 'gemini_general':
                        prompt = f"تفسير عام وشامل للآية القرآنية: {ayah_text} من سورة {surah_name} (السورة رقم {surah_number}). قدم تفسيراً واضحاً ومفيداً."
                        tafsir = ask_gemini(prompt)
                        
                    elif interpreter in ['ibn_kathir', 'qurtubi', 'saadi', 'all']:
                        interpreter_name_arabic = interpreter_names.get(interpreter, "المفسرين")
                        prompt = f"تفسير الآية القرآنية: {ayah_text} من سورة {surah_name} (السورة رقم {surah_number})، بناءً على {interpreter_name_arabic}. إذا كان الخيار 'all'، قم بمقارنة موجزة بين المفسرين الثلاثة (ابن كثير، القرطبي، السعدي) للآية المذكورة."
                        tafsir = ask_gemini(prompt)
                    
                    if tafsir:
                        tafsir_hash = hashlib.md5(f"{surah_number}-{ayah_number_found}-{tafsir}".encode()).hexdigest()
                        favorites = load_favorites()
                        is_favorited = any(fav['hash'] == tafsir_hash for fav in favorites)

                        if lang == 'english':
                            translated = translate_text(tafsir, to_lang='en')
                        audio_base64 = text_to_speech_base64(tafsir, lang='ar')
                    else:
                        error_message = "عذرًا، لم يتمكن التطبيق من جلب التفسير من المصدر المحدد."
                else:
                    error_message = "لم يتمكن التطبيق من الحصول على نص الآية لتفسيرها."

        elif mode == 'dream':
            if dream_text_input:
                prompt = f"أنا {gender_input}. حلمت بما يلي: {dream_text_input}. يرجى تفسير هذا الحلم بناءً على مبادئ تفسير الأحلام في الإسلام، مع ذكر أي رموز أو دلالات واضحة. ابدأ التفسير مباشرة."
                tafsir = ask_gemini(prompt)
                if tafsir:
                    if lang == 'english':
                        translated = translate_text(tafsir, to_lang='en')
                    audio_base64 = text_to_speech_base64(tafsir, lang='ar')
                else:
                    error_message = "عذرًا، لم يتمكن الذكاء الاصطناعي من تفسير الحلم."
            else:
                error_message = "يرجى وصف حلمك لتفسيره."

        elif mode == 'search':
            if search_query_input:
                # First, search locally in Maissar tafsir
                local_results = search_in_tafasir_local(search_query_input, surahs_data, quran_data)
                
                if local_results:
                    search_results = local_results
                    for item in search_results:
                        item['audio_base64'] = text_to_speech_base64(item['tafsir'], lang='ar')
                        favorites = load_favorites()
                        item['is_favorited'] = any(fav['hash'] == item['hash'] for fav in favorites)
                    flash(f"تم العثور على {len(search_results)} نتيجة في التفاسير المحلية.", "success")
                else:
                    # If no local results, try AI for a general Islamic answer related to query
                    flash("لم يتم العثور على نتائج في التفاسير المحلية. سيتم البحث عن إجابة عامة باستخدام الذكاء الاصطناعي.", "warning")
                    prompt = f"قدم معلومات إسلامية شاملة حول موضوع أو كلمة مفتاحية: '{search_query_input}'. يمكنك الاستشهاد بآيات قرآنية أو أحاديث نبوية ذات صلة وتفسيرها بإيجاز."
                    ai_response = ask_gemini(prompt)
                    if ai_response:
                        # Format AI response as a single search result
                        search_results = [{
                            "surah_name": "الذكاء الاصطناعي",
                            "surah_number": "N/A",
                            "ayah_number": "N/A",
                            "ayah_text": f"بحث عن: {search_query_input}",
                            "tafsir": ai_response,
                            "audio_base64": text_to_speech_base64(ai_response, lang='ar'),
                            "is_favorited": False, # AI generated content isn't added to favorites directly
                            "hash": hashlib.md5(ai_response.encode()).hexdigest() # Generate hash for AI result as well
                        }]
                    else:
                        error_message = "عذرًا، لم يتم العثور على نتائج للبحث."
            else:
                error_message = "يرجى إدخال كلمة مفتاحية للبحث."
        
        elif mode == 'favorites':
            favorites_list = load_favorites()
            for fav in favorites_list:
                fav['audio_base64'] = text_to_speech_base64(fav['tafsir'], lang='ar')
                if fav.get('lang') == 'english' and not fav.get('translated'): # Translate if missing for english fav
                    fav['translated'] = translate_text(fav['tafsir'], to_lang='en')
            # No need for error_message here, just show empty list if no favorites

    elif request.method == 'GET' and mode == 'favorites': # Handle direct GET request to show favorites
        favorites_list = load_favorites()
        for fav in favorites_list:
            fav['audio_base64'] = text_to_speech_base64(fav['tafsir'], lang='ar')
            if fav.get('lang') == 'english' and not fav.get('translated'): # Translate if missing for english fav
                fav['translated'] = translate_text(fav['tafsir'], to_lang='en')

    return render_template_string(
        HTML_TEMPLATE,
        surah_name=surah_name,
        surah_number=surah_number,
        ayah_input=request.form.get('ayah_input', ''), # Pass back current ayah input for form
        ayah_number_found=ayah_number_found,
        ayah_text=ayah_text,
        tafsir=tafsir,
        translated=translated,
        audio_base64=audio_base64,
        error_message=error_message,
        lang=lang,
        mode=mode,
        interpreter=interpreter,
        dream_text=dream_text_input,
        gender=gender_input,
        search_query=search_query_input,
        search_results=search_results,
        favorites_list=favorites_list,
        is_favorited=is_favorited,
        tafsir_hash=tafsir_hash,
        total_ayahs=total_ayahs,
        year=datetime.now().year
    )

# --- مسارات إضافية (Additional Routes) ---

@app.route('/add_favorite', methods=['POST'])
def add_favorite():
    data = request.form
    surah_number = data.get('surah_number')
    ayah_number = data.get('ayah_number')
    tafsir = data.get('tafsir')
    ayah_text = data.get('ayah_text')
    surah_name = data.get('surah_name')
    lang = data.get('lang')
    interpreter = data.get('interpreter')
    translated = data.get('translated')
    ayah_hash = data.get('ayah_hash') # Use the hash passed from the form

    if not ayah_hash: # Fallback if hash not passed for some reason (old data)
        ayah_hash = hashlib.md5(f"{surah_number}-{ayah_number}-{tafsir}".encode()).hexdigest()

    favorites = load_favorites()
    
    # Check if already exists using the hash
    if any(fav['hash'] == ayah_hash for fav in favorites):
        flash("هذه الآية والتفسير موجودة بالفعل في المفضلة!", "warning")
    else:
        favorites.append({
            "surah_name": surah_name,
            "surah_number": surah_number,
            "ayah_number": ayah_number,
            "ayah_text": ayah_text,
            "tafsir": tafsir,
            "lang": lang,
            "interpreter": interpreter,
            "translated": translated,
            "timestamp": datetime.now().isoformat(),
            "hash": ayah_hash
        })
        save_favorites(favorites)
        flash("تمت إضافة التفسير إلى المفضلة بنجاح!", "success")
    
    # Redirect back to home, possibly with query parameters to retain context
    return redirect(url_for('home', 
                            mode='quran', # Or the mode it came from
                            surah=surah_number, 
                            ayah_input=ayah_number, 
                            lang=lang,
                            interpreter=interpreter
                           ))


@app.route('/remove_favorite/<fav_hash>', methods=['POST'])
def remove_favorite(fav_hash):
    favorites = load_favorites()
    new_favorites = [fav for fav in favorites if fav.get('hash') != fav_hash]
    
    if len(new_favorites) < len(favorites):
        save_favorites(new_favorites)
        flash("تم حذف التفسير من المفضلة بنجاح!", "success")
    else:
        flash("التفسير غير موجود في المفضلة.", "error")
    
    return redirect(url_for('show_favorites'))

@app.route('/favorites')
def show_favorites():
    # This route explicitly shows the favorites list
    # It calls the home function with mode set to 'favorites'
    # This is a cleaner way to handle displaying a specific section
    return home(mode='favorites') # Pass mode to home function

# --- مثال لصفحة جديدة خاصة بك (Example for your custom page) ---
@app.route('/my_custom_page')
def my_custom_page():
    # يمكنك وضع أي محتوى HTML هنا لصفحتك الجديدة
    custom_html = """
    <!DOCTYPE html>
    <html lang="ar" dir="rtl">
    <head>
        <meta charset="UTF-8">
        <title>صفحتي المخصصة</title>
        <style>
            body { font-family: 'Cairo', sans-serif; text-align: center; margin-top: 50px; background-color: #f0f8ff; color: #333; }
            h1 { color: #0056b3; }
            p { font-size: 1.2em; }
            a { color: #007bff; text-decoration: none; }
            a:hover { text-decoration: underline; }
        </style>
    </head>
    <body>
        <h1>أهلاً بك في صفحتي المخصصة!</h1>
        <p>هذه صفحة يمكنك تعديلها وإضافة محتواك الخاص.</p>
        <p>مثال على كيفية إضافة مسار جديد في تطبيق Flask.</p>
        <p><a href="/">العودة إلى الصفحة الرئيسية للمصباح الوهّاج</a></p>
    </body>
    </html>
    """
    return render_template_string(custom_html)

# --- تشغيل التطبيق (Running the App) ---
if __name__ == '__main__':
    # تأكد من وجود مجلدات البيانات
    os.makedirs(Config.TAFSIR_DIR, exist_ok=True)
    os.makedirs(Config.DATA_DIR, exist_ok=True)
    
    # رسالة للمطور للتأكد من وجود الملفات (أصبحت تحذيرية لا تمنع التشغيل)
    if not os.path.exists(Config.QURAN_FILE):
        logger.error(f"ملف القرآن غير موجود في المسار: {Config.QURAN_FILE}. سيتم جلب الآيات من الذكاء الاصطناعي.")
        print("\n!!! تحذير: ملف quran.json غير موجود. قد يؤثر على الأداء واستقرار جلب الآيات. !!!\n")
    if not os.path.exists(Config.SURAHS_FILE):
        logger.error(f"ملف السور غير موجود في المسار: {Config.SURAHS_FILE}. سيتم استخدام أسماء افتراضية.")
        print("\n!!! تحذير: ملف surahs.json غير موجود. سيتم استخدام أسماء سور افتراضية. !!!\n")
    
import os

if __name__ == "__main__":
    port = int(os.environ.get("PORT", 8080))  # يستخدم 8080 إذا لم يكن PORT موجودًا
    print("\n!!! تحذير: ملف surahs.json غير موجود. سيتم استخدام أسماء سور افتراضية. !!!\n")
    app.run(debug=True, host='0.0.0.0', port=port)
