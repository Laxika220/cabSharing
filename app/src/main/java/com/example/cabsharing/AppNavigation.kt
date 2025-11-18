package com.example.cabsharing

import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.cabsharing.ui.screens.CreateRideScreen
import com.example.cabsharing.ui.screens.LoginScreen
import com.example.cabsharing.ui.screens.SwipeScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(onLoginSuccess = { userId ->
                navController.navigate("swipe/$userId") {
                    popUpTo("login") { inclusive = true }
                }
            })
        }

        composable("swipe/{userId}") { backStackEntry: NavBackStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            SwipeScreen(
                userId = userId,
                onCreateRide = {
                    navController.navigate("create/$userId")
                }
            )
        }

        composable("create/{userId}") { backStackEntry: NavBackStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            CreateRideScreen(
                userId = userId,
                onRideCreated = {
                    navController.popBackStack()
                }
            )
        }
    }
}
