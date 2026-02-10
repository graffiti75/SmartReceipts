package com.cericatto.smartreceipts.domain.repository

import android.graphics.Bitmap
import android.net.Uri
import com.cericatto.smartreceipts.domain.errors.Result
import com.cericatto.smartreceipts.domain.errors.ScanError
import com.cericatto.smartreceipts.domain.model.Receipt
import kotlinx.coroutines.flow.Flow

interface ReceiptRepository {

    suspend fun scanReceiptFromBitmap(bitmap: Bitmap): Result<Receipt, ScanError>

    suspend fun scanReceiptFromUri(uri: Uri): Result<Receipt, ScanError>

    suspend fun parseReceiptText(text: String): Result<Receipt, ScanError>

    suspend fun saveReceipt(receipt: Receipt): Result<Unit, ScanError>

    suspend fun deleteReceipt(id: Long): Result<Unit, ScanError>

    fun getAllReceipts(): Flow<List<Receipt>>

    suspend fun getReceiptById(id: Long): Result<Receipt, ScanError>
}
