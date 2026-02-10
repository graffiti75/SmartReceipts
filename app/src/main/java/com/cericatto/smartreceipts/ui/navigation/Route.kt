package com.cericatto.smartreceipts.ui.navigation

import kotlinx.serialization.Serializable

sealed interface Route {
    @Serializable
    data object HomeScreen : Route

    @Serializable
    data object CameraScreen : Route

    @Serializable
    data class ReceiptDetailScreen(val receiptId: Long) : Route

    @Serializable
    data object HistoryScreen : Route
}
