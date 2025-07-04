import json
import re
import os

def load_and_fix_json(filepath):
    try:
        # حاول فتح الملف وقراءته
        with open(filepath, 'r', encoding='utf-8') as f:
            return json.load(f)  # لو نجح، ممتاز
    except json.JSONDecodeError as e:
        print(f"⚠️ خطأ تنسيقي في الملف: {e}")
        print("🔧 محاولة إصلاح الملف تلقائيًا...")

        try:
            # نقرأه كنص خام
            with open(filepath, 'r', encoding='utf-8') as f:
                raw = f.read()

            # نحذف الأحرف غير الصالحة مثل: \x00 - \x1F
            cleaned = re.sub(r'[\x00-\x1F\x7F]', '', raw)

            # محاولة التحويل بعد التنظيف
            data = json.loads(cleaned)

            # نحفظه مكان القديم (أو باسم مؤقت)
            with open(filepath, 'w', encoding='utf-8') as f:
                json.dump(data, f, ensure_ascii=False, indent=2)

            print("✅ تم الإصلاح بنجاح.")
            return data

        except Exception as fix_error:
            print(f"❌ فشل الإصلاح التلقائي: {fix_error}")
            return None

# مثال استخدام:
if __name__ == "__main__":
    path = input("📁 أدخل مسار ملف التفسير (مثال: tafasir_json/2.json): ").strip()
    if os.path.isfile(path):
        result = load_and_fix_json(path)
        if result:
            print("📖 التفسير جاهز للعرض أو الاستخدام.")
    else:
        print("❌ الملف غير موجود.")
