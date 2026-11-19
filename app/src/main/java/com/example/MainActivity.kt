package com.example

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CurrencyRupee
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.notifications.FleetAppForegroundTracker
import com.example.notifications.FleetNotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.ui.components.AdminAlertsCluster
import com.example.ui.components.ApprovalPendingBanner
import com.example.ui.components.CashLimitBanner
import com.example.ui.components.ConnectivityStatusBanner
import com.example.ui.components.FleetNavigationDrawerSheet
import com.example.ui.components.FleetToastOverlay
import com.example.ui.components.GpsRadarBar
import com.example.ui.components.MaintenanceOverlayScreen
import com.example.ui.components.RiderHeader
import com.example.ui.components.SecurityRibbon
import com.example.ui.components.SecurityThreatBanner
import com.example.ui.screens.ActiveTripsScreen
import com.example.ui.screens.AgreementConsentDialog
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CodCashScreen
import com.example.ui.screens.EarningsScreen
import com.example.ui.screens.FleetModalsHost
import com.example.ui.screens.GoogleOnboardingScreen
import com.example.ui.screens.KycScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.QuickPinUnlockScreen
import com.example.ui.screens.RatingsScreen
import com.example.ui.screens.ShiftDashboardScreen
import com.example.ui.screens.SupportScreen
import com.example.ui.screens.SystemSettingsScreen
import com.example.ui.screens.TripHistoryScreen
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GossaigaonFleetTheme
import com.example.ui.theme.TerminalBg
import com.example.ui.theme.TerminalSurface
import com.example.ui.theme.TextMuted
import com.example.viewmodel.AuthUiState
import com.example.viewmodel.FleetViewModel
import com.example.viewmodel.RiderTab

