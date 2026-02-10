package com.cericatto.smartreceipts.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.cericatto.smartreceipts.ui.camera_screen.CameraScreenRoot
import com.cericatto.smartreceipts.ui.history_screen.HistoryScreenRoot
import com.cericatto.smartreceipts.ui.home_screen.HomeScreenRoot
import com.cericatto.smartreceipts.ui.receipt_detail_screen.ReceiptDetailScreenRoot

@Composable
fun NavHostComposable(
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Route.HomeScreen
    ) {
        composable<Route.HomeScreen> {
            HomeScreenRoot(
                modifier = modifier,
                onNavigateToCamera = {
                    navController.navigate(Route.CameraScreen)
                },
                onNavigateToHistory = {
                    navController.navigate(Route.HistoryScreen)
                },
                onNavigateToReceiptDetail = { receiptId ->
                    navController.navigate(Route.ReceiptDetailScreen(receiptId))
                }
            )
        }

        composable<Route.CameraScreen> {
            CameraScreenRoot(
                modifier = modifier,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onReceiptScanned = { receiptId ->
                    navController.navigate(Route.ReceiptDetailScreen(receiptId)) {
                        popUpTo(Route.HomeScreen)
                    }
                }
            )
        }

        composable<Route.ReceiptDetailScreen> { backStackEntry ->
            val route: Route.ReceiptDetailScreen = backStackEntry.toRoute()
            ReceiptDetailScreenRoot(
                modifier = modifier,
                receiptId = route.receiptId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable<Route.HistoryScreen> {
            HistoryScreenRoot(
                modifier = modifier,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onReceiptClick = { receiptId ->
                    navController.navigate(Route.ReceiptDetailScreen(receiptId))
                }
            )
        }
    }
}
