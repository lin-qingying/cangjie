package org.cangnova.cangjie.codegen.function

import org.cangnova.cangjie.chir.core.attribute.ChirAttribute
import org.cangnova.cangjie.chir.core.attribute.ChirBooleanAttribute
import org.cangnova.cangjie.chir.core.attribute.ChirStringAttribute
import org.cangnova.cangjie.chir.core.controlflow.ChirBranchTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirConditionalBranchTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirReturnTerminator
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

class CGFunction(
    private val declaration: ChirFunctionDeclaration,
    private val context: CGContext,
    private val dispatcher: ExpressionLoweringDispatcher,
) {
    private val parameterNameById: Map<ChirSemanticId, String> = declaration.parameters.associate { parameter ->
        parameter.semanticId to sanitizeIdentifier(parameter.name, "param")
    }
    private val blockLabelById: Map<ChirSemanticId, String> by lazy(::buildBlockLabels)

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

    internal fun lowerType(type: ChirTypeRef): String = context.typeLowering.lower(type)

    internal fun resultRef(semanticId: ChirSemanticId): String = "%${sanitizeIdentifier(semanticId.value, "tmp")}"

    internal fun renderValue(value: ChirValue): String {
        return when (value) {
            is ChirConstantValue -> normalizeConstant(value.literal, lowerType(value.type))
            is ChirLocalValue -> "%${sanitizeIdentifier(value.name, "local")}"
            is ChirParameterValue -> {
                val paramName = parameterNameById[value.semanticId] ?: sanitizeIdentifier(value.name, "param")
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

    internal fun renderTypedValue(value: ChirValue): String = "${lowerType(value.type)} ${renderValue(value)}"

    internal fun blockLabel(blockId: ChirSemanticId): String {
        return blockLabelById[blockId] ?: sanitizeIdentifier(blockId.value, "bb")
    }

    private fun lowerTerminator(terminator: Any): List<String> {
        return when (terminator) {
            is ChirReturnTerminator -> lowerReturnTerminator(terminator)
            is ChirBranchTerminator -> listOf("  br label %${blockLabel(terminator.targetBlockId)}")
            is ChirConditionalBranchTerminator -> listOf(
                "  br i1 ${renderValue(terminator.condition)}, label %${blockLabel(terminator.trueTargetBlockId)}, label %${blockLabel(terminator.falseTargetBlockId)}",
            )
            is ChirThrowTerminator -> {
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

    private fun lowerReturnTerminator(terminator: ChirReturnTerminator): List<String> {
        val functionReturnType = lowerType(declaration.returnType)
        val returnValue = terminator.returnValue
        if (returnValue == null) {
            return if (functionReturnType == "void") {
                listOf("  ret void")
            } else {
                listOf("  ret $functionReturnType ${defaultValueLiteral(functionReturnType)}")
            }
        }
        return listOf("  ret ${lowerType(returnValue.type)} ${renderValue(returnValue)}")
    }

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

    private fun collectAttributeTokens(attributes: Set<ChirAttribute>): List<String> {
        val tokens = mutableListOf<String>()
        attributes.forEach { attribute ->
            when (attribute) {
                is ChirBooleanAttribute -> {
                    if (attribute.enabled && attribute.key !in reservedAttributeKeys) {
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

    private fun attributeValue(attributes: Set<ChirAttribute>, key: String): String? {
        return attributes.asSequence()
            .filterIsInstance<ChirStringAttribute>()
            .firstOrNull { it.key == key }
            ?.value
    }

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

    private fun buildBlockLabels(): Map<ChirSemanticId, String> {
        val usedLabels = linkedMapOf<String, Int>()
        val labels = linkedMapOf<ChirSemanticId, String>()
        declaration.blocks.forEach { block ->
            val base = sanitizeIdentifier(block.name.ifBlank { block.semanticId.value }, "bb")
            labels[block.semanticId] = uniquifyIdentifier(base, usedLabels)
        }
        return labels
    }

    private fun verifyControlFlow() {
        if (!context.options.verifyBeforeWrite) return
        require(declaration.blocks.isNotEmpty()) { "function ${declaration.name} must contain at least one block" }

        val blockIds = declaration.blocks.map { it.semanticId }.toSet()
        require(declaration.entryBlockId in blockIds) {
            "function ${declaration.name} entry block ${declaration.entryBlockId.value} is missing"
        }

        declaration.blocks.forEach { block ->
            when (val terminator = block.terminator) {
                is ChirBranchTerminator -> require(terminator.targetBlockId in blockIds) {
                    "function ${declaration.name} has branch to missing block ${terminator.targetBlockId.value}"
                }
                is ChirConditionalBranchTerminator -> {
                    require(terminator.trueTargetBlockId in blockIds) {
                        "function ${declaration.name} has cond-branch true target ${terminator.trueTargetBlockId.value} missing"
                    }
                    require(terminator.falseTargetBlockId in blockIds) {
                        "function ${declaration.name} has cond-branch false target ${terminator.falseTargetBlockId.value} missing"
                    }
                }
                is ChirUnwindTerminator -> require(terminator.targetBlockId in blockIds) {
                    "function ${declaration.name} has unwind target ${terminator.targetBlockId.value} missing"
                }
                else -> Unit
            }
        }
    }

    private fun normalizeConstant(literal: String, llvmType: String): String {
        return when {
            llvmType == "i1" && literal.equals("true", ignoreCase = true) -> "1"
            llvmType == "i1" && literal.equals("false", ignoreCase = true) -> "0"
            llvmType == "ptr" && literal.equals("null", ignoreCase = true) -> "null"
            else -> literal
        }
    }

    private fun defaultValueLiteral(llvmType: String): String {
        return when {
            llvmType == "void" -> "void"
            llvmType == "i1" -> "0"
            llvmType.matches(Regex("i\\d+")) -> "0"
            llvmType == "half" || llvmType == "float" || llvmType == "double" -> "0.0"
            llvmType == "ptr" -> "null"
            else -> "zeroinitializer"
        }
    }

    private companion object {
        val reservedAttributeKeys = setOf("linkage", "personality", "calling_conv", "cc")
    }
}

