package org.cangnova.cangjie.wasm.codegen.lowering

import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.wasm.codegen.diagnostics.WasmCodegenException
import org.cangnova.cangjie.wasm.ir.WasmDataSegment
import java.io.ByteArrayOutputStream

/** 为 Wasm module 中的字符串常量分配稳定的静态数据地址。 */
internal class WasmDataPool {
    private val requestedStrings = linkedSetOf<StringKey>()
    private var offsets: Map<StringKey, UInt>? = null
    private var segments: List<WasmDataSegment>? = null
    private var endOffset: UInt = INITIAL_DATA_OFFSET.toUInt()

    fun register(literal: String, cString: Boolean, semanticId: ChirSemanticId) {
        ensureOpen(semanticId)
        requestedStrings += StringKey(decodeLiteral(literal, semanticId), cString)
    }

    fun seal(semanticId: ChirSemanticId) {
        if (offsets != null) return
        val assigned = linkedMapOf<StringKey, UInt>()
        var cursor = INITIAL_DATA_OFFSET.toUInt()
        for (key in requestedStrings.sortedWith(compareBy<StringKey>({ it.cString }, { it.value }))) {
            cursor = alignTo(cursor, DATA_ALIGNMENT.toUInt(), semanticId)
            assigned[key] = cursor
            cursor = checkedAdd(cursor, encodedBytes(key).size.toUInt(), semanticId)
        }
        offsets = assigned
        endOffset = alignTo(cursor, STACK_ALIGNMENT.toUInt(), semanticId)
        segments = assigned.entries.map { (key, offset) -> WasmDataSegment(offset, encodedBytes(key)) }
    }

    fun address(literal: String, cString: Boolean, semanticId: ChirSemanticId): UInt {
        val key = StringKey(decodeLiteral(literal, semanticId), cString)
        return offsets?.get(key)
            ?: throw WasmCodegenException("string literal was not included in the Wasm static data plan", semanticId)
    }

    fun moduleSegments(): List<WasmDataSegment> = segments
        ?: error("Wasm static data must be sealed before module assembly")

    fun stackStartOffset(): UInt = endOffset.also {
        check(offsets != null) { "Wasm static data must be sealed before computing the stack start" }
    }

    private fun ensureOpen(semanticId: ChirSemanticId) {
        if (offsets != null) throw WasmCodegenException("cannot add static data after the Wasm data pool is sealed", semanticId)
    }

    private fun encodedBytes(key: StringKey): ByteArray {
        val utf8 = key.value.toByteArray(Charsets.UTF_8)
        if (key.cString) return utf8 + byteArrayOf(0)
        val result = ByteArrayOutputStream(Int.SIZE_BYTES + utf8.size)
        val length = utf8.size
        repeat(Int.SIZE_BYTES) { index -> result.write(length ushr (index * 8) and 0xff) }
        result.write(utf8)
        return result.toByteArray()
    }

    private fun alignTo(value: UInt, alignment: UInt, semanticId: ChirSemanticId): UInt {
        val mask = alignment - 1u
        return checkedAdd(value, mask, semanticId) and mask.inv()
    }

    private fun checkedAdd(left: UInt, right: UInt, semanticId: ChirSemanticId): UInt {
        val sum = left.toULong() + right.toULong()
        if (sum > 0xffff_ffffuL) {
            throw WasmCodegenException("Wasm static data exceeds the memory32 address model", semanticId)
        }
        return sum.toUInt()
    }

    fun decodeLiteral(raw: String, semanticId: ChirSemanticId): String {
        if (raw.length < 2 || raw.first() !in charArrayOf('"', '\'') || raw.last() != raw.first()) return raw
        val output = StringBuilder(raw.length - 2)
        var index = 1
        while (index < raw.lastIndex) {
            val ch = raw[index++]
            if (ch != '\\') {
                output.append(ch)
                continue
            }
            if (index >= raw.lastIndex) {
                throw WasmCodegenException("unterminated escape in CHIR string literal", semanticId)
            }
            when (val escape = raw[index++]) {
                'n' -> output.append('\n')
                'r' -> output.append('\r')
                't' -> output.append('\t')
                '0' -> output.append('\u0000')
                '\\' -> output.append('\\')
                '"' -> output.append('"')
                '\'' -> output.append('\'')
                'u' -> {
                    if (index < raw.lastIndex && raw[index] == '{') {
                        val closing = raw.indexOf('}', index + 1)
                        if (closing <= index + 1 || closing >= raw.lastIndex) {
                            throw WasmCodegenException("invalid Unicode escape in CHIR string literal", semanticId)
                        }
                        val codePoint = raw.substring(index + 1, closing).toIntOrNull(16)
                            ?: throw WasmCodegenException("invalid Unicode escape in CHIR string literal", semanticId)
                        if (!Character.isValidCodePoint(codePoint)) {
                            throw WasmCodegenException("Unicode escape is outside the valid scalar range", semanticId)
                        }
                        output.appendCodePoint(codePoint)
                        index = closing + 1
                    } else {
                        if (index + 4 > raw.lastIndex) {
                            throw WasmCodegenException("short Unicode escape in CHIR string literal", semanticId)
                        }
                        val codeUnit = raw.substring(index, index + 4).toIntOrNull(16)
                            ?: throw WasmCodegenException("invalid Unicode escape in CHIR string literal", semanticId)
                        output.append(codeUnit.toChar())
                        index += 4
                    }
                }
                else -> throw WasmCodegenException("unsupported CHIR string escape '\\$escape'", semanticId)
            }
        }
        return output.toString()
    }

    private data class StringKey(val value: String, val cString: Boolean)

    private companion object {
        const val INITIAL_DATA_OFFSET = 16
        const val DATA_ALIGNMENT = 4
        const val STACK_ALIGNMENT = 16
    }
}
