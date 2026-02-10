package com.cericatto.smartreceipts.data.model

import com.cericatto.smartreceipts.data.local.entity.ReceiptEntity
import com.cericatto.smartreceipts.domain.model.Receipt
import com.cericatto.smartreceipts.domain.model.ReceiptItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

fun Receipt.toEntity(): ReceiptEntity {
    val gson = Gson()
    return ReceiptEntity(
        id = id,
        storeName = storeName,
        cnpj = cnpj,
        address = address,
        dateTime = dateTime,
        itemsJson = gson.toJson(items),
        subtotal = subtotal,
        discount = discount,
        totalAmount = totalAmount,
        paymentMethod = paymentMethod,
        cardNumber = cardNumber,
        totalTaxes = totalTaxes,
        federalTaxes = federalTaxes,
        stateTaxes = stateTaxes,
        accessKey = accessKey,
        nfceNumber = nfceNumber,
        rawText = rawText,
        createdAt = createdAt
    )
}

fun ReceiptEntity.toDomain(): Receipt {
    val gson = Gson()
    val itemsType = object : TypeToken<List<ReceiptItem>>() {}.type
    val items: List<ReceiptItem> = try {
        gson.fromJson(itemsJson, itemsType) ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }

    return Receipt(
        id = id,
        storeName = storeName,
        cnpj = cnpj,
        address = address,
        dateTime = dateTime,
        items = items,
        subtotal = subtotal,
        discount = discount,
        totalAmount = totalAmount,
        paymentMethod = paymentMethod,
        cardNumber = cardNumber,
        totalTaxes = totalTaxes,
        federalTaxes = federalTaxes,
        stateTaxes = stateTaxes,
        accessKey = accessKey,
        nfceNumber = nfceNumber,
        rawText = rawText,
        createdAt = createdAt
    )
}
