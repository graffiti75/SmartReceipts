package com.cericatto.smartreceipts.ui.home_screen

import android.net.Uri
import com.cericatto.smartreceipts.domain.model.Receipt

data class HomeScreenState(
    val isLoading: Boolean = false,
    val isProcessing: Boolean = false,
    val lastScannedReceipt: Receipt? = null,
    val errorMessage: String? = null,
    val selectedImageUri: Uri? = null,
    val showError: Boolean = false
)
