package com.example.quranomind.data.local

import kotlinx.coroutines.flow.Flow

interface FavoriteDao {
    fun getAllFavorites(): Flow<List<FavoriteEntity>>
    suspend fun insertFavorite(favorite: FavoriteEntity): Long
    suspend fun deleteByHash(hash: String)
    suspend fun deleteFavorite(favorite: FavoriteEntity)
    fun isFavorited(hash: String): Flow<Boolean>
}
