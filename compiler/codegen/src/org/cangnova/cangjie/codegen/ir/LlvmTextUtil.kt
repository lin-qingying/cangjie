package org.cangnova.cangjie.codegen.ir

internal data class LlvmSignature(
    val returnType: String,
    val argumentTypes: List<String>,
)

internal fun sanitizeIdentifier(raw: String, prefix: String = "tmp"): String {
    val sanitized = raw
        .replace(Regex("[^A-Za-z0-9_.$]"), "_")
        .trim('_')
    if (sanitized.isBlank()) return prefix
    return if (sanitized.first().isDigit()) "${prefix}_$sanitized" else sanitized
}

internal fun uniquifyIdentifier(base: String, used: MutableMap<String, Int>): String {
    val count = used[base] ?: 0
    used[base] = count + 1
    return if (count == 0) base else "${base}_$count"
}

internal fun parseLlvmSignature(signature: String): LlvmSignature? {
    val trimmed = signature.trim()
    val open = trimmed.indexOf('(')
    val close = trimmed.lastIndexOf(')')
    if (open <= 0 || close <= open) return null

    val returnType = trimmed.substring(0, open).trim()
    val argsPart = trimmed.substring(open + 1, close).trim()
    val args = if (argsPart.isBlank()) {
        emptyList()
    } else {
        argsPart.split(',').map { it.trim() }.filter { it.isNotEmpty() }
    }
    return LlvmSignature(returnType = returnType, argumentTypes = args)
}

