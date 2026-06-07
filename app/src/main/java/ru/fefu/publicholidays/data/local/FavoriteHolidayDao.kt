package ru.fefu.publicholidays.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteHolidayDao {

    @Query(
        """
        SELECT * FROM favorite_holidays
        WHERE userId = :userId
        ORDER BY addedAt DESC
        """
    )
    fun getFavoritesByUser(userId: String): Flow<List<FavoriteHolidayEntity>>

    @Query(
        """
        SELECT favoriteId FROM favorite_holidays
        WHERE userId = :userId
        """
    )
    fun getFavoriteIdsByUser(userId: String): Flow<List<String>>

    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM favorite_holidays
            WHERE userId = :userId
            AND favoriteId = :favoriteId
        )
        """
    )
    suspend fun isFavoriteByUser(
        userId: String,
        favoriteId: String
    ): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(holiday: FavoriteHolidayEntity)

    @Query(
        """
        UPDATE favorite_holidays
        SET favoriteNote = :note
        WHERE userId = :userId
        AND favoriteId = :favoriteId
        """
    )
    suspend fun updateFavoriteNote(
        userId: String,
        favoriteId: String,
        note: String
    )

    @Query(
        """
        DELETE FROM favorite_holidays
        WHERE userId = :userId
        AND favoriteId = :favoriteId
        """
    )
    suspend fun deleteFavoriteByUserAndId(
        userId: String,
        favoriteId: String
    )

    @Query(
        """
    DELETE FROM favorite_holidays
    WHERE userId = :userId
    """
    )
    suspend fun deleteAllFavoritesByUser(userId: String)
}