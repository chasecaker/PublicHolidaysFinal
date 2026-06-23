package ru.fefu.publicholidays.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HolidayHistoryDao {

    @Query(
        """
        SELECT * FROM holiday_history
        WHERE userId = :userId
        ORDER BY timestamp DESC
        LIMIT :limit
        """
    )
    fun getHistoryByUser(
        userId: String,
        limit: Int = 50
    ): Flow<List<HolidayHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoryEntry(entry: HolidayHistoryEntity)

    @Query(
        """
        DELETE FROM holiday_history
        WHERE userId = :userId
        AND date = :date
        AND countryCode = :countryCode
        AND name = :name
        """
    )
    suspend fun deleteDuplicateHistoryEntry(
        userId: String,
        date: String,
        countryCode: String,
        name: String
    )

    @Query(
        """
        DELETE FROM holiday_history
        WHERE userId = :userId
        AND id NOT IN (
            SELECT id FROM holiday_history
            WHERE userId = :userId
            ORDER BY timestamp DESC
            LIMIT :limit
        )
        """
    )
    suspend fun trimHistory(
        userId: String,
        limit: Int
    )

    @Query("DELETE FROM holiday_history WHERE userId = :userId")
    suspend fun clearHistoryByUser(userId: String)
}