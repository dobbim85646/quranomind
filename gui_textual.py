from textual.app import App, ComposeResult
from textual.widgets import Input, Static, Button
import json
import os

def load_tafsir(surah, ayah):
    paths = [
        f"tafasir_json/{surah}.json",
        f"QuranoMind/tafasir_json/{surah}.json",
        f"/data/data/com.termux/files/home/QuranoMind/tafasir_json/{surah}.json"
    ]
    for path in paths:
        if os.path.exists(path):
            try:
                with open(path, "r", encoding="utf-8") as f:
                    data = json.load(f)
                text = data.get(str(ayah))
                if text:
                    return text
            except Exception as e:
                return f"⚠️ Error loading tafsir: {e}"
    return "❌ Tafsir not found."

class TafsirApp(App):
    CSS_PATH = None

    def compose(self) -> ComposeResult:
        yield Static("📘 Tafsir Viewer (Textual UI)\n", id="title")
        yield Input(placeholder="Language (ar/en)", id="lang")
        yield Input(placeholder="Surah number (e.g., 2)", id="surah")
        yield Input(placeholder="Ayah number (e.g., 4)", id="ayah")
        yield Button("Get Tafsir", id="submit")
        yield Static("", id="output")

    def on_button_pressed(self, event: Button.Pressed):
        lang = self.query_one("#lang").value.strip().lower()
        surah = self.query_one("#surah").value.strip()
        ayah = self.query_one("#ayah").value.strip()
        tafsir = load_tafsir(surah, ayah)

        if lang == "en":
            # ترجمة إنجليزية تجريبية فقط
            from deep_translator import GoogleTranslator
            try:
                tafsir_en = GoogleTranslator(source="auto", target="en").translate(tafsir)
                tafsir = f"[AR] {tafsir}\n\n[EN] {tafsir_en}"
            except:
                tafsir = f"[AR] {tafsir}\n\n⚠️ ترجمة غير متوفرة."

        self.query_one("#output").update(tafsir)

if __name__ == "__main__":
    TafsirApp().run()