class MainActivity : ComponentActivity() {
    private val pendingTargetTab = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingTargetTab.value = intent?.getStringExtra(FleetNotificationHelper.EXTRA_TARGET_TAB)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )
        setContent {
            GossaigaonFleetTheme {
                FleetRootScreen(
                    pendingTargetTab = pendingTargetTab.value,
                    onConsumeTargetTab = { pendingTargetTab.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val tab = intent.getStringExtra(FleetNotificationHelper.EXTRA_TARGET_TAB)
        if (!tab.isNullOrBlank()) {
            pendingTargetTab.value = tab
        }
    }

    override fun onStart() {
        super.onStart()
        FleetAppForegroundTracker.setForeground(true)
    }

    override fun onStop() {
        FleetAppForegroundTracker.setForeground(false)
        super.onStop()
    }
}

private data class QuickNavItem(
    val tab: RiderTab?, // null opens the three-line navigation drawer
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val fleetQuickNavItems = listOf(
    QuickNavItem(
        tab = RiderTab.ACTIVE,
        labelRes = R.string.tab_active_trips,
        selectedIcon = Icons.Filled.Navigation,
        unselectedIcon = Icons.Outlined.Navigation,
        testTag = "nav_tab_active"
    ),
    QuickNavItem(
        tab = RiderTab.EARNINGS,
        labelRes = R.string.tab_earnings_cash,
        selectedIcon = Icons.Filled.AccountBalanceWallet,
        unselectedIcon = Icons.Outlined.AccountBalanceWallet,
        testTag = "nav_tab_earnings"
    ),
    QuickNavItem(
        tab = RiderTab.COD_CASH,
        labelRes = R.string.tab_cod_cash,
        selectedIcon = Icons.Filled.CurrencyRupee,
        unselectedIcon = Icons.Outlined.CurrencyRupee,
        testTag = "nav_tab_cod_cash"
    ),
    QuickNavItem(
        tab = null,
        labelRes = R.string.tab_menu_more,
        selectedIcon = Icons.Filled.Menu,
        unselectedIcon = Icons.Outlined.Menu,
        testTag = "nav_tab_menu"
    )
)

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun FleetRootScreen(
    viewModel: FleetViewModel = viewModel(),
    pendingTargetTab: String? = null,
    onConsumeTargetTab: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context.findActivity()
    val lifecycleOwner = LocalLifecycleOwner.current
    var askedNotifOnce by rememberSaveable { mutableStateOf(false) }
    var notifPermissionDeniedOnce by rememberSaveable { mutableStateOf(false) }

    var locationPermissionDeniedOnce by rememberSaveable { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (!granted) locationPermissionDeniedOnce = true
        viewModel.onLocationPermissionResult(granted)
    }

    val requestLocationPermission: () -> Unit = {
        val canPromptAgain = !locationPermissionDeniedOnce ||
            (activity != null && (
                ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_COARSE_LOCATION)
                ))
        if (canPromptAgain) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            runCatching {
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null)
                )
                context.startActivity(intent)
            }
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) notifPermissionDeniedOnce = true
        viewModel.onNotificationPermissionResult(granted)
    }

    val openNotificationSettings: () -> Unit = {
        runCatching {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
            context.startActivity(intent)
        }
    }

    val requestNotifications: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val canPromptAgain = !notifPermissionDeniedOnce ||
                (activity != null && ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.POST_NOTIFICATIONS
                ))
            if (canPromptAgain) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                openNotificationSettings()
            }
        } else {
            openNotificationSettings()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshNotificationPermission()
                viewModel.refreshLocationPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(pendingTargetTab) {
        if (!pendingTargetTab.isNullOrBlank()) {
            viewModel.selectTabByName(pendingTargetTab)
            onConsumeTargetTab()
        }
    }

    LaunchedEffect(
        uiState.authState,
        uiState.isProfileLoaded,
        uiState.needsGoogleOnboarding,
        uiState.isPinLocked,
        uiState.needsAgreementAcceptance
    ) {
        val inTerminal = uiState.authState is AuthUiState.SignedIn &&
            uiState.isProfileLoaded &&
            !uiState.needsGoogleOnboarding &&
            !uiState.isPinLocked &&
            !uiState.needsAgreementAcceptance
        if (inTerminal &&
            !askedNotifOnce &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !uiState.hasNotificationPermission &&
            uiState.savedAuthPrefs.backgroundAlertsEnabled
        ) {
            askedNotifOnce = true
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(uiState.needsLocationPermission) {
        if (uiState.needsLocationPermission) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    LaunchedEffect(uiState.chimeCounter) {
        if (uiState.chimeCounter > 0 && uiState.soundEnabled) {
            withContext(Dispatchers.IO) {
                var tg: ToneGenerator? = null
                try {
                    tg = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85)
                    tg.startTone(ToneGenerator.TONE_PROP_BEEP2, 320)
                    delay(380L)
                } catch (_: Throwable) {
                } finally {
                    runCatching { tg?.release() }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TerminalBg)
    ) {
        when (val auth = uiState.authState) {
            AuthUiState.Checking -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = EmeraldPrimary)
                }
            }

            AuthUiState.SignedOut -> {
                AuthScreen(
                    isLoading = uiState.isAuthLoading,
                    isGoogleLoading = uiState.isGoogleLoading,
                    authError = uiState.authError,
                    isResetLoading = uiState.isResetLoading,
                    resetMessage = uiState.resetMessage,
                    savedAuthPrefs = uiState.savedAuthPrefs,
                    agreementVersion = uiState.adminSettings.agreementVersion,
                    onClearError = viewModel::clearAuthError,
                    onClearResetMessage = viewModel::clearResetMessage,
                    onOpenAgreement = viewModel::openAgreementModal,
                    onSignIn = viewModel::signIn,
                    onGoogleSignIn = {
                        activity?.let(viewModel::signInWithGoogle)
                    },
                    onSendPasswordReset = viewModel::sendPasswordReset,
                    onRegister = viewModel::registerRider
                )
            }

            is AuthUiState.SignedIn -> {
                if (!uiState.isProfileLoaded) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = EmeraldPrimary)
                    }
                } else if (uiState.needsGoogleOnboarding) {
                    GoogleOnboardingScreen(
                        email = auth.email,
                        initialName = uiState.rider.name.ifBlank { auth.displayName },
                        agreementVersion = uiState.adminSettings.agreementVersion,
                        isLoading = uiState.isAuthLoading,
                        authError = uiState.authError,
                        onClearError = viewModel::clearAuthError,
                        onOpenAgreement = viewModel::openAgreementModal,
                        onSubmit = viewModel::completeGoogleOnboarding,
                        onSignOut = viewModel::signOut
                    )
                } else if (uiState.isPinLocked) {
                    QuickPinUnlockScreen(
                        riderName = uiState.rider.name.ifBlank { uiState.savedAuthPrefs.lastRiderName },
                        email = auth.email,
                        pinError = uiState.quickPinError,
                        onUnlock = viewModel::unlockWithQuickPin,
                        onSwitchAccount = viewModel::signOut
                    )
                } else {
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val drawerScope = rememberCoroutineScope()
                val quickTabs = setOf(RiderTab.ACTIVE, RiderTab.EARNINGS, RiderTab.COD_CASH)
                val openTicketsCount = uiState.feedbacks.count { it.status != "RESOLVED" }

                if (drawerState.isOpen) {
                    BackHandler { drawerScope.launch { drawerState.close() } }
                } else if (uiState.selectedTab != RiderTab.ACTIVE) {
                    BackHandler { viewModel.selectTab(RiderTab.ACTIVE) }
                }

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        FleetNavigationDrawerSheet(
                            rider = uiState.rider,
                            selectedTab = uiState.selectedTab,
                            activeTripsCount = uiState.activeTrips.size,
                            completedTripsCount = uiState.completedTrips.size,
                            availablePayoutBalance = uiState.availablePayoutBalance,
                            netCashInHand = uiState.netCashInHand,
                            maxCashLimit = uiState.adminSettings.maxRiderCashLimit,
                            openTicketsCount = openTicketsCount,
                            onSelectTab = { tab ->
                                viewModel.selectTab(tab)
                                drawerScope.launch { drawerState.close() }
                            },
                            onCloseDrawer = { drawerScope.launch { drawerState.close() } },
                            onSignOut = {
                                drawerScope.launch { drawerState.close() }
                                viewModel.signOut()
                            }
                        )
                    }
                ) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val isWideScreen = maxWidth >= 600.dp

                    Scaffold(
                        containerColor = TerminalBg,
                        contentWindowInsets = WindowInsets.navigationBars,
                        bottomBar = {
                            if (!isWideScreen) {
                                NavigationBar(
                                    containerColor = TerminalSurface,
                                    tonalElevation = 4.dp
                                ) {
                                    fleetQuickNavItems.forEach { item ->
                                        val selected = if (item.tab != null) {
                                            uiState.selectedTab == item.tab
                                        } else {
                                            drawerState.isOpen || uiState.selectedTab !in quickTabs
                                        }
                                        val activeBadgeCount = if (item.tab == RiderTab.ACTIVE) uiState.activeTrips.size else 0
                                        NavigationBarItem(
                                            selected = selected,
                                            onClick = {
                                                if (item.tab != null) {
                                                    viewModel.selectTab(item.tab)
                                                } else {
                                                    drawerScope.launch { drawerState.open() }
                                                }
                                            },
                                            icon = {
                                                BadgedBox(
                                                    badge = {
                                                        if (activeBadgeCount > 0) {
                                                            Badge(containerColor = EmeraldPrimary, contentColor = Color.White) {
                                                                Text(activeBadgeCount.toString())
                                                            }
                                                        }
                                                    }
                                                ) {
                                                    Icon(
                                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                                        contentDescription = stringResource(item.labelRes)
                                                    )
                                                }
                                            },
                                            label = {
                                                Text(
                                                    text = stringResource(item.labelRes),
                                                    style = MaterialTheme.typography.labelMedium
                                                )
                                            },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = EmeraldPrimary,
                                                selectedTextColor = EmeraldPrimary,
                                                unselectedIconColor = TextMuted,
                                                unselectedTextColor = TextMuted,
                                                indicatorColor = EmeraldPrimary.copy(alpha = 0.16f)
                                            ),
                                            modifier = Modifier.testTag(item.testTag)
                                        )
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            if (isWideScreen) {
                                NavigationRail(
                                    containerColor = TerminalSurface,
                                    modifier = Modifier.fillMaxHeight().statusBarsPadding()
                                ) {
                                    fleetQuickNavItems.forEach { item ->
                                        val selected = if (item.tab != null) {
                                            uiState.selectedTab == item.tab
                                        } else {
                                            drawerState.isOpen || uiState.selectedTab !in quickTabs
                                        }
                                        val activeBadgeCount = if (item.tab == RiderTab.ACTIVE) uiState.activeTrips.size else 0
                                        NavigationRailItem(
                                            selected = selected,
                                            onClick = {
                                                if (item.tab != null) {
                                                    viewModel.selectTab(item.tab)
                                                } else {
                                                    drawerScope.launch { drawerState.open() }
                                                }
                                            },
                                            icon = {
                                                BadgedBox(
                                                    badge = {
                                                        if (activeBadgeCount > 0) {
                                                            Badge(containerColor = EmeraldPrimary, contentColor = Color.White) {
                                                                Text(activeBadgeCount.toString())
                                                            }
                                                        }
                                                    }
                                                ) {
                                                    Icon(
                                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                                        contentDescription = stringResource(item.labelRes)
                                                    )
                                                }
                                            },
                                            label = {
                                                Text(
                                                    text = stringResource(item.labelRes),
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            },
                                            colors = NavigationRailItemDefaults.colors(
                                                selectedIconColor = EmeraldPrimary,
                                                selectedTextColor = EmeraldPrimary,
                                                unselectedIconColor = TextMuted,
                                                unselectedTextColor = TextMuted,
                                                indicatorColor = EmeraldPrimary.copy(alpha = 0.16f)
                                            ),
                                            modifier = Modifier.testTag(item.testTag)
                                        )
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .statusBarsPadding(),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .widthIn(max = 600.dp)
                                ) {
                                    SecurityRibbon()
                                    ConnectivityStatusBanner(
                                        isNetworkOnline = uiState.isNetworkOnline,
                                        isFirebaseConnected = uiState.isFirebaseConnected
                                    )
                                    AdminAlertsCluster(
                                        rider = uiState.rider,
                                        adminSettings = uiState.adminSettings
                                    )
                                    RiderHeader(
                                        rider = uiState.rider,
                                        soundEnabled = uiState.soundEnabled,
                                        onOpenMenu = { drawerScope.launch { drawerState.open() } },
                                        onToggleSound = viewModel::toggleSound,
                                        onToggleDuty = viewModel::toggleDutyStatus
                                    )
                                    GpsRadarBar(
                                        gpsState = uiState.gpsState,
                                        speedKmh = uiState.riderSpeedKmh,
                                        onRequestPermission = requestLocationPermission,
                                        onForceSync = viewModel::forceGpsSync
                                    )
                                    SecurityThreatBanner(
                                        report = uiState.securityReport,
                                        onOpenSecuritySettings = { viewModel.selectTab(RiderTab.SETTINGS) }
                                    )
                                    if (!uiState.hasNotificationPermission && uiState.savedAuthPrefs.backgroundAlertsEnabled) {
                                        Surface(
                                            color = AmberWarning.copy(alpha = 0.14f),
                                            border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.6f)),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Icon(
                                                        Icons.Default.NotificationsOff,
                                                        contentDescription = null,
                                                        tint = AmberWarning,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(Modifier.width(8.dp))
                                                    Text(
                                                        text = "Enable background alerts for live trips",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = AmberWarning
                                                    )
                                                }
                                                Spacer(Modifier.width(8.dp))
                                                Button(
                                                    onClick = requestNotifications,
                                                    colors = ButtonDefaults.buttonColors(containerColor = AmberWarning),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.testTag("banner_enable_notifications_button")
                                                ) {
                                                    Text("Enable", color = Color.Black, style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                        }
                                    }
                                    CashLimitBanner(
                                        netCashInHand = uiState.netCashInHand,
                                        maxCashLimit = uiState.adminSettings.maxRiderCashLimit,
                                        onDepositClick = {
                                            viewModel.selectTab(RiderTab.COD_CASH)
                                            viewModel.openCodDepositModal()
                                        }
                                    )
                                    ApprovalPendingBanner(approved = uiState.rider.approved)

                                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                        AnimatedContent(
                                            targetState = uiState.selectedTab,
                                            transitionSpec = {
                                                fadeIn(animationSpec = tween(180)) togetherWith fadeOut(animationSpec = tween(140))
                                            },
                                            label = "rider_tab_transition"
                                        ) { tab ->
                                            when (tab) {
                                                RiderTab.ACTIVE -> ActiveTripsScreen(
                                                    activeTrips = uiState.activeTrips,
                                                    stores = uiState.stores,
                                                    adminSettings = uiState.adminSettings,
                                                    countdownByTrip = uiState.countdownByTrip,
                                                    emergencyRequests = uiState.emergencyRequests,
                                                    riderLat = uiState.riderLat ?: uiState.rider.lat,
                                                    riderLng = uiState.riderLng ?: uiState.rider.lng,
                                                    getRiderFee = viewModel::getRiderFeeForOrder,
                                                    onLocationPermissionGranted = { viewModel.onLocationPermissionResult(true) },
                                                    onAcceptTrip = viewModel::acceptTrip,
                                                    onPassTripDirect = viewModel::passTripToNextRider,
                                                    onOpenPassModal = viewModel::openPassTripModal,
                                                    onOpenPickupVerify = viewModel::openPickupVerifyModal,
                                                    onStartDoorstep = viewModel::startCustomerDoorstepTrip,
                                                    onOpenDeliveryVerify = viewModel::openDeliveryVerifyModal,
                                                    onOpenSosModal = viewModel::openEmergencySosModal
                                                )

                                                RiderTab.DASHBOARD -> ShiftDashboardScreen(
                                                    adminSettings = uiState.adminSettings,
                                                    totalRiderEarned = uiState.totalRiderEarned,
                                                    todayTripsCount = uiState.todayTripsCount,
                                                    todayEarnings = uiState.todayEarnings,
                                                    todayCodCollected = uiState.todayCodCollected,
                                                    isOnline = uiState.rider.online && !uiState.rider.suspended,
                                                    shiftElapsedSeconds = uiState.shiftElapsedSeconds,
                                                    weekTripsCount = uiState.weekTripsCount,
                                                    weekEarnings = uiState.weekEarnings,
                                                    completedTrips = uiState.completedTrips,
                                                    stores = uiState.stores
                                                )

                                                RiderTab.EARNINGS -> EarningsScreen(
                                                    rider = uiState.rider,
                                                    adminSettings = uiState.adminSettings,
                                                    availablePayoutBalance = uiState.availablePayoutBalance,
                                                    pendingPayoutAmount = uiState.pendingPayoutAmount,
                                                    totalRiderEarned = uiState.totalRiderEarned,
                                                    totalRiderPaidPayout = uiState.totalRiderPaidPayout,
                                                    payouts = uiState.riderPayouts,
                                                    onOpenWithdrawModal = viewModel::openPayoutModal,
                                                    onOpenProfile = { viewModel.selectTab(RiderTab.PROFILE) }
                                                )

                                                RiderTab.COD_CASH -> CodCashScreen(
                                                    adminSettings = uiState.adminSettings,
                                                    netCashInHand = uiState.netCashInHand,
                                                    pendingCodSettlementAmount = uiState.pendingCodSettlementAmount,
                                                    totalCodCollected = uiState.totalCodCollected,
                                                    totalDeposited = uiState.totalDeposited,
                                                    settlements = uiState.riderSettlements,
                                                    onOpenDepositModal = viewModel::openCodDepositModal
                                                )

                                                RiderTab.TRIP_HISTORY -> TripHistoryScreen(
                                                    adminSettings = uiState.adminSettings,
                                                    completedTrips = uiState.completedTrips,
                                                    stores = uiState.stores
                                                )

                                                RiderTab.RATINGS -> RatingsScreen(
                                                    rider = uiState.rider,
                                                    ratings = uiState.ratings
                                                )

                                                RiderTab.PROFILE -> ProfileScreen(
                                                    rider = uiState.rider,
                                                    email = auth.email,
                                                    onOpenKyc = { viewModel.selectTab(RiderTab.KYC) },
                                                    onSaveProfile = viewModel::saveRiderProfile
                                                )

                                                RiderTab.KYC -> KycScreen(
                                                    rider = uiState.rider,
                                                    isKycSaving = uiState.isKycSaving,
                                                    onSubmitKyc = viewModel::submitRiderKyc
                                                )

                                                RiderTab.SUPPORT -> SupportScreen(
                                                    adminSettings = uiState.adminSettings,
                                                    feedbacks = uiState.feedbacks,
                                                    onOpenFeedbackModal = viewModel::openFeedbackModal
                                                )

                                                RiderTab.SETTINGS -> SystemSettingsScreen(
                                                    rider = uiState.rider,
                                                    adminSettings = uiState.adminSettings,
                                                    hasQuickPinEnabled = uiState.savedAuthPrefs.quickPinUid == auth.uid &&
                                                        uiState.savedAuthPrefs.quickPinHash.isNotBlank(),
                                                    backgroundAlertsEnabled = uiState.savedAuthPrefs.backgroundAlertsEnabled,
                                                    hasNotificationPermission = uiState.hasNotificationPermission,
                                                    isBackgroundServiceActive = uiState.isBackgroundServiceActive,
                                                    isNetworkOnline = uiState.isNetworkOnline,
                                                    isFirebaseConnected = uiState.isFirebaseConnected,
                                                    gpsState = uiState.gpsState,
                                                    riderSpeedKmh = uiState.riderSpeedKmh,
                                                    batteryPct = uiState.batteryPct,
                                                    securityReport = uiState.securityReport,
                                                    blockedThreatsCount = uiState.blockedThreatsCount,
                                                    onRunSecurityScan = { viewModel.runSecurityShieldScan(showResultToast = true) },
                                                    onForceGpsSync = viewModel::forceGpsSync,
                                                    onToggleBackgroundAlerts = viewModel::setBackgroundAlertsEnabled,
                                                    onRequestNotificationPermission = requestNotifications,
                                                    onSendTestNotification = viewModel::sendTestNotification,
                                                    onOpenAgreement = viewModel::openAgreementModal,
                                                    onSetQuickPin = viewModel::setQuickShiftPin,
                                                    onDisableQuickPin = viewModel::disableQuickShiftPin,
                                                    onSignOut = viewModel::signOut
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    FleetModalsHost(
                        activeModal = uiState.activeModal,
                        isBusy = uiState.isModalBusy,
                        rider = uiState.rider,
                        adminSettings = uiState.adminSettings,
                        availablePayoutBalance = uiState.availablePayoutBalance,
                        netCashInHand = uiState.netCashInHand,
                        pendingCodSettlementAmount = uiState.pendingCodSettlementAmount,
                        onDismiss = viewModel::closeModal,
                        onConfirmPickupOtp = viewModel::confirmPickupOtp,
                        onConfirmDeliveryPin = viewModel::confirmDeliveryPin,
                        onConfirmPassTrip = viewModel::confirmPassTrip,
                        onSubmitEmergencySos = viewModel::submitEmergencySos,
                        onSubmitPayout = viewModel::submitPayoutRequest,
                        onSubmitCodDeposit = viewModel::submitCodDeposit,
                        onSubmitFeedback = viewModel::submitFeedback
                    )
                }
                }
                }
            }
        }

        val showMandatoryAgreement = uiState.authState is AuthUiState.SignedIn &&
            uiState.isProfileLoaded &&
            !uiState.needsGoogleOnboarding &&
            !uiState.isPinLocked &&
            uiState.needsAgreementAcceptance

        val viewerTab = uiState.agreementViewerTab
        if (showMandatoryAgreement || viewerTab != null) {
            val mandatory = showMandatoryAgreement
            val canAcceptAgreement = mandatory || (
                uiState.authState is AuthUiState.SignedIn &&
                    uiState.isProfileLoaded &&
                    !uiState.needsGoogleOnboarding &&
                    !uiState.isPinLocked
                )
            AgreementConsentDialog(
                adminSettings = uiState.adminSettings,
                initialTab = viewerTab ?: 0,
                isMandatory = mandatory,
                canAccept = canAcceptAgreement,
                alreadyAccepted = uiState.authState is AuthUiState.SignedIn &&
                    uiState.isProfileLoaded &&
                    uiState.rider.hasAcceptedLatestAgreement(uiState.adminSettings),
                acceptedAtMs = uiState.rider.agreementAcceptedAt,
                isBusy = uiState.isAgreementSaving,
                onAccept = viewModel::acceptRiderAgreement,
                onDeclineOrClose = {
                    if (mandatory) {
                        viewModel.signOut()
                    } else {
                        viewModel.closeAgreementModal()
                    }
                }
            )
        }

        if (uiState.adminSettings.maintenanceMode) {
            MaintenanceOverlayScreen(adminSettings = uiState.adminSettings)
        }

        Box(modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
            FleetToastOverlay(
                toast = uiState.toast,
                onDismiss = viewModel::dismissToast
            )
        }
    }
}
