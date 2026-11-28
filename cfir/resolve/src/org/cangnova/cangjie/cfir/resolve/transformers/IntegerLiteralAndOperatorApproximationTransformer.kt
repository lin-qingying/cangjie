package org.cangnova.cangjie.cfir.resolve.transformers

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.SessionAndScopeSessionHolder
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.expressions.CfirAnonymousFunctionExpression
import org.cangnova.cangjie.cfir.expressions.CfirArgumentList
import org.cangnova.cangjie.cfir.expressions.CfirArrayLiteral
import org.cangnova.cangjie.cfir.expressions.CfirAssignment
import org.cangnova.cangjie.cfir.expressions.CfirBinaryOp
import org.cangnova.cangjie.cfir.expressions.CfirBinaryOpKind
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirCatch
import org.cangnova.cangjie.cfir.expressions.CfirForInExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirHandleClause
import org.cangnova.cangjie.cfir.expressions.CfirIfExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.expressions.CfirLoopExpression
import org.cangnova.cangjie.cfir.expressions.CfirMatchBranch
import org.cangnova.cangjie.cfir.expressions.CfirMatchExpression
import org.cangnova.cangjie.cfir.expressions.CfirOptionalChainExpression
import org.cangnova.cangjie.cfir.expressions.CfirOptionalExpression
import org.cangnova.cangjie.cfir.expressions.CfirReturnExpression
import org.cangnova.cangjie.cfir.expressions.CfirSynchronizedExpression
import org.cangnova.cangjie.cfir.expressions.CfirTryExpression
import org.cangnova.cangjie.cfir.expressions.CfirTupleLiteral
import org.cangnova.cangjie.cfir.expressions.CfirTypeConversion
import org.cangnova.cangjie.cfir.expressions.CfirUnsafeExpression
import org.cangnova.cangjie.cfir.expressions.CfirWrappedExpression
import org.cangnova.cangjie.cfir.references.CfirResolvedBuiltinOperatorReference
import org.cangnova.cangjie.cfir.resolve.body.CfirBuiltinOperatorResolver
import org.cangnova.cangjie.cfir.types.BuiltinPrimitiveOperatorOperation
import org.cangnova.cangjie.cfir.types.BuiltinPrimitiveOperators
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.visitors.transformSingle
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeIdealLiteralType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeStubType
import org.cangnova.cangjie.cfir.types.ConeTypeVariableType
import org.cangnova.cangjie.cfir.types.contains
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.IdealTypeResolver
import org.cangnova.cangjie.cfir.resolve.coalescingTargetType
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.resolve.tupleLiteralTargetTypeOrNull
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.arrayLiteralElementType
import org.cangnova.cangjie.cfir.types.optionElementType
import org.cangnova.cangjie.cfir.types.withArguments
import org.cangnova.cangjie.cfir.visitors.CfirTransformer

/**
 * 整数字面量与基础运算符类型近似 transformer。
 *
 * Kotlin 的 literal/operator 近似职责保持在本层；仓颉额外按 tuple、数组和控制流的字段角色
 * 传播结果目标。类型与引用在同一完成边界同步，不重新解析调用、不重建 CFG。
 */
