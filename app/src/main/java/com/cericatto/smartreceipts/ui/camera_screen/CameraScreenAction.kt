package com.cericatto.smartreceipts.ui.camera_screen

import android.graphics.Bitmap

sealed interface CameraScreenAction {
    data object OnBackClick : CameraScreenAction
    data object OnToggleFlash : CameraScreenAction
    data class OnCaptureImage(val bitmap: Bitmap) : CameraScreenAction
    data class OnPermissionResult(val granted: Boolean) : CameraScreenAction
    data object OnClearError : CameraScreenAction
}
