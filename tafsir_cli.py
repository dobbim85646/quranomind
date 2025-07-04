import json
import os
from pathlib import Path
import pyperclip  # للتعامل مع النسخ
from deep_translator import GoogleTranslator

def load_tafsir(surah_num):
    paths = [
        f"tafasir_json/{surah_num}.json",
        f"QuranoMind/tafasir_json/{surah_num}.json",
        f"./{surah_num}.json"
    ]
    for path in paths:
        if Path(path).exists():
            try:
                with open(path, 'r', encoding='utf-8') as f:
                    return json.load(f)
            except Exception as e:
                print(f"❌ Error reading file: {e}")
                return None
    return None

def translate_text(text, target_lang="en"):
    try:
        return GoogleTranslator(source="auto", target=target_lang).translate(text)
    except Exception as e:
        return f"[⚠️ Translation error] {e}"

def main():
    lang = input("🌐 اختر اللغة (ar للغة العربية / en للغة الإنجليزية): ").strip().lower()
    surah = input("📖 أدخل رقم السورة: ").strip()
    ayah = input("🔢 أدخل رقم الآية: ").strip()

    tafsir_data = load_tafsir(surah)
    if not tafsir_data:
        print("❌ ملف التفسير غير موجود.")
        return

    tafsir_text = tafsir_data.get(ayah)
    if not tafsir_text:
        print("⚠️ لم يتم العثور على التفسير لهذه الآية.")
        return

    print("\n📘 التفسير:")
    print(f"[AR] {tafsir_text}")

    if lang == "en":
        translated = translate_text(tafsir_text, target_lang="en")
        print(f"[EN] {translated}")
        # pyperclip.copy(translated)
        print("📋 تم نسخ التفسير المترجم إلى الحافظة.")
    else:
        pyperclip.copy(tafsir_text)
        print("📋 تم نسخ التفسير بالعربية إلى الحافظة.")

if __name__ == "__main__":
    main()
