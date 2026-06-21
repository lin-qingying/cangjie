package org.cangnova.cangjie.chir.core.printer

import org.cangnova.cangjie.chir.core.controlflow.ChirBranchTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirConditionalBranchTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirReturnTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirThrowTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirUnwindTerminator
import org.cangnova.cangjie.chir.core.declaration.ChirDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.expression.ChirBinaryExpression
import org.cangnova.cangjie.chir.core.expression.ChirCallExpression
import org.cangnova.cangjie.chir.core.expression.ChirExpression
import org.cangnova.cangjie.chir.core.expression.ChirMemoryExpression
import org.cangnova.cangjie.chir.core.expression.ChirOtherExpression
import org.cangnova.cangjie.chir.core.expression.ChirUnaryExpression
import org.cangnova.cangjie.chir.core.model.ChirPackage
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

object ChirPrinter {
    fun print(chirPackage: ChirPackage): String {
        return buildString {
            appendLine(
                "package ${chirPackage.name} id=${chirPackage.semanticId} access=${chirPackage.accessLevel} " +
                    "packageInit=${chirPackage.packageInitFunctionId ?: "_"} literalInit=${chirPackage.packageLiteralInitFunctionId ?: "_"}",
            )
            appendLine(
                "  members globals(vars=${chirPackage.members.globalVariables.size}, funcs=${chirPackage.members.globalFunctions.size}) " +
                    "imports(vars=${chirPackage.members.importedVariables.size}, funcs=${chirPackage.members.importedFunctions.size}) " +
                    "types(local=${chirPackage.typeDefinitions.size}, imported=${chirPackage.importedTypeDefinitions.size})",
            )
            chirPackage.members.globalVariables.sortedBy { it.semanticId.value }.forEach { appendDeclaration(it, "  global") }
            chirPackage.members.globalFunctions.sortedBy { it.semanticId.value }.forEach { appendDeclaration(it, "  global") }
            chirPackage.members.importedVariables.sortedBy { it.semanticId.value }.forEach { appendDeclaration(it, "  imported") }
            chirPackage.members.importedFunctions.sortedBy { it.semanticId.value }.forEach { appendDeclaration(it, "  imported") }
            chirPackage.typeDefinitions.sortedBy { it.semanticId.value }.forEach { appendDeclaration(it, "  type") }
            chirPackage.importedTypeDefinitions.sortedBy { it.semanticId.value }.forEach { appendDeclaration(it, "  imported-type") }
            chirPackage.modules
                .sortedBy { it.semanticId.value }
                .forEach { module ->
                    appendLine("  module ${module.name} id=${module.semanticId}")
                    module.declarations
                        .sortedBy { it.semanticId.value }
                        .forEach { declaration ->
                            appendDeclaration(declaration)
                        }
                }
        }.trimEnd()
    }

    private fun StringBuilder.appendDeclaration(declaration: ChirDeclaration, prefix: String = "    ") {
        val function = declaration as? ChirFunctionDeclaration
        if (function == null) {
            appendLine("$prefix declaration ${declaration.name} id=${declaration.semanticId} kind=${declaration::class.simpleName}")
            return
        }

        appendLine(
            "$prefix function ${function.name} id=${function.semanticId} return=${function.returnType.renderName} entry=${function.entryBlockId}",
        )
        function.parameters.forEachIndexed { index, parameter ->
            appendLine(
                "${prefix}  param[$index] ${parameter.name} id=${parameter.semanticId} type=${parameter.type.renderName} mutable=${parameter.mutable}",
            )
        }
        function.blocks
            .sortedBy { it.semanticId.value }
            .forEachIndexed { index, block ->
                appendLine("${prefix}  block[$index] ${block.name} id=${block.semanticId}")
                block.expressions.forEachIndexed { exprIndex, expression ->
                    appendLine("${prefix}    expr[$exprIndex] ${printExpression(expression)}")
                }
                appendLine("${prefix}    term ${printTerminator(block.terminator)}")
            }
    }

    private fun printExpression(expression: ChirExpression): String {
        return when (expression) {
            is ChirUnaryExpression -> "unary id=${expression.semanticId} op=${expression.operator} operand=${printValue(expression.operand)} type=${expression.resultType.renderName}"
            is ChirBinaryExpression -> "binary id=${expression.semanticId} op=${expression.operator} left=${printValue(expression.left)} right=${printValue(expression.right)} type=${expression.resultType.renderName}"
            is ChirMemoryExpression -> "memory id=${expression.semanticId} op=${expression.operation} address=${printValue(expression.address)} value=${expression.value?.let(::printValue) ?: "_"} type=${expression.resultType?.renderName ?: "_"}"
            is ChirCallExpression -> "call id=${expression.semanticId} callee=${printValue(expression.callee)} args=[${expression.arguments.joinToString { printValue(it) }}] type=${expression.resultType.renderName}"
            is ChirOtherExpression -> "other id=${expression.semanticId} op=${expression.operation} operands=[${expression.operands.joinToString { printValue(it) }}] type=${expression.resultType?.renderName ?: "_"}"
            else -> "unknown id=${expression.semanticId}"
        }
    }

    private fun printTerminator(terminator: ChirTerminator): String {
        return when (terminator) {
            is ChirReturnTerminator -> "return id=${terminator.semanticId} value=${terminator.returnValue?.let(::printValue) ?: "_"}"
            is ChirBranchTerminator -> "branch id=${terminator.semanticId} target=${terminator.targetBlockId}"
            is ChirConditionalBranchTerminator -> "cond id=${terminator.semanticId} cond=${printValue(terminator.condition)} true=${terminator.trueTargetBlockId} false=${terminator.falseTargetBlockId}"
            is ChirThrowTerminator -> "throw id=${terminator.semanticId} exception=${printValue(terminator.exceptionValue)} unwind=${terminator.unwindTargetBlockId ?: "_"}"
            is ChirUnwindTerminator -> "unwind id=${terminator.semanticId} target=${terminator.targetBlockId}"
            else -> "unknown id=${terminator.semanticId}"
        }
    }

    private fun printValue(value: ChirValue): String {
        return when (value) {
            is ChirConstantValue -> "const(id=${value.semanticId},type=${value.type.renderName},literal=${value.literal})"
            is ChirLocalValue -> "local(id=${value.semanticId},type=${value.type.renderName},name=${value.name})"
            is ChirParameterValue -> "param(id=${value.semanticId},type=${value.type.renderName},name=${value.name},owner=${value.ownerFunctionId})"
            is ChirGlobalValue -> "global(id=${value.semanticId},type=${value.type.renderName},name=${value.name})"
            is ChirImportedFunctionValue -> "imported-func(id=${value.semanticId},type=${value.type.renderName},name=${value.name})"
            is ChirImportedVariableValue -> "imported-var(id=${value.semanticId},type=${value.type.renderName},name=${value.name})"
            is ChirFunctionValue -> "func(id=${value.semanticId},type=${value.type.renderName},name=${value.name})"
            is ChirBlockValue -> "block(id=${value.semanticId},type=${value.type.renderName},name=${value.name},owner=${value.ownerFunctionId})"
            is ChirBlockGroupValue -> "block-group(id=${value.semanticId},type=${value.type.renderName},name=${value.name})"
            else -> "value(id=${value.semanticId},kind=${value.kind},type=${value.type.renderName},name=${value.displayName ?: "_"})"
        }
    }
}
