import json
import os
import tkinter as tk
from tkinter import messagebox

# تحميل أسماء السور
with open('data/surahs.json', 'r', encoding='utf-8') as f:
    surah_names = json.load(f)

def get_tafsir(lang, surah, ayah):
    tafsir_file = f"tafasir_json/{surah}.json"
    if not os.path.exists(tafsir_file):
        return "❌ ملف التفسير غير موجود."

    with open(tafsir_file, 'r', encoding='utf-8') as f:
        data = json.load(f)

    tafsir_entry = data.get(str(ayah), None)
    if not tafsir_entry:
        return "❌ لم يتم العثور على التفسير."

    if lang == "ar":
        return tafsir_entry if isinstance(tafsir_entry, str) else tafsir_entry.get("ar", "❌ غير متوفر بالعربية.")
    elif lang == "en":
        ar_text = tafsir_entry if isinstance(tafsir_entry, str) else tafsir_entry.get("ar", "")
        en_text = tafsir_entry.get("en", "❌ الترجمة غير متوفرة.")
        return f"[AR] {ar_text}\n\n[EN] {en_text}"
    else:
        return "❌ لغة غير مدعومة."

def show_tafsir():
    lang = lang_var.get()
    surah = surah_entry.get()
    ayah = ayah_entry.get()

    if not (surah.isdigit() and ayah.isdigit()):
        messagebox.showerror("خطأ", "❗ أدخل أرقام صحيحة للسورة والآية.")
        return

    tafsir = get_tafsir(lang, surah, ayah)
    result_text.delete("1.0", tk.END)
    result_text.insert(tk.END, tafsir)

# واجهة المستخدم
root = tk.Tk()
root.title("QuranoMind - Tafsir Viewer")
root.geometry("600x500")

tk.Label(root, text="اختر اللغة (ar/en):").pack()
lang_var = tk.StringVar(value="ar")
tk.Entry(root, textvariable=lang_var).pack()

tk.Label(root, text="رقم السورة:").pack()
surah_entry = tk.Entry(root)
surah_entry.pack()

tk.Label(root, text="رقم الآية:").pack()
ayah_entry = tk.Entry(root)
ayah_entry.pack()

tk.Button(root, text="عرض التفسير", command=show_tafsir).pack(pady=10)

result_text = tk.Text(root, wrap="word", height=15)
result_text.pack(fill=tk.BOTH, expand=True)

root.mainloop()
