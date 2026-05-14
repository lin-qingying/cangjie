/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package org.cangnova.cangjie.macro.stub

import org.cangnova.cangjie.macro.MacroCallInfo
import org.cangnova.cangjie.macro.MacroExpansionResult
import org.cangnova.cangjie.macro.MacroExecutor
import org.cangnova.cangjie.macro.MacroLibraryLoadFailure
import org.cangnova.cangjie.macro.MacroLibraryLoadResult

/**
 * 测试用宏执行桩实现
 *
 * 从预定义映射返回宏展开结果，不启动任何外部进程。
 * 用于单元测试和 IDE 不需要实际宏展开的场景。
 *
 * ## 用法
 *
 * ```kotlin
 * val stub = StubMacroExecutor()
 * stub.registerExpansion("myMacro") { call ->
 *     MacroExpansionResult.Success(
 *         tokens = emptyList(),
 *         expandedText = "expanded code",
 *     )
 * }
 * val results = stub.execute(calls)
 * ```
 */
class StubMacroExecutor : MacroExecutor {

    /** 宏名称 → 展开结果生成器 */
    private val expansionHandlers = mutableMapOf<String, (MacroCallInfo) -> MacroExpansionResult>()

    /** 默认展开结果（当没有注册对应宏名称的处理器时使用） */
    var defaultResult: ((MacroCallInfo) -> MacroExpansionResult)? = null

    /** 已加载的库路径（按 [loadLibraries] 调用顺序累积）。测试可读取此列表以断言 lib path 流转。 */
    private val libraries = mutableListOf<String>()

    /** 只读视图，供测试断言 `executor.loadLibraries` 是否带着预期的库路径调用。 */
    val loadedLibPaths: List<String>
        get() = libraries.toList()

    /** 记录所有执行过的调用 */
    val executedCalls = mutableListOf<MacroCallInfo>()

    /** 测试可注入的库加载失败结果。 */
    var loadFailure: ((List<String>) -> List<MacroLibraryLoadFailure>)? = null

    /**
     * 注册宏展开处理器
     *
     * @param macroName 宏名称（匹配 [MacroCallInfo.idName]）
     * @param handler 展开结果生成器
     */
    fun registerExpansion(macroName: String, handler: (MacroCallInfo) -> MacroExpansionResult) {
        expansionHandlers[macroName] = handler
    }

    /**
     * 注册固定展开文本
     *
     * @param macroName 宏名称
     * @param expandedText 展开后的文本
     */
    fun registerExpansionText(macroName: String, expandedText: String) {
        expansionHandlers[macroName] = {
            MacroExpansionResult.Success(
                tokens = emptyList(),
                expandedText = expandedText,
            )
        }
    }

    override fun loadLibraries(libPaths: List<String>): MacroLibraryLoadResult {
        loadFailure?.invoke(libPaths)?.takeIf { it.isNotEmpty() }?.let {
            return MacroLibraryLoadResult.Failure(it)
        }
        libraries.addAll(libPaths)
        return MacroLibraryLoadResult.Success(loadedLibPaths = libPaths.toList())
    }

    override fun execute(calls: List<MacroCallInfo>): List<MacroExpansionResult> {
        executedCalls.addAll(calls)
        return calls.map { call ->
            val handler = expansionHandlers[call.idName]
                ?: defaultResult
                ?: { MacroExpansionResult.Failure("未注册的宏: ${call.idName}") }
            handler(call)
        }
    }

    override fun reset() {
        libraries.clear()
        executedCalls.clear()
    }

    override fun isAvailable(): Boolean = true

    override fun close() {
        reset()
    }
}
