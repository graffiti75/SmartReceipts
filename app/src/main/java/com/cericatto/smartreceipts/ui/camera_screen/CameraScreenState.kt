package com.cericatto.smartreceipts.ui.camera_screen

import com.cericatto.smartreceipts.domain.model.Receipt

data class CameraScreenState(
    val isProcessing: Boolean = false,
    val hasCameraPermission: Boolean = false,
    val flashEnabled: Boolean = false,
    val scannedReceipt: Receipt? = null,
    val errorMessage: String? = null,
    val showError: Boolean = false
)
