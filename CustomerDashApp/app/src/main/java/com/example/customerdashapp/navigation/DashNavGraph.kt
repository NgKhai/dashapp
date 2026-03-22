package com.example.customerdashapp.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.navArgument
import com.example.customerdashapp.R
import com.example.customerdashapp.presentation.ai.ItemPhotoScreen
import com.example.customerdashapp.presentation.auth.LoginScreen
import com.example.customerdashapp.presentation.auth.OtpVerifyScreen
import com.example.customerdashapp.presentation.auth.PinInputScreen
import com.example.customerdashapp.presentation.auth.RegisterScreen
import com.example.customerdashapp.presentation.auth.SetPinScreen
import com.example.customerdashapp.presentation.delivery.CreateDeliveryScreen
import com.example.customerdashapp.presentation.delivery.DeliveryDetailScreen
import com.example.customerdashapp.presentation.delivery.DeliveryHistoryScreen
import com.example.customerdashapp.presentation.home.HomeScreen
import com.example.customerdashapp.presentation.map.MapPickerScreen
import com.example.customerdashapp.presentation.profile.ProfileScreen

data class BottomNavItem(
    val labelResId: Int,
    val route: String,
    val icon: ImageVector
)

@Composable
fun DashNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Login.route
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        BottomNavItem(R.string.nav_home, Screen.Home.route, Icons.Default.Home),
        BottomNavItem(R.string.nav_history, Screen.DeliveryHistory.route, Icons.Default.List),
        BottomNavItem(R.string.nav_profile, Screen.Profile.route, Icons.Default.Person)
    )

    val bottomNavRoutes = bottomNavItems.map { it.route }.toSet()
    val showBottomBar = currentRoute in bottomNavRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                PremiumBottomNavBar(
                    items = bottomNavItems,
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(paddingValues)
        ) {
            // ========== AUTH FLOW ==========

            composable(Screen.Login.route) {
                LoginScreen(
                    onNavigateToRegister = {
                        navController.navigate(Screen.Register.route)
                    },
                    onNavigateToHome = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateToPinInput = { phone ->
                        navController.navigate(Screen.PinInput.createRoute(phone))
                    },
                    onNavigateToOtpVerify = { phone ->
                        navController.navigate(Screen.OtpVerify.createRoute(phone))
                    }
                )
            }

            composable(
                route = Screen.PinInput.route,
                arguments = listOf(navArgument("phone") { type = NavType.StringType })
            ) { backStackEntry ->
                val phone = backStackEntry.arguments?.getString("phone") ?: ""
                PinInputScreen(
                    phone = phone,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToHome = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateToOtp = {
                        navController.navigate(Screen.OtpVerify.createRoute(phone)) {
                            popUpTo(Screen.PinInput.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(
                route = Screen.OtpVerify.route,
                arguments = listOf(navArgument("phone") { type = NavType.StringType })
            ) { backStackEntry ->
                val phone = backStackEntry.arguments?.getString("phone") ?: ""
                OtpVerifyScreen(
                    phone = phone,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToSetPin = {
                        navController.navigate(Screen.SetPin.route) {
                            popUpTo(Screen.Login.route)
                        }
                    },
                    onNavigateToHome = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Register.route) {
                RegisterScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToOtpVerify = { phone ->
                        navController.navigate(Screen.OtpVerify.createRoute(phone)) {
                            popUpTo(Screen.Login.route)
                        }
                    }
                )
            }

            composable(Screen.SetPin.route) {
                SetPinScreen(
                    onNavigateToHome = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }

            // ========== MAIN APP (Bottom Nav) ==========

            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToCreateDelivery = {
                        navController.navigate(Screen.CreateDelivery.route)
                    },
                    onNavigateToDeliveryDetail = { deliveryId ->
                        navController.navigate(Screen.DeliveryDetail.createRoute(deliveryId))
                    },
                    onNavigateToDeliveryHistory = {
                        navController.navigate(Screen.DeliveryHistory.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onLogout = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.DeliveryHistory.route) {
                DeliveryHistoryScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToDetail = { deliveryId ->
                        navController.navigate(Screen.DeliveryDetail.createRoute(deliveryId))
                    }
                )
            }

            composable(Screen.Profile.route) {
                ProfileScreen(
                    onLogout = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            // ========== FULL SCREEN FLOWS ==========

            composable(Screen.CreateDelivery.route) {
                // Read detected items passed back from ItemPhotoScreen
                val detectedItems = navController.currentBackStackEntry
                    ?.savedStateHandle
                    ?.get<ArrayList<String>>("detected_items")

                // Read combined map result
                val pickupAddress = navController.currentBackStackEntry
                    ?.savedStateHandle?.get<String>("pickup_address")
                val pickupLat = navController.currentBackStackEntry
                    ?.savedStateHandle?.get<Double>("pickup_lat")
                val pickupLng = navController.currentBackStackEntry
                    ?.savedStateHandle?.get<Double>("pickup_lng")
                val dropOffAddress = navController.currentBackStackEntry
                    ?.savedStateHandle?.get<String>("dropoff_address")
                val dropOffLat = navController.currentBackStackEntry
                    ?.savedStateHandle?.get<Double>("dropoff_lat")
                val dropOffLng = navController.currentBackStackEntry
                    ?.savedStateHandle?.get<Double>("dropoff_lng")
                val routeDistanceKm = navController.currentBackStackEntry
                    ?.savedStateHandle?.get<Double>("route_distance_km")
                val routeDurationMin = navController.currentBackStackEntry
                    ?.savedStateHandle?.get<Double>("route_duration_min")
                val estimatedCost = navController.currentBackStackEntry
                    ?.savedStateHandle?.get<Long>("estimated_cost")
                val routeEncoded = navController.currentBackStackEntry
                    ?.savedStateHandle?.get<String>("route_encoded")

                CreateDeliveryScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onDeliveryCreated = { deliveryId ->
                        navController.navigate(Screen.DeliveryDetail.createRoute(deliveryId)) {
                            popUpTo(Screen.Home.route)
                        }
                    },
                    onNavigateToItemPhoto = {
                        navController.navigate(Screen.ItemPhoto.route)
                    },
                    onNavigateToMapPicker = {
                        navController.navigate(Screen.MapPicker.route)
                    },
                    detectedItems = detectedItems,
                    pickupAddress = pickupAddress,
                    pickupLat = pickupLat,
                    pickupLng = pickupLng,
                    dropOffAddress = dropOffAddress,
                    dropOffLat = dropOffLat,
                    dropOffLng = dropOffLng,
                    routeDistanceKm = routeDistanceKm,
                    routeDurationMin = routeDurationMin,
                    estimatedCost = estimatedCost,
                    routeEncoded = routeEncoded
                )
            }

            composable(Screen.ItemPhoto.route) {
                ItemPhotoScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onItemsDetected = { items ->
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("detected_items", ArrayList(items))
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.MapPicker.route) {
                MapPickerScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onAddressesConfirmed = { pAddr, pLat, pLng, dAddr, dLat, dLng, dist, dur, cost, routeEnc ->
                        navController.previousBackStackEntry?.savedStateHandle?.apply {
                            set("pickup_address", pAddr)
                            set("pickup_lat", pLat)
                            set("pickup_lng", pLng)
                            set("dropoff_address", dAddr)
                            set("dropoff_lat", dLat)
                            set("dropoff_lng", dLng)
                            set("route_distance_km", dist)
                            set("route_duration_min", dur)
                            set("estimated_cost", cost)
                            set("route_encoded", routeEnc)
                        }
                        navController.popBackStack()
                    }
                )
            }

            composable(
                route = Screen.DeliveryDetail.route,
                arguments = listOf(navArgument("deliveryId") { type = NavType.StringType })
            ) { backStackEntry ->
                val deliveryId = backStackEntry.arguments?.getString("deliveryId") ?: ""
                DeliveryDetailScreen(
                    deliveryId = deliveryId,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}

@Composable
fun PremiumBottomNavBar(
    items: List<BottomNavItem>,
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(32.dp),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                )
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(32.dp)
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = currentRoute == item.route
                val animatedColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    label = "color"
                )
                
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onNavigate(item.route) }
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        AnimatedContent(
                            targetState = isSelected,
                            transitionSpec = {
                                scaleIn() togetherWith scaleOut()
                            },
                            label = "icon_anim"
                        ) { selected ->
                            Icon(
                                imageVector = item.icon,
                                contentDescription = stringResource(item.labelResId),
                                tint = animatedColor,
                                modifier = Modifier.size(if (selected) 28.dp else 24.dp)
                            )
                        }
                        
                        AnimatedVisibility(
                            visible = isSelected,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Text(
                                text = stringResource(item.labelResId),
                                color = animatedColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
