package com.example.ui

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.alarm.AlarmScheduler
import com.example.data.AppRepository
import com.example.data.AssetLoader
import com.example.model.*
import com.example.prayer.PrayerCalculator
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(application)
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("Asia/Dhaka")
    }

    val todayDateString: String
        get() = dateFormat.format(Date())

    val selectedDate = MutableStateFlow(todayDateString)

    val settings: StateFlow<UserSettings> = repository.getSettings()
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserSettings())

    val allDays: StateFlow<List<DayRecord>> = repository.getAllDays()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val activeZikrItems: StateFlow<List<ZikrItem>> = repository.getActiveZikrItems()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Bundled content
    val districts: List<District> by lazy { AssetLoader.loadDistricts(application) }
    val morningAzkhar: List<AzkharItem> by lazy { AssetLoader.loadAzkhar(application, "azkhar_morning.json") }
    val eveningAzkhar: List<AzkharItem> by lazy { AssetLoader.loadAzkhar(application, "azkhar_evening.json") }
    val sleepAzkhar: List<AzkharItem> by lazy { AssetLoader.loadAzkhar(application, "azkhar_sleep.json") }
    val wakeAzkhar: List<AzkharItem> by lazy { AssetLoader.loadAzkhar(application, "azkhar_wake.json") }
    val asmaulHusna: List<AllahName> by lazy { AssetLoader.loadAsmaulHusna(application) }

    // Selected day record
    val selectedDayRecord: StateFlow<DayRecord> = selectedDate.flatMapLatest { date ->
        repository.getDay(date)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, DayRecord(date = todayDateString))

    // Today record
    val todayRecord: StateFlow<DayRecord> = repository.getDay(todayDateString)
        .stateIn(viewModelScope, SharingStarted.Eagerly, DayRecord(date = todayDateString))

    // Real-time prayer schedule
    val prayerSchedule: StateFlow<DailyPrayerSchedule> = combine(
        selectedDate,
        settings
    ) { dateStr, userSettings ->
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Dhaka"))
        val date = runCatching { dateFormat.parse(dateStr) }.getOrNull()
        if (date != null) {
            cal.time = date
        }
        val method = CalculationMethod.fromId(userSettings.calcMethod)
        PrayerCalculator.calculateSchedule(cal, userSettings.lat, userSettings.lng, method, cal.timeZone)
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        PrayerCalculator.calculateSchedule(
            Calendar.getInstance(TimeZone.getTimeZone("Asia/Dhaka")),
            23.8103,
            90.4125,
            CalculationMethod.IFB
        )
    )

    // Current & Longest Perfect Day Streaks
    val currentStreak: StateFlow<Int> = allDays.map { days ->
        calculateCurrentStreak(days)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val longestStreak: StateFlow<Int> = allDays.map { days ->
        calculateLongestStreak(days)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val currentUserAccount: StateFlow<com.example.data.AppUser?> = repository.currentUserAccount
    val isFirebaseInitialized: StateFlow<Boolean> = repository.isFirebaseInitialized
    val firebaseStatus: StateFlow<String> = repository.firebaseStatus
    val lastSyncTime: StateFlow<Long> = repository.lastSyncTime

    // Live clock ticker
    val liveTimeMillis = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1000)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), System.currentTimeMillis())

    init {
        // Reschedule alarms whenever settings change
        viewModelScope.launch {
            settings.collectLatest { s ->
                AlarmScheduler.scheduleNext7Days(getApplication(), s)
            }
        }
    }

    // ----------------- DATE NAVIGATION -----------------

    fun selectDate(date: String) {
        // Enforce: Future dates cannot be navigated to for tracking
        if (date <= todayDateString) {
            selectedDate.value = date
        }
    }

    fun stepDate(daysOffset: Int) {
        val cur = runCatching { dateFormat.parse(selectedDate.value) }.getOrNull() ?: Date()
        val cal = Calendar.getInstance().apply {
            time = cur
            add(Calendar.DAY_OF_MONTH, daysOffset)
        }
        val target = dateFormat.format(cal.time)
        if (target <= todayDateString) {
            selectedDate.value = target
        }
    }

    fun isDateToday(): Boolean = selectedDate.value == todayDateString

    // ----------------- TRACKER MUTATIONS -----------------

    fun updatePrayerStatus(prayerKey: String, status: PrayerStatus) {
        val current = selectedDayRecord.value
        val prayers = when (prayerKey.lowercase()) {
            "fajr" -> current.prayers.copy(fajr = status)
            "dhuhr" -> current.prayers.copy(dhuhr = status)
            "asr" -> current.prayers.copy(asr = status)
            "maghrib" -> current.prayers.copy(maghrib = status)
            "isha" -> current.prayers.copy(isha = status)
            else -> current.prayers
        }
        saveDay(current.copy(prayers = prayers))
    }

    fun toggleNafl(type: String) {
        val current = selectedDayRecord.value
        val nafl = when (type.lowercase()) {
            "witr" -> current.nafl.copy(witr = !current.nafl.witr)
            "tahajjud" -> current.nafl.copy(tahajjud = !current.nafl.tahajjud)
            "duha" -> current.nafl.copy(duha = !current.nafl.duha)
            else -> current.nafl
        }
        saveDay(current.copy(nafl = nafl))
    }

    fun toggleSection(sectionKey: String) {
        val current = selectedDayRecord.value
        val sections = when (sectionKey.lowercase()) {
            "wake" -> current.sections.copy(wake = !current.sections.wake)
            "morning" -> current.sections.copy(morning = !current.sections.morning)
            "evening" -> current.sections.copy(evening = !current.sections.evening)
            "sleep" -> current.sections.copy(sleep = !current.sections.sleep)
            "zikr" -> current.sections.copy(zikr = !current.sections.zikr)
            else -> current.sections
        }
        saveDay(current.copy(sections = sections))
    }

    fun incrementCounter(section: String, itemId: String, target: Int) {
        val current = selectedDayRecord.value
        val key = "$section:$itemId"
        val currentCount = current.counters[key] ?: 0
        if (currentCount >= target) return

        val newCount = currentCount + 1
        val newCounters = current.counters.toMutableMap().apply { put(key, newCount) }

        // Check if all items in the section are now complete
        val isSectionCompleted = checkSectionCompletion(section, newCounters)
        val sections = when (section.lowercase()) {
            "wake" -> current.sections.copy(wake = isSectionCompleted)
            "morning" -> current.sections.copy(morning = isSectionCompleted)
            "evening" -> current.sections.copy(evening = isSectionCompleted)
            "sleep" -> current.sections.copy(sleep = isSectionCompleted)
            "zikr" -> current.sections.copy(zikr = isSectionCompleted)
            else -> current.sections
        }

        saveDay(current.copy(counters = newCounters, sections = sections))
    }

    fun resetCounter(section: String, itemId: String) {
        val current = selectedDayRecord.value
        val key = "$section:$itemId"
        val newCounters = current.counters.toMutableMap().apply { remove(key) }
        val isSectionCompleted = checkSectionCompletion(section, newCounters)
        val sections = when (section.lowercase()) {
            "wake" -> current.sections.copy(wake = isSectionCompleted)
            "morning" -> current.sections.copy(morning = isSectionCompleted)
            "evening" -> current.sections.copy(evening = isSectionCompleted)
            "sleep" -> current.sections.copy(sleep = isSectionCompleted)
            "zikr" -> current.sections.copy(zikr = isSectionCompleted)
            else -> current.sections
        }
        saveDay(current.copy(counters = newCounters, sections = sections))
    }

    private fun checkSectionCompletion(section: String, counters: Map<String, Int>): Boolean {
        val items: List<Pair<String, Int>> = when (section.lowercase()) {
            "wake" -> wakeAzkhar.map { it.id to it.target }
            "morning" -> morningAzkhar.map { it.id to it.target }
            "evening" -> eveningAzkhar.map { it.id to it.target }
            "sleep" -> sleepAzkhar.map { it.id to it.target }
            "zikr" -> activeZikrItems.value.map { it.id to it.target }
            else -> emptyList()
        }
        if (items.isEmpty()) return false
        return items.all { (id, target) ->
            val count = counters["$section:$id"] ?: 0
            count >= target
        }
    }

    private fun saveDay(day: DayRecord) {
        viewModelScope.launch {
            repository.saveDay(day)
        }
    }

    // ----------------- CUSTOM ZIKR -----------------

    fun addCustomZikr(text: String, target: Int) {
        viewModelScope.launch {
            val id = "custom_" + UUID.randomUUID().toString().take(8)
            val order = (activeZikrItems.value.maxOfOrNull { it.order } ?: 0) + 1
            repository.saveZikrItem(ZikrItem(id, text, target, order, isDefault = false, archived = false))
        }
    }

    fun updateCustomZikr(item: ZikrItem) {
        viewModelScope.launch {
            repository.saveZikrItem(item)
        }
    }

    fun deleteCustomZikr(id: String) {
        viewModelScope.launch {
            repository.deleteZikrItem(id)
        }
    }

    // ----------------- SETTINGS MUTATIONS -----------------

    fun updateLanguage(lang: String) {
        val cur = settings.value
        saveSettings(cur.copy(language = lang))
    }

    fun updateCalculationMethod(method: String) {
        val cur = settings.value
        saveSettings(cur.copy(calcMethod = method))
    }

    fun selectDistrict(district: District) {
        val cur = settings.value
        saveSettings(
            cur.copy(
                locationMode = "district",
                district = district.nameEn,
                lat = district.lat,
                lng = district.lng
            )
        )
    }

    fun toggleAlarm(key: String, enabled: Boolean) {
        val cur = settings.value
        val toggles = cur.alarmToggles.toMutableMap().apply { put(key, enabled) }
        saveSettings(cur.copy(alarmToggles = toggles))
    }

    fun updateIstigfarConfig(config: IstigfarDurudConfig) {
        val cur = settings.value
        saveSettings(cur.copy(istigfar = config))
    }

    fun updateDurudConfig(config: IstigfarDurudConfig) {
        val cur = settings.value
        saveSettings(cur.copy(durud = config))
    }

    fun updateQuietHours(start: String, end: String) {
        val cur = settings.value
        saveSettings(cur.copy(quietHoursStart = start, quietHoursEnd = end))
    }

    fun updateReminders(morning: Boolean, evening: Boolean) {
        val cur = settings.value
        saveSettings(cur.copy(morningReminder = morning, eveningReminder = evening))
    }

    fun saveSettings(newSettings: UserSettings) {
        viewModelScope.launch {
            repository.saveSettings(newSettings)
        }
    }

    fun updateArabicFontSize(size: Int) {
        val clamped = size.coerceIn(18, 44)
        saveSettings(settings.value.copy(arabicFontSize = clamped))
    }

    fun updateHijriOffset(offset: Int) {
        val clamped = offset.coerceIn(-2, 2)
        saveSettings(settings.value.copy(hijriOffset = clamped))
    }

    fun registerCustomSound(soundKey: String, displayName: String) {
        val updated = settings.value.customSoundNames.toMutableMap()
        updated[soundKey] = displayName
        saveSettings(settings.value.copy(customSoundNames = updated))
    }

    fun unregisterCustomSound(soundKey: String) {
        val updated = settings.value.customSoundNames.toMutableMap()
        updated.remove(soundKey)
        saveSettings(settings.value.copy(customSoundNames = updated))
    }

    fun requestGpsLocation(onResult: (Boolean) -> Unit) {
        try {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { loc: Location? ->
                    if (loc != null) {
                        applyGpsLocation(loc)
                        onResult(true)
                    } else {
                        // Fallback to last known location
                        fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc: Location? ->
                            if (lastLoc != null) {
                                applyGpsLocation(lastLoc)
                                onResult(true)
                            } else {
                                onResult(false)
                            }
                        }.addOnFailureListener { onResult(false) }
                    }
                }.addOnFailureListener {
                    fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc: Location? ->
                        if (lastLoc != null) {
                            applyGpsLocation(lastLoc)
                            onResult(true)
                        } else {
                            onResult(false)
                        }
                    }.addOnFailureListener { onResult(false) }
                }
        } catch (e: SecurityException) {
            onResult(false)
        }
    }

    private fun applyGpsLocation(loc: Location) {
        val cur = settings.value
        val nearest = districts.minByOrNull { d ->
            val dLat = d.lat - loc.latitude
            val dLng = d.lng - loc.longitude
            dLat * dLat + dLng * dLng
        }
        val isWithinBangladesh = loc.latitude in 20.0..27.0 && loc.longitude in 88.0..93.0
        val effectiveLat = if (isWithinBangladesh) loc.latitude else (nearest?.lat ?: 23.8103)
        val effectiveLng = if (isWithinBangladesh) loc.longitude else (nearest?.lng ?: 90.4125)
        val districtLabel = if (nearest != null) {
            "${nearest.nameEn} (GPS)"
        } else {
            "GPS (${String.format(Locale.US, "%.2f, %.2f", effectiveLat, effectiveLng)})"
        }
        saveSettings(
            cur.copy(
                locationMode = "gps",
                lat = effectiveLat,
                lng = effectiveLng,
                district = districtLabel
            )
        )
    }

    // ----------------- AUTH & ACCOUNT -----------------

    fun signInWithEmail(email: String, pass: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = repository.signInWithEmail(email, pass)
            if (result.isSuccess) {
                onResult(true, null)
            } else {
                onResult(false, result.exceptionOrNull()?.message ?: "Sign in failed")
            }
        }
    }

    fun registerWithEmail(email: String, pass: String, displayName: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = repository.registerWithEmail(email, pass, displayName)
            if (result.isSuccess) {
                onResult(true, null)
            } else {
                onResult(false, result.exceptionOrNull()?.message ?: "Registration failed")
            }
        }
    }

    fun updateUserName(newName: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = repository.updateUserName(newName)
            if (res.isSuccess) {
                onResult(true, null)
            } else {
                onResult(false, res.exceptionOrNull()?.message)
            }
        }
    }

    fun signOut() {
        repository.signOut()
    }

    fun deleteAccount(onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = repository.deleteUserAccount()
            if (res.isSuccess) {
                onResult(true, null)
            } else {
                onResult(false, res.exceptionOrNull()?.message)
            }
        }
    }

    // ----------------- CLOUD SYNC & BACKUP -----------------

    fun syncAllToCloud(onResult: (Boolean, Int, String?) -> Unit) {
        viewModelScope.launch {
            val result = repository.syncAllToCloud()
            if (result.isSuccess) {
                onResult(true, result.getOrDefault(0), null)
            } else {
                onResult(false, 0, result.exceptionOrNull()?.message)
            }
        }
    }

    fun checkFirebaseConnection(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val (ok, status) = repository.checkFirebaseConnection()
            onResult(ok, status)
        }
    }

    fun exportBackupFile(onResult: (java.io.File?) -> Unit) {
        viewModelScope.launch {
            try {
                val json = com.example.data.BackupManager.createBackupJson(getApplication())
                val file = com.example.data.BackupManager.saveBackupToCache(getApplication(), json)
                onResult(file)
            } catch (e: Exception) {
                onResult(null)
            }
        }
    }

    fun writeBackupToUri(uri: android.net.Uri, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val json = com.example.data.BackupManager.createBackupJson(getApplication())
                com.example.data.BackupManager.writeBackupToUri(getApplication(), uri, json)
                onResult(true, null)
            } catch (e: Exception) {
                onResult(false, e.message)
            }
        }
    }

    fun restoreBackupFromUri(uri: android.net.Uri, onResult: (Boolean, Int, String?) -> Unit) {
        viewModelScope.launch {
            val res = com.example.data.BackupManager.restoreBackupFromUri(getApplication(), uri)
            if (res.isSuccess) {
                onResult(true, res.getOrDefault(0), null)
            } else {
                onResult(false, 0, res.exceptionOrNull()?.message)
            }
        }
    }

    // ----------------- STREAKS CALCULATION -----------------

    private fun calculateCurrentStreak(days: List<DayRecord>): Int {
        if (days.isEmpty()) return 0
        val dayMap = days.associateBy { it.date }

        val cal = Calendar.getInstance().apply {
            timeZone = TimeZone.getTimeZone("Asia/Dhaka")
        }

        var streak = 0
        // Check today first. If today is not perfect yet, check starting from yesterday
        val todayStr = dateFormat.format(cal.time)
        val todayRecord = dayMap[todayStr]

        if (todayRecord?.perfect == true) {
            streak++
            cal.add(Calendar.DAY_OF_MONTH, -1)
        } else {
            // Check yesterday
            cal.add(Calendar.DAY_OF_MONTH, -1)
            val yRecord = dayMap[dateFormat.format(cal.time)]
            if (yRecord?.perfect != true) {
                return 0
            }
        }

        while (true) {
            val dStr = dateFormat.format(cal.time)
            val r = dayMap[dStr]
            if (r?.perfect == true) {
                if (dStr != todayStr) streak++
                cal.add(Calendar.DAY_OF_MONTH, -1)
            } else {
                break
            }
        }
        return streak
    }

    private fun calculateLongestStreak(days: List<DayRecord>): Int {
        if (days.isEmpty()) return 0
        val perfectDates = days.filter { it.perfect }.map { it.date }.sorted()
        if (perfectDates.isEmpty()) return 0

        var maxStreak = 1
        var curStreak = 1

        val cal = Calendar.getInstance().apply {
            timeZone = TimeZone.getTimeZone("Asia/Dhaka")
        }

        for (i in 0 until perfectDates.size - 1) {
            val d1 = runCatching { dateFormat.parse(perfectDates[i]) }.getOrNull()
            val d2 = runCatching { dateFormat.parse(perfectDates[i + 1]) }.getOrNull()
            if (d1 != null && d2 != null) {
                cal.time = d1
                cal.add(Calendar.DAY_OF_MONTH, 1)
                val expectedNext = dateFormat.format(cal.time)
                if (expectedNext == perfectDates[i + 1]) {
                    curStreak++
                    if (curStreak > maxStreak) {
                        maxStreak = curStreak
                    }
                } else {
                    curStreak = 1
                }
            }
        }
        return maxStreak
    }
}
