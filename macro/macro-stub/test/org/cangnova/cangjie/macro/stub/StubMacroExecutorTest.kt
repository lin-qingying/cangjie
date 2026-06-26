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
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * 覆盖 [StubMacroExecutor] 的注册展开、默认展开、调用记录、重置和可用性行为。
 */
class StubMacroExecutorTest {

    /**
     * 验证已注册的固定展开文本会转换为成功的宏展开结果。
     */
    @Test
    fun `registered expansion returns success`() {
        val executor = StubMacroExecutor()
        executor.registerExpansionText("MyMacro", "let x = 42")

        val call = MacroCallInfo(idName = "MyMacro", methodName = "expand_MyMacro")
        val results = executor.execute(listOf(call))

        assertEquals(1, results.size)
        val result = results[0]
        assertTrue(result is MacroExpansionResult.Success)
        assertEquals("let x = 42", (result as MacroExpansionResult.Success).expandedText)
    }

    /**
     * 验证没有注册处理器且没有默认结果时，桩执行器返回失败结果。
     */
    @Test
    fun `unregistered macro returns failure`() {
        val executor = StubMacroExecutor()

        val call = MacroCallInfo(idName = "Unknown", methodName = "expand_Unknown")
        val results = executor.execute(listOf(call))

        assertEquals(1, results.size)
        assertTrue(results[0] is MacroExpansionResult.Failure)
    }

    /**
     * 验证默认展开结果会处理所有未显式注册的宏调用。
     */
    @Test
    fun `default result is used when no handler registered`() {
        val executor = StubMacroExecutor()
        executor.defaultResult = {
            MacroExpansionResult.Success(tokens = emptyList(), expandedText = "default")
        }

        val call = MacroCallInfo(idName = "Any", methodName = "expand_Any")
        val results = executor.execute(listOf(call))

        assertEquals(1, results.size)
        val result = results[0] as MacroExpansionResult.Success
        assertEquals("default", result.expandedText)
    }

    /**
     * 验证每次执行传入的宏调用都会按原顺序记录到 [StubMacroExecutor.executedCalls]。
     */
    @Test
    fun `executed calls are recorded`() {
        val executor = StubMacroExecutor()
        executor.registerExpansionText("A", "a")
        executor.registerExpansionText("B", "b")

        val calls = listOf(
            MacroCallInfo(idName = "A", methodName = "expand_A"),
            MacroCallInfo(idName = "B", methodName = "expand_B"),
        )
        executor.execute(calls)

        assertEquals(2, executor.executedCalls.size)
        assertEquals("A", executor.executedCalls[0].idName)
        assertEquals("B", executor.executedCalls[1].idName)
    }

    /**
     * 验证 [StubMacroExecutor.reset] 会清空执行调用记录。
     */
    @Test
    fun `reset clears executed calls`() {
        val executor = StubMacroExecutor()
        executor.registerExpansionText("A", "a")
        executor.execute(listOf(MacroCallInfo(idName = "A", methodName = "expand_A")))

        assertEquals(1, executor.executedCalls.size)
        executor.reset()
        assertEquals(0, executor.executedCalls.size)
    }

    /**
     * 验证桩执行器不依赖外部运行时，因此始终报告可用。
     */
    @Test
    fun `isAvailable returns true`() {
        val executor = StubMacroExecutor()
        assertTrue(executor.isAvailable())
    }

    /**
     * 验证自定义处理器能够读取完整的 [MacroCallInfo] 并据此生成展开文本。
     */
    @Test
    fun `custom handler receives call info`() {
        val executor = StubMacroExecutor()
        executor.registerExpansion("Greet") { call ->
            MacroExpansionResult.Success(
                tokens = emptyList(),
                expandedText = "hello ${call.packageName}",
            )
        }

        val call = MacroCallInfo(
            idName = "Greet",
            methodName = "expand_Greet",
            packageName = "my.package",
        )
        val results = executor.execute(listOf(call))

        val result = results[0] as MacroExpansionResult.Success
        assertEquals("hello my.package", result.expandedText)
    }
}
