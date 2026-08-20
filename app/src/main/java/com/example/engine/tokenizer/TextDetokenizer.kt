package com.example.engine.tokenizer

/**
 * Universal Detokenizer for cleaning BPE byte encodings (Ġ, Ċ, SPM, <0xXX>)
 * and formatting model output tokens into clean, readable UTF-8 text without replacement artifacts.
 */
object TextDetokenizer {
  private const val GPT2_SPACE = "Ġ" // \u0120
  private const val GPT2_NEWLINE = "Ċ" // \u010A
  private const val GPT2_TAB = "ĉ" // \u0109
  private const val GPT2_CR = "č" // \u010D
  private const val SPM_SPACE = " " // \u2581
  private const val REPLACEMENT_CHAR = "\uFFFD" // 

  private val SPECIAL_CONTROL_TOKENS = listOf(
    "<|im_start|>", "<|im_end|>", "<|endoftext|>", "<|eot_id|>",
    "<turn_start>", "<turn_end>", "<end_of_turn>", "<start_of_turn>",
    "<s>", "</s>", "<pad>", "<unk>", "<|start_header_id|>", "<|end_header_id|>"
  )

  private val HEX_GROUP_REGEX = Regex("""(?:<0x([0-9a-fA-F]{2})>)+""")
  private val SINGLE_HEX_REGEX = Regex("""<0x([0-9a-fA-F]{2})>""")

  /**
   * Sanitizes a streaming piece of token string, converting GPT-2/BPE byte artifacts
   * and raw hex tokens (<0xXX>) to normal characters.
   */
  fun cleanPiece(raw: String): String {
    if (raw.isEmpty()) return ""
    if (SPECIAL_CONTROL_TOKENS.contains(raw.trim())) return ""

    var cleaned = raw
      .replace(GPT2_SPACE, " ")
      .replace(SPM_SPACE, " ")
      .replace(GPT2_NEWLINE, "\n")
      .replace(GPT2_TAB, "\t")
      .replace(GPT2_CR, "\r")
      .replace(REPLACEMENT_CHAR, "")

    if (cleaned.contains("<0x")) {
      cleaned = decodeHexTokens(cleaned)
    }

    return cleaned
  }

  /**
   * Sanitizes a complete text response, stripping any remaining control sequences
   * and ensuring proper whitespace formatting.
   */
  fun cleanFullText(text: String): String {
    if (text.isEmpty()) return ""
    var cleaned = text
      .replace(GPT2_SPACE, " ")
      .replace(SPM_SPACE, " ")
      .replace(GPT2_NEWLINE, "\n")
      .replace(GPT2_TAB, "\t")
      .replace(GPT2_CR, "\r")
      .replace(REPLACEMENT_CHAR, "")

    for (st in SPECIAL_CONTROL_TOKENS) {
      cleaned = cleaned.replace(st, "")
    }

    if (cleaned.contains("<0x")) {
      cleaned = decodeHexTokens(cleaned)
    }

    return cleaned
  }

  /**
   * Groups contiguous <0xXX> hex tokens and decodes them as a full UTF-8 byte array,
   * avoiding broken multi-byte character errors (such as accented letters or emojis).
   */
  private fun decodeHexTokens(input: String): String {
    return HEX_GROUP_REGEX.replace(input) { match ->
      val bytes = SINGLE_HEX_REGEX.findAll(match.value).mapNotNull {
        it.groupValues[1].toIntOrNull(16)?.toByte()
      }.toList().toByteArray()

      if (bytes.isNotEmpty()) {
        try {
          String(bytes, Charsets.UTF_8).replace(REPLACEMENT_CHAR, "")
        } catch (_: Throwable) {
          ""
        }
      } else {
        ""
      }
    }
  }
}

