package com.cericatto.smartreceipts.ui.receipt_detail_screen

sealed interface ReceiptDetailScreenAction {
    data object OnBackClick : ReceiptDetailScreenAction
    data object OnToggleRawText : ReceiptDetailScreenAction
    data object OnShareClick : ReceiptDetailScreenAction
    data object OnDeleteClick : ReceiptDetailScreenAction
    data object OnClearError : ReceiptDetailScreenAction
}
