package com.example.quranomind.data.local

data class FavoriteEntity(
    val id: Long = 0L,
    val surahNumber: Int,
    val surahName: String,
    val ayahNumber: Int,
    val ayahText: String,
    val tafsir: String,
    val translated: String?,
    val interpreter: String,
    val lang: String,
    val hash: String,
    val timestamp: Long = System.currentTimeMillis()
)
