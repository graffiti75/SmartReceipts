package com.cericatto.smartreceipts.data.model

import com.cericatto.smartreceipts.domain.model.Receipt
import com.cericatto.smartreceipts.domain.model.ReceiptItem
import java.util.regex.Pattern

/**
 * Parser for Brazilian NFC-e (Nota Fiscal de Consumidor Eletrônica) receipts
 *
 * Improved version with better pattern matching for OCR text
 */
class BrazilianReceiptParser {

	companion object {
		private val CNPJ_PATTERN = Pattern.compile("CNPJ[:\\s-]*([0-9]{2}[./]?[0-9]{3}[./]?[0-9]{3}[/]?[0-9]{4}[-]?[0-9]{2})")
		private val DATE_TIME_PATTERN = Pattern.compile("(\\d{2}/\\d{2}/\\d{4})\\s*(\\d{2}:\\d{2}:\\d{2})?")
		private val TOTAL_PATTERN = Pattern.compile("(?:VALOR\\s*A\\s*PAGAR|V\\.?\\s*PAGAR|TOTAL)[:\\s]*R?\\$?\\s*([0-9]+[.,][0-9]{2})", Pattern.CASE_INSENSITIVE)
		private val SUBTOTAL_PATTERN = Pattern.compile("(?:VALOR\\s*TOTAL|SUBTOTAL|V\\.?\\s*TOTAL)[:\\s]*R?\\$?\\s*([0-9]+[.,][0-9]{2})", Pattern.CASE_INSENSITIVE)
		private val DISCOUNT_PATTERN = Pattern.compile("DESCONTO[:\\s]*R?\\$?\\s*-?([0-9]+[.,][0-9]{2})", Pattern.CASE_INSENSITIVE)
		private val NFCE_PATTERN = Pattern.compile("NFC-?e[:\\s]*([0-9]+)", Pattern.CASE_INSENSITIVE)
		private val ACCESS_KEY_PATTERN = Pattern.compile("([0-9]{4}\\s*){11}")
		private val TAX_TOTAL_PATTERN = Pattern.compile("Tributos.*?R\\$\\s*([0-9]+[.,][0-9]{2})", Pattern.CASE_INSENSITIVE)
		private val FEDERAL_TAX_PATTERN = Pattern.compile("(?:Trib|Federal).*?R\\$\\s*([0-9]+[.,][0-9]{2})", Pattern.CASE_INSENSITIVE)
		private val STATE_TAX_PATTERN = Pattern.compile("(?:Estadual|Estaduais).*?R\\$\\s*([0-9]+[.,][0-9]{2})", Pattern.CASE_INSENSITIVE)
		private val CARD_PATTERN = Pattern.compile("([0-9]{4,6}\\*+[0-9]{4})")

		// Item patterns - multiple variations to handle OCR errors
		private val ITEM_PATTERN_1 = Pattern.compile("^(\\d{3})\\s+([0-9]{7,14})\\s+(.+?)\\s+([0-9]+[.,]?[0-9]*)\\s*(UN|KG|PC|LT|ML|G)", Pattern.CASE_INSENSITIVE)
		private val ITEM_PATTERN_2 = Pattern.compile("^(\\d{3})\\s+(.{13,})\\s+([0-9]+[.,][0-9]{2})$")
		private val ITEM_WITH_WEIGHT = Pattern.compile("([0-9]+[.,][0-9]+)\\s*(?:KG|kg)\\s*[xX*]\\s*([0-9]+[.,][0-9]+)")
		private val PRICE_AT_END = Pattern.compile("([0-9]+[.,][0-9]{2})\\s*$")
		private val ITEM_LINE_WITH_PRICE = Pattern.compile("^(.+?)\\s+([0-9]+[.,][0-9]{2})$")

		private val STORE_PATTERNS = listOf(
			"FESTVAL", "CONDOR", "CARREFOUR", "PAO DE ACUCAR", "EXTRA",
			"BIG", "WALMART", "ATACADAO", "ASSAI", "MAKRO", "ANGELONI",
			"MUFFATO", "SUPER MUFFATO", "CIDADE CANCAO", "SUPER CENTER"
		)

		// Words that indicate a line is NOT an item
		private val NON_ITEM_KEYWORDS = listOf(
			"CNPJ", "CPF", "CONSUMIDOR", "DOCUMENTO", "FISCAL", "ELETRONICA",
			"CONSULTA", "CHAVE", "ACESSO", "TRIBUTOS", "FEDERAL", "ESTADUAL",
			"MUNICIPAL", "NFC-e", "PROTOCOLO", "AUTORIZACAO", "QR", "HTTP",
			"WWW", "FAZENDA", "ITEM COD", "DESC QTD", "VL.UNIT", "VL.ITEM",
			"TOTAL", "SUBTOTAL", "DESCONTO", "TROCO", "DINHEIRO", "CARTAO",
			"CREDITO", "DEBITO", "MASTERCARD", "VISA", "ELO", "OPERADOR",
			"CAIXA", "DATA", "HORA", "SERIE", "NUMERO", "VALOR A PAGAR"
		)
	}

