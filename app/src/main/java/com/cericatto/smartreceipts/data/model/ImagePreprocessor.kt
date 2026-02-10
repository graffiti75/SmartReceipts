package com.cericatto.receiptscanner.data.model

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import kotlin.math.max
import kotlin.math.min

/**
 * Image preprocessing utilities to improve OCR accuracy for receipt scanning.
 *
 * Receipts often have:
 * - Low contrast (thermal paper)
 * - Faded text
 * - Uneven lighting
 * - Slight rotation/skew
 *
 * These preprocessing steps help ML Kit recognize text more accurately.
 */
object ImagePreprocessor {

	/**
	 * Apply all preprocessing steps to optimize image for OCR
	 */
	fun preprocessForOcr(bitmap: Bitmap): Bitmap {
		var processed = bitmap.copy(Bitmap.Config.ARGB_8888, true)

		// Step 1: Convert to grayscale
		processed = toGrayscale(processed)

		// Step 2: Increase contrast
		processed = adjustContrast(processed, contrast = 1.5f)

		// Step 3: Apply adaptive thresholding (binarization)
		processed = adaptiveThreshold(processed)

		// Step 4: Sharpen the image
		processed = sharpen(processed)

		return processed
	}

	/**
	 * Convert image to grayscale
	 * This removes color noise and helps focus on text
	 */
	fun toGrayscale(bitmap: Bitmap): Bitmap {
		val width = bitmap.width
		val height = bitmap.height

		val grayscaleBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
		val canvas = Canvas(grayscaleBitmap)

		val paint = Paint()
		val colorMatrix = ColorMatrix()
		colorMatrix.setSaturation(0f) // 0 = grayscale
		paint.colorFilter = ColorMatrixColorFilter(colorMatrix)

		canvas.drawBitmap(bitmap, 0f, 0f, paint)

		return grayscaleBitmap
	}

	/**
	 * Adjust contrast to make text stand out more
	 * @param contrast 1.0 = no change, >1.0 = more contrast
	 */
	fun adjustContrast(bitmap: Bitmap, contrast: Float = 1.5f): Bitmap {
		val width = bitmap.width
		val height = bitmap.height

		val contrastBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
		val canvas = Canvas(contrastBitmap)

		val paint = Paint()

		// Contrast matrix
		val scale = contrast
		val translate = (-.5f * scale + .5f) * 255f

		val colorMatrix = ColorMatrix(
			floatArrayOf(
				scale, 0f, 0f, 0f, translate,
				0f, scale, 0f, 0f, translate,
				0f, 0f, scale, 0f, translate,
				0f, 0f, 0f, 1f, 0f
			)
		)

		paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
		canvas.drawBitmap(bitmap, 0f, 0f, paint)

		return contrastBitmap
	}

	/**
	 * Adjust brightness
	 * @param brightness positive = brighter, negative = darker
	 */
	fun adjustBrightness(bitmap: Bitmap, brightness: Float = 30f): Bitmap {
		val width = bitmap.width
		val height = bitmap.height

		val brightBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
		val canvas = Canvas(brightBitmap)

		val paint = Paint()

		val colorMatrix = ColorMatrix(
			floatArrayOf(
				1f, 0f, 0f, 0f, brightness,
				0f, 1f, 0f, 0f, brightness,
				0f, 0f, 1f, 0f, brightness,
				0f, 0f, 0f, 1f, 0f
			)
		)

		paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
		canvas.drawBitmap(bitmap, 0f, 0f, paint)

		return brightBitmap
	}

	/**
	 * Apply adaptive thresholding (binarization)
	 * Converts image to black and white based on local pixel neighborhoods
	 * This helps with uneven lighting on receipts
	 */
	fun adaptiveThreshold(bitmap: Bitmap, blockSize: Int = 15, constant: Int = 10): Bitmap {
		val width = bitmap.width
		val height = bitmap.height

		val pixels = IntArray(width * height)
		bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

		val result = IntArray(width * height)

		// For each pixel, compute threshold based on local neighborhood
		val halfBlock = blockSize / 2

		for (y in 0 until height) {
			for (x in 0 until width) {
				// Get current pixel grayscale value
				val idx = y * width + x
				val gray = pixels[idx] and 0xFF // Assuming already grayscale

				// Calculate mean of local neighborhood
				var sum = 0
				var count = 0

				for (dy in -halfBlock..halfBlock) {
					for (dx in -halfBlock..halfBlock) {
						val nx = x + dx
						val ny = y + dy

						if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
							val nIdx = ny * width + nx
							sum += pixels[nIdx] and 0xFF
							count++
						}
					}
				}

				val mean = if (count > 0) sum / count else 128
				val threshold = mean - constant

				// Apply threshold
				val newValue = if (gray > threshold) 255 else 0
				result[idx] = (0xFF shl 24) or (newValue shl 16) or (newValue shl 8) or newValue
			}
		}

		val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
		resultBitmap.setPixels(result, 0, width, 0, 0, width, height)

