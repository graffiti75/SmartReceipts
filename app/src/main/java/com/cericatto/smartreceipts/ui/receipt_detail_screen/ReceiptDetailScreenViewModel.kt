package com.cericatto.smartreceipts.ui.receipt_detail_screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cericatto.smartreceipts.domain.errors.Result
import com.cericatto.smartreceipts.domain.errors.toUserMessage
import com.cericatto.smartreceipts.domain.model.Receipt
import com.cericatto.smartreceipts.domain.repository.ReceiptRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ReceiptDetailScreenViewModel @Inject constructor(
    private val receiptRepository: ReceiptRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ReceiptDetailScreenState())
    val state: StateFlow<ReceiptDetailScreenState> = _state.asStateFlow()

    private var _onNavigateBack: (() -> Unit)? = null
    private var _onShare: ((String) -> Unit)? = null

    fun setNavigationCallbacks(
        onNavigateBack: () -> Unit,
        onShare: (String) -> Unit
    ) {
        _onNavigateBack = onNavigateBack
        _onShare = onShare
    }

    fun loadReceipt(receiptId: Long) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            when (val result = receiptRepository.getReceiptById(receiptId)) {
                is Result.Success -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            receipt = result.data,
                            errorMessage = null
                        )
                    }
                }
                is Result.Error -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.error.toUserMessage(),
                            showError = true
                        )
                    }
                }
            }
        }
    }

    fun onAction(action: ReceiptDetailScreenAction) {
        when (action) {
            is ReceiptDetailScreenAction.OnBackClick -> {
                _onNavigateBack?.invoke()
            }
            is ReceiptDetailScreenAction.OnToggleRawText -> {
                _state.update { it.copy(showRawText = !it.showRawText) }
            }
            is ReceiptDetailScreenAction.OnShareClick -> {
                _state.value.receipt?.let { receipt ->
                    val shareText = buildShareText(receipt)
                    _onShare?.invoke(shareText)
                }
            }
            is ReceiptDetailScreenAction.OnDeleteClick -> {
                deleteReceipt()
            }
            is ReceiptDetailScreenAction.OnClearError -> {
                _state.update { it.copy(errorMessage = null, showError = false) }
            }
        }
    }

    private fun deleteReceipt() {
        val receipt = _state.value.receipt ?: return

        viewModelScope.launch {
            when (val result = receiptRepository.deleteReceipt(receipt.id)) {
                is Result.Success -> {
                    _onNavigateBack?.invoke()
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

    private fun buildShareText(receipt: Receipt): String {
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

        return buildString {
            appendLine("=== RECEIPT ===")
            appendLine(receipt.storeName)
            appendLine("CNPJ: ${receipt.cnpj}")
            appendLine(receipt.dateTime)
            appendLine()
            appendLine("--- ITEMS ---")
            receipt.items.forEach { item ->
                if (item.isDiscount) {
                    appendLine("  ${item.description}: -${currencyFormat.format(-item.totalPrice)}")
                } else {
                    appendLine("  ${item.description}: ${currencyFormat.format(item.totalPrice)}")
                }
            }
            appendLine()
            appendLine("Subtotal: ${currencyFormat.format(receipt.subtotal)}")
            if (receipt.discount > 0) {
                appendLine("Discount: -${currencyFormat.format(receipt.discount)}")
            }
            appendLine("TOTAL: ${currencyFormat.format(receipt.totalAmount)}")
            appendLine()
            appendLine("Payment: ${receipt.paymentMethod}")
        }
    }
}
