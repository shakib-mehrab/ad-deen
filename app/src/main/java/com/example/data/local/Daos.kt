package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DayRecordDao {
    @Query("SELECT * FROM days ORDER BY date DESC")
    fun getAllDays(): Flow<List<DayRecordEntity>>

    @Query("SELECT * FROM days WHERE date = :date LIMIT 1")
    fun getDayByDate(date: String): Flow<DayRecordEntity?>

    @Query("SELECT * FROM days WHERE date = :date LIMIT 1")
    suspend fun getDayByDateOnce(date: String): DayRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(day: DayRecordEntity)

    @Query("DELETE FROM days WHERE date = :date")
    suspend fun deleteDay(date: String)

    @Query("DELETE FROM days")
    suspend fun clearAll()
}

@Dao
interface ZikrItemDao {
    @Query("SELECT * FROM zikr_items WHERE archived = 0 ORDER BY orderIndex ASC")
    fun getActiveItems(): Flow<List<ZikrItemEntity>>

    @Query("SELECT * FROM zikr_items ORDER BY orderIndex ASC")
    fun getAllItems(): Flow<List<ZikrItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(item: ZikrItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ZikrItemEntity>)

    @Query("DELETE FROM zikr_items WHERE id = :id")
    suspend fun deleteItem(id: String)

    @Query("DELETE FROM zikr_items")
    suspend fun clearAll()
}

@Dao
interface UserSettingsDao {
    @Query("SELECT * FROM user_settings WHERE id = 1 LIMIT 1")
    fun getSettings(): Flow<UserSettingsEntity?>

    @Query("SELECT * FROM user_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsOnce(): UserSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: UserSettingsEntity)

    @Query("DELETE FROM user_settings")
    suspend fun clearAll()
}
