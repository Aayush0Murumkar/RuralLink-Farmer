package com.example.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.local.AppDatabase
import com.example.data.model.Farmer
import com.example.services.AuthService
import com.example.services.NotificationService
import com.example.services.RequestService
import com.example.services.VoiceService
import com.example.ui.theme.*
import com.example.ui.screens.*
import com.example.ui.theme.AppSecondaryBrown
import com.example.ui.theme.AppSecondaryContainer
import com.example.ui.theme.AppTertiaryGreen
import com.example.ui.theme.AppTertiaryContainer
import kotlinx.coroutines.launch

object FarmerRoutes {
    const val WELCOME = "welcome"
    const val REGISTER = "farmer/register"
    const val VERIFY = "farmer/verify"
    const val TWO_FACTOR_AUTH = "farmer/two_factor_auth"
    const val DASHBOARD = "farmer/dashboard"
    const val BOOK = "farmer/book"
    const val REQUESTS = "farmer/requests"
    const val REQUEST_DETAIL = "farmer/requests/{requestId}"
    const val NOTIFICATIONS = "farmer/notifications"
    const val PROFILE = "farmer/profile"
    const val AGRIMATCH = "farmer/agrimatch"
    const val DIAGNOSTICS = "farmer/diagnostics"
}

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Dashboard : BottomNavItem(FarmerRoutes.DASHBOARD, "Home", Icons.Default.Home)
    object Book : BottomNavItem(FarmerRoutes.BOOK, "Book", Icons.Default.AddCircle)
    object Requests : BottomNavItem(FarmerRoutes.REQUESTS, "Requests", Icons.Default.LocalShipping)
    object Notifications : BottomNavItem(FarmerRoutes.NOTIFICATIONS, "Notifications", Icons.Default.Notifications)
    object Profile : BottomNavItem(FarmerRoutes.PROFILE, "Profile", Icons.Default.Person)
}

