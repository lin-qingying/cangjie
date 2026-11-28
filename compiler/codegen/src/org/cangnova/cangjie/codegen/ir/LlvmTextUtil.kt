package org.cangnova.cangjie.codegen.ir

import org.cangnova.cangjie.chir.core.identity.ChirSemanticId

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

/** LLVM 函数符号及其调用约定参数布局。 */
internal data class LlvmFunctionAbi(
    val symbol: String,
    val returnType: String,
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

/** 返回 CHIR 声明在链接层面的稳定符号身份。 */
internal fun llvmSymbolIdentity(name: String, semanticId: ChirSemanticId): String {
    if (semanticId.value.startsWith('@')) {
        val officialSymbol = semanticId.value.removePrefix("@")
        require(officialSymbol.isNotBlank()) { "official CHIR function identity has no symbol: ${semanticId.value}" }
        return officialSymbol
    }

    val sourceName = sanitizeIdentifier(name, prefix = "")
    if (sourceName.isNotBlank()) return sourceName

    val identity = semanticId.value.encodeToByteArray().joinToString("") { byte ->
        byte.toUByte().toString(radix = 16).padStart(2, '0')
    }
    return "\$$identity"
}

/**
 * 为 LLVM 声明和值引用统一生成稳定符号名，官方 CHIR 优先保留 mangled identity。
 */
internal fun llvmSymbolIdentifier(name: String, semanticId: ChirSemanticId): String =
    llvmIdentifier(llvmSymbolIdentity(name, semanticId))

internal fun llvmIdentifier(raw: String): String {
    if (raw.matches(Regex("[A-Za-z$._][A-Za-z0-9$._]*"))) return raw
    val escaped = raw.encodeToByteArray().joinToString("") { byte ->
        val value = byte.toInt() and 0xFF
        val character = value.toChar()
        if (character in 'A'..'Z' || character in 'a'..'z' || character in '0'..'9' || character in setOf('_', '$', '.')) {
            character.toString()
        } else {
            "\\${value.toString(radix = 16).uppercase().padStart(2, '0')}"
        }
    }
    return "\"$escaped\""
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