	fun parse(rawText: String): Receipt {
		if (rawText.isBlank()) {
			return Receipt(rawText = rawText)
		}

		val lines = rawText.lines().map { it.trim() }.filter { it.isNotEmpty() }

		return Receipt(
			storeName = extractStoreName(lines, rawText),
			cnpj = extractCnpj(rawText),
			address = extractAddress(lines),
			dateTime = extractDateTime(rawText),
			items = extractItems(lines),
			subtotal = extractSubtotal(rawText),
			discount = extractDiscount(rawText),
			totalAmount = extractTotal(rawText),
			paymentMethod = extractPaymentMethod(rawText),
			cardNumber = extractCardNumber(rawText),
			totalTaxes = extractTotalTaxes(rawText),
			federalTaxes = extractFederalTaxes(rawText),
			stateTaxes = extractStateTaxes(rawText),
			accessKey = extractAccessKey(rawText),
			nfceNumber = extractNfceNumber(rawText),
			rawText = rawText
		)
	}

	private fun extractStoreName(lines: List<String>, rawText: String): String {
		// First, check for known store names anywhere in the text
		val upperText = rawText.uppercase()
		for (store in STORE_PATTERNS) {
			if (upperText.contains(store)) {
				return store
			}
		}

		// Look in the first few lines for a store name
		for (line in lines.take(10)) {
			val upperLine = line.uppercase()
			// Skip lines that look like addresses or technical info
			if (upperLine.contains("CNPJ") ||
				upperLine.contains("RUA") ||
				upperLine.contains("AV ") ||
				upperLine.contains("AVENIDA") ||
				upperLine.length < 3) {
				continue
			}
			// Return first reasonable-looking line
			if (line.length in 3..50 && !line.contains("@") && !line.all { it.isDigit() || it == '/' || it == '-' || it == '.' }) {
				return line
			}
		}

		return "Unknown Store"
	}

	private fun extractCnpj(text: String): String {
		val matcher = CNPJ_PATTERN.matcher(text)
		return if (matcher.find()) {
			formatCnpj(matcher.group(1) ?: "")
		} else ""
	}

	private fun formatCnpj(cnpj: String): String {
		val digits = cnpj.replace(Regex("[^0-9]"), "")
		return if (digits.length == 14) {
			"${digits.substring(0,2)}.${digits.substring(2,5)}.${digits.substring(5,8)}/" +
				"${digits.substring(8,12)}-${digits.substring(12,14)}"
		} else cnpj
	}

	private fun extractAddress(lines: List<String>): String {
		val addressKeywords = listOf("RUA", "AV ", "AVENIDA", "R.", "BR-", "BR ", "ROD", "ALAMEDA", "AL.", "PRACA", "PCA")
		for (line in lines.take(15)) {
			val upperLine = line.uppercase()
			if (addressKeywords.any { upperLine.contains(it) }) {
				return line
			}
		}
		return ""
	}

	private fun extractDateTime(text: String): String {
		val matcher = DATE_TIME_PATTERN.matcher(text)
		return if (matcher.find()) {
			val date = matcher.group(1) ?: ""
			val time = matcher.group(2) ?: ""
			"$date $time".trim()
		} else ""
	}

