import json

input_file = "baqara_tafsir.txt"
output_file = "baqara_tafsir.json"

data = {"البقرة": {}}
current_verse = ""
verse_lines = []

with open(input_file, "r", encoding="utf-8") as f:
    for line in f:
        line = line.strip()
        if not line:
            continue
        # لو السطر يبدأ بآية أو رقم آية
        if line.startswith("﴿") or line.startswith("الآية") or line[:2].isdigit():
            if current_verse:
                data["البقرة"][current_verse] = " ".join(verse_lines)
                verse_lines = []
            current_verse = line
        else:
            verse_lines.append(line)

# أضف آخر تفسير
if current_verse and verse_lines:
    data["البقرة"][current_verse] = " ".join(verse_lines)

with open(output_file, "w", encoding="utf-8") as f:
    json.dump(data, f, ensure_ascii=False, indent=2)

print("✅ تم تحويل التفسير إلى", output_file)
