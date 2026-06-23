package ru.fefu.publicholidays.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        FavoriteHolidayEntity::class,
        UserEntity::class,
        HolidayHistoryEntity::class,
        HolidayNoteEntity::class,
        CachedHolidayEntity::class
    ],
    version = 7,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteHolidayDao(): FavoriteHolidayDao
    abstract fun userDao(): UserDao
    abstract fun holidayHistoryDao(): HolidayHistoryDao
    abstract fun holidayNoteDao(): HolidayNoteDao
    abstract fun cachedHolidayDao(): CachedHolidayDao
}