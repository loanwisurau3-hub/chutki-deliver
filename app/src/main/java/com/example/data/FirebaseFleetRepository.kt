package com.example.data

import android.app.Activity
import android.content.Context
import com.example.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseOptions
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.tasks.await
import java.util.Locale

class FirebaseFleetRepository(context: Context) {

    companion object {
        private const val DATABASE_URL = "https://test-2a0d0-default-rtdb.firebaseio.com"
        private const val PROJECT_ID = "test-2a0d0"
        private const val API_KEY = "AIzaSyCuJLxRCdhAVZoN81-5weEFnSU4nDr98lo"
        private const val APP_ID = "1:396740286096:web:430751766ca9c384946a15"
        private const val STORAGE_BUCKET = "test-2a0d0.firebasestorage.app"
        private const val GCM_SENDER_ID = "396740286096"
    }

    private val appContext = context.applicationContext

    private val firebaseApp: FirebaseApp by lazy {
        synchronized(FirebaseFleetRepository::class.java) {
            val existing = FirebaseApp.getApps(appContext)
                .firstOrNull { it.name == FirebaseApp.DEFAULT_APP_NAME }
            val configuredAppId = BuildConfig.FIREBASE_ANDROID_APP_ID.trim()
                .takeUnless { it.isBlank() || it == "OPTIONAL_FIREBASE_ANDROID_APP_ID" }
                ?: APP_ID
            existing ?: FirebaseApp.initializeApp(
                appContext,
                FirebaseOptions.Builder()
                    .setApiKey(API_KEY)
                    .setApplicationId(configuredAppId)
                    .setDatabaseUrl(DATABASE_URL)
                    .setProjectId(PROJECT_ID)
                    .setStorageBucket(STORAGE_BUCKET)
                    .setGcmSenderId(GCM_SENDER_ID)
                    .build()
            )
        }
    }

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance(firebaseApp) }
    private val db: FirebaseDatabase by lazy {
        FirebaseDatabase.getInstance(firebaseApp, DATABASE_URL).also { database ->
            runCatching { database.setPersistenceEnabled(true) }
        }
    }

    fun warmUp() {
        firebaseApp
        auth
        db
    }

    fun observeFirebaseConnected(): Flow<Boolean> = callbackFlow {
        val ref = db.getReference(".info/connected")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.getValue(Boolean::class.java) == true)
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(false)
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun hasAndroidFirebaseAppId(): Boolean =
        !firebaseApp.options.applicationId.contains(":web:", ignoreCase = true)

    fun observeAuthState(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        trySend(auth.currentUser)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun signIn(email: String, password: String): Result<FirebaseUser> {
        val cleanEmail = email
            .filterNot { it.isWhitespace() || it == '\u200B' || it == '\uFEFF' }
            .lowercase(Locale.US)
        return runCatching {
            val result = auth.signInWithEmailAndPassword(cleanEmail, password).await()
            result.user ?: error("Sign in failed")
        }.recoverCatching { throwable ->
            throw IllegalStateException(friendlyAuthError(throwable, cleanEmail))
        }
    }

    suspend fun signInWithGoogleIdToken(idToken: String): Result<FirebaseUser> = runCatching {
        val cred = GoogleAuthProvider.getCredential(idToken, null)
        val result = auth.signInWithCredential(cred).await()
        result.user ?: error("Google Sign-In failed")
    }.recoverCatching { throwable ->
        throw IllegalStateException(friendlyAuthError(throwable))
    }

    suspend fun signInWithGoogleOAuth(activity: Activity): Result<FirebaseUser> = runCatching {
        val pending = auth.pendingAuthResult
        val authResult = if (pending != null) {
            pending.await()
        } else {
            val provider = OAuthProvider.newBuilder("google.com", auth).apply {
                addCustomParameter("prompt", "select_account")
            }
            auth.startActivityForSignInWithProvider(activity, provider.build()).await()
        }
        authResult.user ?: error("Google Sign-In failed")
    }.recoverCatching { throwable ->
        throw IllegalStateException(friendlyAuthError(throwable))
    }

    suspend fun completeGoogleRiderOnboarding(
        uid: String,
        name: String,
        email: String,
        phone: String,
        upiId: String,
        vehicle: String,
        acceptedSettings: AdminSettings? = null
    ): Result<Unit> = runCatching {
        val ref = db.getReference("deliveryPartners").child(uid)
        val existingSnap = ref.get().await()
        val payload = mutableMapOf<String, Any>(
            "name" to SecurityUtils.sanitizeText(name.trim()),
            "email" to email.trim(),
            "phone" to phone.trim(),
            "upiId" to upiId.trim(),
            "vehicle" to SecurityUtils.sanitizeText(vehicle.trim())
        )
        if (acceptedSettings != null) {
            payload["agreementAccepted"] = true
            payload["privacyPolicyAccepted"] = true
            payload["agreementVersion"] = acceptedSettings.agreementVersion
            payload["agreementDocHash"] = acceptedSettings.computeAgreementDocHash()
            payload["agreementAcceptedAt"] = ServerValue.TIMESTAMP
        }
        if (existingSnap.exists()) {
            payload["updatedAt"] = System.currentTimeMillis()
            ref.updateChildren(payload).await()
        } else {
            payload["approved"] = false
            payload["online"] = true
            ref.setValue(payload).await()
        }
        if (acceptedSettings != null) {
            recordAgreementAudit(uid, acceptedSettings)
        }
        return@runCatching
    }

    suspend fun acceptLegalAgreements(uid: String, settings: AdminSettings): Result<Unit> = runCatching {
        val docHash = settings.computeAgreementDocHash()
        db.getReference("deliveryPartners").child(uid).updateChildren(
            mapOf(
                "agreementAccepted" to true,
                "privacyPolicyAccepted" to true,
                "agreementVersion" to settings.agreementVersion,
                "agreementDocHash" to docHash,
                "agreementAcceptedAt" to ServerValue.TIMESTAMP
            )
        ).await()
        recordAgreementAudit(uid, settings)
        return@runCatching
    }

    private fun recordAgreementAudit(uid: String, settings: AdminSettings) {
        db.getReference("agreementAuditLogs").child(uid).push().setValue(
            mapOf(
                "riderUid" to uid,
                "agreementVersion" to settings.agreementVersion,
                "agreementDocHash" to settings.computeAgreementDocHash(),
                "agreementAccepted" to true,
                "privacyPolicyAccepted" to true,
                "acceptedAt" to ServerValue.TIMESTAMP
            )
        )
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> = runCatching {
        auth.sendPasswordResetEmail(email.trim().lowercase(Locale.US)).await()
        Unit
    }.recoverCatching { throwable ->
        throw IllegalStateException(friendlyAuthError(throwable))
    }

    suspend fun lookupEmailByPhone(phoneDigits: String): String? = runCatching {
        val clean10 = phoneDigits.filter { it.isDigit() }.takeLast(10)
        val candidates = listOf(clean10, "+91$clean10", "91$clean10")
        for (candidate in candidates) {
            val snap = db.getReference("deliveryPartners")
                .orderByChild("phone")
                .equalTo(candidate)
                .get()
                .await()
            val found = snap.children.firstOrNull()?.strOrNull("email")
            if (!found.isNullOrBlank()) return@runCatching found.trim().lowercase(Locale.US)
        }
        null
    }.getOrNull()

    suspend fun registerRider(
        name: String,
        email: String,
        phone: String,
        upiId: String,
        vehicle: String,
        password: String,
        acceptedSettings: AdminSettings? = null
    ): Result<FirebaseUser> = runCatching {
        val cleanEmail = email
            .filterNot { it.isWhitespace() || it == '\u200B' || it == '\uFEFF' }
            .lowercase(Locale.US)
        val cred = auth.createUserWithEmailAndPassword(cleanEmail, password).await()
        val user = cred.user ?: error("Account creation failed")
        val profileMap = mutableMapOf<String, Any>(
            "name" to SecurityUtils.sanitizeText(name.trim()),
            "email" to cleanEmail,
            "phone" to phone.trim(),
            "upiId" to upiId.trim(),
            "vehicle" to SecurityUtils.sanitizeText(vehicle.trim()),
            "approved" to false,
            "online" to true
        )
        if (acceptedSettings != null) {
            profileMap["agreementAccepted"] = true
            profileMap["privacyPolicyAccepted"] = true
            profileMap["agreementVersion"] = acceptedSettings.agreementVersion
            profileMap["agreementDocHash"] = acceptedSettings.computeAgreementDocHash()
            profileMap["agreementAcceptedAt"] = ServerValue.TIMESTAMP
        }
        db.getReference("deliveryPartners").child(user.uid).setValue(profileMap).await()
        if (acceptedSettings != null) {
            recordAgreementAudit(user.uid, acceptedSettings)
        }
        user
    }.recoverCatching { throwable ->
        throw IllegalStateException(friendlyAuthError(throwable))
    }

    suspend fun signOut(uid: String?) {
        if (uid != null) {
            runCatching {
                db.getReference("deliveryPartners").child(uid).updateChildren(mapOf("online" to false)).await()
            }
        }
        auth.signOut()
    }

    private fun friendlyAuthError(t: Throwable, attemptedEmail: String? = null): String {
        val msg = t.message.orEmpty()
        val lower = msg.lowercase(Locale.US)
        val errorCode = (t as? FirebaseAuthException)?.errorCode.orEmpty().uppercase(Locale.US)

        if (t is FirebaseNetworkException || "network" in lower || "timeout" in lower) {
            return "Network connection failed. Check your mobile data or Wi-Fi and try again."
        }
        if (t is FirebaseTooManyRequestsException || "too_many_attempts" in lower || "unusual activity" in lower) {
            return "Too many failed login attempts. Wait a minute or tap Forgot Password to reset."
        }
        if (t is FirebaseAuthUserCollisionException || errorCode == "ERROR_EMAIL_ALREADY_IN_USE" || "email_exists" in lower || "already in use" in lower) {
            return "This email is already registered. Please Sign In or reset your password."
        }
        if (t is FirebaseAuthInvalidUserException || errorCode == "ERROR_USER_NOT_FOUND" || "user_not_found" in lower || "no user record" in lower) {
            return "No rider account found for this email. Tap Register Rider to create your account."
        }
        if (errorCode == "ERROR_USER_DISABLED" || "user_disabled" in lower) {
            return "This rider account has been disabled. Contact Gossaigaon Bazar Admin."
        }
        if (errorCode == "ERROR_INVALID_EMAIL" || "invalid_email" in lower || "badly formatted" in lower) {
            return "Please enter a valid email address."
        }
        if (t is FirebaseAuthInvalidCredentialsException ||
            errorCode == "ERROR_INVALID_CREDENTIAL" ||
            errorCode == "ERROR_WRONG_PASSWORD" ||
            "supplied auth credential" in lower ||
            "invalid_login_credentials" in lower ||
            "invalid_password" in lower ||
            "wrong-password" in lower
        ) {
            val targetSuffix = if (!attemptedEmail.isNullOrBlank()) " for $attemptedEmail" else ""
            return "Incorrect email or password$targetSuffix. Double-check your password (tap the eye icon), use Reset Password, or Continue with Google if you registered with Google."
        }
        if ("invalid_app_id" in lower) {
            return "Google Sign-In needs Android app (${BuildConfig.APPLICATION_ID}) registered in Firebase Console (test-2a0d0) with SHA-1 30:EA:37:F5:28:0C:58:0F:00:7A:A4:9D:81:64:5E:BA:DD:8D:EA:02."
        }
        if ("recaptcha" in lower) {
            return "Security verification failed. Please check your internet connection and try signing in again."
        }
        return msg.ifBlank { "Authentication failed. Please try again." }
    }

    fun observeRiderProfile(uid: String): Flow<RiderProfile> = callbackFlow {
        val ref = db.getReference("deliveryPartners").child(uid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(parseRiderProfile(uid, snapshot))
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun observeStores(): Flow<Map<String, StoreInfo>> = callbackFlow {
        val ref = db.getReference("restaurants")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val map = mutableMapOf<String, StoreInfo>()
                for (child in snapshot.children) {
                    val key = child.key ?: continue
                    val storeLink = child.str("locationLink").ifBlank { child.str("mapLink") }
                    val linkCoords = com.example.location.RiderMapsNavigator.extractLatLngFromLocationLink(storeLink)
                    val lat = child.dblOrNull("lat")
                        ?: child.dblOrNull("latitude")
                        ?: child.dblOrNull("storeLat")
                        ?: linkCoords?.first
                    val lng = child.dblOrNull("lng")
                        ?: child.dblOrNull("lon")
                        ?: child.dblOrNull("longitude")
                        ?: child.dblOrNull("storeLng")
                        ?: linkCoords?.second
                    map[key] = StoreInfo(
                        id = key,
                        name = child.str("name", "Store"),
                        address = child.str("address", "Gossaigaon Bazar"),
                        phone = child.str("phone"),
                        lat = lat,
                        lng = lng
                    )
                }
                trySend(map)
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    private fun observeNodeSnapshot(path: String): Flow<DataSnapshot?> = callbackFlow {
        val ref = db.getReference(path)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot)
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(null)
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun observeAdminSettings(): Flow<AdminSettings> = combine(
        observeNodeSnapshot("admin/settings"),
        observeNodeSnapshot("admin/agreement")
    ) { settingsSnap, agreementSnap ->
        fun pickString(vararg keys: String): String? {
            for (k in keys) {
                agreementSnap?.strOrNull(k)?.takeIf { it.isNotBlank() }?.let { return it }
                settingsSnap?.child("legalDocuments")?.strOrNull(k)?.takeIf { it.isNotBlank() }?.let { return it }
                settingsSnap?.strOrNull(k)?.takeIf { it.isNotBlank() }?.let { return it }
            }
            return null
        }

        val baseFee = settingsSnap?.dblOrNull("riderBaseFee")?.takeIf { it > 0 }
            ?: settingsSnap?.dblOrNull("deliveryCharge")?.takeIf { it > 0 }
            ?: 25.0
        val distanceEnabled = settingsSnap?.bool("distanceEarningsEnabled", true) ?: true
        val perKmRate = listOf(
            "riderPerKmRate",
            "perKmRate",
            "perKmEarnAmount",
            "riderPerKmEarn",
            "kmRate",
            "perKmCharge"
        ).firstNotNullOfOrNull { key ->
            settingsSnap?.dblOrNull(key)?.takeIf { it >= 0.0 }
        } ?: 10.0
        val distanceBasePay = listOf(
            "riderDistanceBasePay",
            "riderBasePay",
            "distanceBaseFee"
        ).firstNotNullOfOrNull { key ->
            settingsSnap?.dblOrNull(key)?.takeIf { it >= 0.0 }
        } ?: 0.0
        val baseDistanceKm = listOf(
            "riderBaseDistanceKm",
            "baseDistanceKm",
            "includedKm"
        ).firstNotNullOfOrNull { key ->
            settingsSnap?.dblOrNull(key)?.takeIf { it >= 0.0 }
        } ?: 0.0
        val minTripEarning = listOf(
            "riderMinTripEarning",
            "minTripEarning",
            "minRiderEarning"
        ).firstNotNullOfOrNull { key ->
            settingsSnap?.dblOrNull(key)?.takeIf { it >= 0.0 }
        } ?: 0.0
        val roadMultiplier = listOf(
            "distanceRoadMultiplier",
            "roadDistanceMultiplier"
        ).firstNotNullOfOrNull { key ->
            settingsSnap?.dblOrNull(key)?.takeIf { it >= 1.0 }
        }?.coerceIn(1.0, 2.5) ?: 1.0

        val maxCash = settingsSnap?.dblOrNull("maxRiderCashLimit")?.takeIf { it > 0 } ?: 3000.0
        val timeoutSec = settingsSnap?.longOrNull("riderAcceptTimeoutSec")?.toInt()?.takeIf { it >= 15 } ?: 45
        val minPayout = settingsSnap?.dblOrNull("minRiderPayout")?.takeIf { it >= 1 } ?: 10.0
        val maxPayout = settingsSnap?.dblOrNull("maxRiderPayout")?.takeIf { it >= 10 } ?: 10000.0
        val minDep = settingsSnap?.dblOrNull("minRiderCodDeposit")?.takeIf { it >= 1 } ?: 10.0
        val maxDep = settingsSnap?.dblOrNull("maxRiderCodDeposit")?.takeIf { it >= 10 } ?: 25000.0

        val updatedAt = agreementSnap?.longOrNull("updatedAt")
            ?: agreementSnap?.longOrNull("agreementUpdatedAt")
            ?: settingsSnap?.child("legalDocuments")?.longOrNull("updatedAt")
            ?: settingsSnap?.longOrNull("agreementUpdatedAt")
            ?: 0L

        val requireAgreement = when {
            agreementSnap?.hasChild("requireAgreementAcceptance") == true ->
                agreementSnap.bool("requireAgreementAcceptance", true)
            settingsSnap?.child("legalDocuments")?.hasChild("requireAgreementAcceptance") == true ->
                settingsSnap.child("legalDocuments").bool("requireAgreementAcceptance", true)
            settingsSnap?.hasChild("requireAgreementAcceptance") == true ->
                settingsSnap.bool("requireAgreementAcceptance", true)
            else -> true
        }

        AdminSettings(
            upiId = settingsSnap?.str("upiId", "gossaigaonbazar@apl")?.ifBlank { "gossaigaonbazar@apl" }
                ?: "gossaigaonbazar@apl",
            riderBaseFee = baseFee,
            distanceEarningsEnabled = distanceEnabled,
            riderPerKmRate = perKmRate,
            riderDistanceBasePay = distanceBasePay,
            riderBaseDistanceKm = baseDistanceKm,
            riderMinTripEarning = minTripEarning,
            distanceRoadMultiplier = roadMultiplier,
            maxRiderCashLimit = maxCash,
            riderAcceptTimeoutSec = timeoutSec,
            minRiderPayout = minPayout,
            maxRiderPayout = maxPayout,
            minRiderCodDeposit = minDep,
            maxRiderCodDeposit = maxDep,
            allowRiderPassTrip = settingsSnap?.bool("allowRiderPassTrip", true) ?: true,
            allowRiderSos = settingsSnap?.bool("allowRiderSos", true) ?: true,
            riderAnnouncement = settingsSnap?.str("riderAnnouncement").orEmpty(),
            supportPhone = settingsSnap?.str("supportPhone").orEmpty(),
            supportWhatsapp = settingsSnap?.str("supportWhatsapp").orEmpty(),
            supportHours = settingsSnap?.str("supportHours").orEmpty(),
            agreementVersion = pickString("agreementVersion", "version", "legalVersion")
                ?: LegalDocumentDefaults.DEFAULT_VERSION,
            agreementTitle = pickString("agreementTitle", "title")
                ?: LegalDocumentDefaults.DEFAULT_AGREEMENT_TITLE,
            agreementContent = pickString("agreementContent", "riderAgreement", "agreementText", "content")
                ?: LegalDocumentDefaults.DEFAULT_AGREEMENT_TEXT,
            privacyPolicyTitle = pickString("privacyPolicyTitle", "privacyTitle")
                ?: LegalDocumentDefaults.DEFAULT_PRIVACY_TITLE,
            privacyPolicyContent = pickString("privacyPolicyContent", "privacyPolicy", "privacyPolicyText")
                ?: LegalDocumentDefaults.DEFAULT_PRIVACY_TEXT,
            agreementUrl = pickString("agreementUrl", "termsUrl").orEmpty(),
            privacyPolicyUrl = pickString("privacyPolicyUrl", "privacyUrl").orEmpty(),
            agreementUpdatedAt = updatedAt,
            requireAgreementAcceptance = requireAgreement,
            dailyTripTarget = settingsSnap?.longOrNull("dailyTripTarget")?.toInt()?.coerceAtLeast(0) ?: 10,
            dailyBonusAmount = settingsSnap?.dblOrNull("dailyBonusAmount")?.coerceAtLeast(0.0) ?: 100.0,
            requireDeliveryPhoto = settingsSnap?.bool("requireDeliveryPhoto", false) ?: false,
            maintenanceMode = settingsSnap?.bool("maintenanceMode", false) ?: false,
            maintenanceMessage = settingsSnap?.str("maintenanceMessage").orEmpty()
        )
    }

    fun observeRiderRatings(uid: String): Flow<List<RiderRating>> = callbackFlow {
        val ref = db.getReference("riderRatings").child(uid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { child ->
                    val id = child.key ?: return@mapNotNull null
                    RiderRating(
                        id = id,
                        customerName = child.str("customerName", "Customer"),
                        rating = (child.longOrNull("rating") ?: 5L).toInt().coerceIn(1, 5),
                        comment = child.str("comment"),
                        createdAt = child.longOrNull("createdAt") ?: 0L
                    )
                }.sortedByDescending { it.createdAt }
                trySend(list)
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun observeFeedbacks(uid: String): Flow<List<FeedbackRecord>> = callbackFlow {
        val ref = db.getReference("feedbacks")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { child ->
                    if (child.str("uid") != uid) return@mapNotNull null
                    FeedbackRecord(
                        id = child.key ?: return@mapNotNull null,
                        role = child.str("role", "RIDER"),
                        uid = uid,
                        name = child.str("name"),
                        phone = child.str("phone"),
                        subject = child.str("subject"),
                        message = child.str("message"),
                        status = child.str("status", "OPEN"),
                        adminReply = child.str("adminReply"),
                        createdAt = child.longOrNull("createdAt") ?: 0L
                    )
                }.sortedByDescending { it.createdAt }
                trySend(list)
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun observeEmergencyRequests(uid: String): Flow<List<EmergencyRequestRecord>> = callbackFlow {
        val ordersRef = db.getReference("riderEmergencyRequests")
        val query = ordersRef.orderByChild("riderId").equalTo(uid)
        var fallbackListener: ValueEventListener? = null

        val primaryListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(parseEmergencyRequests(snapshot, uid))
            }
            override fun onCancelled(error: DatabaseError) {
                val fb = object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        trySend(parseEmergencyRequests(snapshot, uid))
                    }
                    override fun onCancelled(e: DatabaseError) {}
                }
                fallbackListener = fb
                ordersRef.addValueEventListener(fb)
            }
        }
        query.addValueEventListener(primaryListener)
        awaitClose {
            query.removeEventListener(primaryListener)
            fallbackListener?.let { ordersRef.removeEventListener(it) }
        }
    }

    fun observeAssignedOrders(uid: String): Flow<Map<String, OrderRecord>> = callbackFlow {
        val ordersRef = db.getReference("orders")
        val query = ordersRef.orderByChild("deliveryUid").equalTo(uid)
        var fallbackListener: ValueEventListener? = null

        val primaryListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(parseAssignedOrders(snapshot, uid))
            }
            override fun onCancelled(error: DatabaseError) {
                val fb = object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        trySend(parseAssignedOrders(snapshot, uid))
                    }
                    override fun onCancelled(e: DatabaseError) {}
                }
                fallbackListener = fb
                ordersRef.addValueEventListener(fb)
            }
        }
        query.addValueEventListener(primaryListener)
        awaitClose {
            query.removeEventListener(primaryListener)
            fallbackListener?.let { ordersRef.removeEventListener(it) }
        }
    }

    fun observeAllSettlements(): Flow<List<SettlementRecord>> = callbackFlow {
        val ref = db.getReference("settlements")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { child ->
                    val id = child.key ?: return@mapNotNull null
                    SettlementRecord(
                        id = id,
                        riderUid = child.str("riderUid"),
                        riderName = child.str("riderName"),
                        amount = child.dblOrNull("amount") ?: 0.0,
                        mode = child.str("mode", "UPI"),
                        utr = child.str("utr"),
                        status = child.str("status", "PENDING"),
                        createdAt = child.longOrNull("createdAt") ?: 0L
                    )
                }.sortedByDescending { it.createdAt }
                trySend(list)
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun observeRiderPayouts(uid: String): Flow<List<RiderPayoutRecord>> = callbackFlow {
        val ref = db.getReference("riderPayouts")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { child ->
                    if (child.str("riderUid") != uid) return@mapNotNull null
                    RiderPayoutRecord(
                        id = child.key ?: return@mapNotNull null,
                        riderUid = uid,
                        riderName = child.str("riderName"),
                        riderPhone = child.str("riderPhone"),
                        riderUpiId = child.str("riderUpiId"),
                        amount = child.dblOrNull("amount") ?: 0.0,
                        status = child.str("status", "PENDING"),
                        adminRef = child.str("adminRef"),
                        createdAt = child.longOrNull("createdAt") ?: 0L
                    )
                }.sortedByDescending { it.createdAt }
                trySend(list)
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun updateRiderOnline(uid: String, online: Boolean, shiftStartedAtMs: Long = 0L) {
        val now = System.currentTimeMillis()
        val updates = mutableMapOf<String, Any?>(
            "online" to online,
            "lastSeen" to ServerValue.TIMESTAMP
        )
        if (online) {
            updates["shiftStartedAt"] = now
        } else {
            updates["shiftEndedAt"] = now
        }
        db.getReference("deliveryPartners").child(uid).updateChildren(updates).await()

        val durationSec = if (!online && shiftStartedAtMs > 0L) {
            ((now - shiftStartedAtMs) / 1000L).coerceAtLeast(0L)
        } else {
            0L
        }
        runCatching {
            db.getReference("riderShiftLogs").child(uid).push().setValue(
                mapOf(
                    "riderUid" to uid,
                    "event" to if (online) "SHIFT_START" else "SHIFT_END",
                    "durationSeconds" to durationSec,
                    "timestamp" to ServerValue.TIMESTAMP
                )
            )
        }
    }

    suspend fun submitRiderKyc(
        uid: String,
        licenseNumber: String,
        vehicleRegNumber: String,
        idLast4: String,
        emergencyContactName: String,
        emergencyContactPhone: String,
        idDocumentPhoto: String,
        selfiePhoto: String
    ) {
        val kycPayload = mutableMapOf<String, Any>(
            "licenseNumber" to SecurityUtils.sanitizeText(licenseNumber.trim().uppercase()),
            "vehicleRegNumber" to SecurityUtils.sanitizeText(vehicleRegNumber.trim().uppercase()),
            "idLast4" to idLast4.filter { it.isDigit() }.takeLast(4),
            "emergencyContactName" to SecurityUtils.sanitizeText(emergencyContactName.trim()),
            "emergencyContactPhone" to emergencyContactPhone.filter { it.isDigit() }.takeLast(10),
            "kycStatus" to "PENDING",
            "kycNote" to "",
            "submittedAt" to ServerValue.TIMESTAMP
        )
        if (idDocumentPhoto.isNotBlank()) {
            kycPayload["idDocumentPhoto"] = idDocumentPhoto
        }
        if (selfiePhoto.isNotBlank()) {
            kycPayload["selfiePhoto"] = selfiePhoto
        }
        db.getReference("deliveryPartners").child(uid).updateChildren(
            mapOf(
                "kyc" to kycPayload,
                "kycStatus" to "PENDING",
                "updatedAt" to ServerValue.TIMESTAMP
            )
        ).await()
    }

    suspend fun updateRiderLocation(
        uid: String,
        lat: Double,
        lng: Double,
        accuracy: Int?,
        online: Boolean,
        speedKmh: Int? = null,
        bearing: Int? = null,
        batteryPct: Int? = null
    ) {
        val map = mutableMapOf<String, Any?>(
            "lat" to lat,
            "lng" to lng,
            "accuracy" to accuracy,
            "online" to online,
            "lastSeen" to ServerValue.TIMESTAMP
        )
        if (speedKmh != null) map["speedKmh"] = speedKmh
        if (bearing != null) map["bearing"] = bearing
        if (batteryPct != null) map["batteryPct"] = batteryPct
        runCatching {
            db.getReference("deliveryPartners").child(uid).updateChildren(map).await()
        }
    }

    suspend fun updateOrderLocationAndTrail(
        orderId: String,
        lat: Double,
        lng: Double,
        accuracy: Int?,
        saveTrailPoint: Boolean
    ) {
        runCatching {
            db.getReference("orders").child(orderId).updateChildren(
                mapOf(
                    "riderLat" to lat,
                    "riderLng" to lng,
                    "lastLocationUpdate" to ServerValue.TIMESTAMP
                )
            ).await()
            if (saveTrailPoint) {
                db.getReference("orderLocationHistory").child(orderId).push().setValue(
                    mapOf(
                        "latitude" to lat,
                        "longitude" to lng,
                        "accuracy" to (accuracy ?: 0),
                        "timestamp" to ServerValue.TIMESTAMP
                    )
                ).await()
            }
        }
    }

    suspend fun updateRiderProfile(uid: String, name: String, phone: String, upiId: String, vehicle: String) {
        db.getReference("deliveryPartners").child(uid).updateChildren(
            mapOf(
                "name" to SecurityUtils.sanitizeText(name.trim()),
                "phone" to phone.trim(),
                "upiId" to upiId.trim(),
                "vehicle" to SecurityUtils.sanitizeText(vehicle.trim()),
                "updatedAt" to System.currentTimeMillis()
            )
        ).await()
    }

    suspend fun acceptOrders(orderIds: List<String>) {
        val now = System.currentTimeMillis()
        for (id in orderIds) {
            db.getReference("orders").child(id).updateChildren(
                mapOf(
                    "riderAccepted" to true,
                    "riderAcceptedAt" to now,
                    "updatedAt" to now
                )
            ).await()
        }
    }

    suspend fun passTripToNextNearestRider(
        orders: List<OrderRecord>,
        currentUid: String,
        riderName: String,
        riderPhone: String,
        reason: String,
        stores: Map<String, StoreInfo>,
        fallbackLat: Double?,
        fallbackLng: Double?
    ): String? {
        // Hard repository-level guard: never pass or cancel once any order is picked up or locked
        if (orders.any { SecurityUtils.isOrderPostPickupLocked(it) }) {
            throw IllegalStateException("ORDER PICKED UP — CANCELLATION LOCK ACTIVE. Only Admin can cancel.")
        }
        for (o in orders) {
            val liveSnap = db.getReference("orders").child(o.orderId).get().await()
            val liveLocked = liveSnap.bool("cancellationLocked", false) ||
                liveSnap.bool("cancelLocked", false) ||
                liveSnap.bool("pickupVerified", false) ||
                liveSnap.str("status") in setOf(
                    "PICKED_UP",
                    "OUT_FOR_DELIVERY",
                    "CANCELLATION_REQUESTED",
                    "EMERGENCY_INVESTIGATION",
                    "RETURNED_TO_STORE",
                    "DELIVERY_FAILED",
                    "ADMIN_CANCELLATION_APPROVED"
                )
            if (liveLocked) {
                throw IllegalStateException("ORDER PICKED UP — CANCELLATION LOCK ACTIVE. Only Admin can cancel.")
            }
        }

        val snap = db.getReference("deliveryPartners").get().await()
        val firstStore = stores[orders.firstOrNull()?.restaurantId.orEmpty()]
        val storeLat = firstStore?.lat ?: fallbackLat ?: 26.4524
        val storeLng = firstStore?.lng ?: fallbackLng ?: 89.9723

        var nextUid: String? = null
        var minDist = Double.POSITIVE_INFINITY

        for (child in snap.children) {
            val uid = child.key ?: continue
            if (uid == currentUid) continue
            val approved = child.bool("approved", false)
            val online = child.bool("online", false)
            val suspended = child.bool("suspended", false)
            val alreadyRejected = orders.any { currentUid != uid && uid in it.rejectedByRiders }
            if (!approved || !online || suspended || alreadyRejected) continue

            val rLat = child.dblOrNull("lat")
            val rLng = child.dblOrNull("lng")
            if (rLat != null && rLng != null) {
                val dist = SecurityUtils.calculateDistanceKm(storeLat, storeLng, rLat, rLng)
                if (dist != null && dist < minDist) {
                    minDist = dist
                    nextUid = uid
                }
            } else if (nextUid == null) {
                nextUid = uid
            }
        }

        val now = System.currentTimeMillis()
        for (o in orders) {
            val updates = mutableMapOf<String, Any?>(
                "deliveryUid" to nextUid,
                "assignedRiderId" to nextUid,
                "riderAccepted" to null,
                "riderAcceptedAt" to null,
                "assignedAt" to if (nextUid != null) now else null,
                "updatedAt" to now,
                "rejectedByRiders/$currentUid" to mapOf(
                    "riderName" to riderName.ifBlank { "Rider" },
                    "riderPhone" to riderPhone,
                    "reason" to reason.ifBlank { "Declined" },
                    "timestamp" to now
                )
            )
            db.getReference("orders").child(o.orderId).updateChildren(updates).await()
        }
        return nextUid
    }

    suspend fun verifyAndLockPickup(
        order: OrderRecord,
        uid: String,
        lat: Double?,
        lng: Double?,
        accuracy: Int?
    ) {
        // Hard repository-level guard: confirm Restaurant has marked order READY FOR PICKUP on RTDB
        val liveSnap = db.getReference("orders").child(order.orderId).get().await()
        val liveStatus = liveSnap.str("status").trim().uppercase()
        val liveReadyFlag = liveSnap.bool("readyForPickup", false) ||
            liveSnap.bool("restaurantReady", false) ||
            liveSnap.bool("storeReady", false) ||
            liveSnap.bool("isReadyForPickup", false)
        val isServerReady = liveStatus in SecurityUtils.READY_FOR_PICKUP_STATUSES || liveReadyFlag
        if (!isServerReady) {
            logSecurityEvent(
                uid = uid,
                orderId = order.orderId,
                eventType = "PREMATURE_PICKUP_BLOCKED",
                attemptedAction = "Rider attempted pickup while restaurant status was '$liveStatus'",
                result = "BLOCKED"
            )
            throw IllegalStateException("Restaurant has not marked this order Ready for Pickup yet! (Status: ${liveStatus.ifBlank { "PREPARING" }})")
        }

        val resolvedCode = SecurityUtils.getDisplayablePickupCode(order)
        val updates = mutableMapOf<String, Any?>(
            "status" to "PICKED_UP",
            "pickupVerified" to true,
            "pickedUpAt" to ServerValue.TIMESTAMP,
            "cancellationLocked" to true,
            "cancelLocked" to true,
            "pickedUpByRiderId" to uid,
            "assignedRiderId" to uid,
            "storePickupVerifiedAt" to ServerValue.TIMESTAMP,
            "deliveryOtpExpiresAt" to (order.deliveryOtpExpiresAt ?: (System.currentTimeMillis() + 24L * 3600L * 1000L)),
            "updatedAt" to ServerValue.TIMESTAMP
        )
        if (!resolvedCode.isNullOrBlank() && order.pickupOtp.isNullOrBlank()) {
            updates["pickupOtp"] = resolvedCode
        }
        if (lat != null && lng != null) {
            updates["riderLat"] = lat
            updates["riderLng"] = lng
            updates["lastLocationUpdate"] = ServerValue.TIMESTAMP
        }
        db.getReference("orders").child(order.orderId).updateChildren(updates).await()
        logSecurityEvent(
            uid = uid,
            orderId = order.orderId,
            eventType = "PICKUP_CONFIRMED_CANCEL_LOCKED",
            attemptedAction = "Store pickup verified — cancellation locked for rider",
            result = "LOCKED"
        )

        if (lat != null && lng != null) {
            db.getReference("orderLocationHistory").child(order.orderId).push().setValue(
                mapOf(
                    "latitude" to lat,
                    "longitude" to lng,
                    "accuracy" to (accuracy ?: 0),
                    "timestamp" to ServerValue.TIMESTAMP
                )
            ).await()
        }
    }

    suspend fun startDoorstepInTransit(orderIds: List<String>, lat: Double?, lng: Double?) {
        for (id in orderIds) {
            val up = mutableMapOf<String, Any?>(
                "status" to "OUT_FOR_DELIVERY",
                "cancellationLocked" to true,
                "cancelLocked" to true,
                "updatedAt" to ServerValue.TIMESTAMP
            )
            if (lat != null && lng != null) {
                up["riderLat"] = lat
                up["riderLng"] = lng
                up["lastLocationUpdate"] = ServerValue.TIMESTAMP
            }
            db.getReference("orders").child(id).updateChildren(up).await()
        }
    }

    suspend fun completeDelivery(
        orderIds: List<String>,
        uid: String,
        deliveryProofPhoto: String? = null,
        orderEarnings: Map<String, OrderEarningBreakdown> = emptyMap()
    ) {
        for (id in orderIds) {
            logSecurityEvent(uid, id, "DELIVERY_OTP_VERIFIED", "Customer Delivery PIN matched", "SUCCESS")
            val payload = mutableMapOf<String, Any>(
                "status" to "DELIVERED",
                "deliveredAt" to ServerValue.TIMESTAMP,
                "updatedAt" to ServerValue.TIMESTAMP
            )
            if (!deliveryProofPhoto.isNullOrBlank()) {
                payload["deliveryProofPhoto"] = deliveryProofPhoto
                payload["deliveryProofAt"] = ServerValue.TIMESTAMP
            }
            val breakdown = orderEarnings[id]
            if (breakdown != null && breakdown.totalEarning > 0.0) {
                payload["riderEarning"] = breakdown.totalEarning
                payload["riderPerKmRate"] = breakdown.perKmRate
                breakdown.distanceKm?.let { dist ->
                    payload["riderDistanceKm"] = dist
                }
            }
            db.getReference("orders").child(id).updateChildren(payload).await()
        }
    }

    suspend fun submitEmergencyRequest(
        orders: List<OrderRecord>,
        uid: String,
        riderName: String,
        riderPhone: String,
        reason: String,
        description: String,
        isPostPickup: Boolean,
        recentCount24h: Int
    ) {
        val cleanDesc = SecurityUtils.sanitizeText(description.trim()).ifBlank { reason }
        val targetStatus = if (reason == "Accident or medical emergency") "EMERGENCY_INVESTIGATION" else "CANCELLATION_REQUESTED"

        for (o in orders) {
            db.getReference("riderEmergencyRequests").push().setValue(
                mapOf(
                    "orderId" to o.orderId,
                    "riderId" to uid,
                    "riderName" to riderName.ifBlank { "Rider" },
                    "riderPhone" to riderPhone,
                    "reason" to reason,
                    "description" to cleanDesc,
                    "status" to "PENDING",
                    "createdAt" to ServerValue.TIMESTAMP
                )
            ).await()

            val orderUpdates = mutableMapOf<String, Any?>(
                "emergencyAlert" to true,
                "emergencyDetails" to "Rider Emergency: $reason${if (description.isNotBlank()) " - $cleanDesc" else ""}",
                "cancellationRequested" to true,
                "cancellationReason" to reason,
                "updatedAt" to ServerValue.TIMESTAMP
            )
            if (isPostPickup) {
                orderUpdates["status"] = targetStatus
                orderUpdates["cancellationLocked"] = true
            }
            db.getReference("orders").child(o.orderId).updateChildren(orderUpdates).await()

            if (recentCount24h >= 2) {
                logSecurityEvent(
                    uid,
                    o.orderId,
                    "REPEATED_EMERGENCY_REQUEST",
                    "Rider submitted ${recentCount24h + 1} emergency requests in 24h ($reason)",
                    "LOGGED"
                )
            }
        }
    }

    suspend fun submitPayoutRequest(
        uid: String,
        riderName: String,
        riderPhone: String,
        upiId: String,
        amount: Double
    ) {
        db.getReference("riderPayouts").push().setValue(
            mapOf(
                "riderUid" to uid,
                "riderName" to riderName.ifBlank { "Rider" },
                "riderPhone" to riderPhone,
                "riderUpiId" to upiId.trim(),
                "amount" to (Math.round(amount * 100.0) / 100.0),
                "status" to "PENDING",
                "createdAt" to System.currentTimeMillis()
            )
        ).await()
    }

    suspend fun submitCodSettlement(
        uid: String,
        riderName: String,
        amount: Double,
        mode: String,
        utr: String
    ) {
        db.getReference("settlements").push().setValue(
            mapOf(
                "riderUid" to uid,
                "riderName" to riderName.ifBlank { "Rider" },
                "amount" to (Math.round(amount * 100.0) / 100.0),
                "mode" to mode,
                "utr" to if (mode == "UPI") utr.trim() else "PHYSICAL_HANDOVER",
                "status" to "PENDING",
                "createdAt" to System.currentTimeMillis()
            )
        ).await()
    }

    suspend fun submitFeedback(
        uid: String,
        riderName: String,
        riderPhone: String,
        subject: String,
        message: String
    ) {
        db.getReference("feedbacks").push().setValue(
            mapOf(
                "role" to "RIDER",
                "uid" to uid,
                "name" to riderName.ifBlank { "Rider" },
                "phone" to riderPhone,
                "subject" to SecurityUtils.sanitizeText(subject.trim()),
                "message" to SecurityUtils.sanitizeText(message.trim()),
                "status" to "OPEN",
                "createdAt" to System.currentTimeMillis()
            )
        ).await()
    }

    fun logSecurityEvent(
        uid: String?,
        orderId: String,
        eventType: String,
        attemptedAction: String,
        result: String = "BLOCKED"
    ) {
        if (uid == null) return
        db.getReference("riderSecurityLogs").push().setValue(
            mapOf(
                "orderId" to orderId.ifBlank { "UNKNOWN" },
                "riderId" to uid,
                "eventType" to eventType,
                "attemptedAction" to SecurityUtils.sanitizeText(attemptedAction),
                "timestamp" to ServerValue.TIMESTAMP,
                "result" to result
            )
        )
    }

    private fun parseRiderProfile(uid: String, snap: DataSnapshot): RiderProfile {
        val kycSnap = snap.child("kyc")
        val rawKycStatus = kycSnap.str("kycStatus").ifBlank { snap.str("kycStatus") }.ifBlank { "NOT_SUBMITTED" }
        val kyc = RiderKycInfo(
            licenseNumber = kycSnap.str("licenseNumber"),
            vehicleRegNumber = kycSnap.str("vehicleRegNumber"),
            idLast4 = kycSnap.str("idLast4"),
            emergencyContactName = kycSnap.str("emergencyContactName"),
            emergencyContactPhone = kycSnap.str("emergencyContactPhone"),
            kycStatus = rawKycStatus.uppercase(),
            kycNote = kycSnap.str("kycNote").ifBlank { snap.str("kycNote") },
            idDocumentPhoto = kycSnap.str("idDocumentPhoto"),
            selfiePhoto = kycSnap.str("selfiePhoto"),
            submittedAt = kycSnap.longOrNull("submittedAt") ?: 0L
        )
        return RiderProfile(
            uid = uid,
            name = snap.str("name"),
            email = snap.str("email"),
            phone = snap.str("phone"),
            upiId = snap.str("upiId"),
            vehicle = snap.str("vehicle"),
            approved = snap.bool("approved", false),
            online = snap.bool("online", false),
            suspended = snap.bool("suspended", false),
            adminNote = snap.str("adminNote"),
            ratingAvg = snap.dblOrNull("ratingAvg") ?: 5.0,
            ratingCount = (snap.longOrNull("ratingCount") ?: 0L).toInt(),
            lat = snap.dblOrNull("lat"),
            lng = snap.dblOrNull("lng"),
            accuracy = snap.longOrNull("accuracy")?.toInt(),
            lastSeen = snap.longOrNull("lastSeen") ?: 0L,
            shiftStartedAt = snap.longOrNull("shiftStartedAt") ?: 0L,
            kyc = kyc,
            agreementAccepted = snap.bool("agreementAccepted", false),
            privacyPolicyAccepted = snap.bool("privacyPolicyAccepted", false),
            agreementAcceptedVersion = snap.str("agreementVersion").ifBlank { snap.str("agreementAcceptedVersion") },
            agreementAcceptedHash = snap.str("agreementDocHash"),
            agreementAcceptedAt = snap.longOrNull("agreementAcceptedAt") ?: 0L
        )
    }

    private fun parseAssignedOrders(snapshot: DataSnapshot, uid: String): Map<String, OrderRecord> {
        val result = mutableMapOf<String, OrderRecord>()
        for (child in snapshot.children) {
            val orderId = child.key ?: continue
            val deliveryUid = child.strOrNull("deliveryUid")
            if (deliveryUid != uid) continue

            val itemsList = mutableListOf<OrderItem>()
            val itemsSnap = child.child("items")
            for (itemSnap in itemsSnap.children) {
                itemsList.add(
                    OrderItem(
                        name = itemSnap.str("name", "Item"),
                        qty = (itemSnap.longOrNull("qty") ?: 1L).toInt()
                    )
                )
            }

            val rejectedSet = child.child("rejectedByRiders").children.mapNotNull { it.key }.toSet()

            val locLink = child.str("locationLink").ifBlank { child.str("mapLink") }
            val linkCoords = com.example.location.RiderMapsNavigator.extractLatLngFromLocationLink(locLink)
            val cLat = child.dblOrNull("customerLat")
                ?: child.dblOrNull("deliveryLat")
                ?: child.dblOrNull("lat")
                ?: child.dblOrNull("latitude")
                ?: linkCoords?.first
            val cLng = child.dblOrNull("customerLng")
                ?: child.dblOrNull("deliveryLng")
                ?: child.dblOrNull("lng")
                ?: child.dblOrNull("longitude")
                ?: linkCoords?.second

            result[orderId] = OrderRecord(
                orderId = orderId,
                batchGroupId = child.str("batchGroupId"),
                customerUid = child.str("customerUid"),
                restaurantId = child.str("restaurantId"),
                phone = child.str("phone"),
                address = child.str("address", "Gossaigaon"),
                customerLat = cLat,
                customerLng = cLng,
                locationLink = locLink,
                paymentMode = child.str("paymentMode", "COD"),
                paymentStatus = child.str("paymentStatus", "COD"),
                status = child.str("status"),
                totalAmount = child.dblOrNull("totalAmount") ?: 0.0,
                deliveryCharge = child.dblOrNull("deliveryCharge"),
                distanceKm = child.dblOrNull("riderDistanceKm")
                    ?: child.dblOrNull("distanceKm")
                    ?: child.dblOrNull("deliveryDistanceKm")
                    ?: child.dblOrNull("tripDistanceKm"),
                riderPerKmRate = child.dblOrNull("riderPerKmRate"),
                riderEarning = child.dblOrNull("riderEarning"),
                deliveryUid = deliveryUid,
                assignedRiderId = child.strOrNull("assignedRiderId"),
                riderAccepted = if (child.hasChild("riderAccepted")) child.bool("riderAccepted", false) else null,
                riderAcceptedAt = child.longOrNull("riderAcceptedAt"),
                assignedAt = child.longOrNull("assignedAt"),
                createdAt = child.longOrNull("createdAt"),
                updatedAt = child.longOrNull("updatedAt"),
                deliveredAt = child.longOrNull("deliveredAt"),
                deliveryProofPhoto = child.str("deliveryProofPhoto"),
                pickupOtp = child.strOrNull("pickupOtp")
                    ?: child.strOrNull("pickupCode")
                    ?: child.strOrNull("storePickupCode")
                    ?: child.strOrNull("storePickupOtp")
                    ?: child.strOrNull("storeOtp"),
                pickupOtpHash = child.strOrNull("pickupOtpHash"),
                readyForPickup = child.bool("readyForPickup", false) ||
                    child.bool("restaurantReady", false) ||
                    child.bool("storeReady", false) ||
                    child.bool("isReadyForPickup", false),
                readyForPickupAt = child.longOrNull("readyForPickupAt")
                    ?: child.longOrNull("restaurantReadyAt"),
                pickupVerified = child.bool("pickupVerified", false),
                cancellationLocked = child.bool("cancellationLocked", false) ||
                    child.bool("cancelLocked", false) ||
                    child.bool("isCancelLocked", false),
                deliveryOtp = child.strOrNull("deliveryOtp"),
                deliveryOtpHash = child.strOrNull("deliveryOtpHash"),
                deliveryOtpExpiresAt = child.longOrNull("deliveryOtpExpiresAt"),
                emergencyAlert = child.bool("emergencyAlert", false),
                emergencyDetails = child.str("emergencyDetails"),
                cancellationRequested = child.bool("cancellationRequested", false),
                cancellationReason = child.str("cancellationReason"),
                lastLocationUpdate = child.longOrNull("lastLocationUpdate"),
                riderLat = child.dblOrNull("riderLat"),
                riderLng = child.dblOrNull("riderLng"),
                rejectedByRiders = rejectedSet,
                items = itemsList
            )
        }
        return result
    }

    private fun parseEmergencyRequests(snapshot: DataSnapshot, uid: String): List<EmergencyRequestRecord> {
        val list = mutableListOf<EmergencyRequestRecord>()
        for (child in snapshot.children) {
            val id = child.key ?: continue
            if (child.str("riderId") != uid) continue
            list.add(
                EmergencyRequestRecord(
                    id = id,
                    orderId = child.str("orderId"),
                    riderId = uid,
                    reason = child.str("reason"),
                    description = child.str("description"),
                    status = child.str("status", "PENDING"),
                    createdAt = child.longOrNull("createdAt") ?: 0L
                )
            )
        }
        return list.sortedByDescending { it.createdAt }
    }

    private fun DataSnapshot.str(key: String, default: String = ""): String =
        child(key).getValue(Any::class.java)?.toString() ?: default

    private fun DataSnapshot.strOrNull(key: String): String? =
        child(key).getValue(Any::class.java)?.toString()?.takeIf { it.isNotBlank() }

    private fun DataSnapshot.bool(key: String, default: Boolean = false): Boolean =
        when (val v = child(key).getValue(Any::class.java)) {
            is Boolean -> v
            is String -> v.equals("true", ignoreCase = true)
            is Number -> v.toInt() != 0
            else -> default
        }

    private fun DataSnapshot.dblOrNull(key: String): Double? =
        when (val v = child(key).getValue(Any::class.java)) {
            is Number -> v.toDouble()
            is String -> v.toDoubleOrNull()
            else -> null
        }

    private fun DataSnapshot.longOrNull(key: String): Long? =
        when (val v = child(key).getValue(Any::class.java)) {
            is Number -> v.toLong()
            is String -> v.toLongOrNull()
            else -> null
        }
}
