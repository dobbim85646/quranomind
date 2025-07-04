import json
import os
import pyperclip
from deep_translator import GoogleTranslator

TAFSIR_DIR = os.path.expanduser("~/QuranoMind/tafasir_json")
SURAHS_FILE = os.path.expanduser("~/QuranoMind/data/surahs.json")

# تحميل أسماء السور
def load_surah_names():
    with open(SURAHS_FILE, 'r', encoding='utf-8') as f:
        return json.load(f)

# تحويل الاسم إلى رقم السورة
def get_surah_number(input_value, surahs):
    input_value = input_value.strip().lower()
    for number, names in surahs.items():
        if input_value == number or input_value == names["arabic"].strip().lower() or input_value == names["english"].strip().lower():
            return int(number)
    return None

# جلب التفسير
def get_tafsir(surah_number, ayah_number):
    file_path = os.path.join(TAFSIR_DIR, f"{surah_number}.json")
    if not os.path.isfile(file_path):
        return None
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            tafsir_data = json.load(f)
        return tafsir_data.get(str(ayah_number), None)
    except Exception as e:
        return f"⚠️ خطأ أثناء قراءة الملف: {e}"

# ترجمة للنص
def translate_text(text, to_lang='en'):
    try:
        return GoogleTranslator(source='auto', target=to_lang).translate(text)
    except Exception as e:
        return f"[خطأ في الترجمة] {str(e)}"

# السكربت الرئيسي
def main():
    surahs = load_surah_names()

    lang = input("🌐 اختر اللغة (Arabic / English): ").strip().lower()
    if lang not in ["arabic", "english"]:
        print("❌ لغة غير مدعومة. استخدم Arabic أو English.")
        return

    surah_input = input("📖 أدخل رقم أو اسم السورة: ")
    ayah_number = input("🔢 أدخل رقم الآية: ")

    surah_number = get_surah_number(surah_input, surahs)
    if surah_number is None:
        print("❌ السورة غير موجودة.")
        return

    tafsir = get_tafsir(surah_number, ayah_number)
    if tafsir is None:
        print("❌ لم يتم العثور على التفسير.")
        return

    print("\n📘 التفسير:")
    print(f"[AR] {tafsir}")
    pyperclip.copy(tafsir)
    print("📋 التفسير العربي تم نسخه إلى الحافظة.")

    if lang == "english":
        translated = translate_text(tafsir)
        print(f"[EN] {translated}")
        pyperclip.copy(translated)
        print("📋 التفسير الإنجليزي تم نسخه إلى الحافظة.")

if __name__ == "__main__":
    main()
