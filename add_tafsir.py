import os
import json

# مجلد التفاسير
TAFSIR_DIR = os.path.expanduser("~/QuranoMind/tafasir_json")

# التأكد من وجود المجلد
os.makedirs(TAFSIR_DIR, exist_ok=True)

def get_next_surah_number():
    existing_files = [
        int(f.replace('.json', ''))
        for f in os.listdir(TAFSIR_DIR)
        if f.endswith('.json') and f.replace('.json', '').isdigit()
    ]
    return max(existing_files, default=3) + 1  # يبدأ من 4 إذا لا شيء موجود

def save_tafsir_file(surah_number, tafsir_data):
    file_path = os.path.join(TAFSIR_DIR, f"{surah_number}.json")
    with open(file_path, 'w', encoding='utf-8') as f:
        json.dump(tafsir_data, f, ensure_ascii=False, indent=2)
    print(f"✅ تم حفظ التفسير في: {file_path}")

def main():
    surah_number = get_next_surah_number()
    print(f"📥 أدخل تفسير سورة رقم {surah_number} بصيغة JSON (ثم اضغط Ctrl+D للحفظ):")
    try:
        tafsir_raw = ""
        while True:
            line = input()
            tafsir_raw += line
    except EOFError:
        pass

    try:
        tafsir_data = json.loads(tafsir_raw)
        save_tafsir_file(surah_number, tafsir_data)
    except json.JSONDecodeError as e:
        print(f"❌ خطأ في التنسيق: {e}")

if __name__ == "__main__":
    main()
