package com.example.smartreceiptsplitter.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.smartreceiptsplitter.ui.presentation.receipts.ReceiptListScreen
import com.example.smartreceiptsplitter.ui.presentation.receiptsForm.ReceiptFormScreen
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
            ScanScreen(
                onManualEntry = { navController.navigate(Routes.receiptForm()) },
                onOpenHistory = { navController.navigate(Routes.RECEIPTS)}
            )
        }
        composable(Routes.RECEIPTS) {
            ReceiptListScreen(
                onBack = { navController.popBackStack() },
                onOpenReceipt = { receiptId -> navController.navigate(Routes.receiptEdit(receiptId))}
            )
        }
        composable(
            route = Routes.RECEIPT_EDIT,
            arguments = listOf(navArgument(Routes.ARG_RECEIPT_ID) { type = NavType.LongType })
        ) {
            ReceiptFormScreen(
                onBack = { navController.popBackStack() },
                onSaved = { receiptId, splitNeedsReview ->
                    if (splitNeedsReview) {
                        navController.navigate(Routes.split(receiptId)) {
                            popUpTo(Routes.RECEIPT_EDIT) { inclusive = true }
                        }
                    } else {
                        navController.popBackStack()
                    }
                }
            )
        }
        composable(
            route = Routes.RECEIPT_FORM,
            arguments = Routes.RECEIPT_FORM_ARGS.map { name ->
                navArgument(name) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            }
        ) {
            ReceiptFormScreen(
                onBack = { navController.popBackStack()},
                onSaved = { receiptId, _ ->
                    navController.navigate(Routes.split(receiptId)) {
                        popUpTo(Routes.RECEIPT_FORM) { inclusive = true }
                    }
                }
            )
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