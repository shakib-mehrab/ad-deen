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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/** One undoable tracker change: the full day record before the change. */
data class UndoEvent(val id: Long, val message: String, val before: DayRecord)

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository.getInstance(application)
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)

    /** All dates/times use the device time zone, the same one the alarms use. */
    private fun dateFormat() = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getDefault() }

    val todayDateString: String
        get() = dateFormat().format(Date())

    // Live clock ticker (1 s) drives countdowns; today's date is derived from it so it rolls over at midnight.
    val liveTimeMillis = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1000)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), System.currentTimeMillis())

    val todayDate: StateFlow<String> = flow {
        while (true) {
            emit(dateFormat().format(Date()))
            delay(30_000)
        }
    }.distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, todayDateString)

    /** Date shown in the Tracker. Today or the past only. */
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

    val selectedDayRecord: StateFlow<DayRecord> = selectedDate.flatMapLatest { repository.getDay(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, DayRecord(date = todayDateString))

    /** Always today's record (Home, Azkhar), follows the date across midnight. */
    val todayRecord: StateFlow<DayRecord> = todayDate.flatMapLatest { repository.getDay(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, DayRecord(date = todayDateString))

    private fun scheduleFor(dateStr: String, s: UserSettings): DailyPrayerSchedule {
        val tz = TimeZone.getDefault()
        val cal = Calendar.getInstance(tz)
        runCatching { dateFormat().parse(dateStr) }.getOrNull()?.let { cal.time = it }
        cal.set(Calendar.HOUR_OF_DAY, 12)
        return PrayerCalculator.calculateSchedule(cal, s.lat, s.lng, CalculationMethod.fromId(s.calcMethod), tz)
    }

    private fun nextDate(dateStr: String): String {
        val cal = Calendar.getInstance(TimeZone.getDefault())
        runCatching { dateFormat().parse(dateStr) }.getOrNull()?.let { cal.time = it }
        cal.add(Calendar.DAY_OF_MONTH, 1)
        return dateFormat().format(cal.time)
    }

    /** Today's prayer schedule (Home + Wakt screen), same engine/location/time zone as the alarms. */
    val prayerSchedule: StateFlow<DailyPrayerSchedule> = combine(todayDate, settings) { d, s -> scheduleFor(d, s) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, scheduleFor(todayDateString, UserSettings()))

    /** Tomorrow's schedule, so after Isha the "next waqt" countdown targets tomorrow, not a past window. */
    val tomorrowSchedule: StateFlow<DailyPrayerSchedule> = combine(todayDate, settings) { d, s -> scheduleFor(nextDate(d), s) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, scheduleFor(nextDate(todayDateString), UserSettings()))

    /** Night = between today's Maghrib and Fajr. Drives the time-aware theme; re-evaluated every 30 s. */
    val isNightTime: StateFlow<Boolean> = combine(
        flow { while (true) { emit(System.currentTimeMillis()); delay(30_000) } },
        prayerSchedule
    ) { now, s -> now >= s.maghrib.startTimeMillis || now < s.fajr.startTimeMillis }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val currentStreak: StateFlow<Int> = combine(allDays, todayDate) { days, today -> calculateCurrentStreak(days, today) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val longestStreak: StateFlow<Int> = allDays.map { calculateLongestStreak(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val currentUserAccount: StateFlow<com.example.data.AppUser?> = repository.currentUserAccount
    val isFirebaseInitialized: StateFlow<Boolean> = repository.isFirebaseInitialized
    val firebaseStatus: StateFlow<String> = repository.firebaseStatus
    val syncError: StateFlow<String?> = repository.syncError
    val lastSyncTime: StateFlow<Long> = repository.lastSyncTime

    private val _undoEvents = MutableSharedFlow<UndoEvent>(extraBufferCapacity = 4)
    val undoEvents: SharedFlow<UndoEvent> = _undoEvents.asSharedFlow()

    init {
        // Reschedule alarms from the stored settings whenever they change (incl. app open).
        // Collect Room directly (not the UI StateFlow) so default values are never scheduled.
        viewModelScope.launch(Dispatchers.IO) {
            repository.getSettings().distinctUntilChanged().collectLatest { s ->
                AlarmScheduler.scheduleAll(getApplication(), s)
            }
        }
        // Keep the tracker on "today" when the day rolls over while it is showing yesterday-as-today.
        viewModelScope.launch {
            var previousToday = todayDate.value
            todayDate.collect { today ->
                if (selectedDate.value == previousToday) selectedDate.value = today
                previousToday = today
            }
        }
    }

    // ----------------- DATE NAVIGATION -----------------

    fun selectDate(date: String) {
        if (date <= todayDateString) selectedDate.value = date
    }

    fun stepDate(daysOffset: Int) {
        val cal = Calendar.getInstance(TimeZone.getDefault())
        runCatching { dateFormat().parse(selectedDate.value) }.getOrNull()?.let { cal.time = it }
        cal.add(Calendar.DAY_OF_MONTH, daysOffset)
        val target = dateFormat().format(cal.time)
        if (target <= todayDateString) selectedDate.value = target
    }

    fun isDateToday(): Boolean = selectedDate.value == todayDateString

    // ----------------- TRACKER MUTATIONS -----------------

    private fun isBn() = settings.value.language == "bn"

    /** Applies a change to [date] (default: Tracker's date). Future dates are locked. Emits an Undo event when [undoMessage] is set. */
    private fun mutateDay(date: String = selectedDate.value, undoMessage: String? = null, transform: (DayRecord) -> DayRecord) {
        if (date > todayDateString) return
        viewModelScope.launch {
            val (before, after) = repository.updateDay(date, transform)
            val changed = before.prayers != after.prayers || before.nafl != after.nafl ||
                before.quran != after.quran || before.sections != after.sections || before.counters != after.counters
            if (undoMessage != null && changed) {
                _undoEvents.emit(UndoEvent(System.nanoTime(), undoMessage, before))
            }
        }
    }

    /** Restores the day exactly as it was before the last change; score/streak/charts recompute and it syncs. */
    fun undo(event: UndoEvent) {
        viewModelScope.launch { repository.updateDay(event.before.date) { event.before } }
    }

    private fun prayerLabel(key: String): String {
        val p = PrayerName.entries.firstOrNull { it.id == key.lowercase() }
        return if (isBn()) p?.titleBn ?: key else p?.titleEn ?: key
    }

    /** Tapping the already-selected status clears it back to "Not marked". */
    fun updatePrayerStatus(prayerKey: String, status: PrayerStatus) {
        val newStatus = selectedDayRecord.value.prayers.withToggled(prayerKey, status).statusOf(prayerKey) ?: return
        val label = prayerLabel(prayerKey)
        val msg = if (newStatus == PrayerStatus.NONE) {
            if (isBn()) "$label: চিহ্ন মুছে ফেলা হয়েছে" else "$label cleared"
        } else {
            val short = when (newStatus) {
                PrayerStatus.JAMAT -> if (isBn()) "জামাত" else "Jamat"
                PrayerStatus.ALONE -> if (isBn()) "একাকী" else "Alone"
                PrayerStatus.QAZA -> if (isBn()) "ক্বাযা" else "Qaza"
                else -> if (isBn()) "মিস" else "Missed"
            }
            "$label: $short"
        }
        // Applied to the freshest stored value so rapid double taps behave predictably.
        mutateDay(undoMessage = msg) { d -> d.copy(prayers = d.prayers.withToggled(prayerKey, status)) }
    }

    fun toggleNafl(type: String) {
        val cur = selectedDayRecord.value.nafl
        val (label, nowOn) = when (type.lowercase()) {
            "witr" -> (if (isBn()) "বিতর" else "Witr") to !cur.witr
            "tahajjud" -> (if (isBn()) "তাহাজ্জুদ" else "Tahajjud") to !cur.tahajjud
            "duha" -> (if (isBn()) "দুহা" else "Duha") to !cur.duha
            else -> return
        }
        mutateDay(undoMessage = doneMessage(label, nowOn)) { d ->
            d.copy(
                nafl = when (type.lowercase()) {
                    "witr" -> d.nafl.copy(witr = !d.nafl.witr)
                    "tahajjud" -> d.nafl.copy(tahajjud = !d.nafl.tahajjud)
                    else -> d.nafl.copy(duha = !d.nafl.duha)
                }
            )
        }
    }

    /** Quran Done / Not done. [onToday] = true for the Home checklist, false for the Tracker's selected date. */
    fun toggleQuran(onToday: Boolean = false) {
        val date = if (onToday) todayDateString else selectedDate.value
        val record = if (onToday) todayRecord.value else selectedDayRecord.value
        if (!record.isQuranApplicable) return
        mutateDay(date = date, undoMessage = doneMessage(if (isBn()) "কুরআন তিলাওয়াত" else "Quran", !record.quran)) { d ->
            if (d.isQuranApplicable) d.copy(quran = !d.quran) else d
        }
    }

    private fun doneMessage(label: String, nowOn: Boolean) =
        if (isBn()) "$label: ${if (nowOn) "সম্পন্ন" else "সম্পন্ন নয়"}" else "$label: ${if (nowOn) "Done" else "Not done"}"

    fun toggleSection(sectionKey: String) {
        mutateDay { d ->
            d.copy(
                sections = when (sectionKey.lowercase()) {
                    "wake" -> d.sections.copy(wake = !d.sections.wake)
                    "morning" -> d.sections.copy(morning = !d.sections.morning)
                    "evening" -> d.sections.copy(evening = !d.sections.evening)
                    "sleep" -> d.sections.copy(sleep = !d.sections.sleep)
                    "zikr" -> d.sections.copy(zikr = !d.sections.zikr)
                    else -> d.sections
                }
            )
        }
    }

    // Azkhar counters always apply to today's record (the Azkhar screen shows today).
    fun incrementCounter(section: String, itemId: String, target: Int) {
        mutateDay(date = todayDateString) { d ->
            val key = "$section:$itemId"
            val count = d.counters[key] ?: 0
            if (count >= target) return@mutateDay d
            val counters = d.counters + (key to count + 1)
            d.copy(counters = counters, sections = withSection(d.sections, section, checkSectionCompletion(section, counters)))
        }
    }

    fun resetCounter(section: String, itemId: String) {
        mutateDay(date = todayDateString) { d ->
            val counters = d.counters - "$section:$itemId"
            d.copy(counters = counters, sections = withSection(d.sections, section, checkSectionCompletion(section, counters)))
        }
    }

    private fun withSection(s: DaySections, section: String, done: Boolean) = when (section.lowercase()) {
        "wake" -> s.copy(wake = done)
        "morning" -> s.copy(morning = done)
        "evening" -> s.copy(evening = done)
        "sleep" -> s.copy(sleep = done)
        "zikr" -> s.copy(zikr = done)
        else -> s
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
        return items.all { (id, target) -> (counters["$section:$id"] ?: 0) >= target }
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
        viewModelScope.launch { repository.saveZikrItem(item) }
    }

    fun deleteCustomZikr(id: String) {
        viewModelScope.launch { repository.deleteZikrItem(id) }
    }

    // ----------------- SETTINGS MUTATIONS -----------------
    // Every mutation transforms the latest STORED settings, so two quick toggles never overwrite each other.

    private fun updateSettings(transform: (UserSettings) -> UserSettings) {
        viewModelScope.launch { repository.updateSettings(transform) }
    }

    fun updateLanguage(lang: String) = updateSettings { it.copy(language = lang) }

    fun updateCalculationMethod(method: String) = updateSettings { it.copy(calcMethod = method) }

    fun selectDistrict(district: District) = updateSettings {
        it.copy(locationMode = "district", district = district.nameEn, lat = district.lat, lng = district.lng)
    }

    /** Turning a toggle off cancels every scheduled alarm of that waqt/type (via the settings-change reschedule). */
    fun toggleAlarm(key: String, enabled: Boolean) = updateSettings { it.copy(alarmToggles = it.alarmToggles + (key to enabled)) }

    /** Saves both alarm toggles of one waqt at once (per-waqt notification sheet). */
    fun setWaqtAlarms(prayerKey: String, start: Boolean, end: Boolean) = updateSettings {
        it.copy(alarmToggles = it.alarmToggles + mapOf("start_$prayerKey" to start, "end_$prayerKey" to end))
    }

    fun updateIstigfarConfig(config: IstigfarDurudConfig) = updateSettings { it.copy(istigfar = config) }

    fun updateDurudConfig(config: IstigfarDurudConfig) = updateSettings { it.copy(durud = config) }

    fun updateQuietHours(start: String, end: String) = updateSettings { it.copy(quietHoursStart = start, quietHoursEnd = end) }

    fun updateReminders(morning: Boolean, evening: Boolean) = updateSettings { it.copy(morningReminder = morning, eveningReminder = evening) }

    fun saveSettings(newSettings: UserSettings) {
        viewModelScope.launch { repository.saveSettings(newSettings) }
    }

    fun updateArabicFontSize(size: Int) = updateSettings { it.copy(arabicFontSize = size.coerceIn(18, 44)) }

    fun updateHijriOffset(offset: Int) = updateSettings { it.copy(hijriOffset = offset.coerceIn(-2, 2)) }

    fun registerCustomSound(soundKey: String, displayName: String) =
        updateSettings { it.copy(customSoundNames = it.customSoundNames + (soundKey to displayName)) }

    fun unregisterCustomSound(soundKey: String) =
        updateSettings { it.copy(customSoundNames = it.customSoundNames - soundKey) }

    /** Posts an Istigfar or Durud reminder immediately (ignores quiet hours) so the user can check sound and text. */
    fun sendTestReminder(isIstigfar: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            com.example.receiver.AlarmReceiver.deliverReminder(getApplication(), isIstigfar, respectQuietHours = false)
        }
    }

    fun requestGpsLocation(onResult: (Boolean) -> Unit) {
        try {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { loc: Location? ->
                    if (loc != null) {
                        applyGpsLocation(loc)
                        onResult(true)
                    } else {
                        fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc: Location? ->
                            if (lastLoc != null) {
                                applyGpsLocation(lastLoc)
                                onResult(true)
                            } else onResult(false)
                        }.addOnFailureListener { onResult(false) }
                    }
                }.addOnFailureListener {
                    fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc: Location? ->
                        if (lastLoc != null) {
                            applyGpsLocation(lastLoc)
                            onResult(true)
                        } else onResult(false)
                    }.addOnFailureListener { onResult(false) }
                }
        } catch (e: SecurityException) {
            onResult(false)
        }
    }

    private fun applyGpsLocation(loc: Location) {
        val nearest = districts.minByOrNull { d ->
            val dLat = d.lat - loc.latitude
            val dLng = d.lng - loc.longitude
            dLat * dLat + dLng * dLng
        }
        val isWithinBangladesh = loc.latitude in 20.0..27.0 && loc.longitude in 88.0..93.0
        val effectiveLat = if (isWithinBangladesh) loc.latitude else (nearest?.lat ?: 23.8103)
        val effectiveLng = if (isWithinBangladesh) loc.longitude else (nearest?.lng ?: 90.4125)
        val districtLabel = if (nearest != null) "${nearest.nameEn} (GPS)"
        else "GPS (${String.format(Locale.US, "%.2f, %.2f", effectiveLat, effectiveLng)})"
        updateSettings { it.copy(locationMode = "gps", lat = effectiveLat, lng = effectiveLng, district = districtLabel) }
    }

    // ----------------- AUTH & ACCOUNT -----------------

    fun signInWithEmail(email: String, pass: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = repository.signInWithEmail(email, pass)
            onResult(result.isSuccess, result.exceptionOrNull()?.message)
        }
    }

    fun registerWithEmail(email: String, pass: String, displayName: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = repository.registerWithEmail(email, pass, displayName)
            onResult(result.isSuccess, result.exceptionOrNull()?.message)
        }
    }

    fun updateUserName(newName: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = repository.updateUserName(newName)
            onResult(res.isSuccess, res.exceptionOrNull()?.message)
        }
    }

    fun signOut() = repository.signOut()

    fun deleteAccount(onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = repository.deleteUserAccount()
            onResult(res.isSuccess, res.exceptionOrNull()?.message)
        }
    }

    // ----------------- CLOUD SYNC & BACKUP -----------------

    fun syncAllToCloud(onResult: (Boolean, Int, String?) -> Unit) {
        viewModelScope.launch {
            val result = repository.syncAllToCloud()
            onResult(result.isSuccess, result.getOrDefault(0), result.exceptionOrNull()?.message)
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
                onResult(com.example.data.BackupManager.saveBackupToCache(getApplication(), json))
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
                // Push restored data to the cloud too when signed in.
                if (currentUserAccount.value != null) repository.syncAllToCloud()
                onResult(true, res.getOrDefault(0), null)
            } else {
                onResult(false, 0, res.exceptionOrNull()?.message)
            }
        }
    }

    // ----------------- STREAKS -----------------

    private fun calculateCurrentStreak(days: List<DayRecord>, today: String): Int {
        val perfect = days.filter { it.perfect }.map { it.date }.toHashSet()
        if (perfect.isEmpty()) return 0
        val fmt = dateFormat()
        val cal = Calendar.getInstance(TimeZone.getDefault())
        runCatching { fmt.parse(today) }.getOrNull()?.let { cal.time = it }
        // Today not finished yet doesn't break the streak; start from yesterday in that case.
        if (fmt.format(cal.time) !in perfect) cal.add(Calendar.DAY_OF_MONTH, -1)
        var streak = 0
        while (fmt.format(cal.time) in perfect) {
            streak++
            cal.add(Calendar.DAY_OF_MONTH, -1)
        }
        return streak
    }

    private fun calculateLongestStreak(days: List<DayRecord>): Int {
        val perfectDates = days.filter { it.perfect }.map { it.date }.sorted()
        if (perfectDates.isEmpty()) return 0
        val fmt = dateFormat()
        val cal = Calendar.getInstance(TimeZone.getDefault())
        var maxStreak = 1
        var curStreak = 1
        for (i in 0 until perfectDates.size - 1) {
            val d1 = runCatching { fmt.parse(perfectDates[i]) }.getOrNull() ?: continue
            cal.time = d1
            cal.add(Calendar.DAY_OF_MONTH, 1)
            if (fmt.format(cal.time) == perfectDates[i + 1]) {
                curStreak++
                maxStreak = maxOf(maxStreak, curStreak)
            } else {
                curStreak = 1
            }
        }
        return maxStreak
    }
}
