import os
import json

DATA_PATH = os.path.expanduser("~/QuranoMind/tafasir_json")

def load_tafsir(surah, ayah):
    filepath = os.path.join(DATA_PATH, f"{surah}.json")
    if not os.path.exists(filepath):
        print("❌ Tafsir file not found.")
        return
    with open(filepath, "r", encoding="utf-8") as f:
        tafsir = json.load(f)
        print(f"\n📖 Surah {surah}, Ayah {ayah} Tafsir:\n")
        print(tafsir.get(str(ayah), "⚠️ Tafsir not available."))

def search_keyword(keyword):
    print(f"\n🔍 Searching for '{keyword}' in all tafsir files...\n")
    for filename in os.listdir(DATA_PATH):
        if filename.endswith(".json"):
            with open(os.path.join(DATA_PATH, filename), "r", encoding="utf-8") as f:
                tafsir = json.load(f)
                for ayah, text in tafsir.items():
                    if keyword in text:
                        print(f"[Surah {filename.replace('.json','')}, Ayah {ayah}] → {text}")

def menu():
    while True:
        print("""
📘 QuranoMind Manager 📘

[1] View Tafsir (Surah / Ayah)
[2] Search for Keyword
[3] Generate New Tafsir File (JSON)
[4] Edit Existing Tafsir
[5] Export to TXT or HTML
[6] About
[0] Exit
        """)
        choice = input("➡️ Enter your choice: ").strip()
        
        if choice == "1":
            surah = input("🔢 Surah number (1-114): ").strip()
            ayah = input("🔢 Ayah number: ").strip()
            load_tafsir(surah, ayah)
        elif choice == "2":
            keyword = input("🔍 Enter keyword to search: ").strip()
            search_keyword(keyword)
        elif choice == "3":
            print("⚙️ Not implemented yet.")
        elif choice == "4":
            print("⚙️ Not implemented yet.")
        elif choice == "5":
            print("⚙️ Not implemented yet.")
        elif choice == "6":
            print("QuranoMind Project by Mohammad ✨")
        elif choice == "0":
            print("👋 Exiting.")
            break
        else:
            print("❌ Invalid choice.")

if __name__ == "__main__":
    menu()
