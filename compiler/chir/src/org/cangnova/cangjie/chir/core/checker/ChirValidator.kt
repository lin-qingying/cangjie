package org.cangnova.cangjie.chir.core.checker

import org.cangnova.cangjie.chir.core.context.ChirReadOnlyContext
import org.cangnova.cangjie.chir.core.controlflow.ChirBranchTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirConditionalBranchTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirReturnTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirThrowTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirUnwindTerminator
import org.cangnova.cangjie.chir.core.declaration.ChirClassDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirEnumDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirExtendDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirStructDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirTypeDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirVariableDeclaration
import org.cangnova.cangjie.chir.core.expression.ChirBinaryExpression
import org.cangnova.cangjie.chir.core.expression.ChirCallExpression
import org.cangnova.cangjie.chir.core.expression.ChirMemoryExpression
import org.cangnova.cangjie.chir.core.expression.ChirOperationSets
import org.cangnova.cangjie.chir.core.expression.ChirOtherExpression
import org.cangnova.cangjie.chir.core.expression.ChirUnaryExpression
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.model.ChirPackage
import org.cangnova.cangjie.chir.core.model.allDeclarations
import org.cangnova.cangjie.chir.core.model.validateMinimalControlFlow
import org.cangnova.cangjie.chir.core.type.ChirBoxType
import org.cangnova.cangjie.chir.core.type.ChirCPointerType
import org.cangnova.cangjie.chir.core.type.ChirCStringType
import org.cangnova.cangjie.chir.core.type.ChirClassType
import org.cangnova.cangjie.chir.core.type.ChirEnumType
import org.cangnova.cangjie.chir.core.type.ChirFunctionType
import org.cangnova.cangjie.chir.core.type.ChirGenericType
import org.cangnova.cangjie.chir.core.type.ChirNamedType
import org.cangnova.cangjie.chir.core.type.ChirPrimitiveType
import org.cangnova.cangjie.chir.core.type.ChirRawArrayType
import org.cangnova.cangjie.chir.core.type.ChirRefType
import org.cangnova.cangjie.chir.core.type.ChirResolvedTypeRef
import org.cangnova.cangjie.chir.core.type.ChirStructType
import org.cangnova.cangjie.chir.core.type.ChirThisType
import org.cangnova.cangjie.chir.core.type.ChirTupleType
import org.cangnova.cangjie.chir.core.type.ChirType
import org.cangnova.cangjie.chir.core.type.ChirTypeRef
import org.cangnova.cangjie.chir.core.type.ChirUnresolvedTypeRef
import org.cangnova.cangjie.chir.core.type.ChirVArrayType
import org.cangnova.cangjie.chir.core.value.ChirValue

interface ChirValidator {
    fun validatePackage(chirPackage: ChirPackage, context: ChirReadOnlyContext? = null): ChirValidationReport
}

class DefaultChirValidator : ChirValidator {
    override fun validatePackage(chirPackage: ChirPackage, context: ChirReadOnlyContext?): ChirValidationReport {
        val issues = mutableListOf<ChirValidationIssue>()
        val allDeclarations = chirPackage.allDeclarations

        allDeclarations.forEach { declaration ->
            validateDeclarationTypes(declaration, issues)
        }

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
        }

