/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package org.cangnova.cangjie.macro.protocol

import MacroMsgFormat.*
import com.google.flatbuffers.FlatBufferBuilder
import org.cangnova.cangjie.macro.MacroCallInfo
import org.cangnova.cangjie.macro.MacroDiagnosticOrigin
import org.cangnova.cangjie.macro.MacroDiagnosticSeverity
import org.cangnova.cangjie.macro.MacroDiagnosticInfo
import org.cangnova.cangjie.macro.MacroExpansionFailureKind
import org.cangnova.cangjie.macro.MacroExpansionResult
import org.cangnova.cangjie.macro.MacroTokenKind
import org.cangnova.cangjie.macro.SourcePosition
import org.cangnova.cangjie.macro.TokenInfo
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * FlatBuffers 消息编解码（纯 JVM，无 IDE 依赖）
 *
 * 从 `external/intellij-cangjie/macro/.../protocol/MacroMsgCodec.kt` 提取，
 * 去除 IntelliJ Platform API 依赖，改用 `macro-common` 中定义的数据模型。
 */
object MacroMsgCodec {

    /**
     * DefLib 消息类型：请求宏服务端加载动态库。
     */
    const val TYPE_DEF_LIB: UByte = 1u
    /**
     * MultiMacroCalls 消息类型：发送一批宏调用。
     */
    const val TYPE_MULTI_CALLS: UByte = 2u
    /**
     * MacroResult 消息类型：宏服务端返回展开结果。
     */
    const val TYPE_MACRO_RESULT: UByte = 3u
    /**
     * ExitTask 消息类型：请求服务端重置或退出。
     */
    const val TYPE_EXIT_TASK: UByte = 4u

    // MacroEvalStatus：与 CJC MacroEvalStatus wire enum 保持同一编号。
    /** 宏服务已初始化，尚未进入求值阶段。 */
    const val STATUS_INIT: UByte = 0u
    /** 宏服务已就绪，尚未返回本次调用的求值结果。 */
    const val STATUS_READY: UByte = 1u
    /**
     * 宏调用已经执行、结果需要按 token 判断是否失败的状态。
     */
    const val STATUS_EVAL: UByte = 2u
    /**
     * 宏展开成功状态。
     */
    const val STATUS_SUCCESS: UByte = 3u
    /**
     * 宏展开失败状态。
     */
    const val STATUS_FAIL: UByte = 4u
    /** 宏服务要求重新求值的中间状态。 */
    const val STATUS_REEVAL: UByte = 5u
    /**
     * 宏服务端完成当前展开批次的状态。
     */
    const val STATUS_FINISH: UByte = 6u
    /** 注解宏中间结果；CJC 完成处理后才以成功状态返回调用方。 */
    const val STATUS_ANNOTATION: UByte = 7u
    /** 宏服务重新求值失败的状态。 */
    const val STATUS_REEVAL_FAILED: UByte = 8u

    // ─── 消息构建 ─────────────────────────────────────────────────────────────

    /** 构建 DefLib 消息：加载宏动态库路径 */
    fun buildDefLib(libPaths: List<String>): ByteArray {
        val b = FlatBufferBuilder(256)
        val pathOffsets = libPaths.map { b.createString(it) }.toIntArray()
        val pathsVec = DefLib.createPathsVector(b, pathOffsets)
        val defLib = DefLib.createDefLib(b, pathsVec)
        val msg = MacroMsg.createMacroMsg(b, TYPE_DEF_LIB, defLib)
        b.finish(msg)
        return b.sizedByteArray()
    }

    /** ExitTask(flag=true)：终止服务端进程 */
    fun buildExitTask(): ByteArray {
        val b = FlatBufferBuilder(64)
        val exitTask = ExitTask.createExitTask(b, true)
        val msg = MacroMsg.createMacroMsg(b, TYPE_EXIT_TASK, exitTask)
        b.finish(msg)
        return b.sizedByteArray()
    }

    /** ExitTask(flag=false)：重置服务端状态（清空宏声明/调用/诊断），进程继续运行 */
    fun buildResetStageTask(): ByteArray {
        val b = FlatBufferBuilder(64)
        val exitTask = ExitTask.createExitTask(b, false)
        val msg = MacroMsg.createMacroMsg(b, TYPE_EXIT_TASK, exitTask)
        b.finish(msg)
        return b.sizedByteArray()
    }

