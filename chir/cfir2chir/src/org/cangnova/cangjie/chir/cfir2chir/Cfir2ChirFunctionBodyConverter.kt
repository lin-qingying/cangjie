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

/**
 * 将单个 CFIR 函数或 code fragment 的 body 降级为 CHIR basic blocks。
 */
internal class Cfir2ChirFunctionBodyConverter(
    /**
     * 当前 package 转换共享的组件集合。
     */
    private val components: Cfir2ChirComponents,
    /**
     * 当前函数所属的包名，用于 absent body 等属性记录。
     */
    private val packageName: String,
    /**
     * 当前函数已预先登记的 CHIR 函数 header。
     */
    private val header: ChirFunctionHeader,
    /**
     * 当前函数的 CFIR body；缺失时会生成 absent-body 占位操作。
     */
    private val body: CfirBlock?,
    /**
     * body 缺失或需要报告错误时使用的源 CFIR 元素。
     */
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

    /**
     * 共享声明存储，用于查询函数、参数、变量和局部变量 header。
     */
    private val storage: Cfir2ChirDeclarationStorage = components.declarationStorage

    /**
     * 共享类型映射器，用于把 CFIR/Cone 类型映射为 CHIR 类型。
     */
    private val typeMapper: Cfir2ChirTypeMapper = components.typeMapper

    /**
     * 当前函数 body 的 CHIR block 构造器。
     */
    private val builder = FunctionBodyBuilder(header.semanticId)

    /**
     * 当前活跃循环到 continue/break 目标 block 的映射。
     */
    private val loopTargets = IdentityHashMap<CfirLoopExpression, LoopBlocks>()

    /**
     * 执行完整函数体 lowering，并生成 CHIR 函数声明。
     */
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

    /**
     * 为没有 CFIR body 的函数生成保留源级语义的占位 CHIR 操作。
     */
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

    /**
     * 顺序 lowering CFIR block 中的语句，并返回最后一个表达式值。
     */
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

    /**
     * Lower 单条 CFIR statement，处理控制流终结语句和表达式语句。
     */
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

    /**
     * Lower 函数体中的局部声明，并保留局部函数等声明值。
     */
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

    /**
     * Lower return 表达式并终结当前 CHIR block。
     */
    private fun lowerReturnExpression(returnExpression: CfirReturnExpression) {
        val result = lowerExpression(returnExpression.result)
        val returnValue = if (header.returnType.isUnit()) {
            null
        } else {
            result ?: throw Cfir2ChirConversionException("non-unit return expression produced no CHIR value", returnExpression)
        }
        builder.terminate(ChirReturnTerminator(builder.nextId("return"), returnValue))
    }

    /**
     * Lower 普通局部变量或 pattern 局部变量声明。
     */
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

    /**
     * Lower pattern 变量声明，并为每个真实 binding 创建局部 slot。
     */
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

    /**
     * 分派 lowering 各类 CFIR 表达式。
     */
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

    /**
     * Lower 字面量表达式，Unit 字面量不产生 CHIR value。
     */
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

    /**
     * Lower 注解调用，保留注解类型和包含声明信息。
     */
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

    /**
     * Lower 注解表达式，保留注解类型与参数值。
     */
    private fun lowerAnnotation(expression: CfirAnnotation): ChirValue? =
        emitOtherValue(
            operation = Cfir2ChirOperation.CFIR_ANNOTATION,
            source = expression,
            operands = expression.arguments.filterIsInstance<CfirExpression>().mapValueOperands("annotation element"),
            resultType = expression.resultTypeOrNull(),
            attributes = attributes("cfir.annotation.type" to expression.typeRef.renderName()),
        )

    /**
     * Lower 字符串插值表达式，按插值片段收集操作数。
     */
    private fun lowerStringInterpolation(expression: CfirStringInterpolation): ChirValue? =
        emitOtherValue(
            operation = Cfir2ChirOperation.CFIR_STRING_INTERPOLATION,
            source = expression,
            operands = expression.parts.mapValueOperands("string interpolation part"),
            resultType = expression.requireResultType(),
        )

    /**
     * Lower 已解析的名称访问，区分成员访问、本地变量、参数和函数引用。
     */
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

    /**
     * Lower qualified access，并把接收者值作为 CFIR 专有操作数保留下来。
     */
    private fun lowerQualifiedAccessExpression(expression: CfirQualifiedAccessExpression): ChirValue =
        emitOtherValue(
            operation = Cfir2ChirOperation.CFIR_QUALIFIED_ACCESS,
            source = expression,
            operands = expression.receiverOperands(),
            resultType = expression.requireResultType(),
            attributes = referenceAttributes(expression.calleeReference),
        ) ?: throw Cfir2ChirConversionException("qualified access produced no value", expression)

    /**
     * Lower super 接收者表达式。
     */
    private fun lowerSuperReceiverExpression(expression: CfirSuperReceiverExpression): ChirValue =
        emitOtherValue(
            operation = Cfir2ChirOperation.CFIR_SUPER_RECEIVER,
            source = expression,
            operands = expression.receiverOperands(),
            resultType = expression.requireResultType(),
            attributes = referenceAttributes(expression.calleeReference),
        ) ?: throw Cfir2ChirConversionException("super receiver expression produced no value", expression)

    /**
     * Lower this 接收者表达式。
     */
    private fun lowerThisReceiverExpression(expression: CfirThisReceiverExpression): ChirValue =
        emitOtherValue(
            operation = Cfir2ChirOperation.CFIR_THIS_RECEIVER,
            source = expression,
            operands = emptyList(),
            resultType = expression.requireResultType(),
            attributes = thisReferenceAttributes(expression.calleeReference),
        ) ?: throw Cfir2ChirConversionException("this receiver expression produced no value", expression)

    /**
     * Lower 不可访问接收者表达式，并保留接收者种类。
     */
    private fun lowerInaccessibleReceiverExpression(expression: CfirInaccessibleReceiverExpression): ChirValue =
        emitOtherValue(
            operation = Cfir2ChirOperation.CFIR_INACCESSIBLE_RECEIVER,
            source = expression,
            operands = emptyList(),
            resultType = expression.requireResultType(),
            attributes = thisReferenceAttributes(expression.calleeReference) +
                    attributes("cfir.receiver.kind" to expression.kind.name),
        ) ?: throw Cfir2ChirConversionException("inaccessible receiver expression produced no value", expression)

    /**
     * Lower 函数调用；普通直接调用生成 `ChirCallExpression`，带接收者或特殊 origin 的调用保留为 CFIR 操作。
     */
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

    /**
     * Lower 局部、全局或导入变量读取。
     */
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

    /**
     * Lower 赋值表达式，优先降为内存 store，成员赋值保留为 CFIR 专有操作。
     */
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

    /**
     * 提取赋值目标表达式需要作为操作数保留的值。
     */
    private fun lowerAssignmentTargetValue(lValue: CfirExpression): List<ChirValue> =
        when (lValue) {
            is CfirNamedAccessExpression -> lValue.receiverOperands()
            else -> listOfNotNull(lowerExpression(lValue))
        }

    /**
     * Lower 比较表达式为 CHIR 二元表达式。
     */
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

    /**
     * Lower if 表达式为条件分支、then/else block 和必要的 phi 值。
     */
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

    /**
     * Lower while/do-while 循环，并登记 break/continue 目标。
     */
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

    /**
     * Lower for-in 循环，显式生成 iterator/hasNext/next CFIR 专有操作。
     */
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

    /**
     * 使用已求值的初始化值 lower pattern binding 变量。
     */
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

    /**
     * Lower break 表达式为跳转到当前循环 break 目标的分支终结符。
     */
    private fun lowerBreakExpression(expression: CfirBreakExpression) {
        val blocks = loopTargets[expression.target.labeledElement]
            ?: throw Cfir2ChirConversionException("break target loop is not active in CHIR lowering", expression)
        builder.terminate(ChirBranchTerminator(builder.nextId("break"), blocks.breakBlockId))
    }

    /**
     * Lower continue 表达式为跳转到当前循环 continue 目标的分支终结符。
     */
    private fun lowerContinueExpression(expression: CfirContinueExpression) {
        val blocks = loopTargets[expression.target.labeledElement]
            ?: throw Cfir2ChirConversionException("continue target loop is not active in CHIR lowering", expression)
        builder.terminate(ChirBranchTerminator(builder.nextId("continue"), blocks.continueBlockId))
    }

    /**
     * Lower match 表达式为 pattern test block、branch body block 和必要的 phi 值。
     */
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

    /**
     * Lower try 表达式，保留 catch/handle/finally 结构并为非 Unit 结果生成 phi 值。
     */
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

    /**
     * Lower throw 表达式并终结当前 block。
     */
    private fun lowerThrowExpression(expression: CfirThrowExpression) {
        val exception = lowerExpression(expression.exception)
            ?: throw Cfir2ChirConversionException("throw expression exception does not produce a value", expression.exception)
        builder.terminate(ChirThrowTerminator(builder.nextId("throw"), exception))
    }

    /**
     * Lower CFIR 二元操作为保留源级 kind 的 CHIR other 操作。
     */
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

    /**
     * Lower 类型转换表达式并记录目标类型。
     */
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

    /**
     * Lower 类型操作表达式并记录操作种类和目标类型。
     */
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

    /**
     * Lower let-pattern 表达式，保留 pattern 形态属性。
     */
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

    /**
     * Lower 数组字面量为 CFIR 专有 array literal 操作。
     */
    private fun lowerArrayLiteral(expression: CfirArrayLiteral): ChirValue? =
        emitOtherValue(
            operation = Cfir2ChirOperation.CFIR_ARRAY_LITERAL,
            source = expression,
            operands = expression.elements.mapValueOperands("array element"),
            resultType = expression.requireResultType(),
        )

    /**
     * Lower tuple 字面量为 CFIR 专有 tuple literal 操作。
     */
    private fun lowerTupleLiteral(expression: CfirTupleLiteral): ChirValue? =
        emitOtherValue(
            operation = Cfir2ChirOperation.CFIR_TUPLE_LITERAL,
            source = expression,
            operands = expression.elements.mapValueOperands("tuple element"),
            resultType = expression.requireResultType(),
        )

    /**
     * Lower range 表达式并记录是否为闭区间。
     */
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

    /**
     * Lower 下标访问表达式，保留 receiver 和 index 操作数。
     */
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

    /**
     * Lower 匿名函数表达式，确保匿名函数声明只转换一次。
     */
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

    /**
     * Lower 自增/自减表达式为保留前缀与操作名的 CFIR 二元操作。
     */
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

    /**
     * Lower 包装表达式，保留指定的 CFIR 专有操作名。
     */
    private fun lowerWrappedExpression(expression: CfirWrappedExpression, operation: Cfir2ChirOperation): ChirValue? =
        emitOtherValue(
            operation = operation,
            source = expression,
            operands = listOfNotNull(lowerExpression(expression.expression)),
            resultType = expression.resultTypeOrNull(),
        )

    /**
     * Lower 单操作数 CFIR 表达式。
     */
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

    /**
     * Lower smart cast 表达式，并记录目标类型、稳定性与上下界信息。
     */
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

    /**
     * Lower resume 表达式，保留 with/throwing 两类 payload。
     */
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

    /**
     * Lower spawn 表达式为独立 body block 与 continuation block。
     */
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

    /**
     * Lower synchronized 表达式，先记录 monitor，再 lower body。
     */
    private fun lowerSynchronizedExpression(expression: CfirSynchronizedExpression): ChirValue? {
        val monitor = lowerExpression(expression.monitor)
            ?: throw Cfir2ChirConversionException("synchronized monitor does not produce a value", expression.monitor)
        emitOtherValue(Cfir2ChirOperation.CFIR_SYNCHRONIZED, expression, listOf(monitor), null)
        return lowerBlock(expression.body)
    }

    /**
     * Lower quote 表达式，保留插值操作数与原始文本。
     */
    private fun lowerQuoteExpression(expression: CfirQuoteExpression): ChirValue? =
        emitOtherValue(
            operation = Cfir2ChirOperation.CFIR_QUOTE,
            source = expression,
            operands = expression.interpolations.mapValueOperands("quote interpolation"),
            resultType = expression.resultTypeOrNull(),
            attributes = attributes("cfir.quote.rawText" to expression.rawText),
        )

    /**
     * Lower catch 子句，记录 pattern 类型并 lower catch body。
     */
    private fun lowerCatchExpression(expression: CfirCatch): ChirValue? =
        emitOtherValue(
            operation = Cfir2ChirOperation.CFIR_CATCH,
            source = expression,
            operands = emptyList(),
            resultType = null,
            attributes = attributes("cfir.catch.pattern" to (expression.pattern::class.simpleName ?: "anonymous")),
        ).let { lowerBlock(expression.body) }

    /**
     * Lower effect handle 子句，记录 command pattern 并 lower handler body。
     */
    private fun lowerHandleClause(expression: CfirHandleClause): ChirValue? =
        emitOtherValue(
            operation = Cfir2ChirOperation.CFIR_HANDLE,
            source = expression,
            operands = emptyList(),
            resultType = null,
            attributes = attributes("cfir.handle.pattern" to (expression.commandPattern::class.simpleName ?: "anonymous")),
        ).let { lowerBlock(expression.body) }

    /**
     * Lower 单个 match branch，供 branch 表达式被独立作为表达式处理时使用。
     */
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

    /**
     * 将已解析 CFIR reference 映射为对应的 CHIR value。
     */
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

    /**
     * 将可调用 reference 映射为本模块函数值或导入函数值。
     */
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

    /**
     * 尝试把赋值左值解析为可 store 的地址值。
     */
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

    /**
     * 生成 CHIR store 内存表达式，并校验地址和值类型匹配。
     */
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

    /**
     * Lower qualified access 的 dispatch/explicit receiver 操作数。
     */
    private fun CfirQualifiedAccessExpression.receiverOperands(): List<ChirValue> =
        listOfNotNull(
            dispatchReceiver?.let(::lowerExpression),
            explicitReceiver?.let(::lowerExpression),
        )

    /**
     * Lower 表达式列表并要求每个表达式都产生 CHIR value。
     */
    private fun List<CfirExpression>.mapValueOperands(subject: String): List<ChirValue> =
        map { expression ->
            lowerExpression(expression) ?: throw Cfir2ChirConversionException("$subject does not produce a CHIR value", expression)
        }

    /**
     * 生成使用 `Cfir2ChirOperation` 命名的 CHIR other 表达式。
     */
    private fun emitOtherValue(
        operation: Cfir2ChirOperation,
        source: CfirElement,
        operands: List<ChirValue>,
        resultType: ChirTypeRef?,
        attributes: Set<ChirAttribute> = emptySet(),
    ): ChirValue? =
        emitOtherValue(operation.canonicalName, source, operands, resultType, attributes)

    /**
     * 生成使用 CHIR core other operation 命名的 CHIR other 表达式。
     */
    private fun emitOtherValue(
        operation: ChirOtherOperation,
        source: CfirElement,
        operands: List<ChirValue>,
        resultType: ChirTypeRef?,
        attributes: Set<ChirAttribute> = emptySet(),
    ): ChirValue? =
        emitOtherValue(operation.canonicalName, source, operands, resultType, attributes)

    /**
     * 生成通用 CHIR other 表达式，并在非 Unit 结果类型下返回对应局部值。
     */
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

    /**
     * 读取表达式解析后的结果类型；缺失时返回空。
     */
    private fun CfirExpression.resultTypeOrNull(): ChirTypeRef? =
        coneTypeOrNull?.let(typeMapper::mapConeTypeRef)

    /**
     * 读取表达式解析后的结果类型；缺失时报转换异常。
     */
    private fun CfirExpression.requireResultType(): ChirTypeRef =
        resultTypeOrNull() ?: throw Cfir2ChirConversionException(
            "resolved CFIR expression ${this::class.qualifiedName} must carry Cone type before CHIR lowering",
            this,
        )

    /**
     * 将 CFIR type ref 映射并渲染为 CHIR 类型名称。
     */
    private fun CfirTypeRef.renderName(): String =
        typeMapper.mapTypeRef(this).renderName

    /**
     * 将 Cone 类型映射并渲染为 CHIR 类型名称。
     */
    private fun ConeCangJieType.renderName(): String =
        typeMapper.mapConeTypeRef(this).renderName

    /**
     * 要求控制流分支 fallthrough 时产生值。
     */
    private fun requireFallthroughValue(
        value: ChirValue?,
        source: CfirElement,
        owner: String,
    ): ChirValue =
        value ?: throw Cfir2ChirConversionException("$owner falls through without a CHIR value", source)

    /**
     * 为 CFIR 变量选择 CHIR 局部名称。
     */
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

    /**
     * 确保局部或匿名函数的 header 已经登记到共享声明存储。
     */
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

    /**
     * 为当前模块外部的可调用符号创建导入函数值。
     */
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

    /**
     * 为当前模块外部的变量符号创建导入变量值。
     */
    private fun importedVariableValue(symbol: CfirVariableSymbol<*>, type: ChirTypeRef): ChirImportedVariableValue =
        ChirImportedVariableValue(
            semanticId = Cfir2ChirIds.declarationId(symbol),
            type = type,
            name = symbol.callableId.toString(),
        )

    /**
     * 把 CFIR reference 的名称和解析 symbol 写入 CHIR attribute。
     */
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

    /**
     * 把 this reference 的隐式性、绑定 symbol 和诊断信息写入 CHIR attribute。
     */
    private fun thisReferenceAttributes(reference: CfirThisReference): Set<ChirAttribute> =
        attributes(
            "cfir.this.implicit" to reference.isImplicit.toString(),
            "cfir.this.symbol" to (reference.boundSymbol?.debugName ?: ""),
            "cfir.this.diagnostic" to (reference.diagnostic?.reason ?: ""),
        )

    /**
     * 把 CFIR pattern 的运行时类型写入 CHIR attribute。
     */
    private fun patternAttributes(pattern: CfirPattern): Set<ChirAttribute> =
        attributes("cfir.pattern.kind" to (pattern::class.simpleName ?: "anonymous"))

    /**
     * 构造非空字符串值对应的 CHIR 字符串属性集合。
     */
    private fun attributes(vararg values: Pair<String, String>): Set<ChirAttribute> =
        values.filterTo(linkedSetOf()) { (_, value) -> value.isNotEmpty() }
            .mapTo(linkedSetOf()) { (key, value) -> ChirStringAttribute(key, value) }

    /**
     * 为非 Unit 类型生成表达式结果局部值。
     */
    private fun ChirTypeRef.valueOrNull(expressionId: ChirSemanticId): ChirValue? {
        if (isUnit()) return null
        return ChirLocalValue(
            semanticId = expressionId,
            type = this,
            name = expressionId.value,
        )
    }

    /**
     * 判断 CHIR 类型是否为 Unit 或 Void。
     */
    private fun ChirTypeRef.isUnit(): Boolean =
        this == ChirResolvedTypeRef(ChirPrimitiveType.UNIT) || this == ChirResolvedTypeRef(ChirPrimitiveType.VOID)

    /**
     * 给值附加 predecessor block ID 属性，用于 phi 输入来源标记。
     */
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

    /**
     * 活跃循环的 continue 与 break 目标 block。
     */
    private data class LoopBlocks(
        /**
         * continue 应跳转到的 block ID。
         */
        val continueBlockId: ChirSemanticId,
        /**
         * break 应跳转到的 block ID。
         */
        val breakBlockId: ChirSemanticId,
    )

    /**
     * 需要参与 phi 合并的分支 fallthrough 值。
     */
    private data class IncomingValue(
        /**
         * 分支 fallthrough 时产生的值；Unit 分支为空。
         */
        val value: ChirValue?,
        /**
         * 产生该值的前驱 block ID。
         */
        val predecessorId: ChirSemanticId,
        /**
         * 该值对应的源 CFIR 元素，用于错误定位。
         */
        val source: CfirElement,
    )

    /**
     * 函数 body lowering 期间用于逐步构造 CHIR block 的可变 builder。
     */
    private class FunctionBodyBuilder(
        /**
         * 当前函数的语义 ID，作为所有合成 block/expression ID 的 owner。
         */
        private val ownerId: ChirSemanticId,
    ) {
        /**
         * 已创建的可变 block 列表。
         */
        private val blocks = mutableListOf<MutableBlock>()

        /**
         * 当前正在接收表达式或终结符的可变 block。
         */
        private var currentBlock: MutableBlock? = null

        /**
         * 当前函数内用于生成唯一 ID 的递增序号。
         */
        private var nextIndex = 0

        /**
         * 当前 block 是否存在且尚未被 terminator 关闭。
         */
        val hasOpenCurrentBlock: Boolean
            get() = currentBlock?.terminator == null

        /**
         * 当前打开 block 的语义 ID。
         */
        val currentBlockId: ChirSemanticId
            get() = openCurrentBlock().semanticId

        /**
         * 使用自动生成 ID 创建并进入新 block。
         */
        fun enterNewBlock(name: String): MutableBlock =
            enterBlock(nextId(name), name)

        /**
         * 使用指定 ID 创建并进入新 block。
         */
        fun enterBlock(id: ChirSemanticId, name: String): MutableBlock {
            val block = MutableBlock(id, name)
            blocks += block
            currentBlock = block
            return block
        }

        /**
         * 清空当前 block，表示当前控制流路径已经无法继续追加表达式。
         */
        fun clearCurrentBlock() {
            currentBlock = null
        }

        /**
         * 向当前 block 追加表达式。
         */
        fun emit(expression: ChirExpression) {
            val block = openCurrentBlock()
            block.expressions += expression
        }

        /**
         * 为当前 block 设置终结符。
         */
        fun terminate(terminator: ChirTerminator) {
            val block = openCurrentBlock()
            check(block.terminator == null) { "CHIR block ${block.name} is already terminated" }
            block.terminator = terminator
        }

        /**
         * 冻结所有可变 block，生成不可变 CHIR block 列表。
         */
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

        /**
         * 生成当前函数内的下一个合成 ID。
         */
        fun nextId(kind: String): ChirSemanticId =
            Cfir2ChirIds.generatedId(kind, ownerId, nextIndex++)

        /**
         * 基于 CFIR 表达式源码信息生成当前函数内的下一个元素 ID。
         */
        fun nextElementId(kind: String, expression: CfirExpression): ChirSemanticId =
            Cfir2ChirIds.elementId(kind, expression, ownerId, nextIndex++)

        /**
         * 返回当前打开 block；不存在时报告转换异常。
         */
        private fun openCurrentBlock(): MutableBlock =
            currentBlock ?: throw Cfir2ChirConversionException("no open CHIR block in function body converter")
    }

    /**
     * FunctionBodyBuilder 内部使用的可变 block 表示。
     */
    private data class MutableBlock(
        /**
         * block 的稳定语义 ID。
         */
        val semanticId: ChirSemanticId,
        /**
         * block 的调试名称。
         */
        val name: String,
        /**
         * block 内已追加的表达式列表。
         */
        val expressions: MutableList<ChirExpression> = mutableListOf(),
        /**
         * block 的终结符；冻结前必须存在。
         */
        var terminator: ChirTerminator? = null,
    )
}
