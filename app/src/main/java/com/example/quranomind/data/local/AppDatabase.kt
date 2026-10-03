package com.example.quranomind.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class AppDatabase private constructor(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
), FavoriteDao {

    private val favoritesFlow = MutableStateFlow<List<FavoriteEntity>>(emptyList())

    init {
        refreshFavorites()
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS favorites (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                surahNumber INTEGER NOT NULL,
                surahName TEXT NOT NULL,
                ayahNumber INTEGER NOT NULL,
                ayahText TEXT NOT NULL,
                tafsir TEXT NOT NULL,
                translated TEXT,
                interpreter TEXT NOT NULL,
                lang TEXT NOT NULL,
                hash TEXT NOT NULL UNIQUE,
                timestamp INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS favorites")
        onCreate(db)
    }

    private fun refreshFavorites() {
        val list = mutableListOf<FavoriteEntity>()
        val db = readableDatabase
        val cursor = db.query(
            "favorites",
            null,
            null,
            null,
            null,
            null,
            "timestamp DESC"
        )
        cursor.use { c ->
            while (c.moveToNext()) {
                list.add(
                    FavoriteEntity(
                        id = c.getLong(c.getColumnIndexOrThrow("id")),
                        surahNumber = c.getInt(c.getColumnIndexOrThrow("surahNumber")),
                        surahName = c.getString(c.getColumnIndexOrThrow("surahName")),
                        ayahNumber = c.getInt(c.getColumnIndexOrThrow("ayahNumber")),
                        ayahText = c.getString(c.getColumnIndexOrThrow("ayahText")),
                        tafsir = c.getString(c.getColumnIndexOrThrow("tafsir")),
                        translated = c.getString(c.getColumnIndexOrThrow("translated")),
                        interpreter = c.getString(c.getColumnIndexOrThrow("interpreter")),
                        lang = c.getString(c.getColumnIndexOrThrow("lang")),
                        hash = c.getString(c.getColumnIndexOrThrow("hash")),
                        timestamp = c.getLong(c.getColumnIndexOrThrow("timestamp"))
                    )
                )
            }
        }
        favoritesFlow.value = list
    }

    override fun getAllFavorites(): Flow<List<FavoriteEntity>> = favoritesFlow.asStateFlow()

    override suspend fun insertFavorite(favorite: FavoriteEntity): Long = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put("surahNumber", favorite.surahNumber)
            put("surahName", favorite.surahName)
            put("ayahNumber", favorite.ayahNumber)
            put("ayahText", favorite.ayahText)
            put("tafsir", favorite.tafsir)
            put("translated", favorite.translated)
            put("interpreter", favorite.interpreter)
            put("lang", favorite.lang)
            put("hash", favorite.hash)
            put("timestamp", favorite.timestamp)
        }
        val id = writableDatabase.insertWithOnConflict(
            "favorites",
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
        refreshFavorites()
        id
    }

    override suspend fun deleteByHash(hash: String) = withContext(Dispatchers.IO) {
        writableDatabase.delete("favorites", "hash = ?", arrayOf(hash))
        refreshFavorites()
        Unit
    }

    override suspend fun deleteFavorite(favorite: FavoriteEntity) = withContext(Dispatchers.IO) {
        writableDatabase.delete("favorites", "hash = ?", arrayOf(favorite.hash))
        refreshFavorites()
        Unit
    }

    override fun isFavorited(hash: String): Flow<Boolean> {
        return favoritesFlow.map { list -> list.any { it.hash == hash } }
    }

    fun favoriteDao(): FavoriteDao = this

    companion object {
        private const val DATABASE_NAME = "quranomind_database.db"
        private const val DATABASE_VERSION = 1

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = AppDatabase(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
