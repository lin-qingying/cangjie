package org.cangnova.cangjie.codegen.types

import org.cangnova.cangjie.chir.core.controlflow.ChirConditionalBranchTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirReturnTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirThrowTerminator
import org.cangnova.cangjie.chir.core.declaration.ChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.expression.ChirBinaryExpression
import org.cangnova.cangjie.chir.core.expression.ChirCallExpression
import org.cangnova.cangjie.chir.core.expression.ChirConstantExpression
import org.cangnova.cangjie.chir.core.expression.ChirExpression
import org.cangnova.cangjie.chir.core.expression.ChirFieldExpression
import org.cangnova.cangjie.chir.core.expression.ChirMemoryExpression
import org.cangnova.cangjie.chir.core.expression.ChirOtherExpression
import org.cangnova.cangjie.chir.core.expression.ChirTupleExpression
import org.cangnova.cangjie.chir.core.expression.ChirUnaryExpression
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.type.ChirPrimitiveType
import org.cangnova.cangjie.chir.core.type.ChirRawArrayType
import org.cangnova.cangjie.chir.core.type.ChirRefType
import org.cangnova.cangjie.chir.core.type.ChirResolvedTypeRef
import org.cangnova.cangjie.chir.core.type.ChirStructType
import org.cangnova.cangjie.chir.core.type.ChirTypeDeclarationId
import org.cangnova.cangjie.chir.core.type.ChirTypeRef
import org.cangnova.cangjie.chir.core.value.ChirConstantKind
import org.cangnova.cangjie.chir.core.value.ChirConstantValue
import org.cangnova.cangjie.chir.core.value.ChirValue

/** 仓颉标准 String 字面量在 LLVM 模块中的共享只读存储。 */
internal data class LlvmStringLiteralRuntimePlan(
    val typeDeclarations: List<String>,
    val globalDeclarations: List<String>,
    val requiredByteArrayTypeInfo: Pair<ChirRawArrayType, ChirSemanticId>?,
    val literalValues: Map<String, String>,
) {
    val isEmpty: Boolean
        get() = globalDeclarations.isEmpty()
}

/**
 * 仓颉 String 字面量的模块级 ABI 计划。
 *
 * 字符串内容集中放入一个只读 RawArray<UInt8>，每个 String 常量保存该数组的字节偏移和长度；
 * 布局与官方 `CreateStringLiteral` / `InitializeCjStringLiteral` 一致。
 */
internal object LlvmStringLiteralRuntime {
    val STRING_TYPE_ID = ChirTypeDeclarationId("std.core", "String")
    val STRING_TYPE_NAME = llvmNominalTypeName(LlvmNominalTypeKind.STRUCT, STRING_TYPE_ID.qualifiedName)
    const val STRING_TYPE_BODY = "type { ptr addrspace(1), i32, i32 }"
    const val BACKING_ARRAY_FIELD_INDEX = 0
    const val BACKING_ARRAY_LLVM_TYPE = "ptr addrspace(1)"

    private val byteArrayType = ChirRawArrayType(ChirResolvedTypeRef(ChirPrimitiveType.UINT8))
    private val byteArrayTypeInfoSourceId = ChirSemanticId("llvm:string-literals:rawarray-uint8")

    fun isStringType(typeRef: ChirTypeRef): Boolean =
        ((typeRef as? ChirResolvedTypeRef)?.type as? ChirStructType)?.declarationId == STRING_TYPE_ID

    fun isBackingArrayField(aggregateType: ChirTypeRef, fieldIndex: Int, fieldType: ChirTypeRef): Boolean =
        isStringType(aggregateType) &&
            fieldIndex == BACKING_ARRAY_FIELD_INDEX &&
            ((fieldType as? ChirResolvedTypeRef)?.type is ChirRawArrayType)

    fun literalReference(literal: String): String =
        LlvmRawArrayRuntime.global(literalGlobalName(literal))

    fun collectLiterals(functions: Collection<ChirFunctionDeclaration>): List<ChirConstantValue> {
        val literalsByIdentity = linkedMapOf<ChirSemanticId, ChirConstantValue>()

        fun visitValue(value: ChirValue) {
            val literal = value as? ChirConstantValue ?: return
            if (isStringType(literal.type) && literal.constantKind != ChirConstantKind.NULL) {
                literalsByIdentity.putIfAbsent(literal.semanticId, literal)
            }
        }

        fun visitExpression(expression: ChirExpression) {
            when (expression) {
                is ChirUnaryExpression -> visitValue(expression.operand)
                is ChirBinaryExpression -> {
                    visitValue(expression.left)
                    visitValue(expression.right)
                }
                is ChirMemoryExpression -> {
                    visitValue(expression.address)
                    expression.value?.let(::visitValue)
                }
                is ChirCallExpression -> {
                    visitValue(expression.callee)
                    expression.dispatchReceiver?.let(::visitValue)
                    expression.arguments.forEach(::visitValue)
                }
                is ChirConstantExpression -> visitValue(expression.value)
                is ChirOtherExpression -> expression.operands.forEach(::visitValue)
                is ChirTupleExpression -> expression.elements.forEach(::visitValue)
                is ChirFieldExpression -> visitValue(expression.base)
            }
        }

        fun visitTerminator(terminator: ChirTerminator?) {
            when (terminator) {
                is ChirReturnTerminator -> terminator.returnValue?.let(::visitValue)
                is ChirConditionalBranchTerminator -> visitValue(terminator.condition)
                is ChirThrowTerminator -> visitValue(terminator.exceptionValue)
                else -> Unit
            }
        }

        functions.forEach { function ->
            function.blocks.forEach { block ->
                block.expressions.forEach(::visitExpression)
                visitTerminator(block.terminator)
            }
        }
        return literalsByIdentity.values.toList()
    }

