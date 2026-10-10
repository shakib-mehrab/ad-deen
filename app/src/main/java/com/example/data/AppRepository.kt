package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.DayRecordEntity
import com.example.data.local.UserSettingsEntity
import com.example.data.local.ZikrItemEntity
import com.example.model.*
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class AppUser(
    val uid: String,
    val email: String,
    val displayName: String,
    val isCloudConnected: Boolean
) {
    val effectiveName: String
        get() = displayName.trim().ifBlank {
            val nameFromEmail = email.substringBefore("@").replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            nameFromEmail.ifBlank { "Account" }
        }
}

class AppRepository(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val dayDao = db.dayRecordDao()
    private val zikrDao = db.zikrItemDao()
    private val settingsDao = db.userSettingsDao()

    private val authPrefs: SharedPreferences = context.getSharedPreferences("addin_auth_prefs", Context.MODE_PRIVATE)

    private val scope = CoroutineScope(Dispatchers.IO)

    private var firebaseAuth: FirebaseAuth? = null
    private var firestore: FirebaseFirestore? = null

    val isFirebaseInitialized = MutableStateFlow(false)
    val firebaseStatus = MutableStateFlow("Local-First Mode")
    val currentUserAccount = MutableStateFlow<AppUser?>(null)
    val lastSyncTime = MutableStateFlow<Long>(authPrefs.getLong("last_sync_time", 0L))

    init {
        initFirebaseIfAvailable()
        loadLocalSession()
        initializeDefaultZikrItems()
    }

    private fun initFirebaseIfAvailable() {
        try {
            // Check if default FirebaseApp exists or initialize with correct options
            val app = if (FirebaseApp.getApps(context).isEmpty()) {
                val resourceOptions = runCatching { FirebaseOptions.fromResource(context) }.getOrNull()
                val options = resourceOptions ?: FirebaseOptions.Builder()
                    .setProjectId("addin-5552a")
                    .setApplicationId("1:258983954163:android:e2c3606142784352f51986")
                    .setApiKey("AIzaSyA3Bq6pE8WXkkGYxekZ24Ufk1cPBwch1Ps")
                    .setGcmSenderId("258983954163")
                    .setStorageBucket("addin-5552a.firebasestorage.app")
                    .build()
                FirebaseApp.initializeApp(context, options)
            } else {
                FirebaseApp.getInstance()
            }

            val auth = FirebaseAuth.getInstance(app)
            val fs = FirebaseFirestore.getInstance(app)

            // Enable Firestore persistent offline cache
            try {
                val settings = FirebaseFirestoreSettings.Builder()
                    .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                    .build()
                fs.firestoreSettings = settings
            } catch (e: Exception) {
                Log.w("AppRepository", "Firestore settings: ${e.message}")
            }

            firebaseAuth = auth
            firestore = fs
            isFirebaseInitialized.value = true
            firebaseStatus.value = "Firebase Connected"

            auth.addAuthStateListener { updatedAuth ->
                val fbUser = updatedAuth.currentUser
                if (fbUser != null) {
                    scope.launch {
                        var resolvedName = fbUser.displayName
                        if (resolvedName.isNullOrBlank()) {
                            val fs = firestore
                            if (fs != null) {
                                try {
                                    val profileDoc = fs.collection("users").document(fbUser.uid).collection("profile").document("main").get().await()
                                    resolvedName = profileDoc.getString("displayName")
                                } catch (e: Exception) {
                                    Log.w("AppRepository", "Profile read on authState: ${e.message}")
                                }
                            }
                        }
                        if (resolvedName.isNullOrBlank()) {
                            resolvedName = authPrefs.getString("user_name", null)
                        }
                        if (resolvedName.isNullOrBlank()) {
                            resolvedName = fbUser.email?.substringBefore("@")?.replaceFirstChar { it.uppercase() } ?: "User"
                        }

                        val appUser = AppUser(
                            uid = fbUser.uid,
                            email = fbUser.email ?: "user@addin.app",
                            displayName = resolvedName,
                            isCloudConnected = true
                        )
                        currentUserAccount.value = appUser
                        saveLocalSession(appUser)
                        attachFirestoreListeners(fbUser.uid)

                        // Auto-sync prayer history from cloud on auth state detection
                        syncAllToCloud()
                    }
                } else {
                    // Check if previous session was cloud connected
                    val wasCloud = authPrefs.getBoolean("user_is_cloud", false)
                    val localUid = authPrefs.getString("user_uid", "")
                    if (wasCloud || localUid?.startsWith("local_") == true) {
                        clearLocalSession()
                        currentUserAccount.value = null
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AppRepository", "Firebase init error: ${e.message}", e)
            isFirebaseInitialized.value = false
            firebaseStatus.value = "Firebase Unavailable"
        }
    }

    private fun loadLocalSession() {
        val uid = authPrefs.getString("user_uid", null)
        val email = authPrefs.getString("user_email", null)
        val name = authPrefs.getString("user_name", null)
        val isCloud = authPrefs.getBoolean("user_is_cloud", false)

        // Clear mock local_ fallback accounts
        if (uid != null && uid.startsWith("local_")) {
            clearLocalSession()
            currentUserAccount.value = null
            return
        }

        if (uid != null && email != null) {
            currentUserAccount.value = AppUser(
                uid = uid,
                email = email,
                displayName = name ?: email.substringBefore("@"),
                isCloudConnected = isCloud
            )
            if (isCloud && firestore != null) {
                attachFirestoreListeners(uid)
            }
        }
    }

    private fun saveLocalSession(user: AppUser) {
        authPrefs.edit()
            .putString("user_uid", user.uid)
            .putString("user_email", user.email)
            .putString("user_name", user.displayName)
            .putBoolean("user_is_cloud", user.isCloudConnected)
            .apply()
    }

    private fun clearLocalSession() {
        authPrefs.edit().clear().apply()
    }

    private fun attachFirestoreListeners(uid: String) {
        val fs = firestore ?: return

        // Settings listener
        fs.collection("users").document(uid).collection("settings").document("main")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                scope.launch {
                    val data = snapshot.data ?: return@launch
                    val current = getSettingsOnce()
                    val updated = current.copy(
                        language = (data["language"] as? String) ?: current.language,
                        calcMethod = (data["calcMethod"] as? String) ?: current.calcMethod,
                        locationMode = (data["locationMode"] as? String) ?: current.locationMode,
                        lat = (data["lat"] as? Number)?.toDouble() ?: current.lat,
                        lng = (data["lng"] as? Number)?.toDouble() ?: current.lng,
                        district = (data["district"] as? String) ?: current.district
                    )
                    saveSettingsLocal(updated)
                }
            }

        // Days collection listener with smart merge
        fs.collection("users").document(uid).collection("days")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                scope.launch {
                    for (doc in snapshot.documents) {
                        val date = doc.id
                        val data = doc.data ?: continue
                        val prayersMap = data["prayers"] as? Map<String, Any?>
                        val naflMap = data["nafl"] as? Map<String, Any?>
                        val sectionsMap = data["sections"] as? Map<String, Any?>
                        val countersMap = (data["counters"] as? Map<*, *>)?.mapNotNull { (k, v) ->
                            val keyStr = k as? String
                            val valInt = (v as? Number)?.toInt()
                            if (keyStr != null && valInt != null) keyStr to valInt else null
                        }?.toMap() ?: emptyMap()

                        val prayers = DayPrayers.fromMap(prayersMap)
                        val nafl = DayNafl.fromMap(naflMap)
                        val sections = DaySections.fromMap(sectionsMap)
                        val cloudScore = (data["score"] as? Number)?.toInt() ?: 0
                        val cloudPerfect = (data["perfect"] as? Boolean) ?: false
                        val cloudQuran = (data["quran"] as? Boolean) == true
                        val cloudScoreVersion = (data["scoreVersion"] as? Number)?.toInt() ?: 2

                        val local = dayDao.getDayByDateOnce(date)
                        if (local == null) {
                            val entity = DayRecordEntity(
                                date = date,
                                fajr = prayers.fajr.code,
                                dhuhr = prayers.dhuhr.code,
                                asr = prayers.asr.code,
                                maghrib = prayers.maghrib.code,
                                isha = prayers.isha.code,
                                witr = nafl.witr,
                                tahajjud = nafl.tahajjud,
                                duha = nafl.duha,
                                quran = cloudQuran,
                                scoreVersion = cloudScoreVersion,
                                wake = sections.wake,
                                morning = sections.morning,
                                evening = sections.evening,
                                sleep = sections.sleep,
                                zikr = sections.zikr,
                                countersJson = mapToJson(countersMap),
                                score = cloudScore,
                                perfect = cloudPerfect,
                                updatedAt = System.currentTimeMillis()
                            )
                            dayDao.insertOrUpdate(entity)
                        } else {
                            val mergedPrayers = DayPrayers(
                                fajr = if (local.fajr != "none" && local.fajr.isNotBlank()) PrayerStatus.fromCode(local.fajr) else prayers.fajr,
                                dhuhr = if (local.dhuhr != "none" && local.dhuhr.isNotBlank()) PrayerStatus.fromCode(local.dhuhr) else prayers.dhuhr,
                                asr = if (local.asr != "none" && local.asr.isNotBlank()) PrayerStatus.fromCode(local.asr) else prayers.asr,
                                maghrib = if (local.maghrib != "none" && local.maghrib.isNotBlank()) PrayerStatus.fromCode(local.maghrib) else prayers.maghrib,
                                isha = if (local.isha != "none" && local.isha.isNotBlank()) PrayerStatus.fromCode(local.isha) else prayers.isha
                            )
                            val mergedNafl = DayNafl(
                                witr = local.witr || nafl.witr,
                                tahajjud = local.tahajjud || nafl.tahajjud,
                                duha = local.duha || nafl.duha
                            )
                            val mergedQuran = local.quran || cloudQuran
                            val mergedScoreVersion = maxOf(local.scoreVersion, cloudScoreVersion)
                            val mergedSections = DaySections(
                                wake = local.wake || sections.wake,
                                morning = local.morning || sections.morning,
                                evening = local.evening || sections.evening,
                                sleep = local.sleep || sections.sleep,
                                zikr = local.zikr || sections.zikr
                            )
                            val localCounters = jsonToMap(local.countersJson)
                            val mergedCounters = (countersMap.keys + localCounters.keys).associateWith { k ->
                                maxOf(countersMap[k] ?: 0, localCounters[k] ?: 0)
                            }
                            val (s, p) = DayRecord.calculateScore(mergedPrayers, mergedNafl, mergedSections, mergedQuran, mergedScoreVersion)

                            dayDao.insertOrUpdate(
                                local.copy(
                                    fajr = mergedPrayers.fajr.code,
                                    dhuhr = mergedPrayers.dhuhr.code,
                                    asr = mergedPrayers.asr.code,
                                    maghrib = mergedPrayers.maghrib.code,
                                    isha = mergedPrayers.isha.code,
                                    witr = mergedNafl.witr,
                                    tahajjud = mergedNafl.tahajjud,
                                    duha = mergedNafl.duha,
                                    quran = mergedQuran,
                                    scoreVersion = mergedScoreVersion,
                                    wake = mergedSections.wake,
                                    morning = mergedSections.morning,
                                    evening = mergedSections.evening,
                                    sleep = mergedSections.sleep,
                                    zikr = mergedSections.zikr,
                                    countersJson = mapToJson(mergedCounters),
                                    score = maxOf(s, cloudScore),
                                    perfect = p || cloudPerfect,
                                    updatedAt = System.currentTimeMillis()
                                )
                            )
                        }
                    }
                }
            }
    }

    private fun initializeDefaultZikrItems() {
        scope.launch {
            val count = zikrDao.getAllItems().firstOrNull()?.size ?: 0
            if (count == 0) {
                val defaults = listOf(
                    ZikrItemEntity("def_1", "Subhanallah (سُبْحَانَ اللَّهِ)", 33, 1, true, false),
                    ZikrItemEntity("def_2", "Alhamdulillah (الْحَمْدُ لِلَّهِ)", 33, 2, true, false),
                    ZikrItemEntity("def_3", "Allahu Akbar (اللَّهُ أَكْبَرُ)", 34, 3, true, false),
                    ZikrItemEntity("def_4", "La ilaha illallah (لَا إِلَهَ إِلَّا اللَّهُ)", 100, 4, true, false),
                    ZikrItemEntity("def_5", "Astaghfirullah (أَسْتَغْفِرُ اللَّهَ)", 100, 5, true, false),
                    ZikrItemEntity("def_6", "Subhanallahi wa bihamdihi (سُبْحَانَ اللَّهِ وَبِحَمْدِهِ)", 100, 6, true, false)
                )
                zikrDao.insertAll(defaults)
            }
        }
    }

    // ----------------- DAYS / TRACKER -----------------

    fun getAllDays(): Flow<List<DayRecord>> {
        return dayDao.getAllDays().map { list ->
            list.map { it.toModel() }
        }
    }

    fun getDay(date: String): Flow<DayRecord> {
        return dayDao.getDayByDate(date).map { entity ->
            entity?.toModel() ?: DayRecord(date = date)
        }
    }

    suspend fun saveDay(day: DayRecord) {
        withContext(Dispatchers.IO) {
            val (score, perfect) = DayRecord.calculateScore(
                day.prayers,
                day.nafl,
                day.sections,
                day.quran,
                day.scoreVersion
            )
            val updatedDay = day.copy(score = score, perfect = perfect, updatedAt = System.currentTimeMillis())

            val entity = DayRecordEntity(
                date = updatedDay.date,
                fajr = updatedDay.prayers.fajr.code,
                dhuhr = updatedDay.prayers.dhuhr.code,
                asr = updatedDay.prayers.asr.code,
                maghrib = updatedDay.prayers.maghrib.code,
                isha = updatedDay.prayers.isha.code,
                witr = updatedDay.nafl.witr,
                tahajjud = updatedDay.nafl.tahajjud,
                duha = updatedDay.nafl.duha,
                quran = updatedDay.quran,
                scoreVersion = updatedDay.scoreVersion,
                wake = updatedDay.sections.wake,
                morning = updatedDay.sections.morning,
                evening = updatedDay.sections.evening,
                sleep = updatedDay.sections.sleep,
                zikr = updatedDay.sections.zikr,
                countersJson = mapToJson(updatedDay.counters),
                score = score,
                perfect = perfect,
                updatedAt = updatedDay.updatedAt
            )
            dayDao.insertOrUpdate(entity)

            // Sync with Firestore if authenticated
            val uid = currentUserAccount.value?.uid
            val fs = firestore
            if (uid != null && fs != null) {
                try {
                    val firestoreMap = hashMapOf<String, Any>(
                        "prayers" to updatedDay.prayers.toMap(),
                        "nafl" to updatedDay.nafl.toMap(),
                        "sections" to updatedDay.sections.toMap(),
                        "quran" to updatedDay.quran,
                        "scoreVersion" to updatedDay.scoreVersion,
                        "counters" to updatedDay.counters,
                        "score" to score,
                        "perfect" to perfect,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    fs.collection("users").document(uid).collection("days").document(updatedDay.date)
                        .set(firestoreMap, SetOptions.merge())
                } catch (e: Exception) {
                    Log.w("AppRepository", "Firestore day sync queued offline: ${e.message}")
                }
            }
        }
    }

    // ----------------- ZIKR ITEMS -----------------

    fun getActiveZikrItems(): Flow<List<ZikrItem>> {
        return zikrDao.getActiveItems().map { list ->
            list.map { ZikrItem(it.id, it.text, it.target, it.orderIndex, it.isDefault, it.archived) }
        }
    }

    suspend fun saveZikrItem(item: ZikrItem) {
        withContext(Dispatchers.IO) {
            val entity = ZikrItemEntity(
                id = item.id,
                text = item.text,
                target = item.target,
                orderIndex = item.order,
                isDefault = item.isDefault,
                archived = item.archived
            )
            zikrDao.insertOrUpdate(entity)

            val uid = currentUserAccount.value?.uid
            val fs = firestore
            if (uid != null && fs != null) {
                try {
                    val map = hashMapOf(
                        "text" to item.text,
                        "target" to item.target,
                        "order" to item.order,
                        "isDefault" to item.isDefault,
                        "archived" to item.archived
                    )
                    fs.collection("users").document(uid).collection("zikrItems").document(item.id)
                        .set(map, SetOptions.merge())
                } catch (e: Exception) {
                    Log.w("AppRepository", "Firestore zikr sync queued offline: ${e.message}")
                }
            }
        }
    }

    suspend fun deleteZikrItem(id: String) {
        withContext(Dispatchers.IO) {
            zikrDao.deleteItem(id)
            val uid = currentUserAccount.value?.uid
            val fs = firestore
            if (uid != null && fs != null) {
                try {
                    fs.collection("users").document(uid).collection("zikrItems").document(id).delete()
                } catch (e: Exception) {
                    Log.w("AppRepository", "Firestore zikr delete offline: ${e.message}")
                }
            }
        }
    }

    // ----------------- USER SETTINGS -----------------

    fun getSettings(): Flow<UserSettings> {
        return settingsDao.getSettings().map { entity ->
            entity?.toModel() ?: UserSettings()
        }
    }

    suspend fun getSettingsOnce(): UserSettings {
        return withContext(Dispatchers.IO) {
            settingsDao.getSettingsOnce()?.toModel() ?: UserSettings()
        }
    }

    suspend fun saveSettings(settings: UserSettings) {
        saveSettingsLocal(settings)

        val uid = currentUserAccount.value?.uid
        val fs = firestore
        if (uid != null && fs != null) {
            withContext(Dispatchers.IO) {
                try {
                    val map = hashMapOf<String, Any>(
                        "language" to settings.language,
                        "calcMethod" to settings.calcMethod,
                        "locationMode" to settings.locationMode,
                        "lat" to settings.lat,
                        "lng" to settings.lng,
                        "district" to settings.district,
                        "alarmToggles" to settings.alarmToggles,
                        "azkharReminders" to mapOf(
                            "morning" to settings.morningReminder,
                            "evening" to settings.eveningReminder
                        ),
                        "istigfar" to mapOf(
                            "mode" to settings.istigfar.mode,
                            "customIntervalMinutes" to settings.istigfar.customIntervalMinutes,
                            "timesPerDay" to settings.istigfar.timesPerDay,
                            "specificTimes" to settings.istigfar.specificTimes,
                            "enabled" to settings.istigfar.enabled
                        ),
                        "durud" to mapOf(
                            "mode" to settings.durud.mode,
                            "customIntervalMinutes" to settings.durud.customIntervalMinutes,
                            "timesPerDay" to settings.durud.timesPerDay,
                            "specificTimes" to settings.durud.specificTimes,
                            "enabled" to settings.durud.enabled
                        ),
                        "quietHours" to mapOf(
                            "start" to settings.quietHoursStart,
                            "end" to settings.quietHoursEnd
                        )
                    )
                    fs.collection("users").document(uid).collection("settings").document("main")
                        .set(map, SetOptions.merge())
                } catch (e: Exception) {
                    Log.w("AppRepository", "Firestore settings save offline: ${e.message}")
                }
            }
        }
    }

    private suspend fun saveSettingsLocal(settings: UserSettings) {
        withContext(Dispatchers.IO) {
            val entity = UserSettingsEntity(
                id = 1,
                language = settings.language,
                calcMethod = settings.calcMethod,
                locationMode = settings.locationMode,
                lat = settings.lat,
                lng = settings.lng,
                district = settings.district,
                alarmTogglesJson = mapToJson(settings.alarmToggles),
                morningReminder = settings.morningReminder,
                eveningReminder = settings.eveningReminder,
                istigfarMode = settings.istigfar.mode,
                istigfarCustomMinutes = settings.istigfar.customIntervalMinutes,
                istigfarTimesPerDay = settings.istigfar.timesPerDay,
                istigfarSpecificTimesJson = listToJson(settings.istigfar.specificTimes),
                istigfarEnabled = settings.istigfar.enabled,
                durudMode = settings.durud.mode,
                durudCustomMinutes = settings.durud.customIntervalMinutes,
                durudTimesPerDay = settings.durud.timesPerDay,
                durudSpecificTimesJson = listToJson(settings.durud.specificTimes),
                durudEnabled = settings.durud.enabled,
                quietHoursStart = settings.quietHoursStart,
                quietHoursEnd = settings.quietHoursEnd,
                hijriOffset = settings.hijriOffset,
                arabicFontSize = settings.arabicFontSize,
                customSoundNamesJson = mapToJson(settings.customSoundNames)
            )
            settingsDao.saveSettings(entity)
        }
    }

    // ----------------- CLOUD SYNC & AUTHENTICATION -----------------

    suspend fun syncAllToCloud(): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val user = currentUserAccount.value ?: throw IllegalStateException("Please sign in first")
            val uid = user.uid
            val fs = firestore ?: throw IllegalStateException("Firestore is not available")

            var totalSyncedDays = 0

            // 1. PULL down all cloud days from Firestore into local Room database
            try {
                val cloudSnapshot = fs.collection("users").document(uid).collection("days").get().await()
                for (doc in cloudSnapshot.documents) {
                    val date = doc.id
                    val data = doc.data ?: continue
                    val prayersMap = data["prayers"] as? Map<String, Any?>
                    val naflMap = data["nafl"] as? Map<String, Any?>
                    val sectionsMap = data["sections"] as? Map<String, Any?>
                    val cloudCounters = (data["counters"] as? Map<*, *>)?.mapNotNull { (k, v) ->
                        val keyStr = k as? String
                        val valInt = (v as? Number)?.toInt()
                        if (keyStr != null && valInt != null) keyStr to valInt else null
                    }?.toMap() ?: emptyMap()

                    val cloudPrayers = DayPrayers.fromMap(prayersMap)
                    val cloudNafl = DayNafl.fromMap(naflMap)
                    val cloudSections = DaySections.fromMap(sectionsMap)
                    val cloudScore = (data["score"] as? Number)?.toInt() ?: 0
                    val cloudPerfect = (data["perfect"] as? Boolean) ?: false

                    val localEntity = dayDao.getDayByDateOnce(date)
                    if (localEntity == null) {
                        val newEntity = DayRecordEntity(
                            date = date,
                            fajr = cloudPrayers.fajr.code,
                            dhuhr = cloudPrayers.dhuhr.code,
                            asr = cloudPrayers.asr.code,
                            maghrib = cloudPrayers.maghrib.code,
                            isha = cloudPrayers.isha.code,
                            witr = cloudNafl.witr,
                            tahajjud = cloudNafl.tahajjud,
                            duha = cloudNafl.duha,
                            wake = cloudSections.wake,
                            morning = cloudSections.morning,
                            evening = cloudSections.evening,
                            sleep = cloudSections.sleep,
                            zikr = cloudSections.zikr,
                            countersJson = mapToJson(cloudCounters),
                            score = cloudScore,
                            perfect = cloudPerfect,
                            updatedAt = System.currentTimeMillis()
                        )
                        dayDao.insertOrUpdate(newEntity)
                        totalSyncedDays++
                    } else {
                        // Merge local and cloud record intelligently:
                        val mergedPrayers = DayPrayers(
                            fajr = if (localEntity.fajr != "none" && localEntity.fajr.isNotBlank()) PrayerStatus.fromCode(localEntity.fajr) else cloudPrayers.fajr,
                            dhuhr = if (localEntity.dhuhr != "none" && localEntity.dhuhr.isNotBlank()) PrayerStatus.fromCode(localEntity.dhuhr) else cloudPrayers.dhuhr,
                            asr = if (localEntity.asr != "none" && localEntity.asr.isNotBlank()) PrayerStatus.fromCode(localEntity.asr) else cloudPrayers.asr,
                            maghrib = if (localEntity.maghrib != "none" && localEntity.maghrib.isNotBlank()) PrayerStatus.fromCode(localEntity.maghrib) else cloudPrayers.maghrib,
                            isha = if (localEntity.isha != "none" && localEntity.isha.isNotBlank()) PrayerStatus.fromCode(localEntity.isha) else cloudPrayers.isha
                        )
                        val mergedNafl = DayNafl(
                            witr = localEntity.witr || cloudNafl.witr,
                            tahajjud = localEntity.tahajjud || cloudNafl.tahajjud,
                            duha = localEntity.duha || cloudNafl.duha
                        )
                        val mergedSections = DaySections(
                            wake = localEntity.wake || cloudSections.wake,
                            morning = localEntity.morning || cloudSections.morning,
                            evening = localEntity.evening || cloudSections.evening,
                            sleep = localEntity.sleep || cloudSections.sleep,
                            zikr = localEntity.zikr || cloudSections.zikr
                        )
                        val localCounters = jsonToMap(localEntity.countersJson)
                        val mergedCounters = (cloudCounters.keys + localCounters.keys).associateWith { key ->
                            maxOf(cloudCounters[key] ?: 0, localCounters[key] ?: 0)
                        }
                        val (calcScore, calcPerfect) = DayRecord.calculateScore(mergedPrayers, mergedNafl, mergedSections)
                        val mergedScore = maxOf(calcScore, cloudScore)
                        val mergedPerfect = calcPerfect || cloudPerfect

                        val updatedEntity = localEntity.copy(
                            fajr = mergedPrayers.fajr.code,
                            dhuhr = mergedPrayers.dhuhr.code,
                            asr = mergedPrayers.asr.code,
                            maghrib = mergedPrayers.maghrib.code,
                            isha = mergedPrayers.isha.code,
                            witr = mergedNafl.witr,
                            tahajjud = mergedNafl.tahajjud,
                            duha = mergedNafl.duha,
                            wake = mergedSections.wake,
                            morning = mergedSections.morning,
                            evening = mergedSections.evening,
                            sleep = mergedSections.sleep,
                            zikr = mergedSections.zikr,
                            countersJson = mapToJson(mergedCounters),
                            score = mergedScore,
                            perfect = mergedPerfect,
                            updatedAt = System.currentTimeMillis()
                        )
                        dayDao.insertOrUpdate(updatedEntity)
                        totalSyncedDays++
                    }
                }
            } catch (e: Exception) {
                Log.w("AppRepository", "Error pulling days from cloud: ${e.message}")
            }

            // 2. PUSH all local days up to Firestore so cloud is fully up to date
            val allLocalDays = dayDao.getAllDays().first()
            for (day in allLocalDays) {
                val counters = jsonToMap(day.countersJson)
                val dayMap = hashMapOf<String, Any>(
                    "prayers" to mapOf(
                        "fajr" to day.fajr,
                        "dhuhr" to day.dhuhr,
                        "asr" to day.asr,
                        "maghrib" to day.maghrib,
                        "isha" to day.isha
                    ),
                    "nafl" to mapOf(
                        "witr" to day.witr,
                        "tahajjud" to day.tahajjud,
                        "duha" to day.duha
                    ),
                    "sections" to mapOf(
                        "wake" to day.wake,
                        "morning" to day.morning,
                        "evening" to day.evening,
                        "sleep" to day.sleep,
                        "zikr" to day.zikr
                    ),
                    "counters" to counters,
                    "score" to day.score,
                    "perfect" to day.perfect,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                fs.collection("users").document(uid).collection("days").document(day.date)
                    .set(dayMap, SetOptions.merge())
            }

            val now = System.currentTimeMillis()
            lastSyncTime.value = now
            authPrefs.edit().putLong("last_sync_time", now).apply()

            maxOf(totalSyncedDays, allLocalDays.size)
        }
    }

    suspend fun checkFirebaseConnection(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val auth = firebaseAuth
        val fs = firestore
        if (auth == null || fs == null) {
            return@withContext Pair(false, "Firebase SDK not initialized")
        }
        val user = currentUserAccount.value
        if (user == null || !user.isCloudConnected) {
            return@withContext Pair(true, "Firebase reachable (Ready for Sign In)")
        }
        try {
            // Verify Firestore read for current user
            fs.collection("users").document(user.uid).collection("days").limit(1).get().await()
            Pair(true, "Firebase Connected & Authorized (${user.email})")
        } catch (e: Exception) {
            Pair(false, "Connection check: ${e.localizedMessage ?: e.message}")
        }
    }

    suspend fun signInWithEmail(email: String, pass: String): Result<AppUser> = withContext(Dispatchers.IO) {
        val auth = firebaseAuth ?: return@withContext Result.failure(Exception("Firebase is not initialized"))
        try {
            val result = auth.signInWithEmailAndPassword(email.trim(), pass).await()
            val fbUser = result.user ?: throw IllegalStateException("Firebase sign in returned no user")

            // Resolve full User Name: check fbUser.displayName, then Firestore profile, then local prefs, then email handle
            var resolvedName = fbUser.displayName
            if (resolvedName.isNullOrBlank()) {
                val fs = firestore
                if (fs != null) {
                    try {
                        val profileDoc = fs.collection("users").document(fbUser.uid).collection("profile").document("main").get().await()
                        resolvedName = profileDoc.getString("displayName")
                    } catch (e: Exception) {
                        Log.w("AppRepository", "Profile read on signIn: ${e.message}")
                    }
                }
            }
            if (resolvedName.isNullOrBlank()) {
                resolvedName = authPrefs.getString("user_name", null)
            }
            if (resolvedName.isNullOrBlank()) {
                resolvedName = email.trim().substringBefore("@").replaceFirstChar { it.uppercase() }
            }

            // Sync back to Firebase Auth profile if not set
            if (fbUser.displayName.isNullOrBlank() && !resolvedName.isNullOrBlank()) {
                try {
                    val req = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                        .setDisplayName(resolvedName)
                        .build()
                    fbUser.updateProfile(req).await()
                } catch (e: Exception) {
                    Log.w("AppRepository", "updateProfile on login: ${e.message}")
                }
            }

            val appUser = AppUser(
                uid = fbUser.uid,
                email = fbUser.email ?: email.trim(),
                displayName = resolvedName,
                isCloudConnected = true
            )
            currentUserAccount.value = appUser
            saveLocalSession(appUser)
            attachFirestoreListeners(fbUser.uid)

            // Immediately synchronize prayer history from and to cloud
            syncAllToCloud()

            Result.success(appUser)
        } catch (e: Exception) {
            Log.e("AppRepository", "Firebase auth failed", e)
            val friendlyMsg = when {
                e is com.google.firebase.auth.FirebaseAuthInvalidUserException ->
                    "No account found with this email. Please switch to 'Register' below to create this account."
                e is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException ->
                    "Incorrect password or invalid email format."
                e is com.google.firebase.FirebaseNetworkException ->
                    "Network error: Unable to connect to Firebase. Check your internet connection."
                else -> e.localizedMessage ?: "Firebase sign-in failed. Please check credentials or register."
            }
            Result.failure(Exception(friendlyMsg))
        }
    }

    suspend fun registerWithEmail(email: String, pass: String, displayName: String): Result<AppUser> = withContext(Dispatchers.IO) {
        val auth = firebaseAuth ?: return@withContext Result.failure(Exception("Firebase is not initialized"))
        val effectiveName = if (displayName.isNotBlank()) displayName.trim() else email.trim().substringBefore("@").replaceFirstChar { it.uppercase() }

        try {
            val result = auth.createUserWithEmailAndPassword(email.trim(), pass).await()
            val fbUser = result.user ?: throw IllegalStateException("Firebase registration returned no user")

            try {
                val req = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                    .setDisplayName(effectiveName)
                    .build()
                fbUser.updateProfile(req).await()
            } catch (e: Exception) {
                Log.w("AppRepository", "updateProfile on register: ${e.message}")
            }

            val appUser = AppUser(
                uid = fbUser.uid,
                email = fbUser.email ?: email.trim(),
                displayName = effectiveName,
                isCloudConnected = true
            )
            currentUserAccount.value = appUser
            saveLocalSession(appUser)
            saveUserProfile(effectiveName, email.trim())
            attachFirestoreListeners(fbUser.uid)

            // Immediately synchronize initial prayer history to cloud
            syncAllToCloud()

            Result.success(appUser)
        } catch (e: Exception) {
            Log.e("AppRepository", "Firebase registration failed", e)
            val friendlyMsg = when {
                e is com.google.firebase.auth.FirebaseAuthUserCollisionException ->
                    "An account with this email already exists. Please switch to 'Sign In'."
                e is com.google.firebase.auth.FirebaseAuthWeakPasswordException ->
                    "Password is too weak. Please use at least 6 characters."
                e is com.google.firebase.FirebaseNetworkException ->
                    "Network error: Unable to connect to Firebase. Check your internet connection."
                else -> e.localizedMessage ?: "Registration failed. Please check your details."
            }
            Result.failure(Exception(friendlyMsg))
        }
    }

    suspend fun updateUserName(newName: String): Result<Unit> = withContext(Dispatchers.IO) {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) return@withContext Result.failure(IllegalArgumentException("Name cannot be empty"))
        val user = currentUserAccount.value ?: return@withContext Result.failure(IllegalStateException("Not signed in"))
        val auth = firebaseAuth
        val fs = firestore
        try {
            val fbUser = auth?.currentUser
            if (fbUser != null) {
                val req = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                    .setDisplayName(trimmed)
                    .build()
                fbUser.updateProfile(req).await()
            }
            if (fs != null) {
                fs.collection("users").document(user.uid).collection("profile").document("main")
                    .set(mapOf("displayName" to trimmed, "email" to user.email), SetOptions.merge())
            }
            val updated = user.copy(displayName = trimmed)
            currentUserAccount.value = updated
            saveLocalSession(updated)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun saveUserProfile(displayName: String, email: String) {
        val uid = currentUserAccount.value?.uid ?: return
        val fs = firestore ?: return
        withContext(Dispatchers.IO) {
            try {
                val profileMap = hashMapOf(
                    "email" to email,
                    "displayName" to displayName,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                fs.collection("users").document(uid).collection("profile").document("main")
                    .set(profileMap, SetOptions.merge())
            } catch (e: Exception) {
                Log.w("AppRepository", "Profile save offline: ${e.message}")
            }
        }
    }

    suspend fun deleteUserAccount(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val uid = currentUserAccount.value?.uid
                val fs = firestore
                val user = firebaseAuth?.currentUser

                if (uid != null && fs != null) {
                    try {
                        val daysSnapshot = fs.collection("users").document(uid).collection("days").get().await()
                        for (doc in daysSnapshot.documents) doc.reference.delete().await()

                        val zikrSnapshot = fs.collection("users").document(uid).collection("zikrItems").get().await()
                        for (doc in zikrSnapshot.documents) doc.reference.delete().await()

                        val settingsSnapshot = fs.collection("users").document(uid).collection("settings").get().await()
                        for (doc in settingsSnapshot.documents) doc.reference.delete().await()

                        val profileSnapshot = fs.collection("users").document(uid).collection("profile").get().await()
                        for (doc in profileSnapshot.documents) doc.reference.delete().await()

                        fs.collection("users").document(uid).delete().await()
                    } catch (e: Exception) {
                        Log.w("AppRepository", "Cloud records cleanup: ${e.message}")
                    }
                }

                try {
                    user?.delete()?.await()
                } catch (e: Exception) {
                    Log.w("AppRepository", "Auth user delete: ${e.message}")
                }

                // Clear local database
                dayDao.clearAll()
                zikrDao.clearAll()
                settingsDao.clearAll()
                initializeDefaultZikrItems()
                clearLocalSession()

                currentUserAccount.value = null
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e("AppRepository", "Error deleting account: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    fun signOut() {
        try {
            firebaseAuth?.signOut()
        } catch (_: Exception) {}
        clearLocalSession()
        currentUserAccount.value = null
    }

    // ----------------- HELPERS -----------------

    private fun DayRecordEntity.toModel(): DayRecord {
        val counters = jsonToMap(countersJson)
        val prayers = DayPrayers(
            fajr = PrayerStatus.fromCode(fajr),
            dhuhr = PrayerStatus.fromCode(dhuhr),
            asr = PrayerStatus.fromCode(asr),
            maghrib = PrayerStatus.fromCode(maghrib),
            isha = PrayerStatus.fromCode(isha)
        )
        val nafl = DayNafl(witr = witr, tahajjud = tahajjud, duha = duha)
        val sections = DaySections(wake = wake, morning = morning, evening = evening, sleep = sleep, zikr = zikr)
        return DayRecord(
            date = date,
            prayers = prayers,
            nafl = nafl,
            sections = sections,
            quran = quran,
            scoreVersion = scoreVersion,
            counters = counters,
            score = score,
            perfect = perfect,
            updatedAt = updatedAt
        )
    }

    private fun UserSettingsEntity.toModel(): UserSettings {
        val alarmToggles = jsonToMapBool(alarmTogglesJson).ifEmpty { UserSettings.defaultAlarmToggles() }
        val istigfarTimes = jsonToList(istigfarSpecificTimesJson).ifEmpty { listOf("09:00", "12:00", "15:00", "18:00", "21:00") }
        val durudTimes = jsonToList(durudSpecificTimesJson).ifEmpty { listOf("09:00", "12:00", "15:00", "18:00", "21:00") }

        return UserSettings(
            language = language,
            calcMethod = calcMethod,
            locationMode = locationMode,
            lat = lat,
            lng = lng,
            district = district,
            alarmToggles = alarmToggles,
            morningReminder = morningReminder,
            eveningReminder = eveningReminder,
            istigfar = IstigfarDurudConfig(
                mode = istigfarMode,
                customIntervalMinutes = istigfarCustomMinutes,
                timesPerDay = istigfarTimesPerDay,
                specificTimes = istigfarTimes,
                enabled = istigfarEnabled
            ),
            durud = IstigfarDurudConfig(
                mode = durudMode,
                customIntervalMinutes = durudCustomMinutes,
                timesPerDay = durudTimesPerDay,
                specificTimes = durudTimes,
                enabled = durudEnabled
            ),
            quietHoursStart = quietHoursStart,
            quietHoursEnd = quietHoursEnd,
            hijriOffset = hijriOffset,
            arabicFontSize = if (arabicFontSize in 18..44) arabicFontSize else 26,
            customSoundNames = jsonToMapString(customSoundNamesJson)
        )
    }

    private fun mapToJson(map: Map<String, Any?>): String {
        val obj = JSONObject()
        map.forEach { (k, v) -> obj.put(k, v) }
        return obj.toString()
    }

    private fun jsonToMapString(json: String?): Map<String, String> {
        if (json.isNullOrBlank()) return emptyMap()
        return runCatching {
            val obj = JSONObject(json)
            val map = mutableMapOf<String, String>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                map[k] = obj.getString(k)
            }
            map
        }.getOrDefault(emptyMap())
    }

    private fun jsonToMap(json: String?): Map<String, Int> {
        if (json.isNullOrBlank()) return emptyMap()
        return runCatching {
            val obj = JSONObject(json)
            val map = mutableMapOf<String, Int>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                map[k] = obj.getInt(k)
            }
            map
        }.getOrDefault(emptyMap())
    }

    private fun jsonToMapBool(json: String?): Map<String, Boolean> {
        if (json.isNullOrBlank()) return emptyMap()
        return runCatching {
            val obj = JSONObject(json)
            val map = mutableMapOf<String, Boolean>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                map[k] = obj.getBoolean(k)
            }
            map
        }.getOrDefault(emptyMap())
    }

    private fun listToJson(list: List<String>): String {
        val arr = JSONArray()
        list.forEach { arr.put(it) }
        return arr.toString()
    }

    private fun jsonToList(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                list.add(arr.getString(i))
            }
            list
        }.getOrDefault(emptyList())
    }
}
