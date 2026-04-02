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
import org.cangnova.cangjie.macro.MacroDiagnosticInfo
import org.cangnova.cangjie.macro.MacroExpansionResult
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

    const val TYPE_DEF_LIB: UByte = 1u
    const val TYPE_MULTI_CALLS: UByte = 2u
    const val TYPE_MACRO_RESULT: UByte = 3u
    const val TYPE_EXIT_TASK: UByte = 4u

    // MacroEvalStatus
    const val STATUS_SUCCESS: UByte = 3u
    const val STATUS_FAIL: UByte = 4u
    const val STATUS_FINISH: UByte = 6u

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

    /**
     * 解析 MacroResult 消息，转换为 [MacroExpansionResult]
     */
    fun parseMacroResult(payload: ByteArray): MacroExpansionResult {
        val buf = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN)
        val msg = MacroMsg.getRootAsMacroMsg(buf)

        val result = msg.content(MacroResult()) as? MacroResult
            ?: return MacroExpansionResult.Failure("无法解析 MacroResult")

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
            )
        }

        val diagnostics = (0 until result.diagsLength).mapNotNull { i ->
            val diag = result.diags(Diagnostic(), i) ?: return@mapNotNull null
            MacroDiagnosticInfo(
                severity = diag.diagSeverity,
                message = diag.errorMessage ?: return@mapNotNull null,
                hint = diag.mainHint ?: "",
            )
        }

        return if (status == STATUS_SUCCESS || status == STATUS_FINISH) {
            MacroExpansionResult.Success(
                tokens = tokens,
                expandedText = rebuildExpandedText(tokens),
                diagnostics = diagnostics,
            )
        } else {
            MacroExpansionResult.Failure(
                message = diagnostics.firstOrNull()?.message ?: "宏展开失败（status=$status）",
                diagnostics = diagnostics,
            )
        }
    }

    // ─── 展开文本重建 ─────────────────────────────────────────────────────────

    /**
     * 根据 token 类型启发式重建展开文本
     *
     * C++ 宏服务返回的展开 token 位置信息指向原始宏调用位置，不代表展开后的布局，
     * 因此无法通过位置重建空白。改用基于 token 值的启发式规则在 token 间插入空格。
     */
    fun rebuildExpandedText(tokens: List<TokenInfo>): String {
        if (tokens.isEmpty()) return ""
        val sb = StringBuilder()
        var prevValue: String? = null
        for (tok in tokens) {
            if (tok.value.contains('\n') || tok.value.contains('\r')) {
                sb.append('\n')
                prevValue = tok.value
                continue
            }
            val prev = prevValue
            if (prev != null && needsSpaceBetween(prev, tok.value)) {
                sb.append(' ')
            }
            sb.append(tok.value)
            prevValue = tok.value
        }
        return sb.toString()
    }

    private val noSpaceAfter = setOf("(", "[", ".", "@", "#", "!")
    private val noSpaceBefore = setOf(")", "]", ",", ";", ".", "(")

    private fun needsSpaceBetween(prevValue: String, currValue: String): Boolean {
        if (prevValue.contains('\n') || prevValue.contains('\r')) return false
        if (prevValue in noSpaceAfter) return false
        if (currValue in noSpaceBefore) return false
        return true
    }
}