    fun plan(
        literals: Collection<ChirConstantValue>,
        additionalLiteralContents: Collection<String> = emptyList(),
    ): LlvmStringLiteralRuntimePlan {
        val contents = (literals.map { it.literal } + additionalLiteralContents).distinct().sorted()
        if (contents.isEmpty()) return LlvmStringLiteralRuntimePlan(emptyList(), emptyList(), null, emptyMap())

        val contentBytes = contents.associateWith { it.toByteArray(Charsets.UTF_8) }
        val rawData = packContents(contentBytes.values)
        val rawArrayTypeInfo = LlvmRawArrayRuntime.global(
            LlvmRawArrayRuntime.typeInfoSymbol(
                ChirResolvedTypeRef(byteArrayType),
                byteArrayTypeInfoSourceId,
            ),
        )
        val dataName = LlvmRawArrayRuntime.global(dataGlobalName(rawData))
        val dataType = "{ ptr, i64, [${rawData.size} x i8] }"
        val literalValues = linkedMapOf<String, String>()
        val globals = buildList {
            add(
                "$dataName = private constant $dataType { ptr $rawArrayTypeInfo, i64 ${rawData.size}, " +
                    "[${rawData.size} x i8] c\"${escapeBytes(rawData)}\" }, align 8",
            )
            contents.forEach { content ->
                val bytes = checkNotNull(contentBytes[content])
                val offset = findSubsequence(rawData, bytes)
                check(offset >= 0) { "packed Cangjie string literal is missing from its backing RawArray" }
                val literalName = LlvmRawArrayRuntime.global(literalGlobalName(content))
                val aggregateValue = "{ " +
                    "ptr addrspace(1) addrspacecast (ptr $dataName to ptr addrspace(1)), " +
                    "i32 $offset, i32 ${bytes.size} }"
                literalValues[content] = aggregateValue
                add(
                    "$literalName = private constant $STRING_TYPE_NAME { " +
                        "ptr addrspace(1) addrspacecast (ptr $dataName to ptr addrspace(1)), " +
                        "i32 $offset, i32 ${bytes.size} }, align 8",
                )
            }
        }
        return LlvmStringLiteralRuntimePlan(
            typeDeclarations = listOf("$STRING_TYPE_NAME = $STRING_TYPE_BODY"),
            globalDeclarations = globals,
            requiredByteArrayTypeInfo = byteArrayType to byteArrayTypeInfoSourceId,
            literalValues = literalValues,
        )
    }

    private fun packContents(contents: Collection<ByteArray>): ByteArray {
        val packed = ArrayList<Byte>()
        contents.sortedWith { first, second ->
            second.size.compareTo(first.size).takeIf { it != 0 } ?: compareBytes(first, second)
        }.forEach { content ->
            val current = packed.toByteArray()
            if (findSubsequence(current, content) < 0) packed.addAll(content.toList())
        }
        return packed.toByteArray()
    }

    private fun compareBytes(first: ByteArray, second: ByteArray): Int {
        val commonLength = minOf(first.size, second.size)
        for (index in 0 until commonLength) {
            val difference = (first[index].toInt() and 0xff) - (second[index].toInt() and 0xff)
            if (difference != 0) return difference
        }
        return first.size.compareTo(second.size)
    }

    private fun findSubsequence(haystack: ByteArray, needle: ByteArray): Int {
        if (needle.isEmpty()) return 0
        if (needle.size > haystack.size) return -1
        for (start in 0..haystack.size - needle.size) {
            var matches = true
            for (offset in needle.indices) {
                if (haystack[start + offset] != needle[offset]) {
                    matches = false
                    break
                }
            }
            if (matches) return start
        }
        return -1
    }

    private fun dataGlobalName(bytes: ByteArray): String = "cangjie.string.data.${sha256(bytes)}"

    private fun literalGlobalName(content: String): String =
        "cangjie.string.literal.${sha256(content.toByteArray(Charsets.UTF_8))}"

    private fun sha256(bytes: ByteArray): String =
        java.security.MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString(separator = "") { byte -> (byte.toInt() and 0xff).toString(16).uppercase().padStart(2, '0') }

    private fun escapeBytes(bytes: ByteArray): String = bytes.joinToString(separator = "") { byte ->
        val unsigned = byte.toInt() and 0xff
        if (unsigned in 0x20..0x7e && unsigned != '"'.code && unsigned != '\\'.code) {
            unsigned.toChar().toString()
        } else {
            "\\${unsigned.toString(16).uppercase().padStart(2, '0')}"
        }
    }
}
