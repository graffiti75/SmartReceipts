package com.cericatto.smartreceipts.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.cericatto.receiptscanner.data.model.ImagePreprocessor
import com.cericatto.smartreceipts.data.local.entity.ReceiptDao
import com.cericatto.smartreceipts.data.model.BrazilianReceiptParser
import com.cericatto.smartreceipts.data.model.toDomain
import com.cericatto.smartreceipts.data.model.toEntity
import com.cericatto.smartreceipts.domain.errors.Result
import com.cericatto.smartreceipts.domain.errors.ScanError
import com.cericatto.smartreceipts.domain.model.Receipt
import com.cericatto.smartreceipts.domain.repository.ReceiptRepository
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ReceiptRepositoryImpl(
	private val context: Context,
	private val dao: ReceiptDao,
	private val parser: BrazilianReceiptParser
) : ReceiptRepository {

	private val recognizer = TextRecognition.getClient(TextRecognizerOptions.Builder().build())

	override suspend fun scanReceiptFromBitmap(bitmap: Bitmap): Result<Receipt, ScanError> {
		return try {
			// Step 1: Scale image if needed (OCR works better on larger images)
			val scaledBitmap = ImagePreprocessor.scaleIfNeeded(bitmap, minSize = 1200)

			// Step 2: Preprocess for better OCR
			val processedBitmap = ImagePreprocessor.preprocessForOcr(scaledBitmap)

			// Step 3: Run OCR
			performOcr(processedBitmap)
		} catch (e: Exception) {
			Result.Error(ScanError.Ocr.UNKNOWN)
		}
	}

	override suspend fun scanReceiptFromUri(uri: Uri): Result<Receipt, ScanError> {
		return try {
			// Load bitmap from URI
			val inputStream = context.contentResolver.openInputStream(uri)
			val originalBitmap = BitmapFactory.decodeStream(inputStream)
			inputStream?.close()

			if (originalBitmap == null) {
				return Result.Error(ScanError.Ocr.IMAGE_LOAD_FAILED)
			}

			// Use the same processing as bitmap
			scanReceiptFromBitmap(originalBitmap)
		} catch (e: Exception) {
			Result.Error(ScanError.Ocr.IMAGE_LOAD_FAILED)
		}
	}

	private suspend fun performOcr(bitmap: Bitmap): Result<Receipt, ScanError> {
		return suspendCoroutine { continuation ->
			val image = InputImage.fromBitmap(bitmap, 0)

			recognizer.process(image)
				.addOnSuccessListener { visionText ->
					val fullText = visionText.text
					if (fullText.isBlank()) {
						continuation.resume(Result.Error(ScanError.Ocr.NO_TEXT_FOUND))
					} else {
						val receipt = parser.parse(fullText)
						continuation.resume(Result.Success(receipt))
					}
				}
				.addOnFailureListener {
					continuation.resume(Result.Error(ScanError.Ocr.TEXT_RECOGNITION_FAILED))
				}
		}
	}

	override suspend fun parseReceiptText(text: String): Result<Receipt, ScanError> {
		return if (text.isBlank()) {
			Result.Error(ScanError.Parser.EMPTY_TEXT)
		} else {
			val receipt = parser.parse(text)
			Result.Success(receipt)
		}
	}

	override suspend fun saveReceipt(receipt: Receipt): Result<Unit, ScanError> {
		return try {
			dao.insertReceipt(receipt.toEntity())
			Result.Success(Unit)
		} catch (e: Exception) {
			Result.Error(ScanError.Local.SAVE_FAILED)
		}
	}

	override suspend fun deleteReceipt(id: Long): Result<Unit, ScanError> {
		return try {
			dao.deleteReceipt(id)
			Result.Success(Unit)
		} catch (e: Exception) {
			Result.Error(ScanError.Local.DELETE_FAILED)
		}
	}

	override fun getAllReceipts(): Flow<List<Receipt>> {
		return dao.getAllReceipts().map { entities ->
			entities.map { it.toDomain() }
		}
	}

	override suspend fun getReceiptById(id: Long): Result<Receipt, ScanError> {
		return try {
			val entity = dao.getReceiptById(id)
			if (entity != null) {
				Result.Success(entity.toDomain())
			} else {
				Result.Error(ScanError.Local.NOT_FOUND)
			}
		} catch (e: Exception) {
			Result.Error(ScanError.Local.LOAD_FAILED)
		}
	}
}
