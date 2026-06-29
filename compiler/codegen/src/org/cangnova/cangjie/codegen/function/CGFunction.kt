package org.cangnova.cangjie.codegen.function

import org.cangnova.cangjie.chir.core.attribute.ChirAttribute
import org.cangnova.cangjie.chir.core.attribute.ChirBooleanAttribute
import org.cangnova.cangjie.chir.core.attribute.ChirStringAttribute
import org.cangnova.cangjie.chir.core.controlflow.ChirBranchTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirConditionalBranchTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirReturnTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirThrowTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirUnwindTerminator
import org.cangnova.cangjie.chir.core.declaration.ChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.type.ChirTypeRef
import org.cangnova.cangjie.chir.core.value.ChirBlockGroupValue
import org.cangnova.cangjie.chir.core.value.ChirBlockValue
import org.cangnova.cangjie.chir.core.value.ChirConstantValue
import org.cangnova.cangjie.chir.core.value.ChirFunctionValue
import org.cangnova.cangjie.chir.core.value.ChirGlobalValue
import org.cangnova.cangjie.chir.core.value.ChirImportedFunctionValue
import org.cangnova.cangjie.chir.core.value.ChirImportedVariableValue
import org.cangnova.cangjie.chir.core.value.ChirLocalValue
import org.cangnova.cangjie.chir.core.value.ChirParameterValue
import org.cangnova.cangjie.chir.core.value.ChirValue
import org.cangnova.cangjie.codegen.context.CGContext
import org.cangnova.cangjie.codegen.diagnostics.CodegenLoweringException
import org.cangnova.cangjie.codegen.dispatcher.ExpressionLoweringDispatcher
import org.cangnova.cangjie.codegen.ir.IRBuilder
import org.cangnova.cangjie.codegen.ir.LlvmFunctionArtifact
import org.cangnova.cangjie.codegen.ir.sanitizeIdentifier
import org.cangnova.cangjie.codegen.ir.uniquifyIdentifier

/**
 * 单个 CHIR 函数到 LLVM function IR 的 lowering 单元。
 */