    /** 构建 MultiMacroCalls 消息：发送一批宏调用 */
    fun buildMultiMacroCalls(calls: List<MacroCallInfo>): ByteArray {
        val b = FlatBufferBuilder(1024)

        val callOffsets = calls.map { call ->
            val libPathOff = b.createString(call.libPath)
            val pkgNameOff = b.createString(call.packageName)
            val methodNameOff = b.createString(call.methodName)
            val idNameOff = b.createString(call.idName)

            val parentOffsets = call.parentNames.map { b.createString(it) }.toIntArray()
            val parentNamesVec = MacroCall.createParentNamesVector(b, parentOffsets)

            val argOffsets = call.argTokens.map { buildToken(b, it) }.toIntArray()
            val argsVec = MacroCall.createArgsVector(b, argOffsets)

            val attrOffsets = call.attrTokens.map { buildToken(b, it) }.toIntArray()
            val attrsVec = MacroCall.createAttrsVector(b, attrOffsets)

            // childMsges 空向量（保持协议兼容性）
            val childMsgesVec = MacroCall.createChildMsgesVector(b, IntArray(0))

            // IdInfo
            IdInfo.startIdInfo(b)
            IdInfo.addName(b, idNameOff)
            IdInfo.addPos(b, Position.createPosition(
                b,
                call.position.fileId.toUInt(),
                call.position.line,
                call.position.column
            ))
            val idInfo = IdInfo.endIdInfo(b)

            // MacroCall
            MacroCall.startMacroCall(b)
            MacroCall.addId(b, idInfo)
            MacroCall.addHasAttrs(b, call.hasAttrs)
            MacroCall.addArgs(b, argsVec)
            MacroCall.addAttrs(b, attrsVec)
            MacroCall.addParentNames(b, parentNamesVec)
            MacroCall.addChildMsges(b, childMsgesVec)
            MacroCall.addMethodName(b, methodNameOff)
            MacroCall.addPackageName(b, pkgNameOff)
            MacroCall.addLibPath(b, libPathOff)
            MacroCall.addBegin(b, Position.createPosition(
                b,
                call.position.fileId.toUInt(),
                call.position.line,
                call.position.column
            ))
            MacroCall.addEnd(b, Position.createPosition(
                b,
                call.endPosition.fileId.toUInt(),
                call.endPosition.line,
                call.endPosition.column
            ))
            MacroCall.endMacroCall(b)
        }.toIntArray()

        val callsVec = MultiMacroCalls.createCallsVector(b, callOffsets)
        val multiCalls = MultiMacroCalls.createMultiMacroCalls(b, callsVec)
        val msg = MacroMsg.createMacroMsg(b, TYPE_MULTI_CALLS, multiCalls)
        b.finish(msg)
        return b.sizedByteArray()
    }

    /**
     * 将公共 token 数据模型写入 FlatBuffers builder，并返回 token 对象偏移。
     */
    private fun buildToken(b: FlatBufferBuilder, tok: TokenInfo): Int {
        val valOff = b.createString(tok.value)
        Token.startToken(b)
        Token.addKind(b, tok.kind)
        Token.addValue(b, valOff)
        Token.addBegin(b, Position.createPosition(
            b,
            tok.begin.fileId.toUInt(),
            tok.begin.line,
            tok.begin.column
        ))
        Token.addEnd(b, Position.createPosition(
            b,
            tok.end.fileId.toUInt(),
            tok.end.line,
            tok.end.column
        ))
        Token.addDelimiterNum(b, tok.delimiterNum.toUInt())
        return Token.endToken(b)
    }

    // ─── 消息解析 ─────────────────────────────────────────────────────────────

