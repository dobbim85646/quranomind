import os
import shutil

base_dir = os.path.expanduser("~/QuranoMind/tafasir_json")
files = os.listdir(base_dir)

# نفترض أن اسم المفسر مذكور في اسم الملف مثل: ibn_katheer_2.json
for file in files:
    if file.endswith(".json") and "_" in file:
        parts = file.split("_")
        if len(parts) >= 2:
            mufassir = parts[0]  # مثل: ibn_katheer
            surah_num = parts[-1].replace(".json", "")  # رقم السورة

            dest_folder = os.path.join(base_dir, mufassir)
            os.makedirs(dest_folder, exist_ok=True)

            src_path = os.path.join(base_dir, file)
            dest_path = os.path.join(dest_folder, f"{surah_num}.json")

            shutil.move(src_path, dest_path)
            print(f"✅ نقل الملف: {file} ➜ {mufassir}/{surah_num}.json")
