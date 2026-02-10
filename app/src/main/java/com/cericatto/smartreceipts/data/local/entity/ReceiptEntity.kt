package com.cericatto.smartreceipts.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "receipts")
data class ReceiptEntity(
    @PrimaryKey
    val id: Long,
    val storeName: String,
    val cnpj: String,
    val address: String,
    val dateTime: String,
    val itemsJson: String, // JSON string of items
    val subtotal: Double,
    val discount: Double,
    val totalAmount: Double,
    val paymentMethod: String,
    val cardNumber: String,
    val totalTaxes: Double,
    val federalTaxes: Double,
    val stateTaxes: Double,
    val accessKey: String,
    val nfceNumber: String,
    val rawText: String,
    val createdAt: Long
)
