import json
import os
from googletrans import Translator

def load_tafsir(surah, ayah):
    possible_paths = [
        f"tafasir_json/{surah}.json",
        f"./tafasir_json/{surah}.json",
        f"/data/data/com.termux/files/home/QuranoMind/tafasir_json/{surah}.json"
    ]
    for path in possible_paths:
        if os.path.exists(path):
            try:
                with open(path, "r", encoding="utf-8") as f:
                    return json.load(f)
            except Exception as e:
                print(f"⚠️ خطأ أثناء قراءة التفسير: {e}")
                return None
    print("❌ ملف التفسير غير موجود.")
    return None

def translate_to_english(text):
    try:
        translator = Translator()
        result = translator.translate(text, src="ar", dest="en")
        return result.text
    except Exception as e:
        return f"⚠️ خطأ في الترجمة: {e}"

def main():
    lang = input("🌐 اختر اللغة (ar للغة العربية / en للغة الإنجليزية): ").strip().lower()
    surah = input("📖 أدخل رقم السورة: ").strip()
    ayah = input("🔢 أدخل رقم الآية: ").strip()

    tafsir_data = load_tafsir(surah, ayah)
    if not tafsir_data:
        return

    tafsir = tafsir_data.get(str(ayah), "❌ لا يوجد تفسير لهذه الآية.")

    if lang == "en":
        translated = translate_to_english(tafsir)
        print("\n📘 Tafsir:")
        print(f"[AR] {tafsir}")
        print(f"[EN] {translated}")
    else:
        print("\n📘 التفسير:")
        print(f"[AR] {tafsir}")

if __name__ == "__main__":
    main()
