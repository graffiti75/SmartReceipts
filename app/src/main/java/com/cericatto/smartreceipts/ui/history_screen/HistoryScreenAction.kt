package com.cericatto.smartreceipts.ui.history_screen

sealed interface HistoryScreenAction {
    data object OnBackClick : HistoryScreenAction
    data class OnReceiptClick(val receiptId: Long) : HistoryScreenAction
    data class OnDeleteReceipt(val receiptId: Long) : HistoryScreenAction
    data object OnClearError : HistoryScreenAction
}
