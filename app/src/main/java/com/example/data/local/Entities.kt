package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "days")
data class DayRecordEntity(
    @PrimaryKey val date: String, // "yyyy-MM-dd"
    val fajr: String,
    val dhuhr: String,
    val asr: String,
    val maghrib: String,
    val isha: String,
    val witr: Boolean,
    val tahajjud: Boolean,
    val duha: Boolean,
    val quran: Boolean = false,
    val scoreVersion: Int = 2,
    val wake: Boolean,
    val morning: Boolean,
    val evening: Boolean,
    val sleep: Boolean,
    val zikr: Boolean,
    val countersJson: String, // json representation of counters map
    val score: Int,
    val perfect: Boolean,
    val updatedAt: Long
)

@Entity(tableName = "zikr_items")
data class ZikrItemEntity(
    @PrimaryKey val id: String,
    val text: String,
    val target: Int,
    val orderIndex: Int,
    val isDefault: Boolean,
    val archived: Boolean
)

@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val language: String,
    val calcMethod: String,
    val locationMode: String,
    val lat: Double,
    val lng: Double,
    val district: String,
    val alarmTogglesJson: String,
    val morningReminder: Boolean,
    val eveningReminder: Boolean,
    val istigfarMode: String,
    val istigfarCustomMinutes: Int,
    val istigfarTimesPerDay: Int,
    val istigfarSpecificTimesJson: String,
    val istigfarEnabled: Boolean,
    val durudMode: String,
    val durudCustomMinutes: Int,
    val durudTimesPerDay: Int,
    val durudSpecificTimesJson: String,
    val durudEnabled: Boolean,
    val quietHoursStart: String,
    val quietHoursEnd: String,
    val hijriOffset: Int = 0,
    val arabicFontSize: Int = 26,
    val customSoundNamesJson: String = "{}"
)

