import json
import os

# 📥 المسار إلى ملف الإدخال
input_path = input("📄 أدخل مسار ملف JSON المراد تحسينه: ").strip()

# 📤 مسار ملف الإخراج (نفس الاسم + _cleaned)
output_path = input_path.replace('.json', '_cleaned.json')

# ✅ تنظيف وتحسين الملف
try:
    with open(input_path, 'r', encoding='utf-8') as f:
        data = json.load(f)

    with open(output_path, 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, indent=2)

    print(f"\n✅ تم إنشاء نسخة محسنة: {output_path}")

except FileNotFoundError:
    print("❌ الملف غير موجود!")
except json.JSONDecodeError:
    print("❌ الملف ليس بتنسيق JSON صالح!")
except Exception as e:
    print(f"❌ حدث خطأ غير متوقع: {e}")