	private fun extractItems(lines: List<String>): List<ReceiptItem> {
		val items = mutableListOf<ReceiptItem>()
		var itemNumber = 1

		// Find the start of items section (usually after header info)
		var startIdx = 0
		for (i in lines.indices) {
			val line = lines[i].uppercase()
			if (line.contains("COD") && (line.contains("DESC") || line.contains("PROD"))) {
				startIdx = i + 1
				break
			}
		}

		// Find the end of items section (usually at TOTAL or payment info)
		var endIdx = lines.size
		for (i in lines.indices) {
			val line = lines[i].uppercase()
			if ((line.contains("TOTAL") && !line.contains("SUBTOTAL")) ||
				line.contains("VALOR A PAGAR") ||
				line.contains("FORMA DE PAGAMENTO") ||
				line.contains("CARTAO")) {
				endIdx = i
				break
			}
		}

		// Process lines in the items section
		var i = startIdx
		while (i < endIdx) {
			val line = lines[i]

			// Skip non-item lines
			if (isNonItemLine(line)) {
				i++
				continue
			}

			// Try to parse as item
			val item = tryParseItemLine(line, lines, i, itemNumber)
			if (item != null) {
				items.add(item)
				itemNumber++
			}

			// Check for discount line
			val discountItem = tryParseDiscount(line)
			if (discountItem != null) {
				items.add(discountItem)
			}

			i++
		}

		return items
	}

	private fun isNonItemLine(line: String): Boolean {
		val upperLine = line.uppercase()

		// Skip if contains non-item keywords
		if (NON_ITEM_KEYWORDS.any { upperLine.contains(it) }) {
			return true
		}

		// Skip very short lines (probably OCR fragments)
		if (line.length < 5) {
			return true
		}

		// Skip lines that are only numbers and punctuation (probably codes or dates)
		if (line.all { it.isDigit() || it in "/.:-* " }) {
			return true
		}

		return false
	}

	private fun tryParseItemLine(line: String, allLines: List<String>, currentIndex: Int, itemNumber: Int): ReceiptItem? {
		// Pattern 1: Standard format with item number, barcode, description
		// Example: "001 7891234567890 BANANA PRATA KG 1.500 x 5.99 8.99"
		val matcher1 = ITEM_PATTERN_1.matcher(line)
		if (matcher1.find()) {
			val barcode = matcher1.group(2) ?: ""
			var description = matcher1.group(3)?.trim() ?: ""

			// Get price from end of line
			val price = extractPriceFromLine(line)

			// Check for weight pattern
			val weightMatcher = ITEM_WITH_WEIGHT.matcher(line)
			return if (weightMatcher.find()) {
				val weight = parseNumber(weightMatcher.group(1) ?: "1")
				val unitPrice = parseNumber(weightMatcher.group(2) ?: "0")
				ReceiptItem(
					itemNumber = itemNumber.toString().padStart(3, '0'),
					barcode = barcode,
					description = description,
					quantity = weight,
					unit = "KG",
					unitPrice = unitPrice,
					totalPrice = price ?: (weight * unitPrice)
				)
			} else {
				ReceiptItem(
					itemNumber = itemNumber.toString().padStart(3, '0'),
					barcode = barcode,
					description = description,
					quantity = 1.0,
					unit = "UN",
					unitPrice = price ?: 0.0,
					totalPrice = price ?: 0.0
				)
			}
		}

		// Pattern 2: Just description and price
		// Example: "BANANA PRATA 8.99"
		val matcher2 = ITEM_LINE_WITH_PRICE.matcher(line)
		if (matcher2.find()) {
			val description = matcher2.group(1)?.trim() ?: ""
			val price = parseNumber(matcher2.group(2) ?: "0")

			// Make sure description is not a keyword
			if (description.length >= 3 && !isNonItemLine(description)) {
				// Check for weight in description
				val weightMatcher = ITEM_WITH_WEIGHT.matcher(line)
				return if (weightMatcher.find()) {
					val weight = parseNumber(weightMatcher.group(1) ?: "1")
					val unitPrice = parseNumber(weightMatcher.group(2) ?: "0")
					ReceiptItem(
						itemNumber = itemNumber.toString().padStart(3, '0'),
						description = description.replace(
							Regex("[0-9]+[.,][0-9]+\\s*KG\\s*[xX*]\\s*[0-9]+[.,][0-9]+"),
							""
						).trim(),
						quantity = weight,
						unit = "KG",
						unitPrice = unitPrice,
						totalPrice = price
					)
				} else {
					ReceiptItem(
						itemNumber = itemNumber.toString().padStart(3, '0'),
						description = description,
						quantity = 1.0,
						unit = "UN",
						unitPrice = price,
						totalPrice = price
					)
				}
			}
		}

		// Pattern 3: Line with barcode followed by description on next line
		if (line.matches(Regex("^\\d{3}\\s+\\d{7,14}.*"))) {
			// This looks like a barcode line, check next line for description
			if (currentIndex + 1 < allLines.size) {
				val nextLine = allLines[currentIndex + 1]
				val price = extractPriceFromLine(nextLine) ?: extractPriceFromLine(line)

				if (price != null && !isNonItemLine(nextLine)) {
					val description = nextLine.replace(Regex("R?\\$?\\s*[0-9]+[.,][0-9]{2}\\s*$"), "").trim()
					if (description.length >= 3) {
						return ReceiptItem(
							itemNumber = itemNumber.toString().padStart(3, '0'),
							barcode = line.split(Regex("\\s+")).getOrNull(1) ?: "",
							description = description,
							quantity = 1.0,
							unit = "UN",
							unitPrice = price,
							totalPrice = price
						)
					}
				}
			}
		}

		return null
	}

