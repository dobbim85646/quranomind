import json

# تحميل أسماء السور
with open('QuranoMind/data/surahs.json', 'r', encoding='utf-8') as f:
    surah_names = json.load(f)

# استدعاء السورة
surah_num = int(input("🔢 أدخل رقم السورة (1-114): "))
surah_name = surah_names.get(str(surah_num), "❓ غير معروفة")

print(f"📖 [{surah_num}] {surah_name}")
