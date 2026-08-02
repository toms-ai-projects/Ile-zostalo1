package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.ui.add.AddEventScreen
import com.example.ui.detail.DetailScreen
import com.example.ui.home.HomeScreen

@Composable
fun CountdownNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = HomeDestination,
        modifier = modifier
    ) {
        composable<HomeDestination> {
            HomeScreen(
                navigateToDetail = { eventId -> navController.navigate(DetailDestination(eventId)) },
                navigateToAdd = { navController.navigate(AddEventDestination) }
            )
        }
        composable<AddEventDestination> {
            AddEventScreen(
                navigateBack = { navController.popBackStack() }
            )
        }
        composable<EditEventDestination> {
            AddEventScreen(
                navigateBack = { navController.popBackStack() }
            )
        }
        composable<DetailDestination> {
            DetailScreen(
                navigateBack = { navController.popBackStack() },
                navigateToEdit = { eventId -> navController.navigate(EditEventDestination(eventId)) }
            )
        }
    }
}
