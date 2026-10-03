package com.example.quranomind.data.repository

import android.content.Context
import com.example.quranomind.data.model.CosmicWord
import com.example.quranomind.data.model.ScienceVerse
import com.example.quranomind.data.model.SearchResult
import com.example.quranomind.data.model.SurahInfo
import com.example.quranomind.util.ArabicNormalizer
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStreamReader

class QuranRepository(private val context: Context) {

    // Verse count map for all 114 Surahs
    private val surahVerseCounts = mapOf(
        1 to 7, 2 to 286, 3 to 200, 4 to 176, 5 to 120, 6 to 165, 7 to 206, 8 to 75, 9 to 129, 10 to 109,
        11 to 123, 12 to 111, 13 to 43, 14 to 52, 15 to 99, 16 to 128, 17 to 111, 18 to 110, 19 to 98, 20 to 135,
        21 to 112, 22 to 78, 23 to 118, 24 to 64, 25 to 77, 26 to 227, 27 to 93, 28 to 88, 29 to 69, 30 to 60,
        31 to 34, 32 to 30, 33 to 73, 34 to 54, 35 to 45, 36 to 83, 37 to 182, 38 to 88, 39 to 75, 40 to 85,
        41 to 54, 42 to 53, 43 to 89, 44 to 59, 45 to 37, 46 to 35, 47 to 38, 48 to 29, 49 to 18, 50 to 45,
        51 to 60, 52 to 49, 53 to 62, 54 to 55, 55 to 78, 56 to 96, 57 to 29, 58 to 22, 59 to 24, 60 to 13,
        61 to 14, 62 to 11, 63 to 11, 64 to 18, 65 to 12, 66 to 12, 67 to 30, 68 to 52, 69 to 52, 70 to 44,
        71 to 28, 72 to 28, 73 to 20, 74 to 56, 75 to 40, 76 to 31, 77 to 50, 78 to 40, 79 to 46, 80 to 42,
        81 to 29, 82 to 19, 83 to 36, 84 to 25, 85 to 22, 86 to 17, 87 to 19, 88 to 26, 89 to 30, 90 to 20,
        91 to 15, 92 to 21, 93 to 11, 94 to 8, 95 to 8, 96 to 19, 97 to 5, 98 to 8, 99 to 8, 100 to 11,
        101 to 11, 102 to 8, 103 to 3, 104 to 9, 105 to 5, 106 to 4, 107 to 7, 108 to 3, 109 to 6, 110 to 3,
        111 to 5, 112 to 4, 113 to 5, 114 to 6
    )

    private val surahsList: List<SurahInfo> by lazy { loadSurahs() }
    private val quranVersesBySurah: Map<String, List<String>> by lazy { loadQuranJson() }
    private val cosmicWords: List<CosmicWord> by lazy { loadCosmicWords() }
    private val scienceVerses: List<ScienceVerse> by lazy { loadScienceExplained() }

    private fun loadSurahs(): List<SurahInfo> {
        val list = mutableListOf<SurahInfo>()
        try {
            val jsonString = context.assets.open("surahs.json").use { it.bufferedReader().readText() }
            val root = JSONObject(jsonString)
            val keys = root.keys().asSequence().mapNotNull { it.toIntOrNull() }.sorted().toList()
            for (num in keys) {
                val obj = root.getJSONObject(num.toString())
                val arabic = obj.optString("arabic", "سورة $num")
                val english = obj.optString("english", "Surah $num")
                val verseCount = surahVerseCounts[num] ?: 7
                list.add(SurahInfo(num, arabic, english, verseCount))
            }
        } catch (_: Exception) {
            // Fallback for all 114 surahs
            for (i in 1..114) {
                list.add(SurahInfo(i, "سورة $i", "Surah $i", surahVerseCounts[i] ?: 7))
            }
        }
        return list
    }

    private fun loadQuranJson(): Map<String, List<String>> {
        val map = mutableMapOf<String, List<String>>()
        try {
            val jsonString = context.assets.open("quran.json").use { it.bufferedReader().readText() }
            val root = JSONObject(jsonString)
            for (key in root.keys()) {
                val array = root.getJSONArray(key)
                val verses = mutableListOf<String>()
                for (i in 0 until array.length()) {
                    verses.add(array.getString(i))
                }
                map[key] = verses
            }
        } catch (_: Exception) {}
        return map
    }

