import os

# 🧠 هذا السكربت يبحث عن تفسيرات في ملفات نصية موجودة في مجلد tafasir_txt
# حيث يحتوي كل ملف على تفسير سورة معينة بصيغة نص عادي

# 👤 يطلب من المستخدم إدخال كلمة أو جملة للبحث عنها
search_term = input("🔍 أدخل اسم السورة أو جزء من الآية: ").strip()

# 📁 اسم المجلد الذي يحتوي على ملفات التفسير النصي
directory = "tafasir_txt"

# 🗂️ قائمة لتخزين النتائج التي سنعثر عليها أثناء البحث
results = []

# 🔍 نمر على كل ملف داخل مجلد tafasir_txt
for filename in os.listdir(directory):
    # ✅ نتأكد أن الملف من نوع .txt فقط
    if filename.endswith(".txt"):
        filepath = os.path.join(directory, filename)
        try:
            # 📖 نفتح الملف مع دعم الترميز العربي
            with open(filepath, "r", encoding="utf-8") as f:
                # 🧾 نقرأ الملف سطرًا سطرًا مع رقم كل سطر
                for line_num, line in enumerate(f, 1):
                    # 🔎 إذا وجدنا الكلمة التي بحث عنها المستخدم
                    if search_term in line:
                        results.append({
                            "file": filename,       # اسم الملف
                            "line": line.strip(),   # السطر نفسه بدون فراغات
                            "line_num": line_num    # رقم السطر
                        })
        except Exception as e:
            print(f"⚠️ خطأ أثناء فتح الملف {filename}: {e}")

# 🖨️ نعرض النتائج التي وجدناها
print("\n🧬 نتائج البحث:")
print("----------------------------------------")

# ✅ إذا وُجدت نتائج نطبعها
if results:
    for res in results:
        print(f"📘 الملف: {res['file']}")
        print(f"📍 السطر {res['line_num']}: {res['line']}\n")
else:
    print("❌ لم يتم العثور على تفسير مطابق.")