class CGFunction(
    /**
     * 待 lowering 的 CHIR 函数声明。
     */
    private val declaration: ChirFunctionDeclaration,
    /**
     * 当前 codegen 共享上下文。
     */
    private val context: CGContext,
    /**
     * 表达式 lowering 分派器。
     */
    private val dispatcher: ExpressionLoweringDispatcher,
) {
    /**
     * CHIR 参数 semanticId 到 LLVM 参数名的映射。
     */
    private val parameterNameById: Map<ChirSemanticId, String> = declaration.parameters.associate { parameter ->
        parameter.semanticId to sanitizeIdentifier(parameter.name, "param")
    }
    /**
     * CHIR block semanticId 到 LLVM label 的映射。
     */
    private val blockLabelById: Map<ChirSemanticId, String> by lazy(::buildBlockLabels)

    /**
     * 降低当前函数并返回 LLVM 函数产物。
     */
    fun lower(): LlvmFunctionArtifact {
        verifyControlFlow()
        val builder = IRBuilder()
        builder.emit(buildFunctionHeader())
        orderedBlocks().forEach { block ->
            builder.label(blockLabel(block.semanticId))
            block.expressions.forEach { expression ->
                dispatcher.lower(this, expression).forEach(builder::emit)
            }
            lowerTerminator(block.terminator).forEach(builder::emit)
        }
        builder.emit("}")
        return LlvmFunctionArtifact(declaration.name, builder.build().joinToString(System.lineSeparator()))
    }

    /**
     * 通过上下文类型 lowering 服务降低 CHIR 类型。
     */
    internal fun lowerType(type: ChirTypeRef): String = context.typeLowering.lower(type)

    /**
     * 生成表达式结果的 LLVM SSA 引用名。
     */
    internal fun resultRef(semanticId: ChirSemanticId): String = "%${sanitizeIdentifier(semanticId.value, "tmp")}"

    /**
     * 将 CHIR value 渲染为 LLVM value 引用或常量。
     */
    internal fun renderValue(value: ChirValue): String {
        return when (value) {
            is ChirConstantValue -> normalizeConstant(value.literal, lowerType(value.type))
            is ChirLocalValue -> "%${sanitizeIdentifier(value.name, "local")}"
            is ChirParameterValue -> {
                val paramName = parameterNameById[value.semanticId] ?: throw CodegenLoweringException(
                    "parameter value ${value.semanticId.value} is not declared in function ${declaration.name}",
                    value.semanticId,
                )
                "%$paramName"
            }
            is ChirGlobalValue -> "@${sanitizeIdentifier(value.name, "global")}"
            is ChirFunctionValue -> "@${sanitizeIdentifier(value.name, "fn")}"
            is ChirImportedFunctionValue -> "@${sanitizeIdentifier(value.name, "imported_fn")}"
            is ChirImportedVariableValue -> "@${sanitizeIdentifier(value.name, "imported_global")}"
            is ChirBlockValue -> "%${blockLabel(value.semanticId)}"
            is ChirBlockGroupValue -> "%${sanitizeIdentifier(value.name, "block_group")}"
        }
    }

    /**
     * 将 CHIR value 渲染为 `type value` 形式。
     */
    internal fun renderTypedValue(value: ChirValue): String = "${lowerType(value.type)} ${renderValue(value)}"

    /**
     * 渲染调用实参，保留允许出现在参数位置的属性 token。
     */
    internal fun renderCallArgument(value: ChirValue): String {
        val tokens = collectAttributeTokens(value.attributes, callArgumentReservedAttributeKeys)
        return if (tokens.isEmpty()) {
            renderTypedValue(value)
        } else {
            "${lowerType(value.type)} ${tokens.joinToString(" ")} ${renderValue(value)}"
        }
    }

    /**
     * 从调用目标属性中解析 tail/musttail/notail 调用种类。
     */
    internal fun callTailKind(attributes: Set<ChirAttribute>): String? {
        val explicit = attributeValue(attributes, "tail")
        if (!explicit.isNullOrBlank()) return explicit
        val enabledBoolean = attributes.asSequence()
            .filterIsInstance<ChirBooleanAttribute>()
            .firstOrNull { it.enabled && it.key in callTailKinds }
        return enabledBoolean?.key
    }

    /**
     * 解析基本块 id 对应的 LLVM label。
     */
    internal fun blockLabel(blockId: ChirSemanticId): String {
        return blockLabelById[blockId] ?: throw CodegenLoweringException(
            "function ${declaration.name} references missing block ${blockId.value}",
            blockId,
        )
    }

    /**
     * 按 semanticId 字符串或 block 名称解析 LLVM label。
     */
    internal fun resolveBlockLabel(reference: String): String? {
        val byId = blockLabelById[ChirSemanticId(reference)]
        if (byId != null) return byId
        val normalizedRef = sanitizeIdentifier(reference, "bb")
        val byName = declaration.blocks
            .firstOrNull { sanitizeIdentifier(it.name.ifBlank { it.semanticId.value }, "bb") == normalizedRef }
            ?.semanticId
        return byName?.let(blockLabelById::get)
    }

    /**
     * 降低 CHIR terminator 到 LLVM terminator 指令。
     */
    private fun lowerTerminator(terminator: ChirTerminator): List<String> {
        return when (terminator) {
            is ChirReturnTerminator -> lowerReturnTerminator(terminator)
            is ChirBranchTerminator -> listOf("  br label %${blockLabel(terminator.targetBlockId)}")
            is ChirConditionalBranchTerminator -> {
                requireSameType("i1", lowerType(terminator.condition.type), terminator.semanticId, "conditional branch condition")
                listOf(
                    "  br i1 ${renderValue(terminator.condition)}, label %${blockLabel(terminator.trueTargetBlockId)}, label %${blockLabel(terminator.falseTargetBlockId)}",
                )
            }
            is ChirThrowTerminator -> {
                requireSameType("ptr", lowerType(terminator.exceptionValue.type), terminator.semanticId, "throw exception")
                val lines = mutableListOf<String>()
                lines += "  call void @cangjie.throw(ptr ${renderValue(terminator.exceptionValue)})"
                val unwindTarget = terminator.unwindTargetBlockId
                if (unwindTarget != null) {
                    lines += "  br label %${blockLabel(unwindTarget)}"
                } else {
                    lines += "  unreachable"
                }
                lines
            }
            is ChirUnwindTerminator -> listOf("  br label %${blockLabel(terminator.targetBlockId)}")
            else -> throw CodegenLoweringException(
                "unsupported terminator type: ${terminator::class.simpleName}",
                declaration.semanticId,
            )
        }
    }

    /**
     * 降低 return terminator，并校验返回值类型与函数返回类型一致。
     */
    private fun lowerReturnTerminator(terminator: ChirReturnTerminator): List<String> {
        val functionReturnType = lowerType(declaration.returnType)
        val returnValue = terminator.returnValue
        if (returnValue == null) {
            return if (functionReturnType == "void") {
                listOf("  ret void")
            } else {
                throw CodegenLoweringException(
                    "function ${declaration.name} returns $functionReturnType but return terminator has no value",
                    terminator.semanticId,
                )
            }
        }
        val valueType = lowerType(returnValue.type)
        requireSameType(functionReturnType, valueType, terminator.semanticId, "return value")
        return listOf("  ret $valueType ${renderValue(returnValue)}")
    }

    /**
     * 构建 LLVM function header。
     */
    private fun buildFunctionHeader(): String {
        val linkage = attributeValue(declaration.attributes, "linkage")
        val callingConvention = attributeValue(declaration.attributes, "calling_conv")
            ?: attributeValue(declaration.attributes, "cc")
        val personality = attributeValue(declaration.attributes, "personality")

        val params = declaration.parameters.joinToString(", ") { parameter ->
            val parameterType = lowerType(parameter.type)
            val parameterName = parameterNameById[parameter.semanticId] ?: sanitizeIdentifier(parameter.name, "param")
            val attrs = collectAttributeTokens(parameter.attributes)
            if (attrs.isEmpty()) {
                "$parameterType %$parameterName"
            } else {
                "$parameterType ${attrs.joinToString(" ")} %$parameterName"
            }
        }

        return buildString {
            append("define ")
            if (!linkage.isNullOrBlank()) {
                append(linkage)
                append(' ')
            }
            if (!callingConvention.isNullOrBlank()) {
                append(callingConvention)
                append(' ')
            }
            append(lowerType(declaration.returnType))
            append(" @")
            append(sanitizeIdentifier(declaration.name, "fn"))
            append('(')
            append(params)
            append(')')

            val trailingAttributes = collectAttributeTokens(declaration.attributes)
            if (trailingAttributes.isNotEmpty()) {
                append(' ')
                append(trailingAttributes.joinToString(" "))
            }
            if (!personality.isNullOrBlank()) {
                append(" personality ptr @")
                append(sanitizeIdentifier(personality, "personality"))
            }
            append(" {")
        }
    }

    /**
     * 收集可直接打印到 LLVM IR 的属性 token。
     */
    internal fun collectAttributeTokens(
        attributes: Set<ChirAttribute>,
        reservedKeys: Set<String> = reservedAttributeKeys,
    ): List<String> {
        val tokens = mutableListOf<String>()
        attributes.forEach { attribute ->
            when (attribute) {
                is ChirBooleanAttribute -> {
                    if (attribute.enabled && attribute.key !in reservedKeys) {
                        tokens += attribute.key
                    }
                }
                is ChirStringAttribute -> {
                    if (attribute.key == "align") {
                        tokens += "align ${attribute.value}"
                    }
                }
            }
        }
        return tokens
    }

    /**
     * 从属性集合中读取指定字符串属性值。
     */
    internal fun attributeValue(attributes: Set<ChirAttribute>, key: String): String? {
        return attributes.asSequence()
            .filterIsInstance<ChirStringAttribute>()
            .firstOrNull { it.key == key }
            ?.value
    }

    /**
     * 返回当前函数的基本块 lowering 顺序。
     */
    private fun orderedBlocks() = buildList {
        val entry = declaration.blocks.firstOrNull { it.semanticId == declaration.entryBlockId }
        if (entry != null) {
            add(entry)
        }
        declaration.blocks.forEach { block ->
            if (entry == null || block.semanticId != entry.semanticId) {
                add(block)
            }
        }
    }

    /**
     * 为函数中所有基本块构建唯一 LLVM label。
     */
    private fun buildBlockLabels(): Map<ChirSemanticId, String> {
        val usedLabels = linkedMapOf<String, Int>()
        val labels = linkedMapOf<ChirSemanticId, String>()
        declaration.blocks.forEach { block ->
            val base = sanitizeIdentifier(block.name.ifBlank { block.semanticId.value }, "bb")
            labels[block.semanticId] = uniquifyIdentifier(base, usedLabels)
        }
        return labels
    }

    /**
     * 在写出函数前校验基本控制流引用。
     */
    private fun verifyControlFlow() {
        if (!context.options.verifyBeforeWrite) return
        if (declaration.blocks.isEmpty()) {
            throw CodegenLoweringException(
                "function ${declaration.name} must contain at least one block",
                declaration.semanticId,
            )
        }

        val blockIds = declaration.blocks.map { it.semanticId }.toSet()
        if (declaration.entryBlockId !in blockIds) {
            throw CodegenLoweringException(
                "function ${declaration.name} entry block ${declaration.entryBlockId.value} is missing",
                declaration.entryBlockId,
            )
        }

        declaration.blocks.forEach { block ->
            when (val terminator = block.terminator) {
                is ChirBranchTerminator -> {
                    if (terminator.targetBlockId !in blockIds) {
                        throw CodegenLoweringException(
                            "function ${declaration.name} has branch to missing block ${terminator.targetBlockId.value}",
                            terminator.semanticId,
                        )
                    }
                }
                is ChirConditionalBranchTerminator -> {
                    if (terminator.trueTargetBlockId !in blockIds) {
                        throw CodegenLoweringException(
                            "function ${declaration.name} has cond-branch true target ${terminator.trueTargetBlockId.value} missing",
                            terminator.semanticId,
                        )
                    }
                    if (terminator.falseTargetBlockId !in blockIds) {
                        throw CodegenLoweringException(
                            "function ${declaration.name} has cond-branch false target ${terminator.falseTargetBlockId.value} missing",
                            terminator.semanticId,
                        )
                    }
                }
                is ChirUnwindTerminator -> {
                    if (terminator.targetBlockId !in blockIds) {
                        throw CodegenLoweringException(
                            "function ${declaration.name} has unwind target ${terminator.targetBlockId.value} missing",
                            terminator.semanticId,
                        )
                    }
                }
                is ChirThrowTerminator -> {
                    val unwindTarget = terminator.unwindTargetBlockId
                    if (unwindTarget != null && unwindTarget !in blockIds) {
                        throw CodegenLoweringException(
                            "function ${declaration.name} has throw unwind target ${unwindTarget.value} missing",
                            terminator.semanticId,
                        )
                    }
                }
                is ChirReturnTerminator -> Unit
            }
        }
    }

    /**
     * 将 CHIR 常量字面量规范化为 LLVM 常量文本。
     */
    private fun normalizeConstant(literal: String, llvmType: String): String {
        return when {
            llvmType == "i1" && literal.equals("true", ignoreCase = true) -> "1"
            llvmType == "i1" && literal.equals("false", ignoreCase = true) -> "0"
            llvmType == "ptr" && literal.equals("null", ignoreCase = true) -> "null"
            else -> literal
        }
    }

    /**
     * 要求两个 LLVM textual type 完全一致。
     */
    private fun requireSameType(expected: String, actual: String, sourceId: ChirSemanticId, subject: String) {
        if (expected != actual) {
            throw CodegenLoweringException(
                "$subject type mismatch: expected $expected, got $actual",
                sourceId,
            )
        }
    }

    private companion object {
        val reservedAttributeKeys = setOf("linkage", "personality", "calling_conv", "cc")
        val callArgumentReservedAttributeKeys = reservedAttributeKeys + setOf("tail", "musttail", "notail")
        val callTailKinds = setOf("tail", "musttail", "notail")
    }
}
