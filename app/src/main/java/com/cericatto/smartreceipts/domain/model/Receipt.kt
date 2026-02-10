package com.cericatto.smartreceipts.domain.model

data class Receipt(
    val id: Long = System.currentTimeMillis(),
    val storeName: String = "",
    val cnpj: String = "",
    val address: String = "",
    val dateTime: String = "",
    val items: List<ReceiptItem> = emptyList(),
    val subtotal: Double = 0.0,
    val discount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val paymentMethod: String = "",
    val cardNumber: String = "",
    val totalTaxes: Double = 0.0,
    val federalTaxes: Double = 0.0,
    val stateTaxes: Double = 0.0,
    val accessKey: String = "",
    val nfceNumber: String = "",
    val rawText: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class ReceiptItem(
    val itemNumber: String = "",
    val barcode: String = "",
    val description: String = "",
    val quantity: Double = 1.0,
    val unit: String = "UN",
    val unitPrice: Double = 0.0,
    val totalPrice: Double = 0.0,
    val isDiscount: Boolean = false
)

fun sampleReceipt() = Receipt(
    id = 1,
    storeName = "FESTVAL",
    cnpj = "78.116.670/0043-14",
    address = "RUA 24 DE MAIO, 765, REBOUÇAS, CURITIBA, PR",
    dateTime = "30/12/2025 08:20:10",
    items = listOf(
        ReceiptItem(
            itemNumber = "001",
            description = "BANANA PRATA",
            quantity = 1.4,
            unit = "KG",
            unitPrice = 5.99,
            totalPrice = 8.39
        ),
        ReceiptItem(
            itemNumber = "002",
            description = "LEITE INTEGRAL",
            quantity = 2.0,
            unit = "UN",
            unitPrice = 5.99,
            totalPrice = 11.98
        )
    ),
    subtotal = 20.37,
    discount = 0.0,
    totalAmount = 20.37,
    paymentMethod = "Credit Card",
    cardNumber = "****8827"
)
