package org.cangnova.cangjie.codegen.ir

/**
 * 解析后的 LLVM 函数签名。
 */
internal data class LlvmSignature(
    /**
     * LLVM 返回类型文本。
     */
    val returnType: String,
    /**
     * LLVM 参数类型文本列表。
     */
    val argumentTypes: List<String>,
)

/**
 * 将任意 CHIR 名称规整为 LLVM identifier 可用片段。
 */
internal fun sanitizeIdentifier(raw: String, prefix: String = "tmp"): String {
    val sanitized = raw
        .replace(Regex("[^A-Za-z0-9_.$]"), "_")
        .trim('_')
    if (sanitized.isBlank()) return prefix
    return if (sanitized.first().isDigit()) "${prefix}_$sanitized" else sanitized
}

/**
 * 在给定使用表中为基础名称生成唯一 identifier。
 */
internal fun uniquifyIdentifier(base: String, used: MutableMap<String, Int>): String {
    val count = used[base] ?: 0
    used[base] = count + 1
    return if (count == 0) base else "${base}_$count"
}

/**
 * 解析形如 `ret(arg, arg)` 的 LLVM 函数签名文本。
 */
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

