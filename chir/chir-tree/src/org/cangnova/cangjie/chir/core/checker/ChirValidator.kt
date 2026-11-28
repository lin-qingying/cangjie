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
import org.cangnova.cangjie.chir.core.expression.ChirBinaryOperator
import org.cangnova.cangjie.chir.core.expression.ChirBinaryOperatorFamily
import org.cangnova.cangjie.chir.core.expression.ChirCallExpression
import org.cangnova.cangjie.chir.core.expression.ChirConstantExpression
import org.cangnova.cangjie.chir.core.expression.ChirExpression
import org.cangnova.cangjie.chir.core.expression.ChirFieldExpression
import org.cangnova.cangjie.chir.core.expression.ChirIntrinsicKind
import org.cangnova.cangjie.chir.core.expression.ChirMemoryExpression
import org.cangnova.cangjie.chir.core.expression.ChirMemoryOperation
import org.cangnova.cangjie.chir.core.expression.ChirOtherExpression
import org.cangnova.cangjie.chir.core.expression.ChirOtherOperation
import org.cangnova.cangjie.chir.core.expression.ChirTupleExpression
import org.cangnova.cangjie.chir.core.expression.ChirUnaryExpression
import org.cangnova.cangjie.chir.core.expression.ChirUnaryOperator
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
import org.cangnova.cangjie.chir.core.type.ChirTypeRelations
import org.cangnova.cangjie.chir.core.type.ChirUnresolvedTypeRef
import org.cangnova.cangjie.chir.core.type.ChirVArrayType
import org.cangnova.cangjie.chir.core.value.ChirConstantValue
import org.cangnova.cangjie.chir.core.value.ChirValue

/**
 * CHIR 图校验器接口。
 */
interface ChirValidator {
    /**
     * 校验完整 CHIR 包。
     */
    fun validatePackage(chirPackage: ChirPackage, context: ChirReadOnlyContext? = null): ChirValidationReport
}

/**
 * 默认 CHIR 图校验器实现。
 */
class DefaultChirValidator : ChirValidator {
    /**
     * 校验包内声明、函数控制流、表达式类型、包初始化函数和上下文符号引用。
     */
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

