package com.cericatto.smartreceipts.domain.errors

sealed interface ScanError : Error {
    enum class Ocr : ScanError {
        IMAGE_LOAD_FAILED,
        TEXT_RECOGNITION_FAILED,
        NO_TEXT_FOUND,
        CAMERA_ERROR,
        PERMISSION_DENIED,
        UNKNOWN
    }

    enum class Parser : ScanError {
        EMPTY_TEXT,
        NO_ITEMS_FOUND,
        INVALID_FORMAT,
        UNKNOWN
    }

    enum class Local : ScanError {
        SAVE_FAILED,
        LOAD_FAILED,
        DELETE_FAILED,
        NOT_FOUND
    }
}

fun ScanError.toUserMessage(): String {
    return when (this) {
        ScanError.Ocr.IMAGE_LOAD_FAILED -> "Failed to load image"
        ScanError.Ocr.TEXT_RECOGNITION_FAILED -> "Failed to recognize text"
        ScanError.Ocr.NO_TEXT_FOUND -> "No text found in image"
        ScanError.Ocr.CAMERA_ERROR -> "Camera error occurred"
        ScanError.Ocr.PERMISSION_DENIED -> "Camera permission denied"
        ScanError.Ocr.UNKNOWN -> "Unknown OCR error"
        ScanError.Parser.EMPTY_TEXT -> "No text to parse"
        ScanError.Parser.NO_ITEMS_FOUND -> "No items found in receipt"
        ScanError.Parser.INVALID_FORMAT -> "Invalid receipt format"
        ScanError.Parser.UNKNOWN -> "Unknown parsing error"
        ScanError.Local.SAVE_FAILED -> "Failed to save receipt"
        ScanError.Local.LOAD_FAILED -> "Failed to load receipts"
        ScanError.Local.DELETE_FAILED -> "Failed to delete receipt"
        ScanError.Local.NOT_FOUND -> "Receipt not found"
    }
}