    private fun loadCosmicWords(): List<CosmicWord> {
        val list = mutableListOf<CosmicWord>()
        try {
            val jsonString = context.assets.open("cosmic_words.json").use { it.bufferedReader().readText() }
            val root = JSONObject(jsonString)
            for (key in root.keys()) {
                val obj = root.getJSONObject(key)
                list.add(
                    CosmicWord(
                        word = key,
                        tafsir = obj.optString("تفسير", ""),
                        scienceTerm = obj.optString("علم", ""),
                        explanation = obj.optString("شرح", "")
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    private fun loadScienceExplained(): List<ScienceVerse> {
        val list = mutableListOf<ScienceVerse>()
        try {
            val jsonString = context.assets.open("science_explained.json").use { it.bufferedReader().readText() }
            val root = JSONObject(jsonString)
            for (surahName in root.keys()) {
                val versesObj = root.getJSONObject(surahName)
                for (verse in versesObj.keys()) {
                    list.add(
                        ScienceVerse(
                            surahName = surahName,
                            verseText = verse,
                            scientificExplanation = versesObj.getString(verse)
                        )
                    )
                }
            }
        } catch (_: Exception) {}
        return list
    }

    fun getAllSurahs(): List<SurahInfo> = surahsList

    fun findSurah(query: String): SurahInfo? {
        val trimmed = query.trim()
        val num = trimmed.toIntOrNull()
        if (num != null && num in 1..114) {
            return surahsList.find { it.number == num }
        }

        val normQuery = ArabicNormalizer.normalize(trimmed)
        return surahsList.find { surah ->
            surah.number.toString() == trimmed ||
            ArabicNormalizer.normalize(surah.arabicName) == normQuery ||
            surah.englishName.equals(trimmed, ignoreCase = true) ||
            ArabicNormalizer.normalize(surah.arabicName).contains(normQuery) ||
            surah.englishName.contains(trimmed, ignoreCase = true)
        }
    }

    fun getAyahText(surahNumber: Int, ayahNumber: Int): String? {
        val surah = surahsList.find { it.number == surahNumber } ?: return null
        
        // 1. Try from quran.json
        val verses = quranVersesBySurah[surah.arabicName]
        if (verses != null && ayahNumber in 1..verses.size) {
            return verses[ayahNumber - 1]
        }

        // 2. Default standard verses for known surahs
        if (surahNumber == 1) {
            val fatiha = listOf(
                "بِسْمِ اللَّهِ الرَّحْمَـٰنِ الرَّحِيمِ",
                "الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ",
                "الرَّحْمَـٰنِ الرَّحِيمِ",
                "مَالِكِ يَوْمِ الدِّينِ",
                "إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ",
                "اهْدِنَا الصِّرَاطَ الْمُسْتَقِيمَ",
                "صِرَاطَ الَّذِينَ أَنْعَمْتَ عَلَيْهِمْ غَيْرِ الْمَغْضُوبِ عَلَيْهِمْ وَلَا الضَّالِّينَ"
            )
            if (ayahNumber in 1..fatiha.size) return fatiha[ayahNumber - 1]
        }

        if (surahNumber == 112) {
            val ikhlas = listOf(
                "قُلْ هُوَ اللَّهُ أَحَدٌ",
                "اللَّهُ الصَّمَدُ",
                "لَمْ يَلِدْ وَلَمْ يُولَدْ",
                "وَلَمْ يَكُن لَّهُ كُفُوًا أَحَدٌ"
            )
            if (ayahNumber in 1..ikhlas.size) return ikhlas[ayahNumber - 1]
        }

        return null
    }

    fun getLocalTafsir(surahNumber: Int, ayahNumber: Int): String? {
        return try {
            val fileName = "tafasir_json/$surahNumber.json"
            val jsonString = context.assets.open(fileName).use { it.bufferedReader().readText() }
            val root = JSONObject(jsonString)
            if (root.has(ayahNumber.toString())) {
                root.getString(ayahNumber.toString())
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun searchLocalTafasir(query: String): List<SearchResult> {
        val results = mutableListOf<SearchResult>()
        val normQuery = ArabicNormalizer.normalize(query)
        if (normQuery.isBlank()) return results

        for (surah in surahsList) {
            try {
                val fileName = "tafasir_json/${surah.number}.json"
                val jsonString = context.assets.open(fileName).use { it.bufferedReader().readText() }
                val root = JSONObject(jsonString)
                val keys = root.keys()
                while (keys.hasNext()) {
                    val ayahKey = keys.next()
                    val tafsirText = root.getString(ayahKey)
                    val normTafsir = ArabicNormalizer.normalize(tafsirText)
                    if (normTafsir.contains(normQuery) || tafsirText.contains(query, ignoreCase = true)) {
                        val ayahNum = ayahKey.toIntOrNull() ?: 1
                        val ayahText = getAyahText(surah.number, ayahNum) ?: "سورة ${surah.arabicName} - الآية $ayahNum"
                        val hash = ArabicNormalizer.md5("${surah.number}-$ayahKey-$tafsirText")
                        results.add(
                            SearchResult(
                                surahName = surah.arabicName,
                                surahNumber = surah.number.toString(),
                                ayahNumber = ayahKey,
                                ayahText = ayahText,
                                tafsir = tafsirText,
                                hash = hash,
                                isAiGenerated = false
                            )
                        )
                    }
                }
            } catch (_: Exception) {}
        }
        return results
    }

    fun getCosmicWordsList(): List<CosmicWord> = cosmicWords
    fun getScienceVersesList(): List<ScienceVerse> = scienceVerses
}
