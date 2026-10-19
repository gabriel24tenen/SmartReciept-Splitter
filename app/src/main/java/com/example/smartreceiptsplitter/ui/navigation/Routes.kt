package com.example.smartreceiptsplitter.ui.navigation

/*
Route constants for the app's NavHost. Kept as a flat object rather than a sealed
class with arguments until a route actually needs one — grows as more screens land
(receipts list/history for F06, OCR review for F02, split screen for F04, ...).
 */
object Routes {
    const val SCAN = "scan"
}