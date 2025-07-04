import re
import json
import os

def backup_file(filepath):
    backup_path = filepath + ".bak"
    if not os.path.exists(backup_path):
        with open(filepath, "r", encoding="utf-8") as f:
            content = f.read()
        with open(backup_path, "w", encoding="utf-8") as f:
            f.write(content)
        print(f"📦 Backup created: {backup_path}")
    else:
        print(f"📦 Backup already exists: {backup_path}")

def sanitize_json_text(text):
    # Remove invisible control characters (except newline/tab)
    text = re.sub(r"[\x00-\x08\x0b-\x0c\x0e-\x1f]", "", text)
    # Ensure proper quotes (in case of Arabic curly quotes etc.)
    text = text.replace("“", "\"").replace("”", "\"").replace("‘", "'").replace("’", "'")
    # Fix common JSON errors: like trailing commas
    text = re.sub(r",\s*([}\]])", r"\1", text)
    return text

def fix_json_file(filepath):
    try:
        with open(filepath, "r", encoding="utf-8") as f:
            original = f.read()

        sanitized = sanitize_json_text(original)

        try:
            data = json.loads(sanitized)
            print("✅ JSON is already valid.")
            return
        except json.JSONDecodeError as e:
            print(f"⚠️ JSON invalid, attempting auto-fix...\n  ⛔ {e}")

        # Attempt to fix manually if it's still bad
        json_object_match = re.findall(r'"(\d+)"\s*:\s*"(.+?)"', sanitized)
        if not json_object_match:
            raise ValueError("Could not recover any JSON objects.")

        fixed_dict = {key: value for key, value in json_object_match}
        with open(filepath, "w", encoding="utf-8") as f:
            json.dump(fixed_dict, f, indent=2, ensure_ascii=False)

        print(f"✅ File fixed and saved: {filepath}")

    except Exception as err:
        print(f"❌ Fatal error: {err}")

def main():
    filepath = input("📁 Enter JSON file path (e.g. tafasir_json/2.json): ").strip()
    if not os.path.exists(filepath):
        print("❌ File does not exist.")
        return
    backup_file(filepath)
    fix_json_file(filepath)

if __name__ == "__main__":
    main()
