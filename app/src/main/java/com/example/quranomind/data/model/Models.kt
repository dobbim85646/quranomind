package com.example.quranomind.data.model

data class SurahInfo(
    val number: Int,
    val arabicName: String,
    val englishName: String,
    val ayahCount: Int,
    val revelationType: String = if (number in listOf(2, 3, 4, 5, 8, 9, 22, 24, 33, 47, 48, 49, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 76, 98, 110)) "مدنية" else "مكية"
)

data class TafsirResult(
    val surahNumber: Int,
    val surahName: String,
    val ayahNumber: Int,
    val ayahText: String,
    val tafsirArabic: String,
    val tafsirEnglish: String? = null,
    val interpreterId: String,
    val interpreterName: String,
    val hash: String,
    val isAiGenerated: Boolean = false
)

data class CosmicWord(
    val word: String,
    val tafsir: String,
    val scienceTerm: String,
    val explanation: String
)

data class ScienceVerse(
    val surahName: String,
    val verseText: String,
    val scientificExplanation: String
)

data class SearchResult(
    val surahName: String,
    val surahNumber: String,
    val ayahNumber: String,
    val ayahText: String,
    val tafsir: String,
    val hash: String,
    val isAiGenerated: Boolean = false
)

enum class InterpreterMode(val id: String, val titleArabic: String, val titleEnglish: String) {
    MAISSAR("maissar", "التفسير الميسر", "Al-Muyassar (Simplified)"),
    IBN_KATHIR("ibn_kathir", "تفسير ابن كثير", "Tafsir Ibn Kathir"),
    QURTUBI("qurtubi", "تفسير القرطبي", "Tafsir Al-Qurtubi"),
    SAADI("saadi", "تفسير السعدي", "Tafsir Al-Sa'di"),
    COMPARATIVE("all", "مقارنة المفسرين الثلاثة", "Comparative Analysis (All 3)"),
    GEMINI_GENERAL("gemini_general", "تدبر وفهم (الذكاء الاصطناعي)", "AI Comprehensive Reflection")
}
