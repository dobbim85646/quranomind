import os
import re

# مجلد التفسير
tafsir_dir = "tafasir_txt"
query = input("🔍 أدخل كلمة أو جزء منها للبحث: ").strip().lower()

# ألوان للعرض (ANSI)
BLUE = '\033[94m'
GREEN = '\033[92m'
YELLOW = '\033[93m'
RESET = '\033[0m'
RED = '\033[91m'

found_any = False

for filename in os.listdir(tafsir_dir):
    if filename.endswith(".txt"):
        path = os.path.join(tafsir_dir, filename)
        with open(path, "r", encoding="utf-8") as f:
            content = f.read()

        # تقسيم حسب كل تفسير
        blocks = re.findall(r"(==== الكلمة:.*?==== نهاية ====)", content, re.DOTALL)

        for block in blocks:
            if query in block.lower():
                found_any = True
                print(f"\n{YELLOW}📘 من الملف: {filename}{RESET}")
                print(f"{GREEN}{block.strip()}{RESET}")
                print("-" * 40)

if not found_any:
    print(f"{RED}❌ لم يتم العثور على نتائج مطابقة.{RESET}")
