package ru.fefu.publicholidays.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HolidayNoteDao {

    @Query(
        """
        SELECT * FROM holiday_notes
        WHERE userId = :userId
        AND holidayDate = :holidayDate
        AND countryCode = :countryCode
        LIMIT 1
        """
    )
    fun getNoteByUserAndHoliday(
        userId: String,
        holidayDate: String,
        countryCode: String
    ): Flow<HolidayNoteEntity?>

    @Query(
        """
        SELECT * FROM holiday_notes
        WHERE userId = :userId
        """
    )
    fun getNotesByUser(userId: String): Flow<List<HolidayNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: HolidayNoteEntity)

    @Query(
        """
        DELETE FROM holiday_notes
        WHERE userId = :userId
        AND holidayDate = :holidayDate
        AND countryCode = :countryCode
        """
    )
    suspend fun deleteNoteByUserAndHoliday(
        userId: String,
        holidayDate: String,
        countryCode: String
    )

    @Query(
        """
    DELETE FROM holiday_notes
    WHERE userId = :userId
    """
    )
    suspend fun deleteAllNotesByUser(userId: String)
}