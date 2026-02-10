package com.cericatto.smartreceipts.ui.receipt_detail_screen

import com.cericatto.smartreceipts.domain.model.Receipt

data class ReceiptDetailScreenState(
    val isLoading: Boolean = true,
    val receipt: Receipt? = null,
    val showRawText: Boolean = false,
    val errorMessage: String? = null,
    val showError: Boolean = false
)
