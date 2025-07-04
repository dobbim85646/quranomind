import os

def search_in_file(file_path, keyword):
    results = []
    with open(file_path, 'r', encoding='utf-8', errors='ignore') as f:
        lines = f.readlines()
        for i, line in enumerate(lines):
            if keyword in line:
                context = lines[i-1].strip() + " | " + line.strip() + " | " + lines[i+1].strip() if i > 0 and i+1 < len(lines) else line.strip()
                results.append((os.path.basename(file_path), i+1, context))
    return results

def search_all_txt(folder_path, keyword):
    all_results = []
    for filename in os.listdir(folder_path):
        if filename.endswith(".txt"):
            file_path = os.path.join(folder_path, filename)
            results = search_in_file(file_path, keyword)
            all_results.extend(results)
    return all_results

# مسار المجلد اللي فيه التفاسير
tafasir_folder = "tafasir_txt"
keyword = input("🔍 أدخل كلمة أو جزء منها للبحث: ").strip()

print("\n🧬 نتائج البحث:")
print("-" * 40)

matches = search_all_txt(tafasir_folder, keyword)

if not matches:
    print("❌ لم يتم العثور على نتائج مطابقة.")
else:
    for file, line_num, text in matches:
        print(f"\n📄 الملف: {file}\n🔢 السطر: {line_num}\n🔹 السياق: {text}\n" + "-" * 40)
