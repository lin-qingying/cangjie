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
    /**
     * 源文件在宏协议中的数字标识；默认值表示调用侧未提供文件映射。
     */
    val fileId: Int = 0,
    /**
     * 一基源码行号；默认值表示未知行位置。
     */
    val line: Int = 0,
    /**
     * 一基源码列号；默认值表示未知列位置。
     */
    val column: Int = 0,
)

/**
 * Token 信息（与 FlatBuffers Token 对齐）
 */
data class TokenInfo(
    /**
     * 官方宏协议中的 token kind 编码。
     */
    val kind: UByte,
    /**
     * token 的源码文本或宏展开文本。
     */
    val value: String,
    /**
     * token 起始位置。
     */
    val begin: SourcePosition = SourcePosition(),
    /**
     * token 结束位置。
     */
    val end: SourcePosition = SourcePosition(),
    /**
     * 字符串字面量等 token 使用的分隔符层数。
     */
    val delimiterNum: Int = 1,
)

/**
 * 宏调用信息（与 FlatBuffers MacroCall 对齐）
 */
data class MacroCallInfo(
    /**
     * 宏标识符名称，对应源码中被调用的宏名。
     */
    val idName: String,
    /**
     * 宏运行时需要调用的展开方法名称。
     */
    val methodName: String,
    /**
     * 宏调用所在包名。
     */
    val packageName: String = "",
    /**
     * 提供该宏实现的动态库路径。
     */
    val libPath: String = "",
    /**
     * 调用是否携带属性宏实参。
     */
    val hasAttrs: Boolean = false,
    /**
     * 普通宏实参 token 序列。
     */
    val argTokens: List<TokenInfo> = emptyList(),
    /**
     * 属性宏实参 token 序列。
     */
    val attrTokens: List<TokenInfo> = emptyList(),
    /**
     * 父级声明名称链，用于服务端定位嵌套声明上下文。
     */
    val parentNames: List<String> = emptyList(),
    /**
     * 宏标识符或调用起始位置。
     */
    val position: SourcePosition = SourcePosition(),
    /**
     * 宏调用结束位置。
     */
    val endPosition: SourcePosition = SourcePosition(),
)

/**
 * 宏诊断信息
 */
data class MacroDiagnosticInfo(
    /**
     * 诊断严重级别，取值见 [MacroDiagnosticSeverity]。
     */
    val severity: Int,
    /**
     * 用户可见的诊断正文。
     */
    val message: String,
    /**
     * 宏服务端提供的附加提示。
     */
    val hint: String = "",
    /**
     * 诊断范围起始位置。
     */
    val begin: SourcePosition = SourcePosition(),
    /**
     * 诊断范围结束位置。
     */
    val end: SourcePosition = SourcePosition(),
    /**
     * 诊断来源，用于区分宏库主动报告和执行器协议错误。
     */
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
    /**
     * 动态库加载成功结果。
     *
     * @property loadedLibPaths 本次确认成功加载的动态库路径。
     */
    data class Success(
        /**
         * 本次确认成功加载的动态库路径。
         */
        val loadedLibPaths: List<String>,
    ) : MacroLibraryLoadResult()

    /**
     * 动态库加载失败结果。
     *
     * @property failures 每个失败库路径对应的结构化失败信息。
     */
    data class Failure(
        /**
         * 每个失败库路径对应的结构化失败信息。
         */
        val failures: List<MacroLibraryLoadFailure>,
    ) : MacroLibraryLoadResult() {
        init {
            require(failures.isNotEmpty()) { "Macro library load failure must contain at least one item." }
        }
    }
}

/**
 * 单个宏动态库加载失败项。
 *
 * @property libPath 加载失败的动态库路径。
 * @property kind 失败分类。
 * @property message 用户或日志可读的失败说明。
 */
data class MacroLibraryLoadFailure(
    /**
     * 加载失败的动态库路径。
     */
    val libPath: String,
    /**
     * 失败分类。
     */
    val kind: MacroLibraryLoadFailureKind,
    /**
     * 用户或日志可读的失败说明。
     */
    val message: String,
)

/**
 * 宏动态库加载阶段的失败分类。
 */
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
        /**
         * 宏服务端返回的展开 token 序列。
         */
        val tokens: List<TokenInfo>,
        /**
         * 根据展开 token 重建的调试展示文本。
         */
        val expandedText: String,
        /**
         * 展开成功时一并返回的宏诊断。
         */
        val diagnostics: List<MacroDiagnosticInfo> = emptyList(),
    ) : MacroExpansionResult()

    /**
     * 展开失败
     */
    data class Failure(
        /**
         * 展开失败说明。
         */
        val message: String,
        /**
         * 展开失败分类。
         */
        val kind: MacroExpansionFailureKind = MacroExpansionFailureKind.EXPAND_FAILED,
        /**
         * 宏服务端或执行器产生的诊断列表。
         */
        val diagnostics: List<MacroDiagnosticInfo> = emptyList(),
    ) : MacroExpansionResult()
}
