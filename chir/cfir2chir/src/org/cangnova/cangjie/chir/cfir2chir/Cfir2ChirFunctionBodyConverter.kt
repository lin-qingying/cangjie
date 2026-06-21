package org.cangnova.cangjie.chir.cfir2chir

import org.cangnova.cangjie.chir.core.attribute.ChirAttribute
import org.cangnova.cangjie.chir.core.attribute.ChirStringAttribute
import org.cangnova.cangjie.chir.core.controlflow.ChirBlock
import org.cangnova.cangjie.chir.core.controlflow.ChirBranchTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirConditionalBranchTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirReturnTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirThrowTerminator
import org.cangnova.cangjie.chir.core.declaration.DefaultChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.declaration.DefaultChirVariableDeclaration
import org.cangnova.cangjie.chir.core.expression.ChirBinaryExpression
import org.cangnova.cangjie.chir.core.expression.ChirCallExpression
import org.cangnova.cangjie.chir.core.expression.ChirExpression
import org.cangnova.cangjie.chir.core.expression.ChirMemoryExpression
import org.cangnova.cangjie.chir.core.expression.ChirOtherExpression
import org.cangnova.cangjie.chir.core.expression.ChirOtherOperation
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.type.ChirFunctionType
import org.cangnova.cangjie.chir.core.type.ChirPrimitiveType
import org.cangnova.cangjie.chir.core.type.ChirRefType
import org.cangnova.cangjie.chir.core.type.ChirResolvedTypeRef
import org.cangnova.cangjie.chir.core.type.ChirTypeRef
import org.cangnova.cangjie.chir.core.value.ChirBlockValue
import org.cangnova.cangjie.chir.core.value.ChirConstantValue
import org.cangnova.cangjie.chir.core.value.ChirFunctionValue
import org.cangnova.cangjie.chir.core.value.ChirGlobalValue
import org.cangnova.cangjie.chir.core.value.ChirImportedFunctionValue
import org.cangnova.cangjie.chir.core.value.ChirImportedVariableValue
import org.cangnova.cangjie.chir.core.value.ChirLocalValue
import org.cangnova.cangjie.chir.core.value.ChirParameterValue
import org.cangnova.cangjie.chir.core.value.ChirValue
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirPatternBindingVariable
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall
import org.cangnova.cangjie.cfir.expressions.CfirAnonymousFunctionExpression
import org.cangnova.cangjie.cfir.expressions.CfirArrayLiteral
import org.cangnova.cangjie.cfir.expressions.CfirAssignment
import org.cangnova.cangjie.cfir.expressions.CfirBinaryOp
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirBreakExpression
import org.cangnova.cangjie.cfir.expressions.CfirCatch
import org.cangnova.cangjie.cfir.expressions.CfirComparisonExpression
import org.cangnova.cangjie.cfir.expressions.CfirContinueExpression
import org.cangnova.cangjie.cfir.expressions.CfirErrorExpression
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirForInExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirHandleClause
import org.cangnova.cangjie.cfir.expressions.CfirIfExpression
import org.cangnova.cangjie.cfir.expressions.CfirIncrementDecrementExpression
import org.cangnova.cangjie.cfir.expressions.CfirInaccessibleReceiverExpression
import org.cangnova.cangjie.cfir.expressions.CfirInoutArgumentExpression
import org.cangnova.cangjie.cfir.expressions.CfirLazyExpression
import org.cangnova.cangjie.cfir.expressions.CfirLetPatternExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.expressions.CfirLoopExpression
import org.cangnova.cangjie.cfir.expressions.CfirMatchBranch
import org.cangnova.cangjie.cfir.expressions.CfirMatchExhaustivenessStatus
import org.cangnova.cangjie.cfir.expressions.CfirMatchExpression
import org.cangnova.cangjie.cfir.expressions.CfirNamedAccessExpression
import org.cangnova.cangjie.cfir.expressions.CfirOptionalChainExpression
import org.cangnova.cangjie.cfir.expressions.CfirOptionalExpression
import org.cangnova.cangjie.cfir.expressions.CfirPerformExpression
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.expressions.CfirQuoteExpression
import org.cangnova.cangjie.cfir.expressions.CfirRangeExpression
import org.cangnova.cangjie.cfir.expressions.CfirResumeExpression
import org.cangnova.cangjie.cfir.expressions.CfirReturnExpression
import org.cangnova.cangjie.cfir.expressions.CfirSmartCastExpression
import org.cangnova.cangjie.cfir.expressions.CfirSpawnExpression
import org.cangnova.cangjie.cfir.expressions.CfirStatement
import org.cangnova.cangjie.cfir.expressions.CfirStringInterpolation
import org.cangnova.cangjie.cfir.expressions.CfirSubscriptExpression
import org.cangnova.cangjie.cfir.expressions.CfirSuperReceiverExpression
import org.cangnova.cangjie.cfir.expressions.CfirSynchronizedExpression
import org.cangnova.cangjie.cfir.expressions.CfirThrowExpression
import org.cangnova.cangjie.cfir.expressions.CfirThisReceiverExpression
import org.cangnova.cangjie.cfir.expressions.CfirTryExpression
import org.cangnova.cangjie.cfir.expressions.CfirTupleLiteral
import org.cangnova.cangjie.cfir.expressions.CfirTypeConversion
import org.cangnova.cangjie.cfir.expressions.CfirTypeOperator
import org.cangnova.cangjie.cfir.expressions.CfirUnsafeExpression
import org.cangnova.cangjie.cfir.expressions.CfirWrappedExpression
import org.cangnova.cangjie.cfir.patterns.CfirPattern
import org.cangnova.cangjie.cfir.patterns.bindingVariables
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.references.CfirReference
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.references.CfirThisReference
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirValueParameterSymbol
import org.cangnova.cangjie.cfir.symbols.CfirVariableSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import java.util.IdentityHashMap

