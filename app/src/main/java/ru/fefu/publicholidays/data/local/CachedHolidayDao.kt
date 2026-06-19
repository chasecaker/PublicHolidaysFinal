package ru.fefu.publicholidays.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedHolidayDao {

    @Query(
        """
        SELECT * FROM cached_holidays
        WHERE countryCode = :countryCode
        AND year = :year
        ORDER BY date ASC
        """
    )
    fun observeCachedHolidays(
        countryCode: String,
        year: Int
    ): Flow<List<CachedHolidayEntity>>

    @Query(
        """
        SELECT * FROM cached_holidays
        WHERE countryCode = :countryCode
        AND year = :year
        ORDER BY date ASC
        """
    )
    suspend fun getCachedHolidays(
        countryCode: String,
        year: Int
    ): List<CachedHolidayEntity>

    @Query(
        """
        SELECT * FROM cached_holidays
        WHERE countryCode = :countryCode
        AND year = :year
        AND date = :holidayDate
        AND name = :name
        LIMIT 1
        """
    )
    suspend fun getCachedHoliday(
        countryCode: String,
        year: Int,
        holidayDate: String,
        name: String
    ): CachedHolidayEntity?

    @Query(
        """
        SELECT MAX(cachedAt) FROM cached_holidays
        WHERE countryCode = :countryCode
        AND year = :year
        """
    )
    suspend fun getLastCacheUpdateTime(
        countryCode: String,
        year: Int
    ): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedHolidays(
        holidays: List<CachedHolidayEntity>
    )

    @Query(
        """
        DELETE FROM cached_holidays
        WHERE countryCode = :countryCode
        AND year = :year
        """
    )
    suspend fun clearCache(
        countryCode: String,
        year: Int
    )

    @Query(
        """
        DELETE FROM cached_holidays
        WHERE cachedAt < :expirationTime
        """
    )
    suspend fun deleteExpiredCache(
        expirationTime: Long
    )
}