package com.example.smartreceiptsplitter.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.smartreceiptsplitter.ui.presentation.scan.ScanScreen

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
    }
}