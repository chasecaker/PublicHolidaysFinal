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
        AND holidayId = :holidayId
        LIMIT 1
        """
    )
    fun getNoteByUserAndHoliday(
        userId: String,
        holidayId: String
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
        AND holidayId = :holidayId
        """
    )
    suspend fun deleteNoteByUserAndHoliday(
        userId: String,
        holidayId: String
    )

    @Query(
        """
        DELETE FROM holiday_notes
        WHERE userId = :userId
        """
    )
    suspend fun deleteAllNotesByUser(userId: String)
}