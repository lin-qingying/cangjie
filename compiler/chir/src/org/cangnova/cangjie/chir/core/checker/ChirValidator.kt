package org.cangnova.cangjie.chir.core.checker

import org.cangnova.cangjie.chir.core.context.ChirContext
import org.cangnova.cangjie.chir.core.controlflow.ChirReturnTerminator
import org.cangnova.cangjie.chir.core.declaration.ChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.expression.ChirBinaryExpression
import org.cangnova.cangjie.chir.core.expression.ChirCallExpression
import org.cangnova.cangjie.chir.core.expression.ChirUnaryExpression
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.model.ChirPackage
import org.cangnova.cangjie.chir.core.model.allDeclarations
import org.cangnova.cangjie.chir.core.model.validateMinimalControlFlow
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

        function.blocks.forEach { block ->
            block.expressions.forEach { expression ->
                when (expression) {
                    is ChirUnaryExpression,
                    is ChirBinaryExpression,
                    is ChirCallExpression,
                    -> if (expression.resultType == null) {
                        issues += ChirValidationIssue(
                            code = "MISSING_RESULT_TYPE",
                            severity = ChirValidationSeverity.ERROR,
                            message = "expression requires a result type",
                            nodeId = expression.semanticId,
                        )
                    }
                }
            }
        }

        val returnType = function.returnType
        val isUnitReturn = returnType is ChirResolvedTypeRef && returnType.type == ChirPrimitiveType.UNIT
        if (!isUnitReturn && function.blocks.isNotEmpty()) {
            val hasValueReturn = function.blocks.any { block ->
                (block.terminator as? ChirReturnTerminator)?.returnValue != null
            }
            if (!hasValueReturn) {
                issues += ChirValidationIssue(
                    code = "RETURN_VALUE_MISMATCH",
                    severity = ChirValidationSeverity.WARNING,
                    message = "non-unit function ${function.name} has no return terminator carrying a value",
                    nodeId = function.semanticId,
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
}
