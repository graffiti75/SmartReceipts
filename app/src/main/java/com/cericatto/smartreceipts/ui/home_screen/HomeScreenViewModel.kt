package com.cericatto.smartreceipts.ui.home_screen

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cericatto.smartreceipts.domain.errors.Result
import com.cericatto.smartreceipts.domain.errors.toUserMessage
import com.cericatto.smartreceipts.domain.repository.ReceiptRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    private val receiptRepository: ReceiptRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HomeScreenState())
    val state: StateFlow<HomeScreenState> = _state.asStateFlow()

    private var _onNavigateToCamera: (() -> Unit)? = null
    private var _onNavigateToHistory: (() -> Unit)? = null
    private var _onNavigateToDetail: ((Long) -> Unit)? = null

    fun setNavigationCallbacks(
        onNavigateToCamera: () -> Unit,
        onNavigateToHistory: () -> Unit,
        onNavigateToDetail: (Long) -> Unit
    ) {
        _onNavigateToCamera = onNavigateToCamera
        _onNavigateToHistory = onNavigateToHistory
        _onNavigateToDetail = onNavigateToDetail
    }

    fun onAction(action: HomeScreenAction) {
        when (action) {
            is HomeScreenAction.OnCameraClick -> {
                _onNavigateToCamera?.invoke()
            }
            is HomeScreenAction.OnGalleryClick -> {
                // Gallery selection is handled by the UI
            }
            is HomeScreenAction.OnHistoryClick -> {
                _onNavigateToHistory?.invoke()
            }
            is HomeScreenAction.OnImageSelected -> {
                processImage(action.uri)
            }
            is HomeScreenAction.OnClearError -> {
                clearError()
            }
            is HomeScreenAction.OnNavigateToDetail -> {
                _onNavigateToDetail?.invoke(action.receiptId)
            }
        }
    }

    private fun processImage(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(isProcessing = true, selectedImageUri = uri) }

            when (val result = receiptRepository.scanReceiptFromUri(uri)) {
                is Result.Success -> {
                    val receipt = result.data
                    // Save the receipt
                    when (val saveResult = receiptRepository.saveReceipt(receipt)) {
                        is Result.Success -> {
                            _state.update {
                                it.copy(
                                    isProcessing = false,
                                    lastScannedReceipt = receipt,
                                    errorMessage = null,
                                    showError = false
                                )
                            }
                            // Navigate to detail
                            _onNavigateToDetail?.invoke(receipt.id)
                        }
                        is Result.Error -> {
                            _state.update {
                                it.copy(
                                    isProcessing = false,
                                    errorMessage = saveResult.error.toUserMessage(),
                                    showError = true
                                )
                            }
                        }
                    }
                }
                is Result.Error -> {
                    _state.update {
                        it.copy(
                            isProcessing = false,
                            errorMessage = result.error.toUserMessage(),
                            showError = true
                        )
                    }
                }
            }
        }
    }

    private fun clearError() {
        _state.update { it.copy(errorMessage = null, showError = false) }
    }
}
