package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.Mappers.toEntity
import com.example.data.Mappers.toFirestoreMap
import com.example.data.Mappers.toModel
import com.example.data.local.AppDatabase
import com.example.data.local.ZikrItemEntity
import com.example.model.*
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class AppUser(
    val uid: String,
    val email: String,
    val displayName: String,
    val isCloudConnected: Boolean
) {
    /** Display name if set, otherwise the part of the email before "@". Never a hard-coded placeholder. */
    val effectiveName: String
        get() = displayName.trim().ifBlank { email.substringBefore("@").trim() }
}

/**
 * Local-first data layer: Room is the source of truth for the UI; Firestore (users/{uid}/...)
 * mirrors it when signed in. Use [getInstance] - one instance per process keeps exactly one
 * set of Firestore listeners.
 */
class AppRepository private constructor(context: Context) {

    companion object {
        private const val TAG = "AppRepository"

        @Volatile private var INSTANCE: AppRepository? = null

        fun getInstance(context: Context): AppRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppRepository(context.applicationContext).also { INSTANCE = it }
            }

        /**
         * Last-writer-wins merge of one day. Each device stamps its edits with updatedAt;
         * the newer edit wins as a whole, so clearing a prayer or Quran also syncs.
         */
        fun mergeDay(local: DayRecord?, cloud: DayRecord): DayRecord =
            if (local == null || cloud.updatedAt > local.updatedAt) cloud else local
    }

    private val context: Context = context.applicationContext
    private val db = AppDatabase.getInstance(context)
    private val dayDao = db.dayRecordDao()
    private val zikrDao = db.zikrItemDao()
    private val settingsDao = db.userSettingsDao()

    private val authPrefs: SharedPreferences = context.getSharedPreferences("addin_auth_prefs", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val writeMutex = Mutex()

    private var firebaseAuth: FirebaseAuth? = null
    private var firestore: FirebaseFirestore? = null
    private val listeners = mutableListOf<ListenerRegistration>()
    private var listenersUid: String? = null

    val isFirebaseInitialized = MutableStateFlow(false)
    val firebaseStatus = MutableStateFlow("Local-First Mode")
    /** Last sync/listener error as a user-readable message (null when healthy). */
    val syncError = MutableStateFlow<String?>(null)
    val currentUserAccount = MutableStateFlow<AppUser?>(null)
    val lastSyncTime = MutableStateFlow(authPrefs.getLong("last_sync_time", 0L))

    init {
        initFirebase()
        initializeDefaultZikrItems()
    }

    // ----------------- FIREBASE SETUP -----------------

    private fun initFirebase() {
        try {
            // google-services plugin generates the options from app/google-services.json.
            val app = FirebaseApp.getApps(context).firstOrNull() ?: FirebaseApp.initializeApp(context)
                ?: throw IllegalStateException("google-services.json missing")
            val fs = FirebaseFirestore.getInstance(app)
            runCatching {
                fs.firestoreSettings = FirebaseFirestoreSettings.Builder()
                    .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                    .build()
            }.onFailure { Log.w(TAG, "Firestore settings already applied: ${it.message}") }
            val auth = FirebaseAuth.getInstance(app)
            firebaseAuth = auth
            firestore = fs
            isFirebaseInitialized.value = true
            firebaseStatus.value = "Firebase Connected"

            // Restore the cached account immediately so the UI does not flicker while Auth loads.
            loadCachedSession(auth)
            auth.addAuthStateListener { a -> onAuthChanged(a.currentUser) }
        } catch (e: Exception) {
            Log.e(TAG, "Firebase init error: ${e.message}", e)
            isFirebaseInitialized.value = false
            firebaseStatus.value = "Firebase Unavailable"
        }
    }

    private fun loadCachedSession(auth: FirebaseAuth) {
        val fbUser = auth.currentUser ?: run { clearLocalSession(); return }
        currentUserAccount.value = AppUser(
            uid = fbUser.uid,
            email = fbUser.email.orEmpty(),
            displayName = fbUser.displayName?.takeIf { it.isNotBlank() } ?: authPrefs.getString("user_name", null).orEmpty(),
            isCloudConnected = true
        )
    }

    private fun onAuthChanged(fbUser: com.google.firebase.auth.FirebaseUser?) {
        if (fbUser == null) {
            detachListeners()
            clearLocalSession()
            currentUserAccount.value = null
            return
        }
        scope.launch {
            val name = resolveDisplayName(fbUser)
            val user = AppUser(fbUser.uid, fbUser.email.orEmpty(), name, isCloudConnected = true)
            currentUserAccount.value = user
            saveLocalSession(user)
            attachListeners(fbUser.uid)
            // Full two-way sync only the first time this account is seen on this install;
            // after that the realtime listeners + offline queue keep both sides in step.
            if (authPrefs.getString("full_synced_uid", null) != fbUser.uid) {
                syncAllToCloud().onSuccess { authPrefs.edit().putString("full_synced_uid", fbUser.uid).apply() }
            }
        }
    }

    private suspend fun resolveDisplayName(fbUser: com.google.firebase.auth.FirebaseUser): String {
        fbUser.displayName?.takeIf { it.isNotBlank() }?.let { return it }
        runCatching {
            firestore?.collection("users")?.document(fbUser.uid)?.collection("profile")?.document("main")?.get()?.await()
                ?.getString("displayName")
        }.getOrNull()?.takeIf { it.isNotBlank() }?.let { return it }
        authPrefs.getString("user_name", null)?.takeIf { it.isNotBlank() }?.let { return it }
        return "" // AppUser.effectiveName falls back to the email handle
    }

    private fun saveLocalSession(user: AppUser) {
        authPrefs.edit()
            .putString("user_uid", user.uid)
            .putString("user_email", user.email)
            .putString("user_name", user.displayName)
            .apply()
    }

    private fun clearLocalSession() {
        // Keep full_synced_uid so signing back into the same account doesn't redo a full upload.
        val keep = authPrefs.getString("full_synced_uid", null)
        val editor = authPrefs.edit().clear()
        if (keep != null) editor.putString("full_synced_uid", keep)
        editor.apply()
    }

    private fun userDoc(uid: String) = firestore!!.collection("users").document(uid)

    private fun reportError(where: String, e: Exception?) {
        if (e == null) return
        Log.w(TAG, "$where: ${e.message}", e)
        syncError.value = friendlyFirestoreError(e)
    }

    private fun friendlyFirestoreError(e: Exception): String = when {
        e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ->
            "Cloud sync blocked: permission denied. Please sign out and sign in again."
        e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.UNAVAILABLE ->
            "You're offline. Changes are saved on this phone and will sync when you reconnect."
        e is FirebaseNetworkException -> "No internet connection. Changes will sync when you reconnect."
        else -> e.localizedMessage ?: "Cloud sync failed"
    }

    // ----------------- REALTIME LISTENERS -----------------

    @Synchronized
    private fun attachListeners(uid: String) {
        if (listenersUid == uid) return
        detachListeners()
        val fs = firestore ?: return
        listenersUid = uid
        val user = fs.collection("users").document(uid)

        listeners += user.collection("settings").document("main").addSnapshotListener { snap, err ->
            if (err != null) return@addSnapshotListener reportError("settings listener", err)
            if (snap == null || !snap.exists() || snap.metadata.hasPendingWrites()) return@addSnapshotListener
            val data = snap.data ?: return@addSnapshotListener
            scope.launch {
                writeMutex.withLock { saveSettingsLocal(applyCloudSettings(getSettingsOnce(), data)) }
                syncError.value = null
            }
        }

        listeners += user.collection("days").addSnapshotListener { snap, err ->
            if (err != null) return@addSnapshotListener reportError("days listener", err)
            if (snap == null) return@addSnapshotListener
            val changes = snap.documentChanges.filter {
                it.type != DocumentChange.Type.REMOVED && !it.document.metadata.hasPendingWrites()
            }
            if (changes.isEmpty()) return@addSnapshotListener
            scope.launch {
                writeMutex.withLock {
                    for (c in changes) {
                        val cloud = Mappers.dayFromFirestore(c.document.id, c.document.data)
                        val local = dayDao.getDayByDateOnce(cloud.date)?.toModel()
                        val merged = mergeDay(local, cloud)
                        if (merged !== local) dayDao.insertOrUpdate(merged.toEntity())
                    }
                }
                syncError.value = null
            }
        }

        listeners += user.collection("zikrItems").addSnapshotListener { snap, err ->
            if (err != null) return@addSnapshotListener reportError("zikr listener", err)
            if (snap == null) return@addSnapshotListener
            val changes = snap.documentChanges.filter { !it.document.metadata.hasPendingWrites() }
            if (changes.isEmpty()) return@addSnapshotListener
            scope.launch {
                for (c in changes) {
                    val d = c.document
                    if (c.type == DocumentChange.Type.REMOVED) {
                        zikrDao.deleteItem(d.id)
                    } else {
                        zikrDao.insertOrUpdate(
                            ZikrItemEntity(
                                id = d.id,
                                text = d.getString("text") ?: continue,
                                target = (d.getLong("target") ?: 33L).toInt(),
                                orderIndex = (d.getLong("order") ?: 0L).toInt(),
                                isDefault = d.getBoolean("isDefault") == true,
                                archived = d.getBoolean("archived") == true
                            )
                        )
                    }
                }
            }
        }
    }

    @Synchronized
    private fun detachListeners() {
        listeners.forEach { it.remove() }
        listeners.clear()
        listenersUid = null
    }

    @Suppress("UNCHECKED_CAST")
    private fun applyCloudSettings(current: UserSettings, data: Map<String, Any?>): UserSettings {
        fun cfg(raw: Any?, cur: IstigfarDurudConfig): IstigfarDurudConfig {
            val m = raw as? Map<String, Any?> ?: return cur
            return cur.copy(
                mode = m["mode"] as? String ?: cur.mode,
                customIntervalMinutes = (m["customIntervalMinutes"] as? Number)?.toInt() ?: cur.customIntervalMinutes,
                timesPerDay = (m["timesPerDay"] as? Number)?.toInt() ?: cur.timesPerDay,
                specificTimes = (m["specificTimes"] as? List<*>)?.filterIsInstance<String>() ?: cur.specificTimes,
                enabled = m["enabled"] as? Boolean ?: cur.enabled
            )
        }
        val toggles = (data["alarmToggles"] as? Map<String, Any?>)
            ?.mapNotNull { (k, v) -> (v as? Boolean)?.let { k to it } }?.toMap()
        val azkhar = data["azkharReminders"] as? Map<String, Any?>
        val quiet = data["quietHours"] as? Map<String, Any?>
        return current.copy(
            language = data["language"] as? String ?: current.language,
            calcMethod = data["calcMethod"] as? String ?: current.calcMethod,
            locationMode = data["locationMode"] as? String ?: current.locationMode,
            lat = (data["lat"] as? Number)?.toDouble() ?: current.lat,
            lng = (data["lng"] as? Number)?.toDouble() ?: current.lng,
            district = data["district"] as? String ?: current.district,
            alarmToggles = if (toggles != null) UserSettings.defaultAlarmToggles() + toggles else current.alarmToggles,
            morningReminder = azkhar?.get("morning") as? Boolean ?: current.morningReminder,
            eveningReminder = azkhar?.get("evening") as? Boolean ?: current.eveningReminder,
            istigfar = cfg(data["istigfar"], current.istigfar),
            durud = cfg(data["durud"], current.durud),
            quietHoursStart = quiet?.get("start") as? String ?: current.quietHoursStart,
            quietHoursEnd = quiet?.get("end") as? String ?: current.quietHoursEnd,
            hijriOffset = (data["hijriOffset"] as? Number)?.toInt() ?: current.hijriOffset,
            arabicFontSize = (data["arabicFontSize"] as? Number)?.toInt() ?: current.arabicFontSize
        )
    }

    private fun settingsToFirestore(s: UserSettings): Map<String, Any> = mapOf(
        "language" to s.language,
        "calcMethod" to s.calcMethod,
        "locationMode" to s.locationMode,
        "lat" to s.lat,
        "lng" to s.lng,
        "district" to s.district,
        "alarmToggles" to s.alarmToggles,
        "azkharReminders" to mapOf("morning" to s.morningReminder, "evening" to s.eveningReminder),
        "istigfar" to reminderMap(s.istigfar),
        "durud" to reminderMap(s.durud),
        "quietHours" to mapOf("start" to s.quietHoursStart, "end" to s.quietHoursEnd),
        "hijriOffset" to s.hijriOffset,
        "arabicFontSize" to s.arabicFontSize
    )

    private fun reminderMap(c: IstigfarDurudConfig) = mapOf(
        "mode" to c.mode,
        "customIntervalMinutes" to c.customIntervalMinutes,
        "timesPerDay" to c.timesPerDay,
        "specificTimes" to c.specificTimes,
        "enabled" to c.enabled
    )

    private fun initializeDefaultZikrItems() {
        scope.launch {
            if ((zikrDao.getAllItems().firstOrNull()?.size ?: 0) == 0) {
                zikrDao.insertAll(
                    listOf(
                        ZikrItemEntity("def_1", "Subhanallah (سُبْحَانَ اللَّهِ)", 33, 1, true, false),
                        ZikrItemEntity("def_2", "Alhamdulillah (الْحَمْدُ لِلَّهِ)", 33, 2, true, false),
                        ZikrItemEntity("def_3", "Allahu Akbar (اللَّهُ أَكْبَرُ)", 34, 3, true, false),
                        ZikrItemEntity("def_4", "La ilaha illallah (لَا إِلَهَ إِلَّا اللَّهُ)", 100, 4, true, false),
                        ZikrItemEntity("def_5", "Astaghfirullah (أَسْتَغْفِرُ اللَّهَ)", 100, 5, true, false),
                        ZikrItemEntity("def_6", "Subhanallahi wa bihamdihi (سُبْحَانَ اللَّهِ وَبِحَمْدِهِ)", 100, 6, true, false)
                    )
                )
            }
        }
    }

    // ----------------- DAYS / TRACKER -----------------

    fun getAllDays(): Flow<List<DayRecord>> = dayDao.getAllDays().map { list -> list.map { it.toModel() } }

    fun getDay(date: String): Flow<DayRecord> = dayDao.getDayByDate(date).map { it?.toModel() ?: DayRecord(date = date) }

    suspend fun getDayOnce(date: String): DayRecord = withContext(Dispatchers.IO) {
        dayDao.getDayByDateOnce(date)?.toModel() ?: DayRecord(date = date)
    }

    /**
     * Atomically reads the day, applies [transform], recomputes score/perfect and saves
     * (Room first, then Firestore). Returns (before, after). Serialised so rapid taps never lose an edit.
     */
    suspend fun updateDay(date: String, transform: (DayRecord) -> DayRecord): Pair<DayRecord, DayRecord> =
        withContext(Dispatchers.IO) {
            writeMutex.withLock {
                val before = dayDao.getDayByDateOnce(date)?.toModel() ?: DayRecord(date = date)
                val after = persistDay(transform(before).copy(date = date, updatedAt = System.currentTimeMillis()))
                before to after
            }
        }

    suspend fun saveDay(day: DayRecord) {
        withContext(Dispatchers.IO) {
            writeMutex.withLock { persistDay(day.copy(updatedAt = System.currentTimeMillis())) }
        }
    }

    private suspend fun persistDay(day: DayRecord): DayRecord {
        val entity = day.toEntity()
        dayDao.insertOrUpdate(entity)
        val saved = entity.toModel()
        pushDay(saved)
        return saved
    }

    private fun pushDay(day: DayRecord) {
        val uid = currentUserAccount.value?.uid ?: return
        if (firestore == null) return
        // Firestore queues the write offline and retries; doc id = date, so no duplicates.
        userDoc(uid).collection("days").document(day.date)
            .set(day.toFirestoreMap())
            .addOnFailureListener { reportError("day write ${day.date}", it) }
    }

    // ----------------- ZIKR ITEMS -----------------

    fun getActiveZikrItems(): Flow<List<ZikrItem>> =
        zikrDao.getActiveItems().map { list -> list.map { ZikrItem(it.id, it.text, it.target, it.orderIndex, it.isDefault, it.archived) } }

    suspend fun saveZikrItem(item: ZikrItem) {
        withContext(Dispatchers.IO) {
            zikrDao.insertOrUpdate(ZikrItemEntity(item.id, item.text, item.target, item.order, item.isDefault, item.archived))
            pushZikr(item.id, item.text, item.target, item.order, item.isDefault, item.archived)
        }
    }

    private fun pushZikr(id: String, text: String, target: Int, order: Int, isDefault: Boolean, archived: Boolean) {
        val uid = currentUserAccount.value?.uid ?: return
        if (firestore == null) return
        userDoc(uid).collection("zikrItems").document(id)
            .set(mapOf("text" to text, "target" to target, "order" to order, "isDefault" to isDefault, "archived" to archived))
            .addOnFailureListener { reportError("zikr write", it) }
    }

    suspend fun deleteZikrItem(id: String) {
        withContext(Dispatchers.IO) {
            zikrDao.deleteItem(id)
            val uid = currentUserAccount.value?.uid ?: return@withContext
            if (firestore == null) return@withContext
            userDoc(uid).collection("zikrItems").document(id).delete()
                .addOnFailureListener { reportError("zikr delete", it) }
        }
    }

    // ----------------- USER SETTINGS -----------------

    fun getSettings(): Flow<UserSettings> = settingsDao.getSettings().map { it?.toModel() ?: UserSettings() }

    suspend fun getSettingsOnce(): UserSettings = withContext(Dispatchers.IO) {
        settingsDao.getSettingsOnce()?.toModel() ?: UserSettings()
    }

    suspend fun saveSettings(settings: UserSettings) {
        withContext(Dispatchers.IO) { saveSettingsLocal(settings) }
        pushSettings(settings)
    }

    /** Applies [transform] to the latest stored settings and saves; avoids lost updates from stale UI copies. */
    suspend fun updateSettings(transform: (UserSettings) -> UserSettings): UserSettings {
        val updated = withContext(Dispatchers.IO) {
            writeMutex.withLock {
                transform(getSettingsOnce()).also { saveSettingsLocal(it) }
            }
        }
        pushSettings(updated)
        return updated
    }

    private fun pushSettings(settings: UserSettings) {
        val uid = currentUserAccount.value?.uid ?: return
        if (firestore == null) return
        userDoc(uid).collection("settings").document("main")
            .set(settingsToFirestore(settings), SetOptions.merge())
            .addOnFailureListener { reportError("settings write", it) }
    }

    private suspend fun saveSettingsLocal(settings: UserSettings) {
        withContext(Dispatchers.IO) { settingsDao.saveSettings(settings.toEntity()) }
    }

    // ----------------- CLOUD SYNC & AUTHENTICATION -----------------

    /** Two-way sync: pull + last-writer-wins merge every day, then push everything local. */
    suspend fun syncAllToCloud(): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val user = currentUserAccount.value ?: throw IllegalStateException("Please sign in first")
            val fs = firestore ?: throw IllegalStateException("Cloud sync is not available")
            val uid = user.uid

            val cloudDocs = try {
                fs.collection("users").document(uid).collection("days").get().await().documents
            } catch (e: Exception) {
                throw IllegalStateException(friendlyFirestoreError(e))
            }

            writeMutex.withLock {
                for (doc in cloudDocs) {
                    val data = doc.data ?: continue
                    val cloud = Mappers.dayFromFirestore(doc.id, data)
                    val local = dayDao.getDayByDateOnce(doc.id)?.toModel()
                    val merged = mergeDay(local, cloud)
                    if (merged !== local) dayDao.insertOrUpdate(merged.toEntity())
                }
            }

            val allLocalDays = dayDao.getAllDays().first().map { it.toModel() }
            val batchLimit = 400
            allLocalDays.chunked(batchLimit).forEach { chunk ->
                val batch = fs.batch()
                chunk.forEach { day -> batch.set(fs.collection("users").document(uid).collection("days").document(day.date), day.toFirestoreMap()) }
                batch.commit().await()
            }
            zikrDao.getAllItems().first().forEach { z -> pushZikr(z.id, z.text, z.target, z.orderIndex, z.isDefault, z.archived) }
            pushSettings(getSettingsOnce())
            ensureProfile(uid, user.email, user.displayName)

            val now = System.currentTimeMillis()
            lastSyncTime.value = now
            authPrefs.edit().putLong("last_sync_time", now).apply()
            syncError.value = null
            allLocalDays.size
        }.onFailure { syncError.value = it.message }
    }

    private fun ensureProfile(uid: String, email: String, displayName: String) {
        val data = mutableMapOf<String, Any>("email" to email)
        if (displayName.isNotBlank()) data["displayName"] = displayName
        userDoc(uid).collection("profile").document("main").set(data, SetOptions.merge())
            .addOnFailureListener { reportError("profile write", it) }
    }

    suspend fun checkFirebaseConnection(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val fs = firestore ?: return@withContext Pair(false, "Firebase SDK not initialized")
        val user = currentUserAccount.value ?: return@withContext Pair(true, "Firebase ready (sign in to sync)")
        try {
            fs.collection("users").document(user.uid).collection("days").limit(1)
                .get(com.google.firebase.firestore.Source.SERVER).await()
            syncError.value = null
            Pair(true, "Connected & authorized (${user.email})")
        } catch (e: Exception) {
            Pair(false, friendlyFirestoreError(e))
        }
    }

    private fun authErrorMessage(e: Exception, fallback: String): String = when (e) {
        is FirebaseAuthInvalidUserException -> "No account found with this email. Switch to 'Register' to create it."
        is FirebaseAuthUserCollisionException -> "An account with this email already exists. Switch to 'Sign In'."
        is FirebaseAuthWeakPasswordException -> "Password is too weak. Please use at least 6 characters."
        is FirebaseAuthInvalidCredentialsException -> "Incorrect email or password."
        is FirebaseNetworkException -> "No internet connection. Please check your network and try again."
        else -> e.localizedMessage ?: fallback
    }

    suspend fun signInWithEmail(email: String, pass: String): Result<AppUser> = withContext(Dispatchers.IO) {
        val auth = firebaseAuth ?: return@withContext Result.failure(Exception("Cloud sign-in is unavailable on this device"))
        try {
            val fbUser = auth.signInWithEmailAndPassword(email.trim(), pass).await().user
                ?: throw IllegalStateException("Sign in returned no user")
            val user = AppUser(fbUser.uid, fbUser.email ?: email.trim(), resolveDisplayName(fbUser), isCloudConnected = true)
            currentUserAccount.value = user
            saveLocalSession(user)
            attachListeners(fbUser.uid)
            syncAllToCloud().onSuccess { authPrefs.edit().putString("full_synced_uid", fbUser.uid).apply() }
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Sign in failed", e)
            Result.failure(Exception(authErrorMessage(e, "Sign in failed")))
        }
    }

    suspend fun registerWithEmail(email: String, pass: String, displayName: String): Result<AppUser> = withContext(Dispatchers.IO) {
        val auth = firebaseAuth ?: return@withContext Result.failure(Exception("Cloud sign-in is unavailable on this device"))
        val name = displayName.trim()
        try {
            val fbUser = auth.createUserWithEmailAndPassword(email.trim(), pass).await().user
                ?: throw IllegalStateException("Registration returned no user")
            if (name.isNotBlank()) {
                runCatching { fbUser.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name).build()).await() }
            }
            val user = AppUser(fbUser.uid, fbUser.email ?: email.trim(), name, isCloudConnected = true)
            currentUserAccount.value = user
            saveLocalSession(user)
            attachListeners(fbUser.uid)
            syncAllToCloud().onSuccess { authPrefs.edit().putString("full_synced_uid", fbUser.uid).apply() }
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Registration failed", e)
            Result.failure(Exception(authErrorMessage(e, "Registration failed")))
        }
    }

    suspend fun updateUserName(newName: String): Result<Unit> = withContext(Dispatchers.IO) {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) return@withContext Result.failure(IllegalArgumentException("Name cannot be empty"))
        val user = currentUserAccount.value ?: return@withContext Result.failure(IllegalStateException("Not signed in"))
        try {
            firebaseAuth?.currentUser?.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(trimmed).build())?.await()
            ensureProfile(user.uid, user.email, trimmed)
            val updated = user.copy(displayName = trimmed)
            currentUserAccount.value = updated
            saveLocalSession(updated)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(authErrorMessage(e, "Could not update name")))
        }
    }

    /**
     * Deletes cloud data, then the auth account, then local data. Reports failure honestly:
     * Firebase requires a recent sign-in to delete an account.
     */
    suspend fun deleteUserAccount(): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = currentUserAccount.value?.uid
        val fbUser = firebaseAuth?.currentUser
        try {
            if (uid != null && firestore != null) {
                detachListeners()
                for (sub in listOf("days", "zikrItems", "settings", "profile")) {
                    val docs = userDoc(uid).collection(sub).get().await().documents
                    docs.chunked(400).forEach { chunk ->
                        val batch = firestore!!.batch()
                        chunk.forEach { batch.delete(it.reference) }
                        batch.commit().await()
                    }
                }
                userDoc(uid).delete().await()
            }
            fbUser?.delete()?.await()
        } catch (e: FirebaseAuthRecentLoginRequiredException) {
            return@withContext Result.failure(Exception("For security, please sign out, sign in again, then delete your account."))
        } catch (e: Exception) {
            Log.e(TAG, "Delete account failed", e)
            return@withContext Result.failure(Exception(friendlyFirestoreError(e)))
        }
        dayDao.clearAll()
        zikrDao.clearAll()
        settingsDao.clearAll()
        initializeDefaultZikrItems()
        authPrefs.edit().clear().apply()
        currentUserAccount.value = null
        Result.success(Unit)
    }

    fun signOut() {
        detachListeners()
        runCatching { firebaseAuth?.signOut() }
        clearLocalSession()
        currentUserAccount.value = null
    }
}
