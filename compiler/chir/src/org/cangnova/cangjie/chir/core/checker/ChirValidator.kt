package org.cangnova.cangjie.chir.core.checker

import org.cangnova.cangjie.chir.core.context.ChirContext
import org.cangnova.cangjie.chir.core.controlflow.ChirReturnTerminator
import org.cangnova.cangjie.chir.core.declaration.ChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.expression.ChirBinaryExpression
import org.cangnova.cangjie.chir.core.expression.ChirCallExpression
import org.cangnova.cangjie.chir.core.expression.ChirMemoryExpression
import org.cangnova.cangjie.chir.core.expression.ChirOtherExpression
import org.cangnova.cangjie.chir.core.expression.ChirUnaryExpression
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.model.ChirPackage
import org.cangnova.cangjie.chir.core.model.allDeclarations
import org.cangnova.cangjie.chir.core.model.validateMinimalControlFlow
import org.cangnova.cangjie.chir.core.type.ChirFunctionType
import org.cangnova.cangjie.chir.core.type.ChirPrimitiveType
import org.cangnova.cangjie.chir.core.type.ChirResolvedTypeRef

interface ChirValidator {
    fun validatePackage(chirPackage: ChirPackage, context: ChirContext? = null): ChirValidationReport
}

class DefaultChirValidator : ChirValidator {
    override fun validatePackage(chirPackage: ChirPackage, context: ChirContext?): ChirValidationReport {
        val issues = mutableListOf<ChirValidationIssue>()
        val allDeclarations = chirPackage.allDeclarations

        val duplicatePackageDeclarations = allDeclarations.groupBy { it.semanticId }.filterValues { it.size > 1 }
        duplicatePackageDeclarations.keys.forEach { declarationId ->
            issues += ChirValidationIssue(
                code = "DUPLICATE_PACKAGE_DECLARATION_ID",
                severity = ChirValidationSeverity.ERROR,
                message = "duplicate declaration id in package ${chirPackage.name}",
                nodeId = declarationId,
            )
        }

        chirPackage.modules.forEach { module ->
            val duplicateDeclarations = module.declarations.groupBy { it.semanticId }.filterValues { it.size > 1 }
            duplicateDeclarations.keys.forEach { declarationId ->
                issues += ChirValidationIssue(
                    code = "DUPLICATE_DECLARATION_ID",
                    severity = ChirValidationSeverity.ERROR,
                    message = "duplicate declaration id in module ${module.name}",
                    nodeId = declarationId,
                )
            }

            module.declarations.filterIsInstance<ChirFunctionDeclaration>().forEach { function ->
                validateFunction(function, issues)
            }
        }

        val functionById = allDeclarations
            .filterIsInstance<ChirFunctionDeclaration>()
            .associateBy { it.semanticId }

        validatePackageInitFunction(
            chirPackage.packageInitFunctionId,
            "PACKAGE_INIT_FUNCTION_MISSING",
            issues,
            functionById,
        )
        validatePackageInitFunction(
            chirPackage.packageLiteralInitFunctionId,
            "PACKAGE_LITERAL_INIT_FUNCTION_MISSING",
            issues,
            functionById,
        )

        if (context != null) {
            context.symbols.forEach { symbol ->
                if (context.findDeclaration(symbol.declarationId) == null) {
                    issues += ChirValidationIssue(
                        code = "BROKEN_SYMBOL_REFERENCE",
                        severity = ChirValidationSeverity.ERROR,
                        message = "symbol ${symbol.name} targets missing declaration ${symbol.declarationId}",
                        nodeId = symbol.semanticId,
                    )
                }
            }
        }

        return ChirValidationReport(issues)
    }

