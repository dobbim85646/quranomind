import os
import requests
from bs4 import BeautifulSoup

# مجلد الحفظ
SAVE_DIR = os.path.expanduser("~/QuranoMind/tafasir_txt")

def fetch_tafsir(surah_number):
    url = f"https://tafsir.net/quran/{surah_number}"
    print(f"🔗 جاري جلب التفسير من: {url}")

    response = requests.get(url)
    if response.status_code != 200:
        print(f"❌ فشل في الوصول إلى الموقع (الكود: {response.status_code})")
        return

    soup = BeautifulSoup(response.text, "html.parser")

    title = soup.find("h1").text.strip() if soup.find("h1") else f"Sura_{surah_number}"
    paragraphs = soup.find_all("p")

    tafsir_text = ""
    for p in paragraphs:
        tafsir_text += p.text.strip() + "\n\n"

    if not tafsir_text.strip():
        print("❌ لم يتم العثور على تفسير.")
        return

    file_name = os.path.join(SAVE_DIR, f"surah_{surah_number}_tafsir.txt")
    with open(file_name, "w", encoding="utf-8") as f:
        f.write(tafsir_text)

    print(f"✅ تم حفظ التفسير في: {file_name}")

if __name__ == "__main__":
    surah_input = input("🔢 أدخل رقم السورة (1-114): ").strip()
    if not surah_input.isdigit():
        print("❌ يجب إدخال رقم سورة صحيح.")
    else:
        fetch_tafsir(surah_input)
