package com.example.smartreceiptsplitter.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.smartreceiptsplitter.ui.presentation.scan.ScanScreen
import com.example.smartreceiptsplitter.ui.presentation.split.SplitScreen

//Scan is the start destination for now
@Composable
fun SmartReceiptSplitterNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Routes.SCAN
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.SCAN) {
            ScanScreen()
        }
        composable(route = Routes.SPLIT,
            arguments = listOf(navArgument(Routes.ARG_RECEIPT_ID) { type = NavType.LongType})
        ) {
            SplitScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }
    }
}