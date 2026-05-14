/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package org.cangnova.cangjie.macro

/**
 * 源码位置信息
 */
data class SourcePosition(
    val fileId: Int = 0,
    val line: Int = 0,
    val column: Int = 0,
)

/**
 * Token 信息（与 FlatBuffers Token 对齐）
 */
data class TokenInfo(
    val kind: UByte,
    val value: String,
    val begin: SourcePosition = SourcePosition(),
    val end: SourcePosition = SourcePosition(),
    val delimiterNum: Int = 1,
)

/**
 * 宏调用信息（与 FlatBuffers MacroCall 对齐）
 */
data class MacroCallInfo(
    val idName: String,
    val methodName: String,
    val packageName: String = "",
    val libPath: String = "",
    val hasAttrs: Boolean = false,
    val argTokens: List<TokenInfo> = emptyList(),
    val attrTokens: List<TokenInfo> = emptyList(),
    val parentNames: List<String> = emptyList(),
    val position: SourcePosition = SourcePosition(),
    val endPosition: SourcePosition = SourcePosition(),
)

/**
 * 宏诊断信息
 */
data class MacroDiagnosticInfo(
    val severity: Int,
    val message: String,
    val hint: String = "",
    val begin: SourcePosition = SourcePosition(),
    val end: SourcePosition = SourcePosition(),
    val origin: MacroDiagnosticOrigin = MacroDiagnosticOrigin.DIAG_REPORT,
)

/**
 * 宏诊断来源。
 *
 * `diagReport` 是宏库主动上报的用户诊断，不能被固定折叠成某个
 * executor failure kind；executor/protocol 自身错误由结构化 failure 承载。
 */
enum class MacroDiagnosticOrigin {
    EXECUTOR,
    DIAG_REPORT,
}

/**
 * 动态库加载阶段的结构化结果。
 */
sealed class MacroLibraryLoadResult {
    data class Success(
        val loadedLibPaths: List<String>,
    ) : MacroLibraryLoadResult()

    data class Failure(
        val failures: List<MacroLibraryLoadFailure>,
    ) : MacroLibraryLoadResult() {
        init {
            require(failures.isNotEmpty()) { "Macro library load failure must contain at least one item." }
        }
    }
}

data class MacroLibraryLoadFailure(
    val libPath: String,
    val kind: MacroLibraryLoadFailureKind,
    val message: String,
)

enum class MacroLibraryLoadFailureKind {
    CANNOT_OPEN_LIB,
    PROTOCOL_ERROR,
    SERVER_DISCONNECTED,
    TIMEOUT,
    SERVER_CRASH,
}

/**
 * 宏执行失败分类。
 */
enum class MacroExpansionFailureKind {
    CANNOT_FIND_METHOD,
    EVALUATE_FAILED,
    EXPAND_FAILED,
    PROTOCOL_ERROR,
    SERVER_DISCONNECTED,
    TIMEOUT,
    SERVER_CRASH,
}

/**
 * 宏展开结果
 */
sealed class MacroExpansionResult {
    /**
     * 展开成功。
     *
     * [expandedText] 仅供测试、日志、LSP/debug 展示使用；语义路径必须消费
     * [tokens] 并进入 construction fragment/splice 流程，不能把文本替换回源码。
     */
    data class Success(
        val tokens: List<TokenInfo>,
        val expandedText: String,
        val diagnostics: List<MacroDiagnosticInfo> = emptyList(),
    ) : MacroExpansionResult()

    /**
     * 展开失败
     */
    data class Failure(
        val message: String,
        val kind: MacroExpansionFailureKind = MacroExpansionFailureKind.EXPAND_FAILED,
        val diagnostics: List<MacroDiagnosticInfo> = emptyList(),
    ) : MacroExpansionResult()
}
