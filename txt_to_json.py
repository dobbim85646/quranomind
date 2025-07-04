import json

input_file = "baqara_tafsir.txt"
output_file = "baqara_tafsir.json"

tafsir_data = {}
sura_name = "البقرة"

with open(input_file, "r", encoding="utf-8") as f:
    lines = f.readlines()

for line in lines:
    if ":" in line:
        parts = line.strip().split(":", 1)
        verse = parts[0].strip()
        tafsir = parts[1].strip()
        if verse and tafsir:
            tafsir_data[verse] = tafsir

with open(output_file, "w", encoding="utf-8") as f_out:
    json.dump({sura_name: tafsir_data}, f_out, ensure_ascii=False, indent=2)

print(f"✅ تم تحويل التفسير إلى {output_file}")
