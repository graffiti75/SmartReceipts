package com.cericatto.smartreceipts.ui.history_screen

import com.cericatto.smartreceipts.domain.model.Receipt

data class HistoryScreenState(
    val isLoading: Boolean = true,
    val receipts: List<Receipt> = emptyList(),
    val errorMessage: String? = null,
    val showError: Boolean = false
)