		return resultBitmap
	}

	/**
	 * Simple global thresholding (faster but less accurate than adaptive)
	 */
	fun globalThreshold(bitmap: Bitmap, threshold: Int = 128): Bitmap {
		val width = bitmap.width
		val height = bitmap.height

		val pixels = IntArray(width * height)
		bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

		for (i in pixels.indices) {
			val gray = pixels[i] and 0xFF
			val newValue = if (gray > threshold) 255 else 0
			pixels[i] = (0xFF shl 24) or (newValue shl 16) or (newValue shl 8) or newValue
		}

		val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
		resultBitmap.setPixels(pixels, 0, width, 0, 0, width, height)

		return resultBitmap
	}

	/**
	 * Sharpen the image to make text edges clearer
	 * Uses a simple sharpening convolution kernel
	 */
	fun sharpen(bitmap: Bitmap, strength: Float = 1.0f): Bitmap {
		val width = bitmap.width
		val height = bitmap.height

		val pixels = IntArray(width * height)
		bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

		val result = IntArray(width * height)

		// Sharpening kernel
		// [ 0, -1,  0]
		// [-1,  5, -1]
		// [ 0, -1,  0]
		val kernel = floatArrayOf(
			0f, -strength, 0f,
			-strength, 1f + 4f * strength, -strength,
			0f, -strength, 0f
		)

		for (y in 1 until height - 1) {
			for (x in 1 until width - 1) {
				var sumR = 0f
				var sumG = 0f
				var sumB = 0f

				var ki = 0
				for (ky in -1..1) {
					for (kx in -1..1) {
						val idx = (y + ky) * width + (x + kx)
						val pixel = pixels[idx]

						val r = (pixel shr 16) and 0xFF
						val g = (pixel shr 8) and 0xFF
						val b = pixel and 0xFF

						sumR += r * kernel[ki]
						sumG += g * kernel[ki]
						sumB += b * kernel[ki]

						ki++
					}
				}

				val newR = clamp(sumR.toInt(), 0, 255)
				val newG = clamp(sumG.toInt(), 0, 255)
				val newB = clamp(sumB.toInt(), 0, 255)

				result[y * width + x] = (0xFF shl 24) or (newR shl 16) or (newG shl 8) or newB
			}
		}

		// Copy border pixels
		for (x in 0 until width) {
			result[x] = pixels[x]
			result[(height - 1) * width + x] = pixels[(height - 1) * width + x]
		}
		for (y in 0 until height) {
			result[y * width] = pixels[y * width]
			result[y * width + width - 1] = pixels[y * width + width - 1]
		}

		val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
		resultBitmap.setPixels(result, 0, width, 0, 0, width, height)

		return resultBitmap
	}

	/**
	 * Remove noise using median filter
	 * Helpful for speckled or noisy receipt images
	 */
	fun medianFilter(bitmap: Bitmap, radius: Int = 1): Bitmap {
		val width = bitmap.width
		val height = bitmap.height

		val pixels = IntArray(width * height)
		bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

		val result = IntArray(width * height)
		val windowSize = (2 * radius + 1) * (2 * radius + 1)
		val rValues = IntArray(windowSize)
		val gValues = IntArray(windowSize)
		val bValues = IntArray(windowSize)

		for (y in radius until height - radius) {
			for (x in radius until width - radius) {
				var idx = 0

				for (ky in -radius..radius) {
					for (kx in -radius..radius) {
						val pixel = pixels[(y + ky) * width + (x + kx)]
						rValues[idx] = (pixel shr 16) and 0xFF
						gValues[idx] = (pixel shr 8) and 0xFF
						bValues[idx] = pixel and 0xFF
						idx++
					}
				}

				rValues.sort()
				gValues.sort()
				bValues.sort()

				val medianIdx = windowSize / 2
				val newR = rValues[medianIdx]
				val newG = gValues[medianIdx]
				val newB = bValues[medianIdx]

				result[y * width + x] = (0xFF shl 24) or (newR shl 16) or (newG shl 8) or newB
			}
		}

		// Copy border pixels
		for (y in 0 until height) {
			for (x in 0 until width) {
				if (y < radius || y >= height - radius || x < radius || x >= width - radius) {
					result[y * width + x] = pixels[y * width + x]
				}
			}
		}

		val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
		resultBitmap.setPixels(result, 0, width, 0, 0, width, height)

		return resultBitmap
	}

	/**
	 * Scale image if it's too small (OCR works better on larger images)
	 * Recommended minimum: 1000px on shortest side
	 */
	fun scaleIfNeeded(bitmap: Bitmap, minSize: Int = 1000): Bitmap {
		val width = bitmap.width
		val height = bitmap.height
		val minDimension = min(width, height)

		if (minDimension >= minSize) {
			return bitmap
		}

		val scale = minSize.toFloat() / minDimension
		val newWidth = (width * scale).toInt()
		val newHeight = (height * scale).toInt()

		return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
	}

	private fun clamp(value: Int, min: Int, max: Int): Int {
		return max(min, min(max, value))
	}
}