        allDeclarations
            .filterIsInstance<ChirFunctionDeclaration>()
            .forEach { function ->
                if (function.isImported) {
                    validateImportedFunction(function, issues)
                } else {
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

    /**
     * 校验函数声明的 CFG、表达式、终结指令和返回类型一致性。
     */
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
        val expressionByResultValueId = linkedMapOf<ChirSemanticId, ChirSemanticId>()

        function.blocks.forEach { block ->
            block.expressions.forEach { expression ->
                validateExpressionResult(expression, expressionByResultValueId, issues)
                when (expression) {
                    is ChirBinaryExpression -> {
                        val operator = ChirBinaryOperator.parse(expression.operator)
                        validateTypeRef(expression.left.type, expression.left.semanticId, "binary.left", issues)
                        validateTypeRef(expression.right.type, expression.right.semanticId, "binary.right", issues)
                        validateTypeRef(expression.resultType, expression.semanticId, "binary.result", issues)
                        if (operator == null) {
                            issues += ChirValidationIssue(
                                code = "UNSUPPORTED_BINARY_OPERATOR",
                                severity = ChirValidationSeverity.ERROR,
                                message = "unsupported binary operator '${expression.operator}'",
                                nodeId = expression.semanticId,
                            )
                        }
                        val hasCompatibleOperands = when (operator) {
                            ChirBinaryOperator.POWER -> {
                                val leftType = expression.left.type.typeOrNull()
                                val rightType = expression.right.type.typeOrNull()
                                when (leftType) {
                                    ChirPrimitiveType.INT64 -> rightType == ChirPrimitiveType.UINT64
                                    ChirPrimitiveType.FLOAT64 -> rightType == ChirPrimitiveType.INT64 || rightType == ChirPrimitiveType.FLOAT64
                                    else -> false
                                }
                            }
                            ChirBinaryOperator.SHIFT_LEFT,
                            ChirBinaryOperator.SIGNED_SHIFT_RIGHT,
                            ChirBinaryOperator.UNSIGNED_SHIFT_RIGHT,
                            -> expression.left.type.isIntegerLikeMemorySize() &&
                                expression.right.type.typeOrNull() == ChirPrimitiveType.INT64
                            else -> ChirTypeRelations.areEquivalent(expression.left.type, expression.right.type)
                        }
                        if (!hasCompatibleOperands) {
                            issues += ChirValidationIssue(
                                code = "BINARY_OPERAND_TYPE_MISMATCH",
                                severity = ChirValidationSeverity.ERROR,
                                message = when {
                                    operator == ChirBinaryOperator.POWER ->
                                        "power expression operands do not match the official power type signatures"
                                    operator?.family == ChirBinaryOperatorFamily.SHIFT ->
                                        "shift expression requires an integer value and an Int64 shift count"
                                    else -> "binary expression operands must have the same type"
                                },
                                nodeId = expression.semanticId,
                            )
                        }
                        if (operator == ChirBinaryOperator.POWER &&
                            !ChirTypeRelations.areEquivalent(expression.left.type, expression.resultType)
                        ) {
                            issues += ChirValidationIssue(
                                code = "BINARY_RESULT_TYPE_MISMATCH",
                                severity = ChirValidationSeverity.ERROR,
                                message = "power expression result type must match its base type",
                                nodeId = expression.semanticId,
                            )
                        }
                    }
                    is ChirUnaryExpression -> {
                        val operator = ChirUnaryOperator.parse(expression.operator)
                        validateTypeRef(expression.operand.type, expression.operand.semanticId, "unary.operand", issues)
                        validateTypeRef(expression.resultType, expression.semanticId, "unary.result", issues)
                        if (operator == null) {
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
                        expression.dispatchReceiver?.let { receiver ->
                            validateTypeRef(receiver.type, receiver.semanticId, "call.dispatchReceiver", issues)
                        }
                        if (expression.virtualMethodContext == null && expression.dispatchReceiver != null) {
                            issues += ChirValidationIssue(
                                code = "CALL_DISPATCH_RECEIVER_WITHOUT_VIRTUAL_CONTEXT",
                                severity = ChirValidationSeverity.ERROR,
                                message = "direct call carries a dynamic dispatch receiver",
                                nodeId = expression.semanticId,
                            )
                        }
                        expression.instantiatedTypeArguments.forEachIndexed { index, typeArgument ->
                            validateTypeRef(typeArgument, expression.semanticId, "call.instantiatedTypeArgument#$index", issues)
                        }
                        expression.thisType?.let { typeRef ->
                            validateTypeRef(typeRef, expression.semanticId, "call.thisType", issues)
                        }
                        expression.virtualMethodContext?.let { context ->
                            if (context.sourceCodeIdentifier.isBlank()) {
                                issues += ChirValidationIssue(
                                    code = "CALL_VIRTUAL_METHOD_IDENTIFIER_MISSING",
                                    severity = ChirValidationSeverity.ERROR,
                                    message = "virtual call has no source code identifier",
                                    nodeId = expression.semanticId,
                                )
                            }
                            validateTypeRef(
                                ChirResolvedTypeRef(context.originalFunctionType),
                                expression.semanticId,
                                "call.virtualMethodContext.originalFunctionType",
                                issues,
                            )
                            context.genericTypeParameters.forEachIndexed { index, typeParameter ->
                                validateTypeRef(
                                    typeParameter,
                                    expression.semanticId,
                                    "call.virtualMethodContext.genericTypeParameter#$index",
                                    issues,
                                )
                                if ((typeParameter as? ChirResolvedTypeRef)?.type !is ChirGenericType) {
                                    issues += ChirValidationIssue(
                                        code = "CALL_VIRTUAL_METHOD_TYPE_PARAMETER_INVALID",
                                        severity = ChirValidationSeverity.ERROR,
                                        message = "virtual call generic type parameter #$index is not a generic type",
                                        nodeId = expression.semanticId,
                                    )
                                }
                            }
                        }
                        val functionType = expression.virtualMethodContext?.originalFunctionType
                            ?: (expression.callee.type as? ChirResolvedTypeRef)?.type as? ChirFunctionType
                        if (functionType != null) {
                            val substitutions = linkedMapOf<String, ChirTypeRef>()
                            val expectedArgumentTypes = if (expression.virtualMethodContext == null) {
                                buildList {
                                    functionType.receiverType?.let(::add)
                                    addAll(functionType.parameterTypes)
                                }
                            } else {
                                val receiverType = functionType.parameterTypes.firstOrNull()
                                val dispatchReceiver = expression.dispatchReceiver
                                if (receiverType == null) {
                                    issues += ChirValidationIssue(
                                        code = "CALL_VIRTUAL_METHOD_RECEIVER_MISSING",
                                        severity = ChirValidationSeverity.ERROR,
                                        message = "virtual call function type has no receiver parameter",
                                        nodeId = expression.semanticId,
                                    )
                                } else if (dispatchReceiver == null) {
                                    issues += ChirValidationIssue(
                                        code = "CALL_VIRTUAL_METHOD_DISPATCH_RECEIVER_MISSING",
                                        severity = ChirValidationSeverity.ERROR,
                                        message = "virtual call has no separate dispatch receiver value",
                                        nodeId = expression.semanticId,
                                    )
                                } else if (!ChirTypeRelations.constrain(receiverType, dispatchReceiver.type, substitutions)) {
                                    issues += ChirValidationIssue(
                                        code = "CALL_VIRTUAL_METHOD_RECEIVER_TYPE_MISMATCH",
                                        severity = ChirValidationSeverity.ERROR,
                                        message = "virtual call receiver type ${dispatchReceiver.type.renderName} does not match ${receiverType.renderName}",
                                        nodeId = expression.semanticId,
                                    )
                                }
                                functionType.parameterTypes.drop(1)
                            }
                            val fixedArgumentCount = expectedArgumentTypes.size
                            val hasInvalidArgumentCount = expression.arguments.size < fixedArgumentCount ||
                                (!functionType.hasVarArg && expression.arguments.size != fixedArgumentCount)
                            if (hasInvalidArgumentCount) {
                                val expectedArgumentCount = if (functionType.hasVarArg) {
                                    "at least $fixedArgumentCount"
                                } else {
                                    fixedArgumentCount.toString()
                                }
                                issues += ChirValidationIssue(
                                    code = "CALL_ARGUMENT_COUNT_MISMATCH",
                                    severity = ChirValidationSeverity.ERROR,
                                    message = "call argument count mismatch: expected $expectedArgumentCount, actual ${expression.arguments.size}",
                                    nodeId = expression.semanticId,
                                )
                            } else {
                                expression.arguments.take(fixedArgumentCount).zip(expectedArgumentTypes)
                                    .forEachIndexed { index, (argument, expectedType) ->
                                    if (!ChirTypeRelations.constrain(expectedType, argument.type, substitutions)) {
                                        issues += ChirValidationIssue(
                                            code = "CALL_ARGUMENT_TYPE_MISMATCH",
                                            severity = ChirValidationSeverity.ERROR,
                                            message = "call argument #$index type mismatch: expected ${expectedType.renderName}, actual ${argument.type.renderName}",
                                            nodeId = expression.semanticId,
                                        )
                                    }
                                }
                            }
                            if (!ChirTypeRelations.constrain(functionType.returnType, expression.resultType, substitutions)) {
                                issues += ChirValidationIssue(
                                    code = "CALL_RESULT_TYPE_MISMATCH",
                                    severity = ChirValidationSeverity.ERROR,
                                    message = "call result type ${expression.resultType.renderName} does not match callee return type ${functionType.returnType.renderName}",
                                    nodeId = expression.semanticId,
                                )
                            }
                        }
                        expression.arguments.forEachIndexed { index, argument ->
                            validateTypeRef(argument.type, argument.semanticId, "call.argument#$index", issues)
                        }
                    }
                    is ChirTupleExpression -> validateTupleExpression(expression, issues)
                    is ChirFieldExpression -> validateFieldExpression(expression, issues)
                    is ChirConstantExpression -> {
                        validateTypeRef(expression.value.type, expression.value.semanticId, "constant.value", issues)
                        validateTypeRef(expression.resultType, expression.semanticId, "constant.result", issues)
                        if (!ChirTypeRelations.isOfficialChirTypeCompatible(
                                expression.resultType,
                                expression.value.type,
                            )
                        ) {
                            issues += ChirValidationIssue(
                                code = "CONSTANT_RESULT_TYPE_MISMATCH",
                                severity = ChirValidationSeverity.ERROR,
                                message = "constant result type ${expression.resultType.renderName} differs from literal type ${expression.value.type.renderName}",
                                nodeId = expression.semanticId,
                            )
                        }
                    }
                    is ChirMemoryExpression -> {
                        val operation = ChirMemoryOperation.parse(expression.operation)
                        validateTypeRef(expression.address.type, expression.address.semanticId, "memory.address", issues)
                        expression.value?.let { validateTypeRef(it.type, it.semanticId, "memory.value", issues) }
                        expression.resultType?.let { validateTypeRef(it, expression.semanticId, "memory.result", issues) }
                        if (operation == null) {
                            issues += ChirValidationIssue(
                                code = "UNSUPPORTED_MEMORY_OPERATION",
                                severity = ChirValidationSeverity.ERROR,
                                message = "unsupported memory operation '${expression.operation}'",
                                nodeId = expression.semanticId,
                            )
                        }
                        val nameBasedOperation = operation == ChirMemoryOperation.GET_ELEMENT_BY_NAME ||
                            operation == ChirMemoryOperation.STORE_ELEMENT_BY_NAME
                        if (nameBasedOperation && expression.memberNames.isEmpty()) {
                            issues += ChirValidationIssue(
                                code = "MEMORY_MEMBER_PATH_MISSING",
                                severity = ChirValidationSeverity.ERROR,
                                message = "member-name memory operation must declare a non-empty member path",
                                nodeId = expression.semanticId,
                            )
                        }
                        if (!nameBasedOperation && expression.memberNames.isNotEmpty()) {
                            issues += ChirValidationIssue(
                                code = "MEMORY_MEMBER_PATH_UNEXPECTED",
                                severity = ChirValidationSeverity.ERROR,
                                message = "memory operation ${expression.operation} cannot carry a member-name path",
                                nodeId = expression.semanticId,
                            )
                        }
                        when (operation) {
                            ChirMemoryOperation.ALLOCA -> {
                                if (expression.value != null) {
                                    issues += ChirValidationIssue(
                                        code = "MEMORY_ALLOCA_VALUE_PRESENT",
                                        severity = ChirValidationSeverity.ERROR,
                                        message = "alloca expression must not carry a stored value",
                                        nodeId = expression.semanticId,
                                    )
                                }
                                validateAlloca(expression, issues)
                            }
                            ChirMemoryOperation.LOAD -> {
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
                            ChirMemoryOperation.STORE -> {
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
                                if (targetType != null && expression.value != null &&
                                    !ChirTypeRelations.isAssignable(expression.value.type, targetType)
                                ) {
                                    issues += ChirValidationIssue(
                                        code = "MEMORY_STORE_VALUE_TYPE_MISMATCH",
                                        severity = ChirValidationSeverity.ERROR,
                                        message = "store value type ${expression.value.type.renderName} does not match address target ${targetType.renderName}",
                                        nodeId = expression.semanticId,
                                    )
                                }
                            }
                            ChirMemoryOperation.GET_ELEMENT_PTR,
                            ChirMemoryOperation.GET_ELEMENT_PTR_INBOUNDS,
                            null -> Unit
                            ChirMemoryOperation.GET_ELEMENT_BY_NAME -> {
                                if (expression.value != null) {
                                    issues += ChirValidationIssue(
                                        code = "MEMORY_GET_ELEMENT_BY_NAME_VALUE_PRESENT",
                                        severity = ChirValidationSeverity.ERROR,
                                        message = "get-element-by-name expression must not carry a stored value",
                                        nodeId = expression.semanticId,
                                    )
                                }
                                if (expression.resultType == null) {
                                    issues += ChirValidationIssue(
                                        code = "MEMORY_GET_ELEMENT_BY_NAME_RESULT_MISSING",
                                        severity = ChirValidationSeverity.ERROR,
                                        message = "get-element-by-name expression must declare its result type",
                                        nodeId = expression.semanticId,
                                    )
                                }
                                if (expression.address.type.typeOrNull() !is ChirRefType) {
                                    issues += ChirValidationIssue(
                                        code = "MEMORY_GET_ELEMENT_BY_NAME_ADDRESS_NOT_REFERENCE",
                                        severity = ChirValidationSeverity.ERROR,
                                        message = "get-element-by-name address must be a reference value",
                                        nodeId = expression.semanticId,
                                    )
                                }
                            }
                            ChirMemoryOperation.STORE_ELEMENT_BY_NAME -> {
                                if (expression.value == null) {
                                    issues += ChirValidationIssue(
                                        code = "MEMORY_STORE_ELEMENT_BY_NAME_VALUE_MISSING",
                                        severity = ChirValidationSeverity.ERROR,
                                        message = "store-element-by-name expression must carry a stored value",
                                        nodeId = expression.semanticId,
                                    )
                                }
                                if (expression.resultType?.typeOrNull() != ChirPrimitiveType.UNIT) {
                                    issues += ChirValidationIssue(
                                        code = "MEMORY_STORE_ELEMENT_BY_NAME_RESULT_NOT_UNIT",
                                        severity = ChirValidationSeverity.ERROR,
                                        message = "store-element-by-name expression must have Unit result type",
                                        nodeId = expression.semanticId,
                                    )
                                }
                                if (expression.address.type.typeOrNull() !is ChirRefType) {
                                    issues += ChirValidationIssue(
                                        code = "MEMORY_STORE_ELEMENT_BY_NAME_ADDRESS_NOT_REFERENCE",
                                        severity = ChirValidationSeverity.ERROR,
                                        message = "store-element-by-name address must be a reference value",
                                        nodeId = expression.semanticId,
                                    )
                                }
                            }
                        }
                    }
                    is ChirOtherExpression -> {
                        val operation = ChirOtherOperation.parse(expression.operation)
                        expression.operands.forEachIndexed { index, operand ->
                            validateTypeRef(operand.type, operand.semanticId, "other.operand#$index", issues)
                        }
                        expression.resultType?.let { validateTypeRef(it, expression.semanticId, "other.result", issues) }
                        expression.debugMetadata?.accurateEnvironmentObjectType?.let {
                            validateTypeRef(it, expression.semanticId, "debug.accurate-environment-object", issues)
                        }
                        if (operation == null && expression.intrinsicKind == null && expression.debugMetadata == null) {
                            issues += ChirValidationIssue(
                                code = "UNSUPPORTED_OTHER_OPERATION",
                                severity = ChirValidationSeverity.ERROR,
                                message = "unsupported other expression operation '${expression.operation}'",
                                nodeId = expression.semanticId,
                            )
                        }
                        expression.intrinsicKind?.let { intrinsicKind ->
                            if (expression.operation != intrinsicKind.canonicalName) {
                                issues += ChirValidationIssue(
                                    code = "INTRINSIC_OPERATION_KIND_MISMATCH",
                                    severity = ChirValidationSeverity.ERROR,
                                    message = "intrinsic operation name does not match its typed kind",
                                    nodeId = expression.semanticId,
                                )
                            }
                            expression.intrinsicTypeArguments.forEachIndexed { index, type ->
                                validateTypeRef(type, expression.semanticId, "intrinsic.typeArgument#$index", issues)
                            }
                            if (expression.resultType == null) {
                                issues += ChirValidationIssue(
                                    code = "INTRINSIC_RESULT_TYPE_MISSING",
                                    severity = ChirValidationSeverity.ERROR,
                                    message = "intrinsic expression must declare its result type",
                                    nodeId = expression.semanticId,
                                )
                            }
                        }
                        if (expression.intrinsicKind == null && expression.intrinsicTypeArguments.isNotEmpty()) {
                            issues += ChirValidationIssue(
                                code = "INTRINSIC_TYPE_ARGUMENTS_WITHOUT_KIND",
                                severity = ChirValidationSeverity.ERROR,
                                message = "intrinsic type arguments require a typed intrinsic kind",
                                nodeId = expression.semanticId,
                            )
                        }
                        if (expression.intrinsicKind == ChirIntrinsicKind.ARRAY_GET_UNCHECKED &&
                            expression.operands.size != 2
                        ) {
                            issues += ChirValidationIssue(
                                code = "ARRAY_GET_UNCHECKED_OPERAND_COUNT_MISMATCH",
                                severity = ChirValidationSeverity.ERROR,
                                message = "unchecked array access requires an array and an index operand",
                                nodeId = expression.semanticId,
                            )
                        }
                        if (operation?.typeCastOverflowStrategy != null) {
                            if (expression.operands.size != 1) {
                                issues += ChirValidationIssue(
                                    code = "TYPECAST_OPERAND_COUNT_MISMATCH",
                                    severity = ChirValidationSeverity.ERROR,
                                    message = "Cangjie typecast must have one source operand",
                                    nodeId = expression.semanticId,
                                )
                            }
                            if (expression.resultType == null) {
                                issues += ChirValidationIssue(
                                    code = "TYPECAST_RESULT_TYPE_MISSING",
                                    severity = ChirValidationSeverity.ERROR,
                                    message = "Cangjie typecast must declare its target result type",
                                    nodeId = expression.semanticId,
                                )
                            }
                        }
                        if (operation == ChirOtherOperation.RAW_ARRAY_ALLOCATE) {
                            if (expression.operands.size != 1 ||
                                expression.operands.singleOrNull()?.type?.typeOrNull() != ChirPrimitiveType.INT64
                            ) {
                                issues += ChirValidationIssue(
                                    code = "RAW_ARRAY_ALLOCATE_SIZE_TYPE_MISMATCH",
                                    severity = ChirValidationSeverity.ERROR,
                                    message = "raw array allocation requires one Int64 size operand",
                                    nodeId = expression.semanticId,
                                )
                            }
                            val resultType = expression.resultType?.typeOrNull()
                            val allocatedType = when (resultType) {
                                is ChirRawArrayType -> resultType
                                is ChirRefType -> resultType.referencedType.typeOrNull() as? ChirRawArrayType
                                else -> null
                            }
                            if (allocatedType == null) {
                                issues += ChirValidationIssue(
                                    code = "RAW_ARRAY_ALLOCATE_RESULT_TYPE_MISMATCH",
                                    severity = ChirValidationSeverity.ERROR,
                                    message = "raw array allocation result must be a raw array or reference to a raw array",
                                    nodeId = expression.semanticId,
                                )
                            }
                        }
                        if (operation == ChirOtherOperation.RAW_ARRAY_LITERAL_INIT) {
                            val arrayType = expression.operands.firstOrNull()?.type?.typeOrNull()?.let { type ->
                                when (type) {
                                    is ChirRawArrayType -> type
                                    is ChirRefType -> type.referencedType.typeOrNull() as? ChirRawArrayType
                                    else -> null
                                }
                            }
                            if (arrayType == null) {
                                issues += ChirValidationIssue(
                                    code = "RAW_ARRAY_LITERAL_INIT_TARGET_TYPE_MISMATCH",
                                    severity = ChirValidationSeverity.ERROR,
                                    message = "raw array literal initialization requires a raw array destination",
                                    nodeId = expression.semanticId,
                                )
                            } else {
                                expression.operands.drop(1).forEachIndexed { index, element ->
                                    if (!ChirTypeRelations.areEquivalent(arrayType.elementType, element.type)) {
                                        issues += ChirValidationIssue(
                                            code = "RAW_ARRAY_LITERAL_INIT_ELEMENT_TYPE_MISMATCH",
                                            severity = ChirValidationSeverity.ERROR,
                                            message = "raw array literal element $index has type ${element.type.renderName}, expected ${arrayType.elementType.renderName}",
                                            nodeId = expression.semanticId,
                                        )
                                    }
                                }
                            }
                            if (expression.resultType?.typeOrNull() !in setOf(null, ChirPrimitiveType.UNIT, ChirPrimitiveType.VOID)) {
                                issues += ChirValidationIssue(
                                    code = "RAW_ARRAY_LITERAL_INIT_HAS_RESULT_TYPE",
                                    severity = ChirValidationSeverity.ERROR,
                                    message = "raw array literal initialization must have no result or a Unit/Void result",
                                    nodeId = expression.semanticId,
                                )
                            }
                        }
                        if (operation == ChirOtherOperation.RAW_ARRAY_INIT_BY_VALUE) {
                            val arrayType = expression.operands.firstOrNull()?.type?.typeOrNull()?.let { type ->
                                when (type) {
                                    is ChirRawArrayType -> type
                                    is ChirRefType -> type.referencedType.typeOrNull() as? ChirRawArrayType
                                    else -> null
                                }
                            }
                            if (expression.operands.size != 3 || arrayType == null) {
                                issues += ChirValidationIssue(
                                    code = "RAW_ARRAY_INIT_BY_VALUE_OPERANDS_MISMATCH",
                                    severity = ChirValidationSeverity.ERROR,
                                    message = "raw array initialization by value requires a destination, element count, and value",
                                    nodeId = expression.semanticId,
                                )
                            } else {
                                if (!expression.operands[1].type.isInt64RawArraySize()) {
                                    issues += ChirValidationIssue(
                                        code = "RAW_ARRAY_INIT_BY_VALUE_SIZE_TYPE_MISMATCH",
                                        severity = ChirValidationSeverity.ERROR,
                                        message = "raw array initialization element count must be Int64",
                                        nodeId = expression.semanticId,
                                    )
                                }
                                if (!ChirTypeRelations.isAssignable(expression.operands[2].type, arrayType.elementType)) {
                                    issues += ChirValidationIssue(
                                        code = "RAW_ARRAY_INIT_BY_VALUE_ELEMENT_TYPE_MISMATCH",
                                        severity = ChirValidationSeverity.ERROR,
                                        message = "raw array initialization value type ${expression.operands[2].type.renderName} is incompatible with ${arrayType.elementType.renderName}",
                                        nodeId = expression.semanticId,
                                    )
                                }
                            }
                            if (expression.resultType?.typeOrNull() !in setOf(null, ChirPrimitiveType.UNIT, ChirPrimitiveType.VOID)) {
                                issues += ChirValidationIssue(
                                    code = "RAW_ARRAY_INIT_BY_VALUE_HAS_RESULT_TYPE",
                                    severity = ChirValidationSeverity.ERROR,
                                    message = "raw array initialization by value must have no result or a Unit/Void result",
                                    nodeId = expression.semanticId,
                                )
                            }
                        }
                    }
                }
            }

            when (val terminator = block.terminator) {
                null -> Unit
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
                    if (returnValue != null && !ChirTypeRelations.areEquivalent(returnValue.type, returnType)) {
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
                    if (!ChirTypeRelations.areEquivalent(terminator.condition.type, ChirResolvedTypeRef(ChirPrimitiveType.BOOL))) {
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

    private fun validateImportedFunction(function: ChirFunctionDeclaration, issues: MutableList<ChirValidationIssue>) {
        validateMinimalControlFlow(function).forEach { message ->
            issues += ChirValidationIssue(
                code = "INVALID_IMPORTED_FUNCTION_BODY",
                severity = ChirValidationSeverity.ERROR,
                message = message,
                nodeId = function.semanticId,
            )
        }
    }

    /**
     * 校验表达式结果的 VALUE 身份、结果类型及反向定义关系。
     */
    private fun validateExpressionResult(
        expression: ChirExpression,
        expressionByResultValueId: MutableMap<ChirSemanticId, ChirSemanticId>,
        issues: MutableList<ChirValidationIssue>,
    ) {
        val resultLocal = expression.resultLocalValue
        val resultType = expression.resultType
        if (resultLocal == null && resultType == null) return
        if (resultLocal == null) {
            issues += ChirValidationIssue(
                code = "EXPRESSION_RESULT_LOCAL_MISSING",
                severity = ChirValidationSeverity.ERROR,
                message = "expression with a result type must define its result local value",
                nodeId = expression.semanticId,
            )
            return
        }
        if (resultType == null) {
            issues += ChirValidationIssue(
                code = "EXPRESSION_RESULT_TYPE_MISSING",
                severity = ChirValidationSeverity.ERROR,
                message = "expression result local value has no result type",
                nodeId = expression.semanticId,
            )
        } else if (!ChirTypeRelations.areEquivalent(resultLocal.type, resultType)) {
            issues += ChirValidationIssue(
                code = "EXPRESSION_RESULT_LOCAL_TYPE_MISMATCH",
                severity = ChirValidationSeverity.ERROR,
                message = "expression result local type ${resultLocal.type.renderName} does not match result type ${resultType.renderName}",
                nodeId = expression.semanticId,
            )
        }
        if (resultLocal.associatedExpressionId != expression.semanticId) {
            issues += ChirValidationIssue(
                code = "EXPRESSION_RESULT_LOCAL_BACKLINK_MISMATCH",
                severity = ChirValidationSeverity.ERROR,
                message = "expression result local does not point back to its defining expression",
                nodeId = expression.semanticId,
            )
        }
        val previousExpressionId = expressionByResultValueId.putIfAbsent(resultLocal.semanticId, expression.semanticId)
        if (previousExpressionId != null) {
            issues += ChirValidationIssue(
                code = "DUPLICATE_EXPRESSION_RESULT_LOCAL_ID",
                severity = ChirValidationSeverity.ERROR,
                message = "result local ${resultLocal.semanticId.value} is defined by multiple expressions",
                nodeId = expression.semanticId,
            )
        }
    }

    /**
     * 校验内存操作地址是否是 ref 或 C pointer，并返回被访问目标类型。
     */
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

    /**
     * 校验局部引用分配与 C 指针动态分配的显式结果类型及元素数量。
     */
    private fun validateAlloca(
        expression: ChirMemoryExpression,
        issues: MutableList<ChirValidationIssue>,
    ) {
        val resultType = expression.resultType?.typeOrNull()
        if (resultType !is ChirRefType && resultType !is ChirCPointerType) {
            issues += ChirValidationIssue(
                code = if (expression.resultType == null) {
                    "MEMORY_ALLOCA_RESULT_MISSING"
                } else {
                    "MEMORY_ALLOCA_RESULT_TYPE_MISMATCH"
                },
                severity = ChirValidationSeverity.ERROR,
                message = "alloca result must be an explicit ref or cpointer type, actual ${expression.resultType?.renderName}",
                nodeId = expression.semanticId,
            )
            return
        }
        if (!expression.address.type.isIntegerLikeMemorySize()) {
            issues += ChirValidationIssue(
                code = "MEMORY_ALLOCA_SIZE_TYPE_MISMATCH",
                severity = ChirValidationSeverity.ERROR,
                message = "pointer alloca size operand must be integer-like, actual ${expression.address.type.renderName}",
                nodeId = expression.semanticId,
            )
        }
        if (resultType is ChirRefType) {
            val size = (expression.address as? ChirConstantValue)?.literal?.toLongOrNull()
            if (size != 1L) {
                issues += ChirValidationIssue(
                    code = "MEMORY_ALLOCA_REFERENCE_COUNT_INVALID",
                    severity = ChirValidationSeverity.ERROR,
                    message = "reference alloca must allocate exactly one local object",
                    nodeId = expression.semanticId,
                )
            }
        }
    }

    /**
     * 校验 CHIR 聚合构造表达式与其结果布局一致。
     */
    private fun validateTupleExpression(
        expression: ChirTupleExpression,
        issues: MutableList<ChirValidationIssue>,
    ) {
        expression.elements.forEachIndexed { index, element ->
            validateTypeRef(element.type, element.semanticId, "tuple.element#$index", issues)
        }
        validateTypeRef(expression.resultType, expression.semanticId, "tuple.result", issues)

        if (expression.elements.isEmpty()) {
            issues += ChirValidationIssue(
                code = "TUPLE_OPERAND_MISSING",
                severity = ChirValidationSeverity.ERROR,
                message = "official CHIR tuple construction requires at least one operand",
                nodeId = expression.semanticId,
            )
            return
        }

        val resultType = expression.resultType.typeOrNull()
        if (resultType is ChirEnumType) {
            val selector = expression.elements.firstOrNull()
            if (selector !is ChirConstantValue) {
                issues += ChirValidationIssue(
                    code = "TUPLE_ENUM_SELECTOR_INVALID",
                    severity = ChirValidationSeverity.ERROR,
                    message = "enum aggregate construction requires a constant selector value",
                    nodeId = expression.semanticId,
                )
                return
            }

            val caseIndex = when (resultType.selectorType) {
                ChirPrimitiveType.BOOL -> if (selector.type.typeOrNull() == ChirPrimitiveType.BOOL) when (selector.literal) {
                    "0", "false" -> 0
                    "1", "true" -> 1
                    else -> null
                } else null
                ChirPrimitiveType.UINT32 -> if (selector.type.typeOrNull() == ChirPrimitiveType.UINT32) selector.literal.toULongOrNull()
                    ?.takeIf { it <= Int.MAX_VALUE.toULong() }
                    ?.toInt() else null
                else -> null
            }
            if (caseIndex == null) {
                issues += ChirValidationIssue(
                    code = "TUPLE_ENUM_SELECTOR_INVALID",
                    severity = ChirValidationSeverity.ERROR,
                    message = "enum aggregate selector must be a ${resultType.selectorType.renderName} constant with a valid literal",
                    nodeId = expression.semanticId,
                )
                return
            }
            val payloadTypes = ChirTypeRelations.instantiatedEnumPayloadTypes(resultType, caseIndex)
            if (payloadTypes == null) {
                issues += ChirValidationIssue(
                    code = "TUPLE_ENUM_SELECTOR_OUT_OF_RANGE",
                    severity = ChirValidationSeverity.ERROR,
                    message = "enum aggregate selector $caseIndex does not identify a constructor in ${resultType.renderName}",
                    nodeId = expression.semanticId,
                )
                return
            }
            val actualPayloads = expression.elements.drop(1)
            if (actualPayloads.size != payloadTypes.size) {
                issues += ChirValidationIssue(
                    code = "TUPLE_ENUM_PAYLOAD_ARITY_MISMATCH",
                    severity = ChirValidationSeverity.ERROR,
                    message = "enum constructor $caseIndex expects ${payloadTypes.size} payload values but got ${actualPayloads.size}",
                    nodeId = expression.semanticId,
                )
                return
            }
            actualPayloads.zip(payloadTypes).forEachIndexed { index, (actual, expected) ->
                if (!ChirTypeRelations.areEquivalent(expected, actual.type)) {
                    issues += ChirValidationIssue(
                        code = "TUPLE_ENUM_PAYLOAD_TYPE_MISMATCH",
                        severity = ChirValidationSeverity.ERROR,
                        message = "enum constructor $caseIndex payload #$index has type ${actual.type.renderName}, expected ${expected.renderName}",
                        nodeId = expression.semanticId,
                    )
                }
            }
            return
        }

        val fieldTypes = when (resultType) {
            is ChirTupleType -> resultType.elementTypes
            is ChirStructType -> ChirTypeRelations.instantiatedStructFieldTypes(resultType)
            else -> {
                issues += ChirValidationIssue(
                    code = "TUPLE_RESULT_TYPE_UNSUPPORTED",
                    severity = ChirValidationSeverity.ERROR,
                    message = "official CHIR tuple construction result must be a tuple, struct, or enum type",
                    nodeId = expression.semanticId,
                )
                return
            }
        }

        if (expression.elements.size != fieldTypes.size) {
            issues += ChirValidationIssue(
                code = "TUPLE_ARITY_MISMATCH",
                severity = ChirValidationSeverity.ERROR,
                message = "aggregate element count ${expression.elements.size} does not match result field count ${fieldTypes.size}",
                nodeId = expression.semanticId,
            )
            return
        }

        expression.elements.zip(fieldTypes).forEachIndexed { index, (element, fieldType) ->
            if (!ChirTypeRelations.areEquivalent(fieldType, element.type)) {
                issues += ChirValidationIssue(
                    code = "TUPLE_ELEMENT_TYPE_MISMATCH",
                    severity = ChirValidationSeverity.ERROR,
                    message = "aggregate element #$index type ${element.type.renderName} does not match field type ${fieldType.renderName}",
                    nodeId = expression.semanticId,
                )
            }
        }
    }

    /**
     * 校验值字段投影路径与操作数聚合布局、结果类型一致。
     */
    private fun validateFieldExpression(
        expression: ChirFieldExpression,
        issues: MutableList<ChirValidationIssue>,
    ) {
        validateTypeRef(expression.base.type, expression.base.semanticId, "field.base", issues)
        validateTypeRef(expression.resultType, expression.semanticId, "field.result", issues)
        val projection = ChirTypeRelations.projectFieldType(expression.base.type, expression.path)
        if (projection == null) {
            issues += ChirValidationIssue(
                code = "FIELD_PATH_INVALID",
                severity = ChirValidationSeverity.ERROR,
                message = "field path ${expression.path} is not valid for ${expression.base.type.renderName}",
                nodeId = expression.semanticId,
            )
            return
        }
        val resultTypeMatches = ChirTypeRelations.areEquivalent(projection.resultType, expression.resultType)
        if (!resultTypeMatches) {
            issues += ChirValidationIssue(
                code = "FIELD_RESULT_TYPE_MISMATCH",
                severity = ChirValidationSeverity.ERROR,
                message = "field path ${expression.path} has type ${projection.resultType.renderName}, " +
                    "not ${expression.resultType.renderName}",
                nodeId = expression.semanticId,
            )
        }
    }

    /**
     * 判断类型引用是否可作为内存 size 操作数。
     */
    private fun ChirTypeRef.isIntegerLikeMemorySize(): Boolean {
        val primitive = (this as? ChirResolvedTypeRef)?.type as? ChirPrimitiveType ?: return false
        return primitive in integerLikeMemorySizeTypes
    }

    private fun ChirTypeRef.isInt64RawArraySize(): Boolean = typeOrNull() == ChirPrimitiveType.INT64

    /**
     * 读取已解析类型；未解析类型不会参与结构化语义检查。
     */
    private fun ChirTypeRef.typeOrNull(): ChirType? = (this as? ChirResolvedTypeRef)?.type

    /**
     * 校验包初始化函数引用是否指向包内函数。
     */
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

    /**
     * 校验声明中出现的类型引用。
     */
    private fun validateDeclarationTypes(declaration: ChirDeclaration, issues: MutableList<ChirValidationIssue>) {
        if (declaration is ChirTypeDeclaration) {
            if (declaration.typeParameterTypes.isNotEmpty() &&
                declaration.typeParameterTypes.size != declaration.typeParameters.size
            ) {
                issues += ChirValidationIssue(
                    code = "INVALID_TYPE_PARAMETER_SHAPE",
                    severity = ChirValidationSeverity.ERROR,
                    message = "type declaration ${declaration.name} has mismatched generic parameter names and types",
                    nodeId = declaration.semanticId,
                )
            }
            declaration.typeParameterTypes.forEachIndexed { index, typeRef ->
                validateTypeRef(typeRef, declaration.semanticId, "type:${declaration.name}.parameter#$index", issues)
            }
        }
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

            is ChirEnumDeclaration -> declaration.caseTypes.forEachIndexed { caseIndex, case ->
                case.payloadTypes.forEachIndexed { payloadIndex, typeRef ->
                    validateTypeRef(
                        typeRef,
                        declaration.semanticId,
                        "enum:${declaration.name}.case:${case.name}.payload#$payloadIndex",
                        issues,
                    )
                }
                if (case.name != declaration.cases.getOrNull(caseIndex)) {
                    issues += ChirValidationIssue(
                        code = "INVALID_ENUM_CASE_SHAPE",
                        severity = ChirValidationSeverity.ERROR,
                        message = "enum declaration ${declaration.name} has misaligned case metadata",
                        nodeId = declaration.semanticId,
                    )
                }
            }
            is ChirTypeDeclaration -> Unit

            is ChirExtendDeclaration -> {
                validateTypeRef(declaration.targetType, declaration.semanticId, "extend:${declaration.name}.target", issues)
                declaration.extendedTypes.forEachIndexed { index, typeRef ->
                    validateTypeRef(typeRef, declaration.semanticId, "extend:${declaration.name}.type#$index", issues)
                }
            }
        }
    }

    /**
     * 校验单个类型引用是否已解析且内部类型结构有效。
     */
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

    /**
     * 递归校验具体类型结构中的嵌套类型引用。
     */
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

    /**
     * 默认校验器使用的静态常量。
     */
    private companion object {
        /**
         * 可作为内存分配大小的整数类原始类型集合。
         */
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