internal class Cfir2ChirFunctionBodyConverter(
    private val components: Cfir2ChirComponents,
    private val packageName: String,
    private val header: ChirFunctionHeader,
    private val body: CfirBlock?,
    private val bodySource: CfirElement,
) {
    constructor(
        components: Cfir2ChirComponents,
        packageName: String,
        function: CfirFunction,
        header: ChirFunctionHeader,
    ) : this(
        components = components,
        packageName = packageName,
        header = header,
        body = function.body,
        bodySource = function,
    )

    private val storage: Cfir2ChirDeclarationStorage = components.declarationStorage
    private val typeMapper: Cfir2ChirTypeMapper = components.typeMapper
    private val builder = FunctionBodyBuilder(header.semanticId)
    private val loopTargets = IdentityHashMap<CfirLoopExpression, LoopBlocks>()

    fun convert(): DefaultChirFunctionDeclaration {
        val entry = builder.enterNewBlock("entry")
        val result = if (body == null) {
            lowerAbsentBody()
        } else {
            lowerBlock(body)
        }

        if (builder.hasOpenCurrentBlock) {
            val returnValue = if (header.returnType.isUnit()) {
                null
            } else {
                result ?: throw Cfir2ChirConversionException(
                    "function ${header.name} body does not produce return value for ${header.returnType.renderName}",
                    body ?: bodySource,
                )
            }
            builder.terminate(ChirReturnTerminator(builder.nextId("return"), returnValue))
        }

        return DefaultChirFunctionDeclaration(
            semanticId = header.semanticId,
            name = header.name,
            returnType = header.returnType,
            parameters = header.parameters,
            blocks = builder.freeze(),
            entryBlockId = entry.semanticId,
        )
    }

    private fun lowerAbsentBody(): ChirValue? =
        emitOtherValue(
            operation = Cfir2ChirOperation.CFIR_ABSENT_BODY,
            source = bodySource,
            operands = emptyList(),
            resultType = header.returnType.takeUnless { it.isUnit() },
            attributes = attributes(
                "cfir.package" to packageName,
                "cfir.declaration" to header.name,
                "cfir.reason" to "no-body",
            ),
        )

    private fun lowerBlock(block: CfirBlock): ChirValue? {
        var lastValue: ChirValue? = null
        block.statements.forEach { statement ->
            if (!builder.hasOpenCurrentBlock) {
                throw Cfir2ChirConversionException("unreachable CFIR statement after terminated CHIR block", statement)
            }
            lastValue = lowerStatement(statement)
        }
        return lastValue
    }

    private fun lowerStatement(statement: CfirStatement): ChirValue? {
        return when (statement) {
            is CfirReturnExpression -> {
                lowerReturnExpression(statement)
                null
            }
            is CfirBreakExpression -> {
                lowerBreakExpression(statement)
                null
            }
            is CfirContinueExpression -> {
                lowerContinueExpression(statement)
                null
            }
            is CfirVariable -> lowerLocalVariable(statement)
            is CfirDeclaration -> lowerLocalDeclaration(statement)
            is CfirExpression -> lowerExpression(statement)
            else -> throw Cfir2ChirConversionException(
                "unsupported CFIR statement for CHIR body lowering: ${statement::class.qualifiedName}",
                statement,
            )
        }
    }

    private fun lowerLocalDeclaration(declaration: CfirDeclaration): ChirValue? {
        val operands = when (declaration) {
            is CfirFunction -> {
                registerFunctionHeaderIfNeeded(declaration)
                listOf(storage.getFunctionHeader(declaration.symbol).asValue())
            }
            else -> emptyList()
        }
        emitOtherValue(
            operation = Cfir2ChirOperation.CFIR_LOCAL_DECLARATION,
            source = declaration,
            operands = operands,
            resultType = null,
            attributes = attributes(
                "cfir.declaration.kind" to (declaration::class.simpleName ?: "anonymous"),
                "cfir.symbol" to declaration.symbol.debugName,
            ),
        )
        return null
    }

    private fun lowerReturnExpression(returnExpression: CfirReturnExpression) {
        val result = lowerExpression(returnExpression.result)
        val returnValue = if (header.returnType.isUnit()) {
            null
        } else {
            result ?: throw Cfir2ChirConversionException("non-unit return expression produced no CHIR value", returnExpression)
        }
        builder.terminate(ChirReturnTerminator(builder.nextId("return"), returnValue))
    }

    private fun lowerLocalVariable(variable: CfirVariable): ChirValue? {
        if (variable is CfirPatternVariable) {
            return lowerPatternVariable(variable)
        }
        val initializerValue = variable.initializer?.let { initializer ->
            lowerExpression(initializer)
                ?: throw Cfir2ChirConversionException("local variable initializer does not produce a CHIR value", initializer)
        }
        return lowerLocalBindingVariable(variable, initializerValue, variable)
    }

    private fun lowerPatternVariable(variable: CfirPatternVariable): ChirValue? {
        val initializer = variable.initializer
            ?: throw Cfir2ChirConversionException("pattern local variable requires initializer before CHIR lowering", variable)
        val initializerValue = lowerExpression(initializer)
            ?: throw Cfir2ChirConversionException("pattern local variable initializer does not produce a CHIR value", initializer)

        val bindingVariables = variable.pattern.bindingVariables()
        if (bindingVariables.isEmpty()) {
            emitOtherValue(
                operation = Cfir2ChirOperation.CFIR_LET_PATTERN,
                source = variable,
                operands = listOf(initializerValue),
                resultType = null,
                attributes = patternAttributes(variable.pattern),
            )
            return null
        }

        bindingVariables.forEachIndexed { index, binding ->
            val bindingType = typeMapper.mapTypeRef(binding.returnTypeRef)
            val value = if (bindingVariables.size == 1) {
                initializerValue
            } else {
                emitOtherValue(
                    operation = Cfir2ChirOperation.CFIR_PATTERN_EXTRACT,
                    source = binding,
                    operands = listOf(initializerValue),
                    resultType = bindingType,
                    attributes = patternAttributes(variable.pattern) + attributes("cfir.binding.index" to index.toString()),
                ) ?: throw Cfir2ChirConversionException("pattern binding extract produced no value", binding)
            }
            lowerLocalBindingVariable(binding, value, binding)
        }
        return null
    }

    /**
     * 将真实进入作用域的 CFIR 局部绑定降为 CHIR 地址。
     *
     * 外层 pattern 容器不占用可读写 slot；所有读写都绑定到 CFIR 解析后的 binding symbol。
     */
    private fun lowerLocalBindingVariable(
        variable: CfirVariable,
        initializerValue: ChirValue?,
        source: CfirElement,
    ): ChirValue? {
        val declaration = DefaultChirVariableDeclaration(
            semanticId = Cfir2ChirIds.declarationId(variable.symbol),
            name = variable.nameForChir(),
            type = typeMapper.mapTypeRef(variable.returnTypeRef),
            mutable = variable.isVar,
        )
        val localVariable = ChirLocalVariableHeader(
            declaration = declaration,
            ownerFunctionId = header.semanticId,
        )
        storage.registerLocalVariable(variable.symbol, localVariable)
        builder.emit(
            ChirMemoryExpression(
                semanticId = builder.nextId("alloca"),
                operation = "alloca",
                address = localVariable.asAddressValue(),
            ),
        )

        initializerValue ?: return null
        emitStore(
            targetAddress = localVariable.asAddressValue(),
            value = initializerValue,
            source = source,
            allowImmutableInitialization = true,
        )
        return null
    }

    private fun lowerExpression(expression: CfirExpression): ChirValue? {
        return when (expression) {
            is CfirErrorExpression -> throw Cfir2ChirConversionException(
                "error CFIR expression cannot be lowered to CHIR: ${expression.diagnostic.reason}",
                expression,
            )
            is CfirLazyExpression -> throw Cfir2ChirConversionException(
                "lazy CFIR expression must be materialized before CHIR lowering",
                expression,
            )
            is CfirAnnotationCall -> lowerAnnotationCall(expression)
            is CfirAnnotation -> lowerAnnotation(expression)
            is CfirLiteralExpression -> lowerLiteralExpression(expression)
            is CfirStringInterpolation -> lowerStringInterpolation(expression)
            is CfirFunctionCall -> lowerFunctionCall(expression)
            is CfirNamedAccessExpression -> lowerNamedAccessExpression(expression)
            is CfirSuperReceiverExpression -> lowerSuperReceiverExpression(expression)
            is CfirInaccessibleReceiverExpression -> lowerInaccessibleReceiverExpression(expression)
            is CfirThisReceiverExpression -> lowerThisReceiverExpression(expression)
            is CfirQualifiedAccessExpression -> lowerQualifiedAccessExpression(expression)
            is CfirComparisonExpression -> lowerComparisonExpression(expression)
            is CfirAssignment -> {
                lowerAssignmentExpression(expression)
                null
            }
            is CfirIfExpression -> lowerIfExpression(expression)
            is CfirForInExpression -> lowerForInExpression(expression)
            is CfirLoopExpression -> lowerLoopExpression(expression)
            is CfirMatchExpression -> lowerMatchExpression(expression)
            is CfirTryExpression -> lowerTryExpression(expression)
            is CfirThrowExpression -> {
                lowerThrowExpression(expression)
                null
            }
            is CfirReturnExpression -> {
                lowerReturnExpression(expression)
                null
            }
            is CfirBreakExpression -> {
                lowerBreakExpression(expression)
                null
            }
            is CfirContinueExpression -> {
                lowerContinueExpression(expression)
                null
            }
            is CfirBlock -> lowerBlock(expression)
            is CfirBinaryOp -> lowerBinaryOpExpression(expression)
            is CfirTypeConversion -> lowerTypeConversion(expression)
            is CfirTypeOperator -> lowerTypeOperator(expression)
            is CfirLetPatternExpression -> lowerLetPatternExpression(expression)
            is CfirArrayLiteral -> lowerArrayLiteral(expression)
            is CfirTupleLiteral -> lowerTupleLiteral(expression)
            is CfirRangeExpression -> lowerRangeExpression(expression)
            is CfirSubscriptExpression -> lowerSubscriptExpression(expression)
            is CfirAnonymousFunctionExpression -> lowerAnonymousFunctionExpression(expression)
            is CfirIncrementDecrementExpression -> lowerIncrementDecrementExpression(expression)
            is CfirOptionalExpression -> lowerWrappedExpression(expression, Cfir2ChirOperation.CFIR_OPTIONAL)
            is CfirOptionalChainExpression -> lowerWrappedExpression(expression, Cfir2ChirOperation.CFIR_OPTIONAL_CHAIN)
            is CfirInoutArgumentExpression -> lowerWrappedExpression(expression, Cfir2ChirOperation.CFIR_INOUT_ARGUMENT)
            is CfirWrappedExpression -> lowerWrappedExpression(expression, Cfir2ChirOperation.CFIR_QUALIFIED_ACCESS)
            is CfirPerformExpression -> lowerUnaryCfirExpression(expression, expression.expression, Cfir2ChirOperation.CFIR_PERFORM)
            is CfirResumeExpression -> lowerResumeExpression(expression)
            is CfirSpawnExpression -> lowerSpawnExpression(expression)
            is CfirSynchronizedExpression -> lowerSynchronizedExpression(expression)
            is CfirUnsafeExpression -> lowerUnaryCfirExpression(expression, expression.body, Cfir2ChirOperation.CFIR_UNSAFE)
            is CfirQuoteExpression -> lowerQuoteExpression(expression)
            is CfirSmartCastExpression -> lowerSmartCastExpression(expression)
            is CfirCatch -> lowerCatchExpression(expression)
            is CfirHandleClause -> lowerHandleClause(expression)
            is CfirMatchBranch -> lowerMatchBranch(expression)
            else -> throw Cfir2ChirConversionException(
                "unsupported CFIR expression for CHIR body lowering: ${expression::class.qualifiedName}",
                expression,
            )
        }
    }

    private fun lowerLiteralExpression(expression: CfirLiteralExpression): ChirValue? {
        if (expression.kind == CfirLiteralKind.UNIT) return null
        val type = expression.coneTypeOrNull?.let(typeMapper::mapConeTypeRef)
            ?: throw Cfir2ChirConversionException("literal ${expression.kind} must carry resolved Cone type before CHIR lowering", expression)
        return ChirConstantValue(
            semanticId = builder.nextElementId("literal", expression),
            type = type,
            literal = when (expression.kind) {
                CfirLiteralKind.STRING -> expression.value?.toString() ?: ""
                CfirLiteralKind.BOOLEAN -> expression.value.toString()
                CfirLiteralKind.INT,
                CfirLiteralKind.FLOAT,
                CfirLiteralKind.RUNE,
                -> expression.value?.toString()
                    ?: throw Cfir2ChirConversionException("literal ${expression.kind} has null value", expression)
                CfirLiteralKind.UNIT -> error("unit literal is handled before value materialization")
            },
        )
    }

    private fun lowerAnnotationCall(expression: CfirAnnotationCall): ChirValue? {
        val operands = expression.argumentList.arguments.mapValueOperands("annotation argument")
        return emitOtherValue(
            operation = Cfir2ChirOperation.CFIR_ANNOTATION_CALL,
            source = expression,
            operands = operands,
            resultType = expression.resultTypeOrNull(),
            attributes = referenceAttributes(expression.calleeReference) + attributes(
                "cfir.annotation.type" to expression.typeRef.renderName(),
                "cfir.containingDeclaration" to expression.containingDeclarationSymbol.debugName,
            ),
        )
    }

    private fun lowerAnnotation(expression: CfirAnnotation): ChirValue? =
        emitOtherValue(
            operation = Cfir2ChirOperation.CFIR_ANNOTATION,
            source = expression,
            operands = expression.arguments.filterIsInstance<CfirExpression>().mapValueOperands("annotation element"),
            resultType = expression.resultTypeOrNull(),
            attributes = attributes("cfir.annotation.type" to expression.typeRef.renderName()),
        )

    private fun lowerStringInterpolation(expression: CfirStringInterpolation): ChirValue? =
        emitOtherValue(
            operation = Cfir2ChirOperation.CFIR_STRING_INTERPOLATION,
            source = expression,
            operands = expression.parts.mapValueOperands("string interpolation part"),
            resultType = expression.requireResultType(),
        )

    private fun lowerNamedAccessExpression(expression: CfirNamedAccessExpression): ChirValue {
        val receiverOperands = expression.receiverOperands()
        if (receiverOperands.isNotEmpty()) {
            return emitOtherValue(
                operation = Cfir2ChirOperation.CFIR_MEMBER_ACCESS,
                source = expression,
                operands = receiverOperands,
                resultType = expression.requireResultType(),
                attributes = referenceAttributes(expression.calleeReference),
            ) ?: throw Cfir2ChirConversionException("member access produced no value", expression)
        }
        val reference = expression.calleeReference as? CfirResolvedNamedReference
            ?: throw Cfir2ChirConversionException("named access must be resolved before CHIR lowering", expression)
        return valueForResolvedReference(reference, expression)
    }

    private fun lowerQualifiedAccessExpression(expression: CfirQualifiedAccessExpression): ChirValue =
        emitOtherValue(
            operation = Cfir2ChirOperation.CFIR_QUALIFIED_ACCESS,
            source = expression,
            operands = expression.receiverOperands(),
            resultType = expression.requireResultType(),
            attributes = referenceAttributes(expression.calleeReference),
        ) ?: throw Cfir2ChirConversionException("qualified access produced no value", expression)

    private fun lowerSuperReceiverExpression(expression: CfirSuperReceiverExpression): ChirValue =
        emitOtherValue(
            operation = Cfir2ChirOperation.CFIR_SUPER_RECEIVER,
            source = expression,
            operands = expression.receiverOperands(),
            resultType = expression.requireResultType(),
            attributes = referenceAttributes(expression.calleeReference),
        ) ?: throw Cfir2ChirConversionException("super receiver expression produced no value", expression)

    private fun lowerThisReceiverExpression(expression: CfirThisReceiverExpression): ChirValue =
        emitOtherValue(
            operation = Cfir2ChirOperation.CFIR_THIS_RECEIVER,
            source = expression,
            operands = emptyList(),
            resultType = expression.requireResultType(),
            attributes = thisReferenceAttributes(expression.calleeReference),
        ) ?: throw Cfir2ChirConversionException("this receiver expression produced no value", expression)

    private fun lowerInaccessibleReceiverExpression(expression: CfirInaccessibleReceiverExpression): ChirValue =
        emitOtherValue(
            operation = Cfir2ChirOperation.CFIR_INACCESSIBLE_RECEIVER,
            source = expression,
            operands = emptyList(),
            resultType = expression.requireResultType(),
            attributes = thisReferenceAttributes(expression.calleeReference) +
                    attributes("cfir.receiver.kind" to expression.kind.name),
        ) ?: throw Cfir2ChirConversionException("inaccessible receiver expression produced no value", expression)

    private fun lowerFunctionCall(functionCall: CfirFunctionCall): ChirValue? {
        val receiverOperands = functionCall.receiverOperands()
        val arguments = functionCall.argumentList.arguments.mapValueOperands("function call argument")
        val resultType = functionCall.resultTypeOrNull()
            ?: (functionCall.calleeReference as? CfirResolvedNamedReference)
                ?.resolvedSymbol
                ?.let(storage::getFunctionHeaderOrNull)
                ?.returnType
            ?: throw Cfir2ChirConversionException("function call result type is missing", functionCall)

        val resolvedReference = functionCall.calleeReference as? CfirResolvedNamedReference
        val callee = resolvedReference?.let { valueForCallableReference(it, functionCall, arguments, resultType) }
        val useDirectCall = callee != null && receiverOperands.isEmpty() && functionCall.origin.name == "Regular"

        val expressionId = builder.nextElementId("call", functionCall)
        if (useDirectCall) {
            builder.emit(
                ChirCallExpression(
                    semanticId = expressionId,
                    callee = callee,
                    arguments = arguments,
                    resultType = resultType,
                ),
            )
            return resultType.valueOrNull(expressionId)
        }

        val operands = buildList {
            if (callee != null) add(callee)
            addAll(receiverOperands)
            addAll(arguments)
        }
        builder.emit(
            ChirOtherExpression(
                semanticId = expressionId,
                operation = Cfir2ChirOperation.CFIR_CALL_WITH_RECEIVER.canonicalName,
                operands = operands,
                resultType = resultType,
                attributes = referenceAttributes(functionCall.calleeReference) + attributes(
                    "cfir.call.origin" to functionCall.origin.name,
                    "cfir.call.trailingLambda" to functionCall.hasTrailingLambda.toString(),
                    "cfir.call.varraySize" to (functionCall.varraySizeLiteral ?: ""),
                ),
            ),
        )
        return resultType.valueOrNull(expressionId)
    }

    private fun lowerLocalVariableAccess(
        symbol: CfirVariableSymbol<*>,
        expression: CfirExpression,
    ): ChirValue {
        val localVariable = storage.getLocalVariableOrNull(symbol) ?: return storage.getVariableOrNull(symbol)?.asValue()
            ?: importedVariableValue(symbol, expression.requireResultType())
        val loadId = builder.nextElementId("load", expression)
        builder.emit(
            ChirMemoryExpression(
                semanticId = loadId,
                operation = "load",
                address = localVariable.asAddressValue(),
                resultType = localVariable.declaration.type,
            ),
        )
        return localVariable.declaration.type.valueOrNull(loadId)
            ?: throw Cfir2ChirConversionException("local variable ${localVariable.declaration.name} cannot be loaded as unit value", expression)
    }

    private fun lowerAssignmentExpression(expression: CfirAssignment) {
        val value = lowerExpression(expression.rValue)
            ?: throw Cfir2ChirConversionException("assignment right hand side does not produce a CHIR value", expression.rValue)
        val targetAddress = assignmentAddressOrNull(expression.lValue)
        if (targetAddress != null) {
            emitStore(
                targetAddress = targetAddress,
                value = value,
                source = expression,
                allowImmutableInitialization = false,
            )
            return
        }

        val target = lowerAssignmentTargetValue(expression.lValue)
        emitOtherValue(
            operation = Cfir2ChirOperation.CFIR_MEMBER_ASSIGN,
            source = expression,
            operands = target + value,
            resultType = null,
            attributes = attributes("cfir.assignment.target" to expression.lValue::class.qualifiedName.orEmpty()),
        )
    }

    private fun lowerAssignmentTargetValue(lValue: CfirExpression): List<ChirValue> =
        when (lValue) {
            is CfirNamedAccessExpression -> lValue.receiverOperands()
            else -> listOfNotNull(lowerExpression(lValue))
        }

    private fun lowerComparisonExpression(expression: CfirComparisonExpression): ChirValue {
        val left = lowerExpression(expression.left)
            ?: throw Cfir2ChirConversionException("comparison left operand does not produce a CHIR value", expression.left)
        val right = lowerExpression(expression.right)
            ?: throw Cfir2ChirConversionException("comparison right operand does not produce a CHIR value", expression.right)
        val resultType = expression.resultTypeOrNull() ?: ChirResolvedTypeRef(ChirPrimitiveType.BOOL)
        val expressionId = builder.nextElementId("comparison", expression)
        builder.emit(
            ChirBinaryExpression(
                semanticId = expressionId,
                operator = expression.operation.symbol,
                left = left,
                right = right,
                resultType = resultType,
            ),
        )
        return resultType.valueOrNull(expressionId)
            ?: throw Cfir2ChirConversionException("comparison result cannot be unit", expression)
    }

    private fun lowerIfExpression(expression: CfirIfExpression): ChirValue? {
        val condition = lowerExpression(expression.condition)
            ?: throw Cfir2ChirConversionException("if condition does not produce a CHIR value", expression.condition)
        val resultType = expression.resultTypeOrNull()
        val thenBlockId = builder.nextId("then")
        val elseBlockId = builder.nextId("else")
        val continuationBlockId = builder.nextId("if_cont")
        builder.terminate(
            ChirConditionalBranchTerminator(
                semanticId = builder.nextId("if"),
                condition = condition,
                trueTargetBlockId = thenBlockId,
                falseTargetBlockId = elseBlockId,
            ),
        )

        builder.enterBlock(thenBlockId, "then")
        val thenValue = lowerBlock(expression.thenBranch)
        val thenPred = builder.currentBlockId
        val thenFallsThrough = builder.hasOpenCurrentBlock
        if (thenFallsThrough) {
            builder.terminate(ChirBranchTerminator(builder.nextId("branch"), continuationBlockId))
        }

        builder.enterBlock(elseBlockId, "else")
        val elseValue = expression.elseBranch?.let(::lowerExpression)
        val elsePred = builder.currentBlockId
        val elseFallsThrough = builder.hasOpenCurrentBlock
        if (elseFallsThrough) {
            builder.terminate(ChirBranchTerminator(builder.nextId("branch"), continuationBlockId))
        }

        if (!thenFallsThrough && !elseFallsThrough) {
            builder.clearCurrentBlock()
            return null
        }

        builder.enterBlock(continuationBlockId, "if_cont")
        if (resultType == null || resultType.isUnit()) return null
        val incoming = buildList {
            if (thenFallsThrough) {
                add(
                    requireFallthroughValue(
                        value = thenValue,
                        source = expression.thenBranch,
                        owner = "if then branch",
                    ).withPredecessor(thenPred),
                )
            }
            if (elseFallsThrough) {
                add(
                    requireFallthroughValue(
                        value = elseValue,
                        source = expression.elseBranch ?: expression,
                        owner = "if else branch",
                    ).withPredecessor(elsePred),
                )
            }
        }
        if (incoming.size == 1) return incoming.single()
        return emitOtherValue(
            operation = ChirOtherOperation.PHI,
            source = expression,
            operands = incoming,
            resultType = resultType,
        )
    }

    private fun lowerLoopExpression(expression: CfirLoopExpression): ChirValue? {
        val conditionBlockId = builder.nextId("loop_condition")
        val bodyBlockId = builder.nextId("loop_body")
        val afterBlockId = builder.nextId("loop_after")
        loopTargets[expression] = LoopBlocks(conditionBlockId, afterBlockId)

        builder.terminate(ChirBranchTerminator(builder.nextId("loop_enter"), if (expression.isDoWhile) bodyBlockId else conditionBlockId))

        builder.enterBlock(conditionBlockId, "loop_condition")
        val condition = lowerExpression(expression.condition)
            ?: throw Cfir2ChirConversionException("loop condition does not produce a CHIR value", expression.condition)
        builder.terminate(
            ChirConditionalBranchTerminator(
                semanticId = builder.nextId("loop_branch"),
                condition = condition,
                trueTargetBlockId = bodyBlockId,
                falseTargetBlockId = afterBlockId,
            ),
        )

        builder.enterBlock(bodyBlockId, "loop_body")
        lowerBlock(expression.body)
        if (builder.hasOpenCurrentBlock) {
            builder.terminate(ChirBranchTerminator(builder.nextId("loop_continue"), conditionBlockId))
        }

        builder.enterBlock(afterBlockId, "loop_after")
        loopTargets.remove(expression)
        return null
    }

    private fun lowerForInExpression(expression: CfirForInExpression): ChirValue? {
        val iterable = lowerExpression(expression.iterable)
            ?: throw Cfir2ChirConversionException("for-in iterable does not produce a CHIR value", expression.iterable)
        val iterator = emitOtherValue(
            operation = Cfir2ChirOperation.CFIR_FOR_IN_ITERATOR,
            source = expression.iterable,
            operands = listOf(iterable),
            resultType = iterable.type,
        ) ?: iterable

        val conditionBlockId = builder.nextId("for_condition")
        val bodyBlockId = builder.nextId("for_body")
        val afterBlockId = builder.nextId("for_after")
        loopTargets[expression] = LoopBlocks(conditionBlockId, afterBlockId)
        builder.terminate(ChirBranchTerminator(builder.nextId("for_enter"), conditionBlockId))

        builder.enterBlock(conditionBlockId, "for_condition")
        val hasNext = emitOtherValue(
            operation = Cfir2ChirOperation.CFIR_FOR_IN_HAS_NEXT,
            source = expression,
            operands = listOf(iterator),
            resultType = ChirResolvedTypeRef(ChirPrimitiveType.BOOL),
        ) ?: throw Cfir2ChirConversionException("for-in hasNext produced no value", expression)
        builder.terminate(
            ChirConditionalBranchTerminator(
                semanticId = builder.nextId("for_branch"),
                condition = hasNext,
                trueTargetBlockId = bodyBlockId,
                falseTargetBlockId = afterBlockId,
            ),
        )

        builder.enterBlock(bodyBlockId, "for_body")
        val elementValue = emitOtherValue(
            operation = Cfir2ChirOperation.CFIR_FOR_IN_NEXT,
            source = expression.variable,
            operands = listOf(iterator),
            resultType = typeMapper.mapTypeRef(expression.variable.returnTypeRef),
        ) ?: throw Cfir2ChirConversionException("for-in next produced no value", expression.variable)
        lowerPatternVariableWithValue(expression.variable, elementValue)
        lowerBlock(expression.body)
        if (builder.hasOpenCurrentBlock) {
            builder.terminate(ChirBranchTerminator(builder.nextId("for_continue"), conditionBlockId))
        }

        builder.enterBlock(afterBlockId, "for_after")
        loopTargets.remove(expression)
        return null
    }

    private fun lowerPatternVariableWithValue(variable: CfirPatternVariable, initializerValue: ChirValue) {
        val bindingVariables = variable.pattern.bindingVariables()
        if (bindingVariables.isEmpty()) return
        bindingVariables.forEachIndexed { index, binding ->
            val bindingType = typeMapper.mapTypeRef(binding.returnTypeRef)
            val value = if (bindingVariables.size == 1) {
                initializerValue
            } else {
                emitOtherValue(
                    operation = Cfir2ChirOperation.CFIR_PATTERN_EXTRACT,
                    source = binding,
                    operands = listOf(initializerValue),
                    resultType = bindingType,
                    attributes = patternAttributes(variable.pattern) + attributes("cfir.binding.index" to index.toString()),
                ) ?: throw Cfir2ChirConversionException("for-in pattern extract produced no value", binding)
            }
            lowerLocalBindingVariable(binding, value, binding)
        }
    }

    private fun lowerBreakExpression(expression: CfirBreakExpression) {
        val blocks = loopTargets[expression.target.labeledElement]
            ?: throw Cfir2ChirConversionException("break target loop is not active in CHIR lowering", expression)
        builder.terminate(ChirBranchTerminator(builder.nextId("break"), blocks.breakBlockId))
    }

    private fun lowerContinueExpression(expression: CfirContinueExpression) {
        val blocks = loopTargets[expression.target.labeledElement]
            ?: throw Cfir2ChirConversionException("continue target loop is not active in CHIR lowering", expression)
        builder.terminate(ChirBranchTerminator(builder.nextId("continue"), blocks.continueBlockId))
    }

    private fun lowerMatchExpression(expression: CfirMatchExpression): ChirValue? {
        val subject = expression.subject?.let(::lowerExpression)
        val operands = listOfNotNull(subject)
        val resultType = expression.resultTypeOrNull()
        val continuationBlockId = builder.nextId("match_cont")
        val incoming = mutableListOf<IncomingValue>()
        var nextTestBlockId = builder.nextId("match_test")
        val isExhaustive = expression.exhaustiveness is CfirMatchExhaustivenessStatus.Exhaustive
        builder.terminate(ChirBranchTerminator(builder.nextId("match_enter"), nextTestBlockId))

        expression.branches.forEachIndexed { index, branch ->
            val testBlockId = nextTestBlockId
            val bodyBlockId = builder.nextId("match_body")
            nextTestBlockId = if (index == expression.branches.lastIndex) continuationBlockId else builder.nextId("match_test")
            val isLastExhaustiveBranch = isExhaustive && index == expression.branches.lastIndex

            builder.enterBlock(testBlockId, "match_test_$index")
            val patternMatch = emitOtherValue(
                operation = Cfir2ChirOperation.CFIR_PATTERN_MATCH,
                source = branch.pattern,
                operands = operands,
                resultType = ChirResolvedTypeRef(ChirPrimitiveType.BOOL),
                attributes = patternAttributes(branch.pattern),
            ) ?: throw Cfir2ChirConversionException("match pattern test produced no value", branch)
            val guard = branch.guard?.let(::lowerExpression)
            val condition = guard ?: patternMatch
            if (isLastExhaustiveBranch && guard == null) {
                builder.terminate(ChirBranchTerminator(builder.nextId("match_branch"), bodyBlockId))
            } else {
                builder.terminate(
                    ChirConditionalBranchTerminator(
                        semanticId = builder.nextId("match_branch"),
                        condition = condition,
                        trueTargetBlockId = bodyBlockId,
                        falseTargetBlockId = nextTestBlockId,
                    ),
                )
            }

            builder.enterBlock(bodyBlockId, "match_body_$index")
            val branchValue = lowerBlock(branch.body)
            val pred = builder.currentBlockId
            if (builder.hasOpenCurrentBlock) {
                builder.terminate(ChirBranchTerminator(builder.nextId("match_body_exit"), continuationBlockId))
                incoming += IncomingValue(branchValue, pred, branch.body)
            }
        }

        builder.enterBlock(continuationBlockId, "match_cont")
        emitOtherValue(
            operation = Cfir2ChirOperation.CFIR_MATCH,
            source = expression,
            operands = operands,
            resultType = null,
            attributes = attributes("cfir.exhaustiveness" to expression.exhaustiveness::class.simpleName.orEmpty()),
        )
        if (resultType == null || resultType.isUnit()) return null
        val hasUnmatchedFallthrough = expression.branches.isEmpty() ||
                !isExhaustive ||
                expression.branches.last().guard != null
        if (hasUnmatchedFallthrough) {
            throw Cfir2ChirConversionException("non-unit match expression can fall through without a branch value", expression)
        }
        val values = incoming.map { incomingValue ->
            requireFallthroughValue(
                value = incomingValue.value,
                source = incomingValue.source,
                owner = "match branch",
            ).withPredecessor(incomingValue.predecessorId)
        }
        if (values.isEmpty()) {
            throw Cfir2ChirConversionException("non-unit match expression has no fallthrough value", expression)
        }
        if (values.size == 1) return values.single()
        return emitOtherValue(
            operation = ChirOtherOperation.PHI,
            source = expression,
            operands = values,
            resultType = resultType,
        )
    }

    private fun lowerTryExpression(expression: CfirTryExpression): ChirValue? {
        val resourceValues = expression.resources.mapNotNull { resource ->
            lowerLocalVariable(resource)
            storage.getLocalVariableOrNull(resource.symbol)?.asAddressValue()
        }
        val resultType = expression.resultTypeOrNull()
        val tryBlockId = builder.nextId("try")
        val continuationBlockId = builder.nextId("try_cont")
        builder.terminate(ChirBranchTerminator(builder.nextId("try_enter"), tryBlockId))

        builder.enterBlock(tryBlockId, "try")
        val tryValue = lowerBlock(expression.tryBlock)
        val tryPred = builder.currentBlockId
        val tryFallsThrough = builder.hasOpenCurrentBlock
        if (tryFallsThrough) {
            builder.terminate(ChirBranchTerminator(builder.nextId("try_exit"), continuationBlockId))
        }

        val incoming = mutableListOf<IncomingValue>()
        if (tryFallsThrough) {
            incoming += IncomingValue(tryValue, tryPred, expression.tryBlock)
        }
        expression.catches.forEachIndexed { index, catch ->
            val catchBlockId = builder.nextId("catch")
            builder.enterBlock(catchBlockId, "catch_$index")
            val value = lowerCatchExpression(catch)
            val pred = builder.currentBlockId
            if (builder.hasOpenCurrentBlock) {
                builder.terminate(ChirBranchTerminator(builder.nextId("catch_exit"), continuationBlockId))
                incoming += IncomingValue(value, pred, catch.body)
            }
        }
        expression.handlers.forEachIndexed { index, handler ->
            val handlerBlockId = builder.nextId("handle")
            builder.enterBlock(handlerBlockId, "handle_$index")
            lowerHandleClause(handler)
            if (builder.hasOpenCurrentBlock) {
                builder.terminate(ChirBranchTerminator(builder.nextId("handle_exit"), continuationBlockId))
            }
        }
        expression.finallyBlock?.let { finallyBlock ->
            val finallyBlockId = builder.nextId("finally")
            builder.enterBlock(finallyBlockId, "finally")
            lowerBlock(finallyBlock)
            if (builder.hasOpenCurrentBlock) {
                builder.terminate(ChirBranchTerminator(builder.nextId("finally_exit"), continuationBlockId))
            }
        }

        builder.enterBlock(continuationBlockId, "try_cont")
        emitOtherValue(
            operation = Cfir2ChirOperation.CFIR_TRY,
            source = expression,
            operands = resourceValues,
            resultType = null,
            attributes = attributes(
                "cfir.catch.count" to expression.catches.size.toString(),
                "cfir.handle.count" to expression.handlers.size.toString(),
                "cfir.has.finally" to (expression.finallyBlock != null).toString(),
            ),
        )
        if (resultType == null || resultType.isUnit()) return null
        val values = incoming.map { incomingValue ->
            requireFallthroughValue(
                value = incomingValue.value,
                source = incomingValue.source,
                owner = "try expression branch",
            ).withPredecessor(incomingValue.predecessorId)
        }
        if (values.isEmpty()) {
            throw Cfir2ChirConversionException("non-unit try expression has no fallthrough value", expression)
        }
        if (values.size == 1) return values.single()
        return emitOtherValue(
            operation = ChirOtherOperation.PHI,
            source = expression,
            operands = values,
            resultType = resultType,
        )
    }

    private fun lowerThrowExpression(expression: CfirThrowExpression) {
        val exception = lowerExpression(expression.exception)
            ?: throw Cfir2ChirConversionException("throw expression exception does not produce a value", expression.exception)
        builder.terminate(ChirThrowTerminator(builder.nextId("throw"), exception))
    }

    private fun lowerBinaryOpExpression(expression: CfirBinaryOp): ChirValue? =
        emitOtherValue(
            operation = Cfir2ChirOperation.CFIR_BINARY_OP,
            source = expression,
            operands = listOf(
                lowerExpression(expression.left)
                    ?: throw Cfir2ChirConversionException("binary op left operand does not produce a value", expression.left),
                lowerExpression(expression.right)
                    ?: throw Cfir2ChirConversionException("binary op right operand does not produce a value", expression.right),
            ),
            resultType = expression.requireResultType(),
            attributes = attributes("cfir.binary.kind" to expression.kind.name),
        )

    private fun lowerTypeConversion(expression: CfirTypeConversion): ChirValue? =
        emitOtherValue(
            operation = Cfir2ChirOperation.CFIR_TYPE_CONVERSION,
            source = expression,
            operands = listOf(
                lowerExpression(expression.argument)
                    ?: throw Cfir2ChirConversionException("type conversion argument does not produce a value", expression.argument),
            ),
            resultType = expression.requireResultType(),
            attributes = attributes("cfir.targetType" to expression.targetTypeRef.renderName()),
        )

    private fun lowerTypeOperator(expression: CfirTypeOperator): ChirValue? =
        emitOtherValue(
            operation = Cfir2ChirOperation.CFIR_TYPE_OPERATOR,
            source = expression,
            operands = listOf(
                lowerExpression(expression.argument)
                    ?: throw Cfir2ChirConversionException("type operator argument does not produce a value", expression.argument),
            ),
            resultType = expression.requireResultType(),
            attributes = attributes(
                "cfir.typeOperator.kind" to expression.operation.name,
                "cfir.typeOperator.type" to expression.typeRef.renderName(),
            ),
        )

    private fun lowerLetPatternExpression(expression: CfirLetPatternExpression): ChirValue? {
        val initializer = lowerExpression(expression.initializer)
            ?: throw Cfir2ChirConversionException("let-pattern initializer does not produce a value", expression.initializer)
        return emitOtherValue(
            operation = Cfir2ChirOperation.CFIR_LET_PATTERN,
            source = expression,
            operands = listOf(initializer),
            resultType = expression.requireResultType(),
            attributes = patternAttributes(expression.pattern),
        )
    }

    private fun lowerArrayLiteral(expression: CfirArrayLiteral): ChirValue? =
        emitOtherValue(
            operation = Cfir2ChirOperation.CFIR_ARRAY_LITERAL,
            source = expression,
            operands = expression.elements.mapValueOperands("array element"),
            resultType = expression.requireResultType(),
        )

    private fun lowerTupleLiteral(expression: CfirTupleLiteral): ChirValue? =
        emitOtherValue(
            operation = Cfir2ChirOperation.CFIR_TUPLE_LITERAL,
            source = expression,
            operands = expression.elements.mapValueOperands("tuple element"),
            resultType = expression.requireResultType(),
        )

    private fun lowerRangeExpression(expression: CfirRangeExpression): ChirValue? =
        emitOtherValue(
            operation = Cfir2ChirOperation.CFIR_RANGE,
            source = expression,
            operands = listOfNotNull(
                lowerExpression(expression.start)
                    ?: throw Cfir2ChirConversionException("range start does not produce a value", expression.start),
                lowerExpression(expression.end)
                    ?: throw Cfir2ChirConversionException("range end does not produce a value", expression.end),
                expression.step?.let {
                    lowerExpression(it) ?: throw Cfir2ChirConversionException("range step does not produce a value", it)
                },
            ),
            resultType = expression.requireResultType(),
            attributes = attributes("cfir.range.inclusive" to expression.isInclusive.toString()),
        )

    private fun lowerSubscriptExpression(expression: CfirSubscriptExpression): ChirValue? =
        emitOtherValue(
            operation = Cfir2ChirOperation.CFIR_SUBSCRIPT,
            source = expression,
            operands = listOf(
                lowerExpression(expression.receiver)
                    ?: throw Cfir2ChirConversionException("subscript receiver does not produce a value", expression.receiver),
            ) + expression.indices.mapValueOperands("subscript index"),
            resultType = expression.requireResultType(),
        )

    private fun lowerAnonymousFunctionExpression(expression: CfirAnonymousFunctionExpression): ChirValue {
        registerFunctionHeaderIfNeeded(expression.anonymousFunction)
        val anonymousHeader = storage.getFunctionHeader(expression.anonymousFunction.symbol)
        if (!storage.hasFunction(expression.anonymousFunction.symbol)) {
            val declaration = Cfir2ChirFunctionBodyConverter(
                components = components,
                packageName = packageName,
                function = expression.anonymousFunction,
                header = anonymousHeader,
            ).convert()
            storage.registerFunction(expression.anonymousFunction.symbol, declaration)
        }
        emitOtherValue(
            operation = Cfir2ChirOperation.CFIR_ANONYMOUS_FUNCTION,
            source = expression,
            operands = listOf(anonymousHeader.asValue()),
            resultType = anonymousHeader.functionType,
            attributes = attributes("cfir.trailingLambda" to expression.isTrailingLambda.toString()),
        )
        return anonymousHeader.asValue()
    }

    private fun lowerIncrementDecrementExpression(expression: CfirIncrementDecrementExpression): ChirValue? =
        emitOtherValue(
            operation = Cfir2ChirOperation.CFIR_BINARY_OP,
            source = expression,
            operands = listOf(
                lowerExpression(expression.expression)
                    ?: throw Cfir2ChirConversionException("increment/decrement operand does not produce a value", expression.expression),
            ),
            resultType = expression.resultTypeOrNull(),
            attributes = attributes(
                "cfir.binary.kind" to expression.operationName.asString(),
                "cfir.prefix" to expression.isPrefix.toString(),
            ),
        )

    private fun lowerWrappedExpression(expression: CfirWrappedExpression, operation: Cfir2ChirOperation): ChirValue? =
        emitOtherValue(
            operation = operation,
            source = expression,
            operands = listOfNotNull(lowerExpression(expression.expression)),
            resultType = expression.resultTypeOrNull(),
        )

    private fun lowerUnaryCfirExpression(
        source: CfirExpression,
        payload: CfirExpression,
        operation: Cfir2ChirOperation,
    ): ChirValue? =
        emitOtherValue(
            operation = operation,
            source = source,
            operands = listOfNotNull(lowerExpression(payload)),
            resultType = source.resultTypeOrNull(),
        )

    private fun lowerSmartCastExpression(expression: CfirSmartCastExpression): ChirValue {
        val originalValue = lowerExpression(expression.originalExpression)
            ?: throw Cfir2ChirConversionException("smart cast original expression does not produce a CHIR value", expression.originalExpression)
        return emitOtherValue(
            operation = Cfir2ChirOperation.CFIR_SMART_CAST,
            source = expression,
            operands = listOf(originalValue),
            resultType = typeMapper.mapTypeRef(expression.smartcastType),
            attributes = attributes(
                "cfir.smartcast.targetType" to expression.smartcastType.renderName(),
                "cfir.smartcast.stability" to expression.smartcastStability.name,
                "cfir.smartcast.upperTypes" to expression.upperTypesFromSmartCast.joinToString("|") { it.renderName() },
                "cfir.smartcast.lowerTypes" to expression.lowerTypesFromSmartCast.joinToString("|") { it.renderName() },
            ),
        ) ?: throw Cfir2ChirConversionException("smart cast expression produced no value", expression)
    }

    private fun lowerResumeExpression(expression: CfirResumeExpression): ChirValue? =
        emitOtherValue(
            operation = Cfir2ChirOperation.CFIR_RESUME,
            source = expression,
            operands = listOfNotNull(
                expression.withExpression?.let(::lowerExpression),
                expression.throwingExpression?.let(::lowerExpression),
            ),
            resultType = expression.resultTypeOrNull(),
        )

    private fun lowerSpawnExpression(expression: CfirSpawnExpression): ChirValue? {
        val bodyBlockId = builder.nextId("spawn_body")
        val continuationBlockId = builder.nextId("spawn_cont")
        val threadContext = expression.threadContextArgument?.let(::lowerExpression)
        emitOtherValue(
            operation = Cfir2ChirOperation.CFIR_SPAWN,
            source = expression,
            operands = listOfNotNull(threadContext),
            resultType = null,
        )
        builder.terminate(ChirBranchTerminator(builder.nextId("spawn_enter"), bodyBlockId))
        builder.enterBlock(bodyBlockId, "spawn_body")
        lowerBlock(expression.body)
        if (builder.hasOpenCurrentBlock) {
            builder.terminate(ChirBranchTerminator(builder.nextId("spawn_exit"), continuationBlockId))
        }
        builder.enterBlock(continuationBlockId, "spawn_cont")
        return expression.resultTypeOrNull()?.let { resultType ->
            emitOtherValue(Cfir2ChirOperation.CFIR_SPAWN, expression, listOfNotNull(threadContext), resultType)
        }
    }

    private fun lowerSynchronizedExpression(expression: CfirSynchronizedExpression): ChirValue? {
        val monitor = lowerExpression(expression.monitor)
            ?: throw Cfir2ChirConversionException("synchronized monitor does not produce a value", expression.monitor)
        emitOtherValue(Cfir2ChirOperation.CFIR_SYNCHRONIZED, expression, listOf(monitor), null)
        return lowerBlock(expression.body)
    }

    private fun lowerQuoteExpression(expression: CfirQuoteExpression): ChirValue? =
        emitOtherValue(
            operation = Cfir2ChirOperation.CFIR_QUOTE,
            source = expression,
            operands = expression.interpolations.mapValueOperands("quote interpolation"),
            resultType = expression.resultTypeOrNull(),
            attributes = attributes("cfir.quote.rawText" to expression.rawText),
        )

    private fun lowerCatchExpression(expression: CfirCatch): ChirValue? =
        emitOtherValue(
            operation = Cfir2ChirOperation.CFIR_CATCH,
            source = expression,
            operands = emptyList(),
            resultType = null,
            attributes = attributes("cfir.catch.pattern" to (expression.pattern::class.simpleName ?: "anonymous")),
        ).let { lowerBlock(expression.body) }

    private fun lowerHandleClause(expression: CfirHandleClause): ChirValue? =
        emitOtherValue(
            operation = Cfir2ChirOperation.CFIR_HANDLE,
            source = expression,
            operands = emptyList(),
            resultType = null,
            attributes = attributes("cfir.handle.pattern" to (expression.commandPattern::class.simpleName ?: "anonymous")),
        ).let { lowerBlock(expression.body) }

    private fun lowerMatchBranch(expression: CfirMatchBranch): ChirValue? {
        val guard = expression.guard?.let(::lowerExpression)
        emitOtherValue(
            operation = Cfir2ChirOperation.CFIR_PATTERN_MATCH,
            source = expression.pattern,
            operands = listOfNotNull(guard),
            resultType = ChirResolvedTypeRef(ChirPrimitiveType.BOOL),
            attributes = patternAttributes(expression.pattern),
        )
        return lowerBlock(expression.body)
    }

    private fun valueForResolvedReference(
        reference: CfirResolvedNamedReference,
        expression: CfirExpression,
    ): ChirValue {
        return when (val symbol = reference.resolvedSymbol) {
            is CfirValueParameterSymbol -> storage.getParameter(symbol).asValue()
            is CfirVariableSymbol<*> -> lowerLocalVariableAccess(symbol, expression)
            is CfirFunctionSymbol<*> -> storage.getFunctionHeaderOrNull(symbol)?.asValue()
                ?: importedFunctionValue(symbol, expression.requireResultType(), emptyList())
            is CfirCallableSymbol<*> -> storage.getFunctionHeaderOrNull(symbol)?.asValue()
                ?: importedFunctionValue(symbol, expression.requireResultType(), emptyList())
            else -> throw Cfir2ChirConversionException(
                "resolved CFIR reference ${reference.name} targets unsupported symbol ${symbol::class.qualifiedName}",
                expression,
            )
        }
    }

    private fun valueForCallableReference(
        reference: CfirResolvedNamedReference,
        expression: CfirExpression,
        arguments: List<ChirValue>,
        returnType: ChirTypeRef,
    ): ChirValue {
        val symbol = reference.resolvedSymbol
        return storage.getFunctionHeaderOrNull(symbol)?.asValue()
            ?: if (symbol is CfirCallableSymbol<*>) {
                importedFunctionValue(symbol, returnType, arguments)
            } else {
                valueForResolvedReference(reference, expression)
            }
    }

    private fun assignmentAddressOrNull(lValue: CfirExpression): ChirValue? {
        val namedAccess = lValue as? CfirNamedAccessExpression ?: return null
        if (namedAccess.dispatchReceiver != null || namedAccess.explicitReceiver != null) return null
        val reference = namedAccess.calleeReference as? CfirResolvedNamedReference
            ?: throw Cfir2ChirConversionException("assignment target must be resolved before CHIR lowering", lValue)
        return when (val symbol = reference.resolvedSymbol) {
            is CfirValueParameterSymbol -> storage.getParameter(symbol).asAddressValue()
            is CfirVariableSymbol<*> -> storage.getLocalVariableOrNull(symbol)?.asAddressValue()
                ?: storage.getVariableOrNull(symbol)?.asAddressValue()
            else -> null
        }
    }

    private fun emitStore(
        targetAddress: ChirValue,
        value: ChirValue,
        source: CfirElement,
        allowImmutableInitialization: Boolean,
    ) {
        val referenceType = (targetAddress.type as? ChirResolvedTypeRef)?.type as? ChirRefType
            ?: throw Cfir2ChirConversionException(
                "assignment target address must have CHIR ref type, actual ${targetAddress.type.renderName}",
                source,
            )
        if (!referenceType.mutable && !allowImmutableInitialization) {
            throw Cfir2ChirConversionException("immutable CHIR address ${targetAddress.displayName} cannot be assigned", source)
        }
        if (value.type != referenceType.referencedType) {
            throw Cfir2ChirConversionException(
                "assignment value type ${value.type.renderName} does not match target ${referenceType.referencedType.renderName}",
                source,
            )
        }
        builder.emit(
            ChirMemoryExpression(
                semanticId = builder.nextId("store"),
                operation = "store",
                address = targetAddress,
                value = value,
            ),
        )
    }

    private fun CfirQualifiedAccessExpression.receiverOperands(): List<ChirValue> =
        listOfNotNull(
            dispatchReceiver?.let(::lowerExpression),
            explicitReceiver?.let(::lowerExpression),
        )

    private fun List<CfirExpression>.mapValueOperands(subject: String): List<ChirValue> =
        map { expression ->
            lowerExpression(expression) ?: throw Cfir2ChirConversionException("$subject does not produce a CHIR value", expression)
        }

    private fun emitOtherValue(
        operation: Cfir2ChirOperation,
        source: CfirElement,
        operands: List<ChirValue>,
        resultType: ChirTypeRef?,
        attributes: Set<ChirAttribute> = emptySet(),
    ): ChirValue? =
        emitOtherValue(operation.canonicalName, source, operands, resultType, attributes)

    private fun emitOtherValue(
        operation: ChirOtherOperation,
        source: CfirElement,
        operands: List<ChirValue>,
        resultType: ChirTypeRef?,
        attributes: Set<ChirAttribute> = emptySet(),
    ): ChirValue? =
        emitOtherValue(operation.canonicalName, source, operands, resultType, attributes)

    private fun emitOtherValue(
        operationName: String,
        source: CfirElement,
        operands: List<ChirValue>,
        resultType: ChirTypeRef?,
        attributes: Set<ChirAttribute> = emptySet(),
    ): ChirValue? {
        val expressionId = when (source) {
            is CfirExpression -> builder.nextElementId(operationName.substringAfterLast('.'), source)
            else -> builder.nextId(operationName.substringAfterLast('.'))
        }
        builder.emit(
            ChirOtherExpression(
                semanticId = expressionId,
                operation = operationName,
                operands = operands,
                resultType = resultType,
                attributes = attributes,
            ),
        )
        return resultType?.valueOrNull(expressionId)
    }

    private fun CfirExpression.resultTypeOrNull(): ChirTypeRef? =
        coneTypeOrNull?.let(typeMapper::mapConeTypeRef)

    private fun CfirExpression.requireResultType(): ChirTypeRef =
        resultTypeOrNull() ?: throw Cfir2ChirConversionException(
            "resolved CFIR expression ${this::class.qualifiedName} must carry Cone type before CHIR lowering",
            this,
        )

    private fun CfirTypeRef.renderName(): String =
        typeMapper.mapTypeRef(this).renderName

    private fun ConeCangJieType.renderName(): String =
        typeMapper.mapConeTypeRef(this).renderName

    private fun requireFallthroughValue(
        value: ChirValue?,
        source: CfirElement,
        owner: String,
    ): ChirValue =
        value ?: throw Cfir2ChirConversionException("$owner falls through without a CHIR value", source)

    private fun CfirVariable.nameForChir(): String =
        when (this) {
            is CfirFieldVariable -> name.asString()
            is CfirPatternBindingVariable -> name.asString()
            is CfirPatternVariable -> throw Cfir2ChirConversionException(
                "pattern variable container requires pattern binding lowering before CHIR local allocation",
                this,
            )
            is CfirValueParameter -> symbol.name.asString()
        }

    private fun registerFunctionHeaderIfNeeded(function: CfirFunction) {
        if (storage.hasFunctionHeader(function.symbol)) return
        val functionId = Cfir2ChirIds.callableId(function)
        val parameters = function.valueParameters.map { parameter ->
            val declaration = DefaultChirVariableDeclaration(
                semanticId = Cfir2ChirIds.parameterId(parameter),
                name = parameter.name.asString(),
                type = typeMapper.mapTypeRef(parameter.returnTypeRef),
                mutable = parameter.isVar,
            )
            storage.registerParameter(
                parameter.symbol,
                ChirParameterHeader(declaration, functionId),
            )
            declaration
        }
        storage.registerFunctionHeader(
            function.symbol,
            ChirFunctionHeader(
                semanticId = functionId,
                name = function.symbol.name.asString(),
                returnType = typeMapper.mapTypeRef(function.returnTypeRef),
                parameters = parameters,
                receiverType = function.dispatchReceiverType?.let(typeMapper::mapConeTypeRef),
            ),
        )
    }

    private fun importedFunctionValue(
        symbol: CfirCallableSymbol<*>,
        returnType: ChirTypeRef,
        arguments: List<ChirValue>,
    ): ChirImportedFunctionValue {
        val functionType = ChirResolvedTypeRef(ChirFunctionType(arguments.map { it.type }, returnType))
        return ChirImportedFunctionValue(
            semanticId = Cfir2ChirIds.declarationId(symbol),
            type = functionType,
            name = symbol.callableId.toString(),
        )
    }

    private fun importedVariableValue(symbol: CfirVariableSymbol<*>, type: ChirTypeRef): ChirImportedVariableValue =
        ChirImportedVariableValue(
            semanticId = Cfir2ChirIds.declarationId(symbol),
            type = type,
            name = symbol.callableId.toString(),
        )

    private fun referenceAttributes(reference: CfirReference): Set<ChirAttribute> {
        val attributes = linkedSetOf<ChirAttribute>()
        if (reference is CfirNamedReference) {
            attributes += ChirStringAttribute("cfir.reference.name", reference.name.asString())
        }
        if (reference is CfirResolvedNamedReference) {
            attributes += ChirStringAttribute("cfir.reference.symbol", reference.resolvedSymbol.debugName)
        }
        return attributes
    }

    private fun thisReferenceAttributes(reference: CfirThisReference): Set<ChirAttribute> =
        attributes(
            "cfir.this.implicit" to reference.isImplicit.toString(),
            "cfir.this.symbol" to (reference.boundSymbol?.debugName ?: ""),
            "cfir.this.diagnostic" to (reference.diagnostic?.reason ?: ""),
        )

    private fun patternAttributes(pattern: CfirPattern): Set<ChirAttribute> =
        attributes("cfir.pattern.kind" to (pattern::class.simpleName ?: "anonymous"))

    private fun attributes(vararg values: Pair<String, String>): Set<ChirAttribute> =
        values.filterTo(linkedSetOf()) { (_, value) -> value.isNotEmpty() }
            .mapTo(linkedSetOf()) { (key, value) -> ChirStringAttribute(key, value) }

    private fun ChirTypeRef.valueOrNull(expressionId: ChirSemanticId): ChirValue? {
        if (isUnit()) return null
        return ChirLocalValue(
            semanticId = expressionId,
            type = this,
            name = expressionId.value,
        )
    }

    private fun ChirTypeRef.isUnit(): Boolean =
        this == ChirResolvedTypeRef(ChirPrimitiveType.UNIT) || this == ChirResolvedTypeRef(ChirPrimitiveType.VOID)

    private fun ChirValue.withPredecessor(predecessorId: ChirSemanticId): ChirValue {
        val nextAttributes = attributes + ChirStringAttribute("pred", predecessorId.value)
        return when (this) {
            is ChirLocalValue -> copy(attributes = nextAttributes)
            is ChirConstantValue -> copy(attributes = nextAttributes)
            is ChirParameterValue -> copy(attributes = nextAttributes)
            is ChirGlobalValue -> copy(attributes = nextAttributes)
            is ChirImportedFunctionValue -> copy(attributes = nextAttributes)
            is ChirImportedVariableValue -> copy(attributes = nextAttributes)
            is ChirFunctionValue -> copy(attributes = nextAttributes)
            is ChirBlockValue -> copy(attributes = nextAttributes)
            else -> this
        }
    }

    private data class LoopBlocks(
        val continueBlockId: ChirSemanticId,
        val breakBlockId: ChirSemanticId,
    )

    private data class IncomingValue(
        val value: ChirValue?,
        val predecessorId: ChirSemanticId,
        val source: CfirElement,
    )

    private class FunctionBodyBuilder(
        private val ownerId: ChirSemanticId,
    ) {
        private val blocks = mutableListOf<MutableBlock>()
        private var currentBlock: MutableBlock? = null
        private var nextIndex = 0

        val hasOpenCurrentBlock: Boolean
            get() = currentBlock?.terminator == null

        val currentBlockId: ChirSemanticId
            get() = openCurrentBlock().semanticId

        fun enterNewBlock(name: String): MutableBlock =
            enterBlock(nextId(name), name)

        fun enterBlock(id: ChirSemanticId, name: String): MutableBlock {
            val block = MutableBlock(id, name)
            blocks += block
            currentBlock = block
            return block
        }

        fun clearCurrentBlock() {
            currentBlock = null
        }

        fun emit(expression: ChirExpression) {
            val block = openCurrentBlock()
            block.expressions += expression
        }

        fun terminate(terminator: ChirTerminator) {
            val block = openCurrentBlock()
            check(block.terminator == null) { "CHIR block ${block.name} is already terminated" }
            block.terminator = terminator
        }

        fun freeze(): List<ChirBlock> {
            return blocks.map { block ->
                ChirBlock(
                    semanticId = block.semanticId,
                    name = block.name,
                    expressions = block.expressions,
                    terminator = block.terminator
                        ?: throw Cfir2ChirConversionException("CHIR block ${block.name} has no terminator"),
                )
            }
        }

        fun nextId(kind: String): ChirSemanticId =
            Cfir2ChirIds.generatedId(kind, ownerId, nextIndex++)

        fun nextElementId(kind: String, expression: CfirExpression): ChirSemanticId =
            Cfir2ChirIds.elementId(kind, expression, ownerId, nextIndex++)

        private fun openCurrentBlock(): MutableBlock =
            currentBlock ?: throw Cfir2ChirConversionException("no open CHIR block in function body converter")
    }

    private data class MutableBlock(
        val semanticId: ChirSemanticId,
        val name: String,
        val expressions: MutableList<ChirExpression> = mutableListOf(),
        var terminator: ChirTerminator? = null,
    )
}
