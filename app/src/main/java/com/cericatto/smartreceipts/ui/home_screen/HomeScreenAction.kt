package com.cericatto.smartreceipts.ui.home_screen

import android.net.Uri

sealed interface HomeScreenAction {
    data object OnCameraClick : HomeScreenAction
    data object OnGalleryClick : HomeScreenAction
    data object OnHistoryClick : HomeScreenAction
    data class OnImageSelected(val uri: Uri) : HomeScreenAction
    data object OnClearError : HomeScreenAction
    data class OnNavigateToDetail(val receiptId: Long) : HomeScreenAction
}