    private fun validateFunction(function: ChirFunctionDeclaration, issues: MutableList<ChirValidationIssue>) {
        val cfgErrors = validateMinimalControlFlow(function)
        cfgErrors.forEach { message ->
            issues += ChirValidationIssue(
                code = "INVALID_CFG",
                severity = ChirValidationSeverity.ERROR,
                message = message,
                nodeId = function.semanticId,
            )
        }
        val returnType = function.returnType
        val isUnitReturn = returnType is ChirResolvedTypeRef && returnType.type == ChirPrimitiveType.UNIT

        function.blocks.forEach { block ->
            block.expressions.forEach { expression ->
                when (expression) {
                    is ChirBinaryExpression -> {
                        if (expression.left.type != expression.right.type) {
                            issues += ChirValidationIssue(
                                code = "BINARY_OPERAND_TYPE_MISMATCH",
                                severity = ChirValidationSeverity.ERROR,
                                message = "binary expression operands must have the same type",
                                nodeId = expression.semanticId,
                            )
                        }
                    }
                    is ChirUnaryExpression -> Unit
                    is ChirCallExpression -> {
                        val functionType = (expression.callee.type as? ChirResolvedTypeRef)?.type as? ChirFunctionType
                        if (functionType != null) {
                            if (functionType.parameterTypes.size != expression.arguments.size) {
                                issues += ChirValidationIssue(
                                    code = "CALL_ARGUMENT_COUNT_MISMATCH",
                                    severity = ChirValidationSeverity.ERROR,
                                    message = "call argument count mismatch: expected ${functionType.parameterTypes.size}, actual ${expression.arguments.size}",
                                    nodeId = expression.semanticId,
                                )
                            } else {
                                expression.arguments.zip(functionType.parameterTypes).forEachIndexed { index, (argument, expectedType) ->
                                    if (argument.type != expectedType) {
                                        issues += ChirValidationIssue(
                                            code = "CALL_ARGUMENT_TYPE_MISMATCH",
                                            severity = ChirValidationSeverity.ERROR,
                                            message = "call argument #$index type mismatch: expected ${expectedType.renderName}, actual ${argument.type.renderName}",
                                            nodeId = expression.semanticId,
                                        )
                                    }
                                }
                            }
                        }
                    }
                    is ChirMemoryExpression -> {
                        val operation = expression.operation.lowercase()
                        if (operation !in supportedMemoryOperations) {
                            issues += ChirValidationIssue(
                                code = "UNSUPPORTED_MEMORY_OPERATION",
                                severity = ChirValidationSeverity.ERROR,
                                message = "unsupported memory operation '${expression.operation}'",
                                nodeId = expression.semanticId,
                            )
                        }
                    }
                    is ChirOtherExpression -> {
                        val operation = expression.operation.lowercase()
                        if (operation !in supportedOtherOperations) {
                            issues += ChirValidationIssue(
                                code = "UNSUPPORTED_OTHER_OPERATION",
                                severity = ChirValidationSeverity.ERROR,
                                message = "unsupported other expression operation '${expression.operation}'",
                                nodeId = expression.semanticId,
                            )
                        }
                    }
                }
            }

            val returnTerminator = block.terminator as? ChirReturnTerminator ?: return@forEach
            val returnValue = returnTerminator.returnValue
            if (returnValue == null && !isUnitReturn) {
                issues += ChirValidationIssue(
                    code = "RETURN_VALUE_MISMATCH",
                    severity = ChirValidationSeverity.ERROR,
                    message = "non-unit function ${function.name} must return a value",
                    nodeId = function.semanticId,
                )
            }
            if (returnValue != null && returnValue.type != returnType) {
                issues += ChirValidationIssue(
                    code = "RETURN_TYPE_MISMATCH",
                    severity = ChirValidationSeverity.ERROR,
                    message = "return value type ${returnValue.type.renderName} does not match function return type ${returnType.renderName}",
                    nodeId = returnTerminator.semanticId,
                )
            }
        }
    }

    private fun validatePackageInitFunction(
        functionId: ChirSemanticId?,
        code: String,
        issues: MutableList<ChirValidationIssue>,
        functionById: Map<ChirSemanticId, ChirFunctionDeclaration>,
    ) {
        if (functionId == null) return
        if (functionById[functionId] == null) {
            issues += ChirValidationIssue(
                code = code,
                severity = ChirValidationSeverity.ERROR,
                message = "package references missing function $functionId",
                nodeId = functionId,
            )
        }
    }

    private companion object {
        val supportedMemoryOperations = setOf("load", "store", "alloca", "gep", "getelementptr", "getelementptr.inbounds")
        val supportedOtherOperations = setOf(
            "select",
            "bitcast",
            "ptrtoint",
            "inttoptr",
            "trunc",
            "zext",
            "sext",
            "fptrunc",
            "fpext",
            "sitofp",
            "uitofp",
            "fptosi",
            "fptoui",
            "phi",
        )
    }
}