        val importedFunctionIds = chirPackage.members.importedFunctions.mapTo(hashSetOf()) { it.semanticId }
        allDeclarations
            .filterIsInstance<ChirFunctionDeclaration>()
            .filterNot { it.semanticId in importedFunctionIds }
            .forEach { function -> validateFunction(function, issues) }

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
                        val operator = expression.operator.lowercase()
                        validateTypeRef(expression.left.type, expression.left.semanticId, "binary.left", issues)
                        validateTypeRef(expression.right.type, expression.right.semanticId, "binary.right", issues)
                        validateTypeRef(expression.resultType, expression.semanticId, "binary.result", issues)
                        if (operator !in ChirOperationSets.binaryOperators) {
                            issues += ChirValidationIssue(
                                code = "UNSUPPORTED_BINARY_OPERATOR",
                                severity = ChirValidationSeverity.ERROR,
                                message = "unsupported binary operator '${expression.operator}'",
                                nodeId = expression.semanticId,
                            )
                        }
                        if (expression.left.type != expression.right.type) {
                            issues += ChirValidationIssue(
                                code = "BINARY_OPERAND_TYPE_MISMATCH",
                                severity = ChirValidationSeverity.ERROR,
                                message = "binary expression operands must have the same type",
                                nodeId = expression.semanticId,
                            )
                        }
                    }
                    is ChirUnaryExpression -> {
                        val operator = expression.operator.lowercase()
                        validateTypeRef(expression.operand.type, expression.operand.semanticId, "unary.operand", issues)
                        validateTypeRef(expression.resultType, expression.semanticId, "unary.result", issues)
                        if (operator !in ChirOperationSets.unaryOperators) {
                            issues += ChirValidationIssue(
                                code = "UNSUPPORTED_UNARY_OPERATOR",
                                severity = ChirValidationSeverity.ERROR,
                                message = "unsupported unary operator '${expression.operator}'",
                                nodeId = expression.semanticId,
                            )
                        }
                    }
                    is ChirCallExpression -> {
                        validateTypeRef(expression.callee.type, expression.callee.semanticId, "call.callee", issues)
                        validateTypeRef(expression.resultType, expression.semanticId, "call.result", issues)
                        val functionType = (expression.callee.type as? ChirResolvedTypeRef)?.type as? ChirFunctionType
                        if (functionType != null) {
                            val expectedArgumentTypes = buildList {
                                functionType.receiverType?.let(::add)
                                addAll(functionType.parameterTypes)
                            }
                            if (expectedArgumentTypes.size != expression.arguments.size) {
                                issues += ChirValidationIssue(
                                    code = "CALL_ARGUMENT_COUNT_MISMATCH",
                                    severity = ChirValidationSeverity.ERROR,
                                    message = "call argument count mismatch: expected ${expectedArgumentTypes.size}, actual ${expression.arguments.size}",
                                    nodeId = expression.semanticId,
                                )
                            } else {
                                expression.arguments.zip(expectedArgumentTypes).forEachIndexed { index, (argument, expectedType) ->
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
                        expression.arguments.forEachIndexed { index, argument ->
                            validateTypeRef(argument.type, argument.semanticId, "call.argument#$index", issues)
                        }
                    }
                    is ChirMemoryExpression -> {
                        val operation = expression.operation.lowercase()
                        validateTypeRef(expression.address.type, expression.address.semanticId, "memory.address", issues)
                        expression.value?.let { validateTypeRef(it.type, it.semanticId, "memory.value", issues) }
                        expression.resultType?.let { validateTypeRef(it, expression.semanticId, "memory.result", issues) }
                        if (operation !in ChirOperationSets.memoryOperations) {
                            issues += ChirValidationIssue(
                                code = "UNSUPPORTED_MEMORY_OPERATION",
                                severity = ChirValidationSeverity.ERROR,
                                message = "unsupported memory operation '${expression.operation}'",
                                nodeId = expression.semanticId,
                            )
                        }
                        when (operation) {
                            "alloca" -> {
                                if (expression.value != null) {
                                    issues += ChirValidationIssue(
                                        code = "MEMORY_ALLOCA_VALUE_PRESENT",
                                        severity = ChirValidationSeverity.ERROR,
                                        message = "alloca expression must not carry a stored value",
                                        nodeId = expression.semanticId,
                                    )
                                }
                                if (expression.resultType == null) {
                                    validateLocalAllocaAddress(expression.address, expression, issues)
                                } else {
                                    validatePointerAlloca(expression, issues)
                                }
                            }
                            "load" -> {
                                val targetType = validateMemoryAddress(expression.address, expression, issues)
                                if (expression.value != null) {
                                    issues += ChirValidationIssue(
                                        code = "MEMORY_LOAD_VALUE_PRESENT",
                                        severity = ChirValidationSeverity.ERROR,
                                        message = "load expression must not carry a stored value",
                                        nodeId = expression.semanticId,
                                    )
                                }
                                if (expression.resultType == null) {
                                    issues += ChirValidationIssue(
                                        code = "MEMORY_LOAD_RESULT_MISSING",
                                        severity = ChirValidationSeverity.ERROR,
                                        message = "load expression must declare loaded resultType",
                                        nodeId = expression.semanticId,
                                    )
                                } else if (targetType != null && expression.resultType != targetType) {
                                    issues += ChirValidationIssue(
                                        code = "MEMORY_LOAD_RESULT_TYPE_MISMATCH",
                                        severity = ChirValidationSeverity.ERROR,
                                        message = "load result type ${expression.resultType.renderName} does not match address target ${targetType.renderName}",
                                        nodeId = expression.semanticId,
                                    )
                                }
                            }
                            "store" -> {
                                val targetType = validateMemoryAddress(expression.address, expression, issues)
                                if (expression.value == null) {
                                    issues += ChirValidationIssue(
                                        code = "MEMORY_STORE_VALUE_MISSING",
                                        severity = ChirValidationSeverity.ERROR,
                                        message = "store expression must carry a value",
                                        nodeId = expression.semanticId,
                                    )
                                }
                                if (expression.resultType != null) {
                                    issues += ChirValidationIssue(
                                        code = "MEMORY_STORE_RESULT_PRESENT",
                                        severity = ChirValidationSeverity.ERROR,
                                        message = "store expression must not carry resultType",
                                        nodeId = expression.semanticId,
                                    )
                                }
                                if (targetType != null && expression.value != null && expression.value.type != targetType) {
                                    issues += ChirValidationIssue(
                                        code = "MEMORY_STORE_VALUE_TYPE_MISMATCH",
                                        severity = ChirValidationSeverity.ERROR,
                                        message = "store value type ${expression.value.type.renderName} does not match address target ${targetType.renderName}",
                                        nodeId = expression.semanticId,
                                    )
                                }
                            }
                        }
                    }
                    is ChirOtherExpression -> {
                        val operation = expression.operation.lowercase()
                        expression.operands.forEachIndexed { index, operand ->
                            validateTypeRef(operand.type, operand.semanticId, "other.operand#$index", issues)
                        }
                        expression.resultType?.let { validateTypeRef(it, expression.semanticId, "other.result", issues) }
                        if (operation !in ChirOperationSets.otherOperations) {
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

            when (val terminator = block.terminator) {
                is ChirReturnTerminator -> {
                    val returnValue = terminator.returnValue
                    validateTypeRef(returnType, function.semanticId, "function.return", issues)
                    if (returnValue == null && !isUnitReturn) {
                        issues += ChirValidationIssue(
                            code = "RETURN_VALUE_MISMATCH",
                            severity = ChirValidationSeverity.ERROR,
                            message = "non-unit function ${function.name} must return a value",
                            nodeId = function.semanticId,
                        )
                    }
                    if (returnValue != null && returnValue.type != returnType) {
                        validateTypeRef(returnValue.type, terminator.semanticId, "return.value", issues)
                        issues += ChirValidationIssue(
                            code = "RETURN_TYPE_MISMATCH",
                            severity = ChirValidationSeverity.ERROR,
                            message = "return value type ${returnValue.type.renderName} does not match function return type ${returnType.renderName}",
                            nodeId = terminator.semanticId,
                        )
                    }
                }
                is ChirBranchTerminator -> Unit
                is ChirConditionalBranchTerminator -> {
                    validateTypeRef(terminator.condition.type, terminator.semanticId, "branch.condition", issues)
                    if (terminator.condition.type != ChirResolvedTypeRef(ChirPrimitiveType.BOOL)) {
                        issues += ChirValidationIssue(
                            code = "BRANCH_CONDITION_TYPE_MISMATCH",
                            severity = ChirValidationSeverity.ERROR,
                            message = "conditional branch condition must be bool, actual ${terminator.condition.type.renderName}",
                            nodeId = terminator.semanticId,
                        )
                    }
                }
                is ChirThrowTerminator, is ChirUnwindTerminator -> Unit
                else -> {
                    issues += ChirValidationIssue(
                        code = "UNSUPPORTED_TERMINATOR",
                        severity = ChirValidationSeverity.ERROR,
                        message = "unsupported terminator type '${terminator::class.simpleName}'",
                        nodeId = terminator.semanticId,
                    )
                }
            }
        }
    }

    private fun validateMemoryAddress(
        address: ChirValue,
        expression: ChirMemoryExpression,
        issues: MutableList<ChirValidationIssue>,
    ): ChirTypeRef? {
        return when (val addressType = (address.type as? ChirResolvedTypeRef)?.type) {
            is ChirRefType -> addressType.referencedType
            is ChirCPointerType -> addressType.pointeeType
            else -> {
                issues += ChirValidationIssue(
                    code = "MEMORY_ADDRESS_TYPE_MISMATCH",
                    severity = ChirValidationSeverity.ERROR,
                    message = "memory operation '${expression.operation}' requires ref or cpointer address, actual ${address.type.renderName}",
                    nodeId = expression.semanticId,
                )
                null
            }
        }
    }

    private fun validateLocalAllocaAddress(
        address: ChirValue,
        expression: ChirMemoryExpression,
        issues: MutableList<ChirValidationIssue>,
    ) {
        if (((address.type as? ChirResolvedTypeRef)?.type as? ChirRefType) == null) {
            issues += ChirValidationIssue(
                code = "MEMORY_ALLOCA_ADDRESS_TYPE_MISMATCH",
                severity = ChirValidationSeverity.ERROR,
                message = "local alloca without resultType requires ref address, actual ${address.type.renderName}",
                nodeId = expression.semanticId,
            )
        }
    }

    private fun validatePointerAlloca(
        expression: ChirMemoryExpression,
        issues: MutableList<ChirValidationIssue>,
    ) {
        if (((expression.resultType as? ChirResolvedTypeRef)?.type as? ChirCPointerType) == null) {
            issues += ChirValidationIssue(
                code = "MEMORY_ALLOCA_RESULT_TYPE_MISMATCH",
                severity = ChirValidationSeverity.ERROR,
                message = "pointer alloca with resultType must produce cpointer, actual ${expression.resultType?.renderName}",
                nodeId = expression.semanticId,
            )
        }
        if (!expression.address.type.isIntegerLikeMemorySize()) {
            issues += ChirValidationIssue(
                code = "MEMORY_ALLOCA_SIZE_TYPE_MISMATCH",
                severity = ChirValidationSeverity.ERROR,
                message = "pointer alloca size operand must be integer-like, actual ${expression.address.type.renderName}",
                nodeId = expression.semanticId,
            )
        }
    }

    private fun ChirTypeRef.isIntegerLikeMemorySize(): Boolean {
        val primitive = (this as? ChirResolvedTypeRef)?.type as? ChirPrimitiveType ?: return false
        return primitive in integerLikeMemorySizeTypes
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

    private fun validateDeclarationTypes(declaration: ChirDeclaration, issues: MutableList<ChirValidationIssue>) {
        when (declaration) {
            is ChirVariableDeclaration -> validateTypeRef(
                declaration.type,
                declaration.semanticId,
                "declaration:${declaration.name}.type",
                issues,
            )

            is ChirFunctionDeclaration -> {
                validateTypeRef(declaration.returnType, declaration.semanticId, "function:${declaration.name}.return", issues)
                declaration.parameters.forEach { parameter ->
                    validateTypeRef(
                        parameter.type,
                        parameter.semanticId,
                        "function:${declaration.name}.parameter:${parameter.name}",
                        issues,
                    )
                }
            }

            is ChirClassDeclaration -> {
                declaration.superTypes.forEachIndexed { index, typeRef ->
                    validateTypeRef(typeRef, declaration.semanticId, "class:${declaration.name}.super#$index", issues)
                }
                declaration.implementedTypes.forEachIndexed { index, typeRef ->
                    validateTypeRef(typeRef, declaration.semanticId, "class:${declaration.name}.implements#$index", issues)
                }
            }

            is ChirStructDeclaration -> {
                declaration.fieldDeclarations.forEach { field ->
                    validateTypeRef(
                        field.type,
                        field.semanticId,
                        "struct:${declaration.name}.field:${field.name}",
                        issues,
                    )
                }
            }

            is ChirEnumDeclaration -> Unit
            is ChirTypeDeclaration -> Unit

            is ChirExtendDeclaration -> {
                validateTypeRef(declaration.targetType, declaration.semanticId, "extend:${declaration.name}.target", issues)
                declaration.extendedTypes.forEachIndexed { index, typeRef ->
                    validateTypeRef(typeRef, declaration.semanticId, "extend:${declaration.name}.type#$index", issues)
                }
            }
        }
    }

    private fun validateTypeRef(
        typeRef: ChirTypeRef,
        nodeId: ChirSemanticId,
        location: String,
        issues: MutableList<ChirValidationIssue>,
    ) {
        when (typeRef) {
            is ChirUnresolvedTypeRef -> {
                issues += ChirValidationIssue(
                    code = "UNRESOLVED_TYPE_REFERENCE",
                    severity = ChirValidationSeverity.ERROR,
                    message = "unresolved type reference '$location' -> '${typeRef.symbol}'",
                    nodeId = nodeId,
                )
            }

            is ChirResolvedTypeRef -> validateType(typeRef.type, nodeId, location, issues)
        }
    }

    private fun validateType(
        type: ChirType,
        nodeId: ChirSemanticId,
        location: String,
        issues: MutableList<ChirValidationIssue>,
    ) {
        when (type) {
            is ChirPrimitiveType -> Unit
            is ChirNamedType -> type.typeArguments.forEachIndexed { index, nested ->
                validateTypeRef(nested, nodeId, "$location.namedArg#$index", issues)
            }

            is ChirTupleType -> type.elementTypes.forEachIndexed { index, nested ->
                validateTypeRef(nested, nodeId, "$location.tuple#$index", issues)
            }

            is ChirFunctionType -> {
                type.parameterTypes.forEachIndexed { index, nested ->
                    validateTypeRef(nested, nodeId, "$location.fnArg#$index", issues)
                }
                validateTypeRef(type.returnType, nodeId, "$location.fnReturn", issues)
                type.receiverType?.let { validateTypeRef(it, nodeId, "$location.fnReceiver", issues) }
            }

            is ChirStructType -> type.fieldTypes.forEachIndexed { index, nested ->
                validateTypeRef(nested, nodeId, "$location.structField#$index", issues)
            }

            is ChirClassType -> {
                type.fieldTypes.forEachIndexed { index, nested ->
                    validateTypeRef(nested, nodeId, "$location.classField#$index", issues)
                }
                type.superTypes.forEachIndexed { index, nested ->
                    validateTypeRef(nested, nodeId, "$location.classSuper#$index", issues)
                }
                type.typeArguments.forEachIndexed { index, nested ->
                    validateTypeRef(nested, nodeId, "$location.classArg#$index", issues)
                }
            }

            is ChirEnumType -> {
                type.cases.forEachIndexed { caseIndex, enumCaseType ->
                    enumCaseType.payloadTypes.forEachIndexed { payloadIndex, nested ->
                        validateTypeRef(nested, nodeId, "$location.enumCase#$caseIndex.payload#$payloadIndex", issues)
                    }
                }
                type.typeArguments.forEachIndexed { index, nested ->
                    validateTypeRef(nested, nodeId, "$location.enumArg#$index", issues)
                }
            }

            is ChirRawArrayType -> validateTypeRef(type.elementType, nodeId, "$location.rawArrayElement", issues)
            is ChirVArrayType -> validateTypeRef(type.elementType, nodeId, "$location.varrayElement", issues)
            is ChirCPointerType -> validateTypeRef(type.pointeeType, nodeId, "$location.pointerPointee", issues)
            is ChirCStringType -> Unit
            is ChirGenericType -> type.upperBounds.forEachIndexed { index, nested ->
                validateTypeRef(nested, nodeId, "$location.genericBound#$index", issues)
            }

            is ChirRefType -> validateTypeRef(type.referencedType, nodeId, "$location.refTarget", issues)
            is ChirBoxType -> validateTypeRef(type.boxedType, nodeId, "$location.boxTarget", issues)
            is ChirThisType -> Unit
        }
    }

    private companion object {
        val integerLikeMemorySizeTypes = setOf(
            ChirPrimitiveType.INT8,
            ChirPrimitiveType.INT16,
            ChirPrimitiveType.INT32,
            ChirPrimitiveType.INT64,
            ChirPrimitiveType.INT_NATIVE,
            ChirPrimitiveType.UINT8,
            ChirPrimitiveType.UINT16,
            ChirPrimitiveType.UINT32,
            ChirPrimitiveType.UINT64,
            ChirPrimitiveType.UINT_NATIVE,
        )
    }
}
