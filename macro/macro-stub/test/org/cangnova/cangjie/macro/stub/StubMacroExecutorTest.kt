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

class StubMacroExecutorTest {

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

    @Test
    fun `unregistered macro returns failure`() {
        val executor = StubMacroExecutor()

        val call = MacroCallInfo(idName = "Unknown", methodName = "expand_Unknown")
        val results = executor.execute(listOf(call))

        assertEquals(1, results.size)
        assertTrue(results[0] is MacroExpansionResult.Failure)
    }

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

    @Test
    fun `reset clears executed calls`() {
        val executor = StubMacroExecutor()
        executor.registerExpansionText("A", "a")
        executor.execute(listOf(MacroCallInfo(idName = "A", methodName = "expand_A")))

        assertEquals(1, executor.executedCalls.size)
        executor.reset()
        assertEquals(0, executor.executedCalls.size)
    }

    @Test
    fun `isAvailable returns true`() {
        val executor = StubMacroExecutor()
        assertTrue(executor.isAvailable())
    }

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
