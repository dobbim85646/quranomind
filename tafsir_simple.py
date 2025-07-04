def get_tafsir(lang, surah, ayah):
    if lang == "ar":
        # هنا تجيب التفسير بالعربي من ملفات JSON أو قاعدة بيانات
        return f"تفسير الآية {ayah} من السورة {surah} بالعربية."
    elif lang == "en":
        # هنا تجيب التفسير بالإنجليزي (لو موجود)
        return f"Tafsir of Ayah {ayah} from Surah {surah} in English."
    else:
        return "اللغة غير مدعومة."

def main():
    lang = input("اختر اللغة (ar للغة العربية / en للغة الإنجليزية): ").strip().lower()
    surah = input("أدخل رقم السورة: ").strip()
    ayah = input("أدخل رقم الآية: ").strip()
    tafsir = get_tafsir(lang, surah, ayah)
    print(tafsir)

if __name__ == "__main__":
    main()