class IntegerLiteralAndOperatorApproximationTransformer(
    /** 当前 CFIR session。 */
    override val session: CfirSession,
    /** 当前 scope session。 */
    override val scopeSession: ScopeSession,
) : CfirTransformer<ConeCangJieType?>(), SessionAndScopeSessionHolder {
    /**
     * Kotlin 分发直接落到 literal/operator 的专用入口；仓颉的 Ideal 也出现在普通表达式树。
     * 因此表达式统一进入近似入口，声明／类型／引用保持原身份，不跨声明传播外层期望类型。
     */
    override fun <E : CfirElement> transformElement(element: E, data: ConeCangJieType?): E {
        @Suppress("UNCHECKED_CAST")
        return if (element is CfirExpression) transformExpression(element, data) as E else element
    }

    /** lambda 拥有独立的签名/正文完成域；近似器不提前解析或重写尚未完成的函数 header。 */
    override fun transformAnonymousFunctionExpression(expression: CfirAnonymousFunctionExpression, data: ConeCangJieType?): CfirExpression =
        expression

    /** 参数列表属于求值子图，必须递归完成其表达式，不能被非表达式默认分发跳过。 */
    override fun transformArgumentList(argumentList: CfirArgumentList, data: ConeCangJieType?): CfirArgumentList =
        argumentList.transformArguments(this, data)

    /** tuple 元素只消费同位置目标，使用正规 replace API 保持字面量节点与 CFG 身份。 */
    override fun transformTupleLiteral(tuple: CfirTupleLiteral, data: ConeCangJieType?): CfirExpression {
        val target = data?.tupleLiteralTargetTypeOrNull(session)
        val elementTargets = target?.elementTypes?.takeIf { it.size == tuple.elements.size }
        tuple.replaceElements(tuple.elements.mapIndexed { index, element ->
            element.transformSingle(this, elementTargets?.get(index))
        })
        // BODY 保存的目标 tuple 可以含 Option/interface 装箱，不能用子表达式未装箱类型覆盖它。
        tuple.coneTypeOrNull?.let { tuple.replaceConeTypeOrNull(it.approximateIntegerLiteralType(data)) }
        return tuple
    }

    /** 已检查的 ? 节点拥有 payload 类型，输入仍是 Option 值，不能当作透明包装。 */
    override fun transformOptionalExpression(expression: CfirOptionalExpression, data: ConeCangJieType?): CfirExpression {
        expression.transformExpression(this, null)
        expression.coneTypeOrNull?.let { expression.replaceConeTypeOrNull(it.approximateIntegerLiteralType(data)) }
        return expression
    }

    /** 整条 optional chain 先独立综合 selector，保留其 lifted Option，外部目标不回传 selector。 */
    override fun transformOptionalChainExpression(expression: CfirOptionalChainExpression, data: ConeCangJieType?): CfirExpression {
        expression.transformExpression(this, null)
        expression.coneTypeOrNull?.let { expression.replaceConeTypeOrNull(IdealTypeResolver.replaceIdealTypes(it)) }
        return expression
    }

    /** Array/VArray 元素目标与整体目标分离，数组种类、尺寸与类型属性继续由原类型拥有。 */
    override fun transformArrayLiteral(array: CfirArrayLiteral, data: ConeCangJieType?): CfirExpression {
        val targetElement = data?.fullyExpandedType(session)?.arrayLiteralElementType
        array.replaceElements(array.elements.map { it.transformSingle(this, targetElement) })
        val oldType = array.coneTypeOrNull
        val oldElement = oldType?.arrayLiteralElementType
        if (oldType != null && oldType !is ConeErrorType && oldElement != null) {
            val newElement = oldElement.approximateIntegerLiteralType(targetElement)
            if (newElement !== oldElement) array.replaceConeTypeOrNull(oldType.withArguments(listOf(newElement)))
        }
        return array
    }

    /** return 的目标来自被绑定函数，自身保持 Nothing；不能继承当前分支的临时结果目标。 */
    override fun transformReturnExpression(expression: CfirReturnExpression, data: ConeCangJieType?): CfirExpression {
        expression.transformResult(this, expression.target.labeledElement.returnTypeRef.coneTypeOrNull)
        return expression
    }

    /** block 的非尾表达式和局部声明分别使用自己的类型语境。 */
    override fun transformBlock(block: CfirBlock, data: ConeCangJieType?): CfirBlock {
        block.replaceStatements(block.statements.mapIndexed { index, statement ->
            when (statement) {
                is CfirReturnExpression -> statement.transformSingle(this, statement.target.labeledElement.returnTypeRef.coneTypeOrNull)
                is CfirExpression -> statement.transformSingle(this, data.takeIf { index == block.statements.lastIndex })
                is CfirVariable -> statement.apply {
                    initializer?.let { replaceInitializer(it.transformSingle(this@IntegerLiteralAndOperatorApproximationTransformer, returnTypeRef.coneTypeOrNull)) }
                }
                // 局部函数由自己的 BODY / completion owner 近似，不继承外层函数返回目标。
                is CfirFunction -> statement
                else -> statement
            }
        })
        block.replaceConeTypeOrNull(block.coneTypeOrNull?.approximateIntegerLiteralType(data))
        return block
    }

    /** 内建运算按已经选择的 signature 定型每个操作数，再同步重建引用与结果类型。 */
    override fun transformFunctionCall(call: CfirFunctionCall, data: ConeCangJieType?): CfirExpression {
        val reference = call.calleeReference as? CfirResolvedBuiltinOperatorReference
        if (reference == null) {
            call.transformChildren(this, null)
            call.replaceConeTypeOrNull(call.coneTypeOrNull?.approximateIntegerLiteralType(data))
            return call
        }
        val signature = reference.signature
        val resultType = call.coneTypeOrNull?.approximateIntegerLiteralType(data)
        val resultKind = BuiltinPrimitiveOperators.primitiveOperandKind(resultType)
        fun operandTarget(kind: PrimitiveTypeKind, receiver: Boolean): ConeCangJieType {
            val resultProvidesTarget = signature.returnKind != PrimitiveTypeKind.BOOLEAN &&
                (receiver || signature.operation !in setOf(BuiltinPrimitiveOperatorOperation.SHIFT_LEFT,
                    BuiltinPrimitiveOperatorOperation.SHIFT_RIGHT, BuiltinPrimitiveOperatorOperation.POWER))
            val expected = resultKind?.takeIf { kind.isIdeal && resultProvidesTarget && !it.isIdeal }?.let(::ConePrimitiveType)
            return ConePrimitiveType(kind).approximateIntegerLiteralType(expected)
        }
        val oldExplicit = call.explicitReceiver
        val oldDispatch = call.dispatchReceiver
        oldExplicit?.let { call.replaceExplicitReceiver(it.transformSingle(this, operandTarget(signature.receiverKind, true))) }
        oldDispatch?.let {
            call.replaceDispatchReceiver(if (it === oldExplicit) call.explicitReceiver else it.transformSingle(this, operandTarget(signature.receiverKind, true)))
        }
        check(signature.parameterKinds.size == call.argumentList.arguments.size) { "Builtin operand arity changed before approximation" }
        signature.parameterKinds.singleOrNull()?.let {
            call.replaceArgumentList(call.argumentList.transformArguments(this, operandTarget(it, false)))
        }
        if (resultType != null && resultType != call.coneTypeOrNull) call.replaceConeTypeOrNull(resultType)
        CfirBuiltinOperatorResolver.completeResolution(call)
        return call
    }

    /**
     * 对位 Kotlin 的具体字面量入口；generated visitor 不经 transformExpression 转发。
     * 只收敛当前字面量的 ideal 类型，保留显式后缀、已固定类型和仍未完成的目标。
     */
    override fun transformLiteralExpression(literalExpression: CfirLiteralExpression, data: ConeCangJieType?): CfirExpression {
        if (data?.contains { it is ConeTypeVariableType || it is ConeStubType || it is ConeErrorType } == true) {
            return literalExpression
        }
        val actualType = literalExpression.coneTypeOrNull ?: return literalExpression
        val approximatedType = actualType.approximateIntegerLiteralType(data)
        if (approximatedType != actualType) literalExpression.replaceConeTypeOrNull(approximatedType)
        return literalExpression
    }

    /** 递归处理表达式子节点，并按期望类型近似当前表达式结果类型。 */
    override fun transformExpression(expression: CfirExpression, data: ConeCangJieType?): CfirExpression {
        when (expression) {
            is CfirIfExpression -> {
                expression.transformCondition(this, ConePrimitiveType(PrimitiveTypeKind.BOOLEAN))
                val branchTarget = data.takeIf { expression.elseBranch != null && expression.coneTypeOrNull != ConePrimitiveType.UNIT }
                expression.transformThenBranch(this, branchTarget)
                expression.transformElseBranch(this, branchTarget)
            }
            is CfirMatchExpression -> {
                expression.transformSubject(this, null)
                expression.transformBranches(this, data)
            }
            is CfirMatchBranch -> {
                expression.transformGuard(this, ConePrimitiveType(PrimitiveTypeKind.BOOLEAN))
                expression.transformBody(this, data)
            }
            is CfirTryExpression -> {
                expression.resources.forEach { resource ->
                    resource.initializer?.let { resource.replaceInitializer(it.transformSingle(this, resource.returnTypeRef.coneTypeOrNull)) }
                }
                val branchTarget = data.takeIf { expression.resources.isEmpty() }
                expression.transformTryBlock(this, branchTarget)
                expression.transformCatches(this, branchTarget)
                expression.transformHandlers(this, null)
                expression.transformFinallyBlock(this, null)
            }
            is CfirCatch -> expression.transformBody(this, data)
            is CfirHandleClause -> expression.transformBody(this, null)
            is CfirForInExpression -> {
                expression.transformIterable(this, null)
                expression.transformPatternGuard(this, ConePrimitiveType(PrimitiveTypeKind.BOOLEAN))
                expression.transformBody(this, null)
            }
            is CfirLoopExpression -> {
                if (expression.isDoWhile) expression.transformBody(this, null)
                expression.transformCondition(this, ConePrimitiveType(PrimitiveTypeKind.BOOLEAN))
                if (!expression.isDoWhile) expression.transformBody(this, null)
            }
            is CfirBinaryOp -> when (expression.kind) {
                CfirBinaryOpKind.AND, CfirBinaryOpKind.OR -> {
                    expression.transformLeft(this, ConePrimitiveType(PrimitiveTypeKind.BOOLEAN))
                    expression.transformRight(this, ConePrimitiveType(PrimitiveTypeKind.BOOLEAN))
                }
                CfirBinaryOpKind.COALESCING -> {
                    expression.transformLeft(this, null)
                    val payload = expression.left.coneTypeOrNull?.fullyExpandedType(session)?.optionElementType
                    expression.transformRight(this, payload?.let { coalescingTargetType(it, data, session) })
                }
                CfirBinaryOpKind.PIPELINE, CfirBinaryOpKind.COMPOSITION -> expression.transformChildren(this, null)
            }
            is CfirAssignment -> {
                expression.transformLValue(this, null)
                expression.transformRValue(this, expression.lValue.coneTypeOrNull)
            }
            // 显式转换改变结果类型，输入仍按自身类型求值，不能提前变成转换目标类型。
            is CfirTypeConversion -> expression.transformArgument(this, null)
            is CfirWrappedExpression -> {
                val originalType = expression.coneTypeOrNull
                expression.transformExpression(this, data)
                if (originalType !is ConeErrorType) expression.replaceConeTypeOrNull(expression.expression.coneTypeOrNull)
            }
            is CfirUnsafeExpression -> expression.transformBody(this, data)
            is CfirSynchronizedExpression -> {
                expression.transformMonitor(this, null)
                expression.transformBody(this, data)
            }
            is CfirAnonymousFunctionExpression -> {
                return expression
            }
            else -> expression.transformChildren(this, null)
        }
        val approximatedType = expression.coneTypeOrNull?.approximateIntegerLiteralType(data)
        if (approximatedType != expression.coneTypeOrNull) {
            expression.replaceConeTypeOrNull(approximatedType)
        }
        return expression
    }

    /** 对外提供单个类型的 ideal literal 近似入口。 */
    fun approximateType(type: ConeCangJieType?, expectedType: ConeCangJieType? = null): ConeCangJieType? {
        return type?.approximateIntegerLiteralType(expectedType)
    }

    /** 根据期望类型把 ideal literal 或 primitive ideal 类型近似为具体类型。 */
    private fun ConeCangJieType.approximateIntegerLiteralType(expectedType: ConeCangJieType?): ConeCangJieType {
        val expandedExpectedType = expectedType?.fullyExpandedType(session)
        return when (this) {
            is ConeIdealLiteralType -> BuiltinPrimitiveOperators.normalizePrimitiveOperand(expandedExpectedType)?.let { expected ->
                IdealTypeResolver.resolveIfIdeal(checkNotNull(BuiltinPrimitiveOperators.normalizePrimitiveOperand(this)), expected)
            } ?: getApproximatedType()
            is ConePrimitiveType -> IdealTypeResolver.resolveIfIdeal(this, BuiltinPrimitiveOperators.normalizePrimitiveOperand(expandedExpectedType))
            is ConeTupleType -> {
                val targets = expectedType?.tupleLiteralTargetTypeOrNull(session)?.elementTypes
                    ?.takeIf { it.size == elementTypes.size }
                val types = elementTypes.mapIndexed { index, type -> type.approximateIntegerLiteralType(targets?.get(index)) }
                if (types.indices.all { types[it] === elementTypes[it] }) this else ConeTupleType(types, attributes)
            }
            else -> this
        }
    }
}
