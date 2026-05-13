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
 * 宏执行后端抽象
 *
 * 接收序列化的宏调用信息，返回展开后的 token 流。
 * 两种实现：
 * - LspMacroServerMacroExecutor（`:macro:macro-process`）：LSPMacroServer 外部进程
 * - [StubMacroExecutor][org.cangnova.cangjie.macro.stub.StubMacroExecutor]：测试桩
 */
interface MacroExecutor : AutoCloseable {
    /**
     * 加载宏动态库路径。
     *
     * 返回结构化结果，调用方必须区分动态库打开失败、协议错误和 server 断开，
     * 不允许把 DefLib 阶段失败折叠成普通宏展开失败。
     */
    fun loadLibraries(libPaths: List<String>): MacroLibraryLoadResult

    /** 执行一批宏调用 */
    fun execute(calls: List<MacroCallInfo>): List<MacroExpansionResult>

    /** 重置状态 */
    fun reset()

    /** 是否可用 */
    fun isAvailable(): Boolean

    /**
     * Executor ABI / 协议版本（CFIR PLAN.md §11 cache key 第 9 维）。
     *
     * 默认 `"v1"`；具体实现（如 LSP / process executor）须覆盖为真实协议版本，
     * 任何向后不兼容的协议/序列化/动态库 ABI 变更都必须递增字符串值。
     * 上游 macro cache 据此整体失效。
     */
    val abiVersion: String
        get() = DEFAULT_ABI_VERSION

    companion object {
        const val DEFAULT_ABI_VERSION: String = "v1"
    }
}
