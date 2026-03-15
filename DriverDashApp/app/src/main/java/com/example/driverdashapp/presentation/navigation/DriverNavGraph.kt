package com.example.driverdashapp.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.example.driverdashapp.R
import com.example.driverdashapp.presentation.active.ActiveDeliveryEvent
import com.example.driverdashapp.presentation.active.ActiveDeliveryScreen
import com.example.driverdashapp.presentation.active.ActiveDeliveryViewModel
import com.example.driverdashapp.presentation.auth.*
import com.example.driverdashapp.presentation.earnings.EarningsEvent
import com.example.driverdashapp.presentation.earnings.EarningsScreen
import com.example.driverdashapp.presentation.earnings.EarningsViewModel
import com.example.driverdashapp.presentation.history.HistoryEvent
import com.example.driverdashapp.presentation.history.HistoryScreen
import com.example.driverdashapp.presentation.history.HistoryViewModel
import com.example.driverdashapp.presentation.home.HomeEvent
import com.example.driverdashapp.presentation.home.HomeScreen
import com.example.driverdashapp.presentation.home.HomeViewModel
import com.example.driverdashapp.presentation.pending.PendingEvent
import com.example.driverdashapp.presentation.pending.PendingScreen
import com.example.driverdashapp.presentation.pending.PendingViewModel
import com.example.driverdashapp.presentation.profile.ProfileEvent
import com.example.driverdashapp.presentation.profile.ProfileScreen
import com.example.driverdashapp.presentation.profile.ProfileViewModel

data class BottomNavItem(
    val label: String,
    val route: String,
    val icon: ImageVector
)

