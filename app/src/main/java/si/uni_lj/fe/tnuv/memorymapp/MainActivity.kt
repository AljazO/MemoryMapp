package si.uni_lj.fe.tnuv.memorymapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import si.uni_lj.fe.tnuv.memorymapp.ui.screens.*
import si.uni_lj.fe.tnuv.memorymapp.ui.theme.DarkBg
import si.uni_lj.fe.tnuv.memorymapp.ui.theme.MemoryMappTheme

import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import si.uni_lj.fe.tnuv.memorymapp.service.LocationService
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import java.util.Calendar
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import si.uni_lj.fe.tnuv.memorymapp.service.MediaScanner

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MemoryMappTheme {
                MemoryMappApp()
            }
        }
    }
}

@Composable
fun MemoryMappApp() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    var isTracking by remember { mutableStateOf(LocationService.isRunning) }
    
    // Media Scanner instance
    val mediaScanner = remember { MediaScanner(context) }

    // Content Observer to sync with gallery real-time (Deletions/Additions)
    DisposableEffect(Unit) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                scope.launch {
                    mediaScanner.scanGallery()
                }
            }
        }
        
        context.contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            observer
        )
        context.contentResolver.registerContentObserver(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            true,
            observer
        )
        
        onDispose {
            context.contentResolver.unregisterContentObserver(observer)
        }
    }

    // Shared period state
    var sharedStartDate by remember { mutableStateOf(Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }) }
    var sharedEndDate by remember { mutableStateOf(Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }) }
    
    var shouldShowAddTripInitially by remember { mutableStateOf(false) }

    // Handle Notification Permission for Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        // Initial scan on startup
        scope.launch {
            mediaScanner.scanGallery()
        }
    }

    // Start/Stop Service based on tracking state
    LaunchedEffect(isTracking) {
        val intent = Intent(context, LocationService::class.java)
        if (isTracking) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } else {
            context.stopService(intent)
        }
    }

    // Close drawer when route changes to prevent it from starting open on new screens
    LaunchedEffect(currentRoute) {
        drawerState.close()
    }

    // Define which screens should show the bottom bar and drawer
    val showMainUI = currentRoute in listOf("activity", "memories", "trips", "account_settings")

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            if (showMainUI) {
                ModalDrawerSheet(
                    drawerContainerColor = DarkBg,
                    modifier = Modifier.width(300.dp)
                ) {
                    SidebarContent(
                        isTracking = isTracking,
                        onToggleTracking = { isTracking = !isTracking },
                        onCloseClick = { scope.launch { drawerState.close() } }
                    )
                }
            }
        }
    ) {
        Scaffold(
            bottomBar = {
                if (showMainUI) {
                    NavigationBar(
                        containerColor = DarkBg,
                        contentColor = Color.White,
                        tonalElevation = 0.dp
                    ) {
                        NavigationBarItem(
                            selected = currentRoute == "activity",
                            onClick = { navController.navigate("activity") },
                            icon = { Icon(Icons.AutoMirrored.Filled.DirectionsRun, contentDescription = null) },
                            label = { Text("Activity") },
                            colors = navItemColors()
                        )
                        NavigationBarItem(
                            selected = currentRoute == "memories",
                            onClick = { navController.navigate("memories") },
                            icon = { Icon(Icons.Default.Image, contentDescription = null) },
                            label = { Text("Memories") },
                            colors = navItemColors()
                        )
                        NavigationBarItem(
                            selected = currentRoute == "trips",
                            onClick = { navController.navigate("trips") },
                            icon = { Icon(Icons.Default.Route, contentDescription = null) },
                            label = { Text("Saved trips") },
                            colors = navItemColors()
                        )
                        NavigationBarItem(
                            selected = currentRoute == "account_settings",
                            onClick = { navController.navigate("account_settings") },
                            icon = { Icon(Icons.Default.AccountCircle, contentDescription = null) },
                            label = { Text("Account") },
                            colors = navItemColors()
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController, 
                startDestination = "title",
                modifier = Modifier.padding(if (showMainUI) innerPadding else PaddingValues(0.dp))
            ) {
                composable("title") {
                    TitleScreen(onStartClick = { navController.navigate("account") })
                }
                composable("account") {
                    AccountScreen(
                        onBackClick = { navController.popBackStack() },
                        onCreateAccountClick = { navController.navigate("activity") }
                    )
                }
                composable("activity") {
                    ActivityScreen(
                        onMenuClick = { scope.launch { drawerState.open() } },
                        isTracking = isTracking,
                        onToggleTracking = { isTracking = it },
                        startDate = sharedStartDate,
                        endDate = sharedEndDate,
                        onPeriodChange = { start, end ->
                            sharedStartDate = start
                            sharedEndDate = end
                        },
                        onAddTrip = {
                            shouldShowAddTripInitially = true
                            navController.navigate("trips")
                        }
                    )
                }
                composable("memories") {
                    MemoriesScreen(
                        onMenuClick = { scope.launch { drawerState.open() } },
                        startDate = sharedStartDate,
                        endDate = sharedEndDate,
                        onPeriodChange = { start, end ->
                            sharedStartDate = start
                            sharedEndDate = end
                        },
                        onAddTrip = {
                            shouldShowAddTripInitially = true
                            navController.navigate("trips")
                        }
                    )
                }
                composable("trips") {
                    TripsScreen(
                        onMenuClick = { scope.launch { drawerState.open() } },
                        initialStartDate = sharedStartDate,
                        initialEndDate = sharedEndDate,
                        showAddInitially = shouldShowAddTripInitially
                    )
                    // Reset flag after use
                    SideEffect {
                        shouldShowAddTripInitially = false
                    }
                }
                composable("account_settings") {
                    AccountSettingsScreen(
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onLogoutClick = { 
                            navController.navigate("account") {
                                popUpTo("activity") { inclusive = true }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun navItemColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = Color(0xFF6E6EF7),
    selectedTextColor = Color(0xFF6E6EF7),
    unselectedIconColor = Color.White,
    unselectedTextColor = Color.White,
    indicatorColor = Color.Transparent
)