	private fun tryParseDiscount(line: String): ReceiptItem? {
		val upperLine = line.uppercase()

		if ((upperLine.contains("DESCONTO") || upperLine.contains("DESC ")) &&
			(upperLine.contains("ITEM") || upperLine.matches(Regex(".*-\\s*[0-9]+[.,][0-9]{2}.*")))) {
			val priceMatch = Regex("-?\\s*([0-9]+[.,][0-9]{2})").find(line)
			if (priceMatch != null) {
				val discountValue = parseNumber(priceMatch.groupValues[1])
				return ReceiptItem(
					description = line.trim(),
					quantity = 1.0,
					unit = "UN",
					unitPrice = -discountValue,
					totalPrice = -discountValue,
					isDiscount = true
				)
			}
		}
		return null
	}

	private fun extractPriceFromLine(line: String): Double? {
		val priceMatch = PRICE_AT_END.matcher(line)
		return if (priceMatch.find()) {
			parseNumber(priceMatch.group(1) ?: "0")
		} else null
	}

	private fun extractSubtotal(text: String): Double {
		val matcher = SUBTOTAL_PATTERN.matcher(text)
		return if (matcher.find()) parseNumber(matcher.group(1) ?: "0") else 0.0
	}

	private fun extractDiscount(text: String): Double {
		val matcher = DISCOUNT_PATTERN.matcher(text)
		return if (matcher.find()) parseNumber(matcher.group(1) ?: "0") else 0.0
	}

	private fun extractTotal(text: String): Double {
		val matcher = TOTAL_PATTERN.matcher(text)
		return if (matcher.find()) parseNumber(matcher.group(1) ?: "0") else 0.0
	}

	private fun extractPaymentMethod(text: String): String {
		val upperText = text.uppercase()
		return when {
			upperText.contains("CREDITO") || upperText.contains("CRÉDITO") -> "Credit Card"
			upperText.contains("DEBITO") || upperText.contains("DÉBITO") -> "Debit Card"
			upperText.contains("PIX") -> "PIX"
			upperText.contains("DINHEIRO") -> "Cash"
			upperText.contains("MASTERCARD") -> "Credit Card"
			upperText.contains("VISA") -> "Credit Card"
			upperText.contains("ELO") -> "Credit Card"
			else -> "Unknown"
		}
	}

	private fun extractCardNumber(text: String): String {
		val matcher = CARD_PATTERN.matcher(text)
		return if (matcher.find()) matcher.group(1) ?: "" else ""
	}

	private fun extractTotalTaxes(text: String): Double {
		val matcher = TAX_TOTAL_PATTERN.matcher(text)
		return if (matcher.find()) parseNumber(matcher.group(1) ?: "0") else 0.0
	}

	private fun extractFederalTaxes(text: String): Double {
		val matcher = FEDERAL_TAX_PATTERN.matcher(text)
		return if (matcher.find()) parseNumber(matcher.group(1) ?: "0") else 0.0
	}

	private fun extractStateTaxes(text: String): Double {
		val matcher = STATE_TAX_PATTERN.matcher(text)
		return if (matcher.find()) parseNumber(matcher.group(1) ?: "0") else 0.0
	}

	private fun extractAccessKey(text: String): String {
		val matcher = ACCESS_KEY_PATTERN.matcher(text)
		return if (matcher.find()) matcher.group(0)?.trim() ?: "" else ""
	}

	private fun extractNfceNumber(text: String): String {
		val matcher = NFCE_PATTERN.matcher(text)
		return if (matcher.find()) matcher.group(1) ?: "" else ""
	}

	private fun parseNumber(text: String): Double {
		return try {
			text.replace(",", ".").replace(Regex("[^0-9.]"), "").toDouble()
		} catch (e: Exception) {
			0.0
		}
	}
}