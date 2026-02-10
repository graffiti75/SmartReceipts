package com.cericatto.smartreceipts.ui.history_screen

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
class HistoryScreenViewModel @Inject constructor(
    private val receiptRepository: ReceiptRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HistoryScreenState())
    val state: StateFlow<HistoryScreenState> = _state.asStateFlow()

    private var _onNavigateBack: (() -> Unit)? = null
    private var _onReceiptClick: ((Long) -> Unit)? = null

    init {
        loadReceipts()
    }

    fun setNavigationCallbacks(
        onNavigateBack: () -> Unit,
        onReceiptClick: (Long) -> Unit
    ) {
        _onNavigateBack = onNavigateBack
        _onReceiptClick = onReceiptClick
    }

    private fun loadReceipts() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            receiptRepository.getAllReceipts().collect { receipts ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        receipts = receipts
                    )
                }
            }
        }
    }

    fun onAction(action: HistoryScreenAction) {
        when (action) {
            is HistoryScreenAction.OnBackClick -> {
                _onNavigateBack?.invoke()
            }
            is HistoryScreenAction.OnReceiptClick -> {
                _onReceiptClick?.invoke(action.receiptId)
            }
            is HistoryScreenAction.OnDeleteReceipt -> {
                deleteReceipt(action.receiptId)
            }
            is HistoryScreenAction.OnClearError -> {
                _state.update { it.copy(errorMessage = null, showError = false) }
            }
        }
    }

    private fun deleteReceipt(receiptId: Long) {
        viewModelScope.launch {
            when (val result = receiptRepository.deleteReceipt(receiptId)) {
                is Result.Success -> {
                    // Receipt will be removed from the list automatically via Flow
                }
                is Result.Error -> {
                    _state.update {
                        it.copy(
                            errorMessage = result.error.toUserMessage(),
                            showError = true
                        )
                    }
                }
            }
        }
    }
}
