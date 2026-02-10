package com.cericatto.smartreceipts.ui.camera_screen

import android.graphics.Bitmap
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
class CameraScreenViewModel @Inject constructor(
    private val receiptRepository: ReceiptRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CameraScreenState())
    val state: StateFlow<CameraScreenState> = _state.asStateFlow()

    private var _onNavigateBack: (() -> Unit)? = null
    private var _onReceiptScanned: ((Long) -> Unit)? = null

    fun setNavigationCallbacks(
        onNavigateBack: () -> Unit,
        onReceiptScanned: (Long) -> Unit
    ) {
        _onNavigateBack = onNavigateBack
        _onReceiptScanned = onReceiptScanned
    }

    fun onAction(action: CameraScreenAction) {
        when (action) {
            is CameraScreenAction.OnBackClick -> {
                _onNavigateBack?.invoke()
            }
            is CameraScreenAction.OnToggleFlash -> {
                _state.update { it.copy(flashEnabled = !it.flashEnabled) }
            }
            is CameraScreenAction.OnCaptureImage -> {
                processImage(action.bitmap)
            }
            is CameraScreenAction.OnPermissionResult -> {
                _state.update { it.copy(hasCameraPermission = action.granted) }
                if (!action.granted) {
                    _state.update {
                        it.copy(
                            errorMessage = "Camera permission is required",
                            showError = true
                        )
                    }
                }
            }
            is CameraScreenAction.OnClearError -> {
                _state.update { it.copy(errorMessage = null, showError = false) }
            }
        }
    }

    private fun processImage(bitmap: Bitmap) {
        viewModelScope.launch {
            _state.update { it.copy(isProcessing = true) }

            when (val result = receiptRepository.scanReceiptFromBitmap(bitmap)) {
                is Result.Success -> {
                    val receipt = result.data
                    // Save the receipt
                    when (val saveResult = receiptRepository.saveReceipt(receipt)) {
                        is Result.Success -> {
                            _state.update {
                                it.copy(
                                    isProcessing = false,
                                    scannedReceipt = receipt,
                                    errorMessage = null,
                                    showError = false
                                )
                            }
                            _onReceiptScanned?.invoke(receipt.id)
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
}
