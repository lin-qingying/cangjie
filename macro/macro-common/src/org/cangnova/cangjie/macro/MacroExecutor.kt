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
 * - [ProcessMacroExecutor][org.cangnova.cangjie.macro.process.ProcessMacroExecutor]：外部进程
 * - [StubMacroExecutor][org.cangnova.cangjie.macro.stub.StubMacroExecutor]：测试桩
 */
interface MacroExecutor : AutoCloseable {
    /** 加载宏动态库路径 */
    fun loadLibraries(libPaths: List<String>)

    /** 执行一批宏调用 */
    fun execute(calls: List<MacroCallInfo>): List<MacroExpansionResult>

    /** 重置状态 */
    fun reset()

    /** 是否可用 */
    fun isAvailable(): Boolean
}