@Composable
fun RuralLinkFarmerAppNavHost(
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val authService = remember { AuthService(context) }
    val requestService = remember { RequestService(context) }
    val notificationService = remember { NotificationService(context) }

    val currentFarmerDb by authService.currentFarmer.collectAsStateWithLifecycle(initialValue = null)
    val fallbackFarmer = remember {
        Farmer(
            id = AuthService.MOCK_FARMER_ID,
            name = "Ramesh Patil",
            address = "Nashik APMC Market Yard, Maharashtra",
            mobileNumber = "9876543210",
            email = "ramesh.farmer@gmail.com",
            aadhaarMasked = "XXXX XXXX 8899",
            mobileVerified = true
        )
    }
    val currentFarmer = currentFarmerDb ?: fallbackFarmer
    val activeFarmerId = currentFarmer.id

    // Startup Session & Profile Check
    LaunchedEffect(Unit) {
        authService.checkSessionOnStart()
    }

    // Initial seed check on launch
    LaunchedEffect(activeFarmerId) {
        if (activeFarmerId.isNotBlank()) {
            requestService.seedInitialDataIfEmpty(activeFarmerId)
        }
    }

    val requests by requestService.getFarmerRequests(activeFarmerId).collectAsStateWithLifecycle(initialValue = emptyList())
    val notifications by notificationService.notifications.collectAsStateWithLifecycle(initialValue = emptyList())

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showNav = currentRoute in listOf(
        FarmerRoutes.DASHBOARD,
        FarmerRoutes.BOOK,
        FarmerRoutes.REQUESTS,
        FarmerRoutes.NOTIFICATIONS,
        FarmerRoutes.PROFILE
    )

    var pendingVoiceData by remember { mutableStateOf<VoiceService.VoiceParsedRequest?>(null) }
    var authErrorMessage by remember { mutableStateOf<String?>(null) }

    BoxWithConstraints {
        val isWideScreen = maxWidth >= 720.dp

        Row(modifier = Modifier.fillMaxSize()) {
            // Desktop / Tablet Sidebar Navigation (Rule #9)
            if (isWideScreen && showNav) {
                    FarmerNavigationRail(
                    currentRoute = currentRoute,
                    unreadCount = notifications.count { !it.read },
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(FarmerRoutes.DASHBOARD) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            Scaffold(
                modifier = Modifier.weight(1f),
                bottomBar = {
                    if (!isWideScreen) {
                        AnimatedVisibility(
                            visible = showNav,
                            enter = slideInVertically(initialOffsetY = { it }),
                            exit = slideOutVertically(targetOffsetY = { it })
                        ) {
                            FarmerBottomBar(
                                currentRoute = currentRoute,
                                unreadCount = notifications.count { !it.read },
                                onNavigate = { route ->
                                    navController.navigate(route) {
                                        popUpTo(FarmerRoutes.DASHBOARD) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = FarmerRoutes.WELCOME,
                    modifier = Modifier.padding(innerPadding)
                ) {
                    // 1. Welcome Screen
                    composable(FarmerRoutes.WELCOME) {
                        WelcomeScreen(
                            onContinueAsFarmer = { selectedFarmer ->
                                coroutineScope.launch {
                                    authService.loginAsMockFarmer(selectedFarmer)
                                    navController.navigate(FarmerRoutes.DASHBOARD) {
                                        popUpTo(FarmerRoutes.WELCOME) { inclusive = true }
                                    }
                                }
                            },
                            onNavigateToRegister = {
                                navController.navigate(FarmerRoutes.REGISTER)
                            }
                        )
                    }

                    // 2. Register / Sign In Screen
                    composable(FarmerRoutes.REGISTER) {
                        FarmerRegisterScreen(
                            onBack = { navController.popBackStack() },
                            errorMessageExternal = authErrorMessage,
                            onRegisterSuccess = { name, address, mobile, aadhaar, email, password ->
                                authErrorMessage = null
                                coroutineScope.launch {
                                    val res = authService.registerFarmer(
                                        name = name,
                                        address = address,
                                        mobileNumber = mobile,
                                        aadhaarInput = aadhaar,
                                        email = email,
                                        password = password
                                    )
                                    if (res is com.example.services.AuthResult.Success) {
                                        authErrorMessage = null
                                        navController.navigate(FarmerRoutes.VERIFY)
                                    } else if (res is com.example.services.AuthResult.Error) {
                                        authErrorMessage = res.message
                                    }
                                }
                            },
                            onSignInSuccess = { email, password ->
                                authErrorMessage = null
                                coroutineScope.launch {
                                    val res = authService.signInFarmer(email, password)
                                    if (res is com.example.services.AuthResult.Success) {
                                        authErrorMessage = null
                                        navController.navigate(FarmerRoutes.TWO_FACTOR_AUTH) {
                                            popUpTo(FarmerRoutes.REGISTER) { inclusive = true }
                                        }
                                    } else if (res is com.example.services.AuthResult.Error) {
                                        authErrorMessage = res.message
                                    }
                                }
                            }
                        )
                    }

                    // 3. Verify Screen
                    composable(FarmerRoutes.VERIFY) {
                        FarmerVerifyScreen(
                            mobileNumber = currentFarmer?.mobileNumber ?: "9876543210",
                            onBack = { navController.popBackStack() },
                            onChangeMobile = {
                                navController.navigate(FarmerRoutes.REGISTER) {
                                    popUpTo(FarmerRoutes.REGISTER) { inclusive = true }
                                }
                            },
                            onVerifyOtp = { otp, onError ->
                                coroutineScope.launch {
                                    val result = authService.verifyMobileOtp(otp)
                                    if (result is com.example.services.AuthResult.Success) {
                                        navController.navigate(FarmerRoutes.TWO_FACTOR_AUTH) {
                                            popUpTo(FarmerRoutes.VERIFY) { inclusive = true }
                                        }
                                    } else if (result is com.example.services.AuthResult.Error) {
                                        onError(result.message)
                                    }
                                }
                            },
                            onResendOtp = { onSuccess, onError ->
                                coroutineScope.launch {
                                    val mobile = currentFarmer?.mobileNumber ?: "9876543210"
                                    val result = authService.resendOtp(mobile)
                                    if (result is com.example.services.AuthResult.Success) {
                                        onSuccess("Verification code sent to +91 $mobile.")
                                    } else if (result is com.example.services.AuthResult.Error) {
                                        onError(result.message)
                                    }
                                }
                            }
                        )
                    }

                    // 3.5. Two Factor Auth Screen
                    composable(FarmerRoutes.TWO_FACTOR_AUTH) {
                        TwoFactorAuthScreen(
                            onBack = { navController.popBackStack() },
                            onVerifySuccess = {
                                navController.navigate(FarmerRoutes.DASHBOARD) {
                                    popUpTo(FarmerRoutes.TWO_FACTOR_AUTH) { inclusive = true }
                                }
                            }
                        )
                    }

                    // 4. Dashboard Screen
                    composable(FarmerRoutes.DASHBOARD) {
                        FarmerDashboardScreen(
                            farmer = currentFarmer,
                            requests = requests,
                            notifications = notifications,
                            onBookTransportClick = { navController.navigate(FarmerRoutes.BOOK) },
                            onViewAllRequestsClick = { navController.navigate(FarmerRoutes.REQUESTS) },
                            onViewNotificationsClick = { navController.navigate(FarmerRoutes.NOTIFICATIONS) },
                            onProfileClick = { navController.navigate(FarmerRoutes.PROFILE) },
                            onAgriMatchClick = { navController.navigate(FarmerRoutes.AGRIMATCH) },
                            onNavigateToDiagnostics = { navController.navigate(FarmerRoutes.DIAGNOSTICS) },
                            onRequestSelected = { req ->
                                navController.navigate("farmer/requests/${req.id}")
                            },
                            onVoiceRequestParsed = { voiceParsed ->
                                pendingVoiceData = voiceParsed
                                navController.navigate(FarmerRoutes.BOOK)
                            }
                        )
                    }

            // 5. Book Transport Screen
            composable(FarmerRoutes.BOOK) {
                val voiceData = pendingVoiceData
                BookTransportScreen(
                    initialVoiceData = voiceData,
                    onBack = { navController.popBackStack() },
                    onViewRequestsClick = {
                        pendingVoiceData = null
                        navController.navigate(FarmerRoutes.REQUESTS) {
                            popUpTo(FarmerRoutes.DASHBOARD) { inclusive = false }
                        }
                    },
                    onBackToDashboardClick = {
                        pendingVoiceData = null
                        navController.navigate(FarmerRoutes.DASHBOARD) {
                            popUpTo(FarmerRoutes.DASHBOARD) { inclusive = false }
                        }
                    },
                    onSubmitRequest = { material, weight, unit, pickup, drop, date, time, notes ->
                        val farmerIdToUse = if (activeFarmerId.isNotBlank()) activeFarmerId else (authService.getCurrentUserId() ?: "")
                        val req = requestService.createRequest(
                            farmerId = farmerIdToUse,
                            pickupPoint = pickup,
                            dropPoint = drop,
                            weight = weight,
                            weightUnit = unit,
                            materialType = material,
                            requiredDate = date,
                            requiredTime = time,
                            notes = notes
                        )
                        pendingVoiceData = null
                        req
                    }
                )
            }

            // 6. Requests List Screen
            composable(FarmerRoutes.REQUESTS) {
                LaunchedEffect(activeFarmerId) {
                    if (activeFarmerId.isNotBlank()) {
                        requestService.syncSupabaseRequests(activeFarmerId)
                    }
                }
                RequestsListScreen(
                    requests = requests,
                    onRequestClick = { reqId ->
                        navController.navigate("farmer/requests/$reqId")
                    },
                    onNewBookingClick = { navController.navigate(FarmerRoutes.BOOK) },
                    onRefresh = {
                        coroutineScope.launch {
                            requestService.syncSupabaseRequests(activeFarmerId)
                            requestService.seedInitialDataIfEmpty(activeFarmerId)
                        }
                    }
                )
            }

            // 6b. Request Detail Screen
            composable(
                route = FarmerRoutes.REQUEST_DETAIL,
                arguments = listOf(androidx.navigation.navArgument("requestId") { type = androidx.navigation.NavType.StringType })
            ) { backStackEntry ->
                val requestId = backStackEntry.arguments?.getString("requestId") ?: ""
                RequestDetailScreen(
                    requestId = requestId,
                    farmerId = activeFarmerId,
                    requestService = requestService,
                    onBack = { navController.popBackStack() },
                    onBookNewTransport = { navController.navigate(FarmerRoutes.BOOK) }
                )
            }

            // 7. Notifications Screen
            composable(FarmerRoutes.NOTIFICATIONS) {
                NotificationsScreen(
                    notifications = notifications,
                    onNotificationClick = { notif ->
                        coroutineScope.launch {
                            notificationService.markAsRead(notif.id)
                        }
                    },
                    onMarkAllAsRead = {
                        coroutineScope.launch {
                            notificationService.markAllAsRead()
                        }
                    }
                )
            }

            // 8. Profile Screen
            composable(FarmerRoutes.PROFILE) {
                ProfileScreen(
                    farmer = currentFarmer,
                    onUpdateProfile = { name, address, mobile, email ->
                        authService.updateFarmerProfile(name, address, mobile, email)
                    },
                    onNavigateToDiagnostics = {
                        navController.navigate(FarmerRoutes.DIAGNOSTICS)
                    },
                    onLogoutClick = {
                        coroutineScope.launch {
                            authService.logout()
                            navController.navigate(FarmerRoutes.WELCOME) {
                                popUpTo(FarmerRoutes.DASHBOARD) { inclusive = true }
                            }
                        }
                    },
                    onResetDemoClick = {
                        coroutineScope.launch {
                            authService.clearDemoData()
                            requestService.seedInitialDataIfEmpty()
                            navController.navigate(FarmerRoutes.WELCOME) {
                                popUpTo(FarmerRoutes.DASHBOARD) { inclusive = true }
                            }
                        }
                    }
                )
            }
            // 9. AgriMatch Screen
            composable(FarmerRoutes.AGRIMATCH) {
                AgriMatchScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            // 10. Supabase Diagnostics Screen
            composable(FarmerRoutes.DIAGNOSTICS) {
                SupabaseDiagnosticsScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
}
}

@Composable
private fun FarmerNavigationRail(
    currentRoute: String?,
    unreadCount: Int,
    onNavigate: (String) -> Unit
) {
    val items = listOf(
        BottomNavItem.Dashboard to "Home",
        BottomNavItem.Book to "Book Transport",
        BottomNavItem.Requests to "My Requests",
        BottomNavItem.Notifications to "Notifications",
        BottomNavItem.Profile to "Profile"
    )

    NavigationRail(
        containerColor = CardBackground,
        header = {
            Column(
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Agriculture,
                    contentDescription = null,
                    tint = EmeraldGreen,
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    text = "RuralLink",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = EmeraldGreen
                )
            }
        }
    ) {
        items.forEach { (item, labelText) ->
            val selected = currentRoute == item.route
            NavigationRailItem(
                selected = selected,
                onClick = { onNavigate(item.route) },
                icon = {
                    BadgedBox(
                        badge = {
                            if (item is BottomNavItem.Notifications && unreadCount > 0) {
                                Badge(containerColor = BrightLeaf) {
                                    Text("$unreadCount", color = EmeraldGreen, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = labelText,
                            tint = if (selected) EmeraldGreen else SecondaryText
                        )
                    }
                },
                label = {
                    Text(
                        text = labelText,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 11.sp,
                        color = if (selected) EmeraldGreen else SecondaryText
                    )
                },
                colors = NavigationRailItemDefaults.colors(
                    indicatorColor = BrightLeafLight,
                    selectedIconColor = EmeraldGreen,
                    unselectedIconColor = SecondaryText,
                    selectedTextColor = EmeraldGreen,
                    unselectedTextColor = SecondaryText
                )
            )
        }
    }
}

@Composable
private fun FarmerBottomBar(
    currentRoute: String?,
    unreadCount: Int,
    onNavigate: (String) -> Unit
) {
    val items = listOf(
        BottomNavItem.Dashboard,
        BottomNavItem.Book,
        BottomNavItem.Requests,
        BottomNavItem.Profile
    )

    NavigationBar(
        containerColor = CardBackground,
        tonalElevation = 2.dp
    ) {
        items.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(item.route) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        tint = if (selected) PrimaryGreen else SecondaryText
                    )
                },
                label = {
                    Text(
                        text = item.title,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 11.sp,
                        color = if (selected) PrimaryGreen else SecondaryText
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = PrimaryGreenContainer,
                    selectedIconColor = PrimaryGreen,
                    unselectedIconColor = SecondaryText,
                    selectedTextColor = PrimaryGreen,
                    unselectedTextColor = SecondaryText
                ),
                modifier = Modifier.testTag("nav_item_${item.title.lowercase()}")
            )
        }
    }
}