@Composable
fun DriverNavGraph(
    navController: NavHostController,
    startDestination: String
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        BottomNavItem(stringResource(R.string.nav_home), Screen.Home.route, Icons.Default.Home),
        BottomNavItem(stringResource(R.string.nav_history), Screen.History.route, Icons.Default.Home),
        BottomNavItem(stringResource(R.string.nav_earnings), Screen.Earnings.route, Icons.Default.Home),
        BottomNavItem(stringResource(R.string.nav_profile), Screen.Profile.route, Icons.Default.Person)
    )

    val showBottomBar = currentRoute in bottomNavItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(padding)
        ) {
            // ============================================
            // AUTH
            // ============================================
            composable(Screen.Login.route) {
                val viewModel = hiltViewModel<AuthViewModel>()
                val state = viewModel.uiState

                LaunchedEffect(state.navigateToPin) {
                    if (state.navigateToPin) {
                        navController.navigate(Screen.PinInput.route)
                        viewModel.onEvent(AuthEvent.ClearNavigation)
                    }
                }
                LaunchedEffect(state.navigateToOtp) {
                    state.navigateToOtp?.let {
                        navController.navigate(Screen.OtpVerify.createRoute(it))
                        viewModel.onEvent(AuthEvent.ClearNavigation)
                    }
                }
                LaunchedEffect(state.navigateToHome) {
                    if (state.navigateToHome) {
                        navController.navigate(Screen.Home.route) { popUpTo(0) { inclusive = true } }
                        viewModel.onEvent(AuthEvent.ClearNavigation)
                    }
                }

                LoginScreen(
                    uiState = state,
                    onEvent = viewModel::onEvent,
                    onNavigateToRegister = { navController.navigate(Screen.Register.route) }
                )
            }

            composable(Screen.PinInput.route) {
                val parentEntry = remember { try { navController.getBackStackEntry(Screen.Login.route) } catch (e: Exception) { null } }
                val viewModel = if (parentEntry != null) hiltViewModel<AuthViewModel>(parentEntry) else hiltViewModel<AuthViewModel>()
                val state = viewModel.uiState

                LaunchedEffect(state.navigateToHome) {
                    if (state.navigateToHome) {
                        navController.navigate(Screen.Home.route) { popUpTo(0) { inclusive = true } }
                        viewModel.onEvent(AuthEvent.ClearNavigation)
                    }
                }

                PinInputScreen(
                    uiState = state,
                    onEvent = viewModel::onEvent,
                    onForgotPin = {
                        viewModel.onEvent(AuthEvent.ResendOtp(state.phone))
                        navController.navigate(Screen.OtpVerify.createRoute(state.phone))
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.OtpVerify.route,
                arguments = listOf(navArgument("phone") { type = NavType.StringType })
            ) { entry ->
                val phone = entry.arguments?.getString("phone") ?: ""
                val loginEntry = remember { try { navController.getBackStackEntry(Screen.Login.route) } catch (e: Exception) { null } }
                val viewModel = if (loginEntry != null) hiltViewModel<AuthViewModel>(loginEntry) else hiltViewModel<AuthViewModel>()
                val state = viewModel.uiState

                LaunchedEffect(state.navigateToSetPin) {
                    if (state.navigateToSetPin) {
                        navController.navigate(Screen.SetPin.route)
                        viewModel.onEvent(AuthEvent.ClearNavigation)
                    }
                }

                OtpVerifyScreen(
                    phone = phone,
                    uiState = state,
                    onEvent = viewModel::onEvent,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.SetPin.route) {
                val loginEntry = remember { try { navController.getBackStackEntry(Screen.Login.route) } catch (e: Exception) { null } }
                val viewModel = if (loginEntry != null) hiltViewModel<AuthViewModel>(loginEntry) else hiltViewModel<AuthViewModel>()
                val state = viewModel.uiState

                LaunchedEffect(state.navigateToHome) {
                    if (state.navigateToHome) {
                        navController.navigate(Screen.Home.route) { popUpTo(0) { inclusive = true } }
                        viewModel.onEvent(AuthEvent.ClearNavigation)
                    }
                }

                SetPinScreen(uiState = state, onEvent = viewModel::onEvent)
            }

            composable(Screen.Register.route) {
                val viewModel = hiltViewModel<AuthViewModel>()
                val state = viewModel.uiState

                LaunchedEffect(state.navigateToOtpFromRegister) {
                    state.navigateToOtpFromRegister?.let {
                        navController.navigate(Screen.OtpVerify.createRoute(it))
                        viewModel.onEvent(AuthEvent.ClearNavigation)
                    }
                }

                RegisterScreen(
                    uiState = state,
                    onEvent = viewModel::onEvent,
                    onBack = { navController.popBackStack() }
                )
            }

            // ============================================
            // MAIN SCREENS
            // ============================================
            composable(Screen.Home.route) {
                val viewModel = hiltViewModel<HomeViewModel>()
                val state by viewModel.uiState.collectAsState()
                HomeScreen(
                    uiState = state,
                    onToggleOnline = { viewModel.onEvent(HomeEvent.ToggleOnline) },
                    onRefresh = { viewModel.onEvent(HomeEvent.Refresh) },
                    onNavigateToPending = { navController.navigate(Screen.Pending.route) },
                    onNavigateToActiveDelivery = { navController.navigate(Screen.ActiveDelivery.createRoute(it)) }
                )
            }

            composable(Screen.Pending.route) {
                val viewModel = hiltViewModel<PendingViewModel>()
                val state by viewModel.uiState.collectAsState()

                LaunchedEffect(state.acceptedDeliveryId) {
                    state.acceptedDeliveryId?.let {
                        navController.navigate(Screen.ActiveDelivery.createRoute(it)) {
                            popUpTo(Screen.Home.route)
                        }
                        viewModel.onEvent(PendingEvent.ClearAccepted)
                    }
                }

                PendingScreen(
                    uiState = state,
                    onAccept = { viewModel.onEvent(PendingEvent.Accept(it)) },
                    onRefresh = { viewModel.onEvent(PendingEvent.Load) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.ActiveDelivery.route,
                arguments = listOf(navArgument("deliveryId") { type = NavType.StringType })
            ) {
                val viewModel = hiltViewModel<ActiveDeliveryViewModel>()
                val state by viewModel.uiState.collectAsState()

                LaunchedEffect(state.isCompleted, state.isCancelled) {
                    if (state.isCompleted || state.isCancelled) {
                        navController.popBackStack(Screen.Home.route, false)
                    }
                }

                ActiveDeliveryScreen(
                    uiState = state,
                    onAdvanceStatus = { viewModel.onEvent(ActiveDeliveryEvent.AdvanceStatus) },
                    onCancel = { viewModel.onEvent(ActiveDeliveryEvent.Cancel(it)) },
                    onBack = { navController.popBackStack() },
                    onLocationPermissionGranted = { viewModel.onEvent(ActiveDeliveryEvent.PermissionGranted) }
                )
            }

            composable(Screen.History.route) {
                val viewModel = hiltViewModel<HistoryViewModel>()
                val state by viewModel.uiState.collectAsState()
                HistoryScreen(
                    uiState = state,
                    onFilterChanged = { viewModel.onEvent(HistoryEvent.Load(it)) },
                    onLoadMore = { viewModel.onEvent(HistoryEvent.LoadMore) },
                    onDeliveryClick = { navController.navigate(Screen.ActiveDelivery.createRoute(it)) }
                )
            }

            composable(Screen.Earnings.route) {
                val viewModel = hiltViewModel<EarningsViewModel>()
                val state by viewModel.uiState.collectAsState()
                EarningsScreen(
                    uiState = state,
                    onRefresh = { viewModel.onEvent(EarningsEvent.Refresh) }
                )
            }

            composable(Screen.Profile.route) {
                val viewModel = hiltViewModel<ProfileViewModel>()
                val state by viewModel.uiState.collectAsState()

                LaunchedEffect(state.isLoggedOut) {
                    if (state.isLoggedOut) {
                        navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
                    }
                }

                ProfileScreen(
                    uiState = state,
                    onLogout = { viewModel.onEvent(ProfileEvent.Logout) },
                    onSetPrimary = { viewModel.onEvent(ProfileEvent.SetPrimary(it)) }
                )
            }
        }
    }
}