    /** 获取消息类型 */
    fun getMsgType(payload: ByteArray): UByte {
        val buf = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN)
        return MacroMsg.getRootAsMacroMsg(buf).contentType
    }

    /** 解析 DefLib ack 中携带的库路径。空列表表示服务端接受本次加载。 */
    fun parseDefLibPaths(payload: ByteArray): List<String> {
        val buf = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN)
        val msg = MacroMsg.getRootAsMacroMsg(buf)
        val defLib = msg.content(DefLib()) as? DefLib ?: return emptyList()
        return (0 until defLib.pathsLength).mapNotNull { index -> defLib.paths(index) }
    }

    /**
     * 解析 MacroResult 消息，转换为 [MacroExpansionResult]
     */
    fun parseMacroResult(payload: ByteArray): MacroExpansionResult {
        val buf = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN)
        val msg = MacroMsg.getRootAsMacroMsg(buf)

        val result = msg.content(MacroResult()) as? MacroResult
            ?: return MacroExpansionResult.Failure(
                message = "无法解析 MacroResult",
                kind = MacroExpansionFailureKind.PROTOCOL_ERROR,
            )

        val status = result.status
        val tokens = (0 until result.tksLength).mapNotNull { i ->
            val tok = result.tks(Token(), i) ?: return@mapNotNull null
            val begin = tok.begin
            val end = tok.end
            TokenInfo(
                kind = (tok.kind.toInt() and 0xFF).toUByte(),
                value = tok.value ?: "",
                begin = SourcePosition(
                    fileId = begin?.fileId?.toInt() ?: 0,
                    line = begin?.line ?: 0,
                    column = begin?.column ?: 0,
                ),
                end = SourcePosition(
                    fileId = end?.fileId?.toInt() ?: 0,
                    line = end?.line ?: 0,
                    column = end?.column ?: 0,
                ),
                delimiterNum = tok.delimiterNum.toInt(),
            )
        }

        val diagnostics = (0 until result.diagsLength).mapNotNull { i ->
            val diag = result.diags(Diagnostic(), i) ?: return@mapNotNull null
            MacroDiagnosticInfo(
                severity = diag.diagSeverity,
                message = diag.errorMessage ?: return@mapNotNull null,
                hint = diag.mainHint ?: "",
                begin = diag.begin.toSourcePosition(),
                end = diag.end.toSourcePosition(),
            )
        }

        // CJC 在子进程中执行 assertParentContext 时通过 assertParents 返回失败的上下文约束；
        // 这不是普通 DiagReport，遗漏该字段会把可解释的上下文失败折叠成 status=4。
        val callPosition = result.id?.pos?.toSourcePosition() ?: SourcePosition()
        val parentContextDiagnostics = (0 until result.assertParentsLength).mapNotNull { i ->
            val requiredParent = result.assertParents(i)?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            MacroDiagnosticInfo(
                severity = MacroDiagnosticSeverity.ERROR,
                message = "未找到要求的父宏上下文 `$requiredParent`。",
                hint = "assertParentContext(\"$requiredParent\")",
                begin = callPosition,
                end = callPosition,
                origin = MacroDiagnosticOrigin.EXECUTOR,
            )
        }
        val allDiagnostics = parentContextDiagnostics + diagnostics

        return if (isMacroResultSuccessful(status, tokens)) {
            MacroExpansionResult.Success(
                tokens = tokens,
                expandedText = rebuildExpandedText(tokens),
                diagnostics = allDiagnostics,
            )
        } else {
            MacroExpansionResult.Failure(
                message = allDiagnostics.firstOrNull()?.message ?: "宏展开失败（status=$status）",
                kind = MacroExpansionFailureKind.EXPAND_FAILED,
                diagnostics = allDiagnostics,
            )
        }
    }

    /**
     * 对齐官方 `MacroExpandFailed` / `SetMacroCallEvalResult`：
     * LSPMacroServer 会把 runtime 已执行但尚未由父进程归一化的结果以 EVAL 返回，
     * 客户端必须用单个 ILLEGAL token 判定失败，否则视为成功。
     */
    private fun macroExpandFailed(tokens: List<TokenInfo>): Boolean =
        tokens.size == 1 && tokens.single().kind == TOKEN_ILLEGAL

    /**
     * 共享定义宏协议结果的成功语义，供 CFIR 和 IDE 预览统一使用。
     * 未知状态以及官方失败哨兵 token 都不是成功结果。
     */
    fun isMacroResultSuccessful(status: UByte, tokens: List<TokenInfo>): Boolean =
        status in SUCCESS_STATUSES && !macroExpandFailed(tokens)

    private val SUCCESS_STATUSES = setOf(STATUS_SUCCESS, STATUS_FINISH, STATUS_EVAL)

    /**
     * 将协议层位置对象转换为公共源码位置模型。
     */
    private fun Position?.toSourcePosition(): SourcePosition {
        return SourcePosition(
            fileId = this?.fileId?.toInt() ?: 0,
            line = this?.line ?: 0,
            column = this?.column ?: 0,
        )
    }

    // ─── 展开文本重建 ─────────────────────────────────────────────────────────

    /**
     * 按官方 CJC TokenKind 规则把展开 token 流还原为可重新解析的源码。
     *
     * 宏协议中的字符串 token 值不包含源码定界符；格式化必须依据 token kind 和
     * delimiterNum 恢复字面量，再按 CJC 的相邻 token 空格规则拼接，不能从 token
     * value 猜测语法类别。协议不携带原始单/双引号选择，因此字符串使用双引号规范化。
     */
    fun rebuildExpandedText(tokens: List<TokenInfo>): String {
        if (tokens.isEmpty()) return ""
        return buildString {
            tokens.forEachIndexed { index, token ->
                append(tokenToSource(token))
                val next = tokens.getOrNull(index + 1) ?: return@forEachIndexed
                if (needsSpaceBetween(token.kind, next.kind)) append(' ')
            }
        }
    }

    private val noSpaceAfterKinds = setOf(
        "DOT",
        "QUEST",
        "DOLLAR",
        "LPAREN",
        "LSQUARE",
        "AT",
        "AT_EXCL",
        "ILLEGAL",
        "NL",
    ).mapTo(hashSetOf(), MacroTokenKind::kindOf)

    private val noSpaceBeforeKinds = setOf(
        "DOT",
        "COLON",
        "COMMA",
        "SEMI",
        "QUEST",
        "LPAREN",
        "RPAREN",
        "LSQUARE",
        "RSQUARE",
        "NL",
        "END",
    ).mapTo(hashSetOf(), MacroTokenKind::kindOf)

    private val noSpaceBetweenKinds = setOf(
        MacroTokenKind.kindOf("GT") to MacroTokenKind.kindOf("GT"),
        MacroTokenKind.kindOf("GT") to MacroTokenKind.kindOf("ASSIGN"),
        MacroTokenKind.kindOf("QUEST") to MacroTokenKind.kindOf("QUEST"),
        MacroTokenKind.kindOf("LPAREN") to MacroTokenKind.kindOf("RPAREN"),
        MacroTokenKind.kindOf("LSQUARE") to MacroTokenKind.kindOf("RSQUARE"),
        MacroTokenKind.kindOf("IDENTIFIER") to MacroTokenKind.kindOf("NOT"),
        MacroTokenKind.kindOf("BITNOT") to MacroTokenKind.kindOf("INIT"),
    )

    private fun tokenToSource(token: TokenInfo): String = when (MacroTokenKind.nameOf(token.kind)) {
        "STRING_LITERAL" -> "\"${processSingleLineLiteralValue(token.value)}\""
        "JSTRING_LITERAL" -> "J\"${processSingleLineLiteralValue(token.value)}\""
        "RUNE_LITERAL" -> if (token.value == "'") "r'\\''" else "r'${token.value}'"
        "MULTILINE_STRING" -> "\"\"\"${System.lineSeparator()}${processMultilineLiteralValue(token.value)}\"\"\""
        "MULTILINE_RAW_STRING" -> {
            val delimiter = "#".repeat(token.delimiterNum)
            "$delimiter\"${token.value}\"$delimiter"
        }
        "NL" -> System.lineSeparator()
        else -> token.value
    }

    /** 对齐 CJC 的单行字符串重建，保留插值表达式内部的引号。 */
    private fun processSingleLineLiteralValue(value: String): String = buildString {
        var inInterpolation = false
        var interpolationBraceDepth = 0
        for (character in value) {
            if (isNotEmpty() && last() == '$' && character == '{') inInterpolation = true
            if (inInterpolation && character == '{') interpolationBraceDepth++
            if (inInterpolation && character == '}') {
                if (interpolationBraceDepth > 0) interpolationBraceDepth--
                if (interpolationBraceDepth == 0) inInterpolation = false
            }
            if (inInterpolation) {
                append(character)
                continue
            }
            when {
                character == '"' && shouldEscapeQuote(this) -> append("\\\"")
                character == '\r' && lastOrNull() != '\\' -> append("\\r")
                character == '\n' && lastOrNull() != '\\' -> append("\\n")
                else -> append(character)
            }
        }
    }

    /** 对齐 CJC 多行字符串的连续三引号转义规则。 */
    private fun processMultilineLiteralValue(value: String): String = buildString {
        var inInterpolation = false
        var interpolationBraceDepth = 0
        var pendingQuotes = 0
        for (character in value) {
            if (isNotEmpty() && last() == '$' && character == '{') inInterpolation = true
            if (inInterpolation && character == '{') interpolationBraceDepth++
            if (inInterpolation && character == '}') {
                if (interpolationBraceDepth > 0) interpolationBraceDepth--
                if (interpolationBraceDepth == 0) inInterpolation = false
            }
            if (inInterpolation) {
                append(character)
                continue
            }
            if (character == '"' && (isEmpty() || last() != '\\')) {
                pendingQuotes++
                if (pendingQuotes == 3) {
                    append("\\\"\\\"\\\"")
                    pendingQuotes = 0
                }
                continue
            }
            repeat(pendingQuotes) { append('"') }
            pendingQuotes = 0
            append(character)
        }
        repeat(pendingQuotes) { append('"') }
    }

    /** 判断按官方规则还原单行字面量时，当前位置的双引号是否需要转义。 */
    private fun shouldEscapeQuote(value: CharSequence): Boolean {
        if (value.isEmpty() || value.last() != '\\') return true
        var precedingBackslashes = 0
        for (index in value.lastIndex downTo 0) {
            if (value[index] != '\\') break
            precedingBackslashes++
        }
        return precedingBackslashes % 2 == 0
    }

    /**
     * 官方宏协议中表示非法 token 的 kind 编码。
     */
    private val TOKEN_ILLEGAL: UByte = MacroTokenKind.kindOf("ILLEGAL")

    /** 按官方 `CheckAddSpace` 的 token kind 表决定是否插入空格。 */
    private fun needsSpaceBetween(previousKind: UByte, nextKind: UByte): Boolean {
        if (previousKind in noSpaceAfterKinds) return false
        if (nextKind in noSpaceBeforeKinds) return false
        return (previousKind to nextKind) !in noSpaceBetweenKinds
    }
}
