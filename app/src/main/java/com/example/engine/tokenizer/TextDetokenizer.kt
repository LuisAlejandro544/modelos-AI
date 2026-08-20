package com.example.engine.tokenizer

/**
 * Universal Detokenizer for cleaning BPE byte encodings (Ġ, Ċ, SPM, <0xXX>)
 * and formatting model output tokens into clean, readable UTF-8 text.
 */
object TextDetokenizer {
  private const val GPT2_SPACE = "Ġ" // \u0120
  private const val GPT2_NEWLINE = "Ċ" // \u010A
  private const val GPT2_TAB = "ĉ" // \u0109
  private const val GPT2_CR = "č" // \u010D
  private const val SPM_SPACE = " " // \u2581

  private val SPECIAL_CONTROL_TOKENS = listOf(
    "<|im_start|>", "<|im_end|>", "<|endoftext|>", "<|eot_id|>",
    "<turn_start>", "<turn_end>", "<end_of_turn>", "<start_of_turn>",
    "<s>", "</s>", "<pad>", "<unk>"
  )

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

    if (cleaned.contains("<0x")) {
      cleaned = cleaned.replace(Regex("<0x([0-9a-fA-F]{2})>")) { match ->
        val byteVal = match.groupValues[1].toIntOrNull(16)
        if (byteVal != null) byteVal.toChar().toString() else match.value
      }
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

    for (st in SPECIAL_CONTROL_TOKENS) {
      cleaned = cleaned.replace(st, "")
    }

    if (cleaned.contains("<0x")) {
      cleaned = cleaned.replace(Regex("<0x([0-9a-fA-F]{2})>")) { match ->
        val byteVal = match.groupValues[1].toIntOrNull(16)
        if (byteVal != null) byteVal.toChar().toString() else match.value
      }
    }

    return cleaned
  }
}
