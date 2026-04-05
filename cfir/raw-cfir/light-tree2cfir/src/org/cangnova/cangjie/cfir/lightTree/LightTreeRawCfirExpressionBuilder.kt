package org.cangnova.cangjie.cfir.lightTree

import com.intellij.lang.LighterASTNode
import com.intellij.psi.tree.IElementType
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.builder.*
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.builder.*
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.expressions.builder.*
import org.cangnova.cangjie.cfir.patterns.*
import org.cangnova.cangjie.cfir.patterns.builder.*
import org.cangnova.cangjie.cfir.references.builder.buildSuperReference
import org.cangnova.cangjie.cfir.references.builder.buildThisReference
import org.cangnova.cangjie.cfir.types.builder.buildImplicitTypeRef
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjNodeTypes
import org.cangnova.cangjie.source.CjFakeSourceElementKind
import org.cangnova.cangjie.source.fakeElement

/**
 * LightTree → Raw CFIR 表达式构建器（对齐 PsiRawCfirBuilder 的表达式转换部分）。
 *
 * 通过 `when(node.tokenType)` 手动分发代替 PSI Visitor 模式，
 * 遍历 LightTree 子节点构建 CFIR 表达式。
 */
class LightTreeRawCfirExpressionBuilder(
    session: CfirSession,
    tree: FlyweightCapableTreeStructure<LighterASTNode>,
    source: CharSequence,
    context: Context<LighterASTNode>,
    private val declarationBuilder: LightTreeRawCfirDeclarationBuilder,
) : AbstractLightTreeRawCfirBuilder(session, tree, source, context) {

    // ===== 公共 API =====

    override fun buildExpression(expression: LighterASTNode): CfirExpression =
        convertExpression(expression)

    fun convertExpression(node: LighterASTNode): CfirExpression = when (node.tokenType) {
        CjNodeTypes.BLOCK, CjNodeTypes.CASE_BLOCK -> convertBlock(node)

        // 字面量
        CjNodeTypes.INTEGER_CONSTANT -> convertLiteral(node, CfirLiteralKind.INT)
        CjNodeTypes.FLOAT_CONSTANT -> convertLiteral(node, CfirLiteralKind.FLOAT)
        CjNodeTypes.RUNE_CONSTANT -> convertLiteral(node, CfirLiteralKind.RUNE)
        CjNodeTypes.BOOLEAN_CONSTANT -> convertBooleanLiteral(node)
        CjNodeTypes.UNIT_CONSTANT -> buildLiteralExpression {
            source = node.toSource(); kind = CfirLiteralKind.UNIT; value = null
        }
        CjNodeTypes.STRING_TEMPLATE -> convertStringTemplate(node)

        // 二元/一元
        CjNodeTypes.BINARY_EXPRESSION -> convertBinary(node)
        CjNodeTypes.RANGE_EXPRESSION -> convertRange(node)
        CjNodeTypes.PREFIX_EXPRESSION -> convertPrefix(node)
        CjNodeTypes.POSTFIX_EXPRESSION -> convertPostfix(node)

        // 访问
        CjNodeTypes.DOT_QUALIFIED_EXPRESSION -> convertDotQualified(node)
        CjNodeTypes.SAFE_ACCESS_EXPRESSION -> convertDotQualified(node)
        CjNodeTypes.REFERENCE_EXPRESSION -> convertNameReference(node)

        // 调用
        CjNodeTypes.CALL_EXPRESSION -> convertCall(node)
        CjNodeTypes.SPAWN_EXPRESSION -> convertSpawn(node)

        // 控制流
        CjNodeTypes.IF -> convertIf(node)
        CjNodeTypes.MATCH -> convertMatch(node)
        CjNodeTypes.FOR -> convertFor(node)
        CjNodeTypes.WHILE -> convertWhile(node)
        CjNodeTypes.DO_WHILE -> convertDoWhile(node)

        // 跳转 / 异常
        CjNodeTypes.RETURN -> convertReturn(node)
        CjNodeTypes.BREAK -> buildJumpExpression { source = node.toSource(); kind = CfirJumpKind.BREAK }
        CjNodeTypes.CONTINUE -> buildJumpExpression { source = node.toSource(); kind = CfirJumpKind.CONTINUE }
        CjNodeTypes.THROW -> convertThrow(node)
        CjNodeTypes.TRY -> convertTry(node)

        // Lambda
        CjNodeTypes.LAMBDA_EXPRESSION -> convertLambda(node)

        // 括号
        CjNodeTypes.PARENTHESIZED -> {
            val inner = findFirstExpression(node)
            if (inner != null) convertExpression(inner) else buildErrorExpression(node.toSourceElement(), "Empty parenthesized expression")
        }

        // 下标 / 集合 / 元组
        CjNodeTypes.ARRAY_ACCESS_EXPRESSION -> convertSubscript(node)
        CjNodeTypes.COLLECTION_LITERAL_EXPRESSION -> convertArrayLiteral(node)
        CjNodeTypes.TUPLE_EXPRESSION -> convertTupleLiteral(node)

        // 类型检查
        CjNodeTypes.IS_EXPRESSION -> convertTypeCheck(node)

        // 同步 / 不安全 / Quote / 宏
        CjNodeTypes.SYNCHRONIZED_EXPRESSION -> convertSynchronized(node)
        CjNodeTypes.UNSAFE_EXPRESSION -> convertUnsafe(node)
        CjNodeTypes.QUOTE_EXPRESSION -> convertQuote(node)
        CjNodeTypes.MACRO_EXPRESSION -> convertMacroExpression(node)

        // this / super
        CjNodeTypes.THIS_EXPRESSION -> buildThisReceiverExpression {
            source = node.toSource()
            calleeReference = buildThisReference {
                source = node.toSource()
                isImplicit = false
            }
        }
        CjNodeTypes.SUPER_EXPRESSION -> convertSuperExpression(node)

        else -> buildErrorExpression(node.toSourceElement(), "Unsupported expression: ${node.tokenType}")
    }

    // ===== Block =====

    fun convertBlock(node: LighterASTNode): CfirBlock {
        val statements = withLocalContext {
            val stmts = mutableListOf<CfirElement>()
            tree.forEachChildren(node) { child ->
                val tt = child.tokenType
                when {
                    isDeclarationToken(tt) -> stmts.add(declarationBuilder.convertDeclaration(child))
                    isExpressionToken(tt) -> stmts.add(convertExpression(child))
                }
            }
            stmts
        }
        return buildBlock {
            source = node.toSource()
            this.statements.addAll(statements)
        }
    }

    // ===== Literal =====

    private fun convertLiteral(node: LighterASTNode, kind: CfirLiteralKind): CfirLiteralExpression {
        val text = node.asText()
        return buildLiteralExpression {
            source = node.toSource()
            this.kind = kind
            this.value = text
        }
    }

    private fun convertBooleanLiteral(node: LighterASTNode): CfirLiteralExpression {
        val text = node.asText()
        return buildLiteralExpression {
            source = node.toSource()
            kind = CfirLiteralKind.BOOLEAN
            value = text == "true"
        }
    }

    private fun convertStringTemplate(node: LighterASTNode): CfirExpression {
        val parts = mutableListOf<CfirExpression>()
        var hasInterpolation = false
        tree.forEachChildren(node) { child ->
            when (child.tokenType) {
                CjNodeTypes.LITERAL_STRING_TEMPLATE_ENTRY,
                CjNodeTypes.ESCAPE_STRING_TEMPLATE_ENTRY -> {
                    parts.add(buildLiteralExpression {
                        source = child.toSource()
                        kind = CfirLiteralKind.STRING
                        value = child.asText()
                    })
                }
                CjNodeTypes.SHORT_STRING_TEMPLATE_ENTRY,
                CjNodeTypes.LONG_STRING_TEMPLATE_ENTRY -> {
                    hasInterpolation = true
                    val expr = findFirstExpression(child)
                    if (expr != null) {
                        parts.add(convertExpression(expr))
                    }
                }
            }
        }
        if (!hasInterpolation) {
            // 无插值：合并为单个字符串字面量
            val text = parts.joinToString("") { (it as? CfirLiteralExpression)?.value?.toString() ?: "" }
            return buildLiteralExpression {
                source = node.toSource()
                kind = CfirLiteralKind.STRING
                value = text
            }
        }
        return buildStringInterpolation {
            source = node.toSource()
            this.parts.addAll(parts)
        }
    }

    // ===== Binary & Unary =====

    private fun convertBinary(node: LighterASTNode): CfirExpression {
        var left: LighterASTNode? = null
        var right: LighterASTNode? = null
        var opToken: IElementType? = null

        tree.forEachChildren(node) { child ->
            when (child.tokenType) {
                CjNodeTypes.OPERATION_REFERENCE -> {
                    // OPERATION_REFERENCE 内部包含实际操作符 Token
                    tree.forEachChildren(child) { opChild ->
                        opToken = opChild.tokenType
                    }
                }
                else -> if (isExpressionToken(child.tokenType)) {
                    if (left == null) left = child
                    else if (opToken != null && right == null) right = child
                }
            }
        }

        val leftExpr = left?.let { convertExpression(it) }
            ?: return buildErrorExpression(node.toSourceElement(), "Missing left operand")
        val rightExpr = right?.let { convertExpression(it) }
            ?: return buildErrorExpression(node.toSourceElement(), "Missing right operand")
        val op = opToken ?: return buildErrorExpression(node.toSourceElement(), "Missing operator")

        // 赋值
        if (op.isAssignmentToken()) {
            if (op == CjTokens.EQ) {
                return buildAssignment {
                    source = node.toSource()
                    lValue = leftExpr
                    rValue = rightExpr
                }
            }
            val opName = op.toCompoundAssignName()?.asString() ?: "<error>"
            return buildAssignment {
                source = node.toSource()
                lValue = leftExpr
                rValue = buildFunctionCall {
                    source = node.toSource()
                    calleeReference = buildNamedReference(Name.identifier(opName), node.toSource())
                    argumentList = buildArgumentList {
                        arguments.add(rightExpr)
                    }
                    explicitReceiver = leftExpr
                    origin = CfirFunctionCallOrigin.Operator
                }
            }
        }

        // 逻辑/空合/管道
        op.toBinaryOpKind()?.let { kind ->
            return buildBinaryOp {
                source = node.toSource()
                this.kind = kind
                this.left = leftExpr
                this.right = rightExpr
            }
        }

        // 比较
        op.toComparisonOp()?.let { compOp ->
            return buildComparisonExpression {
                source = node.toSource()
                operation = compOp
                this.left = leftExpr
                this.right = rightExpr
            }
        }

        // 可重载运算符 → 函数调用
        val operatorName = op.toBinaryName() ?: Name.identifier("<op:$op>")
        return buildFunctionCall {
            source = node.toSource()
            calleeReference = buildNamedReference(operatorName, node.toSource())
            argumentList = buildArgumentList {
                arguments.add(rightExpr)
            }
            explicitReceiver = leftExpr
            origin = CfirFunctionCallOrigin.Operator
        }
    }

    private fun convertRange(node: LighterASTNode): CfirRangeExpression {
        var left: LighterASTNode? = null
        var right: LighterASTNode? = null
        var isInclusive = false

        tree.forEachChildren(node) { child ->
            when (child.tokenType) {
                CjNodeTypes.OPERATION_REFERENCE -> {
                    tree.forEachChildren(child) { opChild ->
                        if (opChild.tokenType == CjTokens.RANGEEQ) isInclusive = true
                    }
                }
                else -> if (isExpressionToken(child.tokenType)) {
                    if (left == null) left = child
                    else right = child
                }
            }
        }

        val start = left?.let { convertExpression(it) }
            ?: buildErrorExpression(reason = "Missing range start")
        val end = right?.let { convertExpression(it) }
            ?: buildErrorExpression(reason = "Missing range end")

        return buildRangeExpression {
            source = node.toSource()
            this.start = start
            this.end = end
            this.isInclusive = isInclusive
        }
    }

    private fun convertPrefix(node: LighterASTNode): CfirExpression {
        var opToken: IElementType? = null
        var operandNode: LighterASTNode? = null

        tree.forEachChildren(node) { child ->
            when (child.tokenType) {
                CjNodeTypes.OPERATION_REFERENCE -> {
                    tree.forEachChildren(child) { opChild ->
                        opToken = opChild.tokenType
                    }
                }
                else -> if (isExpressionToken(child.tokenType)) { operandNode = child }
            }
        }

        val base = operandNode?.let { convertExpression(it) }
            ?: return buildErrorExpression(node.toSourceElement(), "Missing prefix operand")
        val opName = opToken?.toPrefixUnaryName() ?: Name.identifier("<prefix>")
        return buildFunctionCall {
            source = node.toSource()
            calleeReference = buildNamedReference(opName, node.toSource())
            argumentList = buildArgumentList()
            explicitReceiver = base
            origin = CfirFunctionCallOrigin.Operator
        }
    }

    private fun convertPostfix(node: LighterASTNode): CfirExpression {
        var opToken: IElementType? = null
        var operandNode: LighterASTNode? = null

        tree.forEachChildren(node) { child ->
            when (child.tokenType) {
                CjNodeTypes.OPERATION_REFERENCE -> {
                    tree.forEachChildren(child) { opChild ->
                        opToken = opChild.tokenType
                    }
                }
                else -> if (isExpressionToken(child.tokenType)) { operandNode = child }
            }
        }

        val base = operandNode?.let { convertExpression(it) }
            ?: return buildErrorExpression(node.toSourceElement(), "Missing postfix operand")
        val opName = opToken?.toPostfixUnaryName() ?: Name.identifier("<postfix>")
        return buildFunctionCall {
            source = node.toSource()
            calleeReference = buildNamedReference(opName, node.toSource())
            argumentList = buildArgumentList()
            explicitReceiver = base
            origin = CfirFunctionCallOrigin.Operator
        }
    }

    // ===== Call & Access =====

    private fun convertCall(node: LighterASTNode): CfirExpression {
        var calleeNode: LighterASTNode? = null
        val argNodes = mutableListOf<LighterASTNode>()
        val typeArgNodes = mutableListOf<LighterASTNode>()
        val lambdaArgNodes = mutableListOf<LighterASTNode>()

        tree.forEachChildren(node) { child ->
            when (child.tokenType) {
                CjNodeTypes.VALUE_ARGUMENT_LIST -> {
                    tree.forEachChildren(child) { arg ->
                        if (arg.tokenType == CjNodeTypes.VALUE_ARGUMENT) {
                            argNodes.add(arg)
                        }
                    }
                }
                CjNodeTypes.TYPE_ARGUMENT_LIST -> {
                    tree.forEachChildren(child) { typeArg ->
                        if (typeArg.tokenType == CjNodeTypes.TYPE_PROJECTION) {
                            val typeRef = tree.findChildByType(typeArg, CjNodeTypes.TYPE_REFERENCE)
                            if (typeRef != null) typeArgNodes.add(typeRef)
                        }
                    }
                }
                CjNodeTypes.LAMBDA_ARGUMENT -> lambdaArgNodes.add(child)
                else -> {
                    if (calleeNode == null && isExpressionToken(child.tokenType)) {
                        calleeNode = child
                    }
                }
            }
        }

        val callArguments = argNodes.mapNotNull { convertCallArgument(it) }.toMutableList()
        val directTypeArgs = typeArgNodes.map { typeRefNode ->
            convertTypeReference(typeRefNode, tree, source) { it.toSourceElement() }
        }
        val typeArgs = if (directTypeArgs.isNotEmpty()) {
            directTypeArgs
        } else {
            collectTypeArgumentsFromCallee(calleeNode)
        }
        val lambdaArgs = lambdaArgNodes.mapNotNull { lambdaArg ->
            val lambdaExpr = tree.findChildByType(lambdaArg, CjNodeTypes.LAMBDA_EXPRESSION)
            lambdaExpr?.let { convertLambda(it) }
        }
        callArguments.addAll(lambdaArgs)

        val (receiver, reference) = resolveCalleeReference(calleeNode)

        return buildFunctionCall {
            source = node.toSource()
            calleeReference = reference
            argumentList = buildArgumentList {
                arguments.addAll(callArguments)
            }
            explicitReceiver = receiver
            typeArguments.addAll(typeArgs)
            origin = CfirFunctionCallOrigin.Regular
        }
    }

    /**
     * LightTree 路径下同样需要保留 named argument 的外层语法，
     * 这样参数映射阶段才能在不依赖 PSI 的情况下恢复参数名前缀。
     */
    private fun convertCallArgument(valueArgumentNode: LighterASTNode): CfirExpression? {
        val expressionNode = findFirstExpression(valueArgumentNode) ?: return null
        val convertedExpression = convertExpression(expressionNode)
        val hasName = tree.findChildByType(valueArgumentNode, CjNodeTypes.VALUE_ARGUMENT_NAME) != null
        if (!hasName) return convertedExpression

        return buildWrappedExpression {
            source = valueArgumentNode.toSource()
            expression = convertedExpression
        }
    }

    private fun resolveCalleeReference(calleeNode: LighterASTNode?): Pair<CfirExpression?, org.cangnova.cangjie.cfir.references.CfirNamedReference> {
        if (calleeNode == null) {
            return null to buildNamedReference(Name.identifier("<error>"))
        }
        return when (calleeNode.tokenType) {
            CjNodeTypes.REFERENCE_EXPRESSION -> {
                null to buildNamedReference(referenceNameFromText(calleeNode.asText()), calleeNode.toSource())
            }
            CjNodeTypes.DOT_QUALIFIED_EXPRESSION -> {
                var receiverNode: LighterASTNode? = null
                var selectorNode: LighterASTNode? = null
                var afterDot = false
                tree.forEachChildren(calleeNode) { child ->
                    val tt = child.tokenType
                    when {
                        tt == CjTokens.DOT -> afterDot = true
                        !afterDot && isSemanticToken(tt) -> receiverNode = child
                        afterDot && selectorNode == null && isSemanticToken(tt) -> selectorNode = child
                    }
                }
                val recv = receiverNode?.let { convertExpression(it) }
                val refName = if (selectorNode?.tokenType == CjNodeTypes.REFERENCE_EXPRESSION) {
                    selectorNode!!.asText()
                } else {
                    selectorNode?.asText() ?: "<error>"
                }
                recv to buildNamedReference(referenceNameFromText(refName), selectorNode?.toSource() ?: calleeNode.toSource())
            }
            else -> null to buildNamedReference(referenceNameFromText(calleeNode.asText()), calleeNode.toSource())
        }
    }

    private fun convertSpawn(node: LighterASTNode): CfirExpression {
        val lambdaNode = tree.findChildByType(node, CjNodeTypes.LAMBDA_EXPRESSION)
        val body = if (lambdaNode != null) {
            val bodyNode = tree.findChildByType(lambdaNode, CjNodeTypes.BLOCK)
            bodyNode?.let { convertBlock(it) } ?: buildBlock { source = lambdaNode.toSource() }
        } else {
            buildBlock { source = node.toSource() }
        }
        return buildSpawnExpression { source = node.toSource(); this.body = body }
    }

    private fun convertDotQualified(node: LighterASTNode): CfirExpression {
        var receiverNode: LighterASTNode? = null
        var selectorNode: LighterASTNode? = null
        var afterDot = false

        tree.forEachChildren(node) { child ->
            val tt = child.tokenType
            when {
                tt == CjTokens.DOT || tt == CjTokens.QUEST -> afterDot = true
                !afterDot && isSemanticToken(tt) -> receiverNode = child
                afterDot && selectorNode == null && isSemanticToken(tt) -> selectorNode = child
            }
        }

        val receiver = receiverNode?.let { convertExpression(it) }
            ?: return buildErrorExpression(node.toSourceElement(), "Missing receiver")
        val selector = selectorNode
            ?: return buildErrorExpression(node.toSourceElement(), "Missing selector")

        // selector 为 CALL_EXPRESSION
        if (selector.tokenType == CjNodeTypes.CALL_EXPRESSION) {
            var calleeRef: LighterASTNode? = null
            val argNodes = mutableListOf<LighterASTNode>()
            val typeArgNodes = mutableListOf<LighterASTNode>()
            val lambdaArgNodes = mutableListOf<LighterASTNode>()

            tree.forEachChildren(selector) { child ->
                when (child.tokenType) {
                    CjNodeTypes.VALUE_ARGUMENT_LIST -> {
                        tree.forEachChildren(child) { arg ->
                            if (arg.tokenType == CjNodeTypes.VALUE_ARGUMENT) {
                                argNodes.add(arg)
                            }
                        }
                    }
                    CjNodeTypes.TYPE_ARGUMENT_LIST -> {
                        tree.forEachChildren(child) { typeArg ->
                            if (typeArg.tokenType == CjNodeTypes.TYPE_PROJECTION) {
                                val typeRef = tree.findChildByType(typeArg, CjNodeTypes.TYPE_REFERENCE)
                                if (typeRef != null) typeArgNodes.add(typeRef)
                            }
                        }
                    }
                    CjNodeTypes.LAMBDA_ARGUMENT -> lambdaArgNodes.add(child)
                    else -> {
                        if (calleeRef == null && isExpressionToken(child.tokenType)) {
                            calleeRef = child
                        }
                    }
                }
            }

            val ref = if (calleeRef?.tokenType == CjNodeTypes.REFERENCE_EXPRESSION) {
                buildNamedReference(referenceNameFromText(calleeRef!!.asText()), calleeRef!!.toSource())
            } else {
                buildNamedReference(referenceNameFromText(calleeRef?.asText() ?: "<error>"), calleeRef?.toSource() ?: selector.toSource())
            }
            val callArguments = argNodes.mapNotNull { convertCallArgument(it) }.toMutableList()
            val directTypeArgs = typeArgNodes.map { typeRefNode ->
                convertTypeReference(typeRefNode, tree, source) { it.toSourceElement() }
            }
            val typeArgs = if (directTypeArgs.isNotEmpty()) {
                directTypeArgs
            } else {
                collectTypeArgumentsFromCallee(calleeRef)
            }
            val lambdaArgs = lambdaArgNodes.mapNotNull { lambdaArg ->
                val lambdaExpr = tree.findChildByType(lambdaArg, CjNodeTypes.LAMBDA_EXPRESSION)
                lambdaExpr?.let { convertLambda(it) }
            }
            callArguments.addAll(lambdaArgs)

            return buildFunctionCall {
                source = node.toSource()
                calleeReference = ref
                argumentList = buildArgumentList {
                    arguments.addAll(callArguments)
                }
                explicitReceiver = receiver
                typeArguments.addAll(typeArgs)
                origin = CfirFunctionCallOrigin.Regular
            }
        }

        // selector 为简单名称引用
        if (selector.tokenType == CjNodeTypes.REFERENCE_EXPRESSION) {
            val typeArgs = collectReferenceTypeArguments(selector)
            if (typeArgs.isNotEmpty()) {
                return buildNamedAccessExpression {
                    source = node.toSource()
                    calleeReference = buildNamedReference(referenceNameFromText(selector.asText()), selector.toSource())
                    explicitReceiver = receiver
                    this.typeArguments.addAll(typeArgs)
                }
            }
            return buildNamedAccessExpression {
                source = node.toSource()
                calleeReference = buildNamedReference(referenceNameFromText(selector.asText()), selector.toSource())
                explicitReceiver = receiver
            }
        }

        return buildErrorExpression(node.toSourceElement(), "Unsupported selector: ${selector.tokenType}")
    }

    private fun convertNameReference(node: LighterASTNode): CfirExpression {
        val referencedName = referenceNameFromText(node.asText())
        val typeArguments = collectReferenceTypeArguments(node)
        if (referencedName.asString() == "this" && typeArguments.isEmpty()) {
            return buildThisReceiverExpression {
                source = node.toSource()
                calleeReference = buildThisReference {
                    source = node.toSource()
                    isImplicit = false
                }
            }
        }

        return buildNamedAccessExpression {
            source = node.toSource()
            calleeReference = buildNamedReference(referencedName, node.toSource())
            this.typeArguments.addAll(typeArguments)
        }
    }

    /**
     * 将 `super` 表达式构造成专用接收者节点，
     * 这样 body resolve 可以按仓颉语义统一推导直接父类型，而不是退化成普通名字访问。
     */
    private fun convertSuperExpression(node: LighterASTNode): CfirSuperReceiverExpression {
        val sourceElement = node.toSource()
        return buildSuperReceiverExpression {
            source = sourceElement
            calleeReference = buildSuperReference {
                source = sourceElement.fakeElement(CjFakeSourceElementKind.ReferenceInAtomicQualifiedAccess)
                superTypeRef = buildImplicitTypeRef()
            }
        }
    }

    private fun collectReferenceTypeArguments(node: LighterASTNode): List<org.cangnova.cangjie.cfir.types.CfirTypeRef> {
        if (node.tokenType != CjNodeTypes.REFERENCE_EXPRESSION) return emptyList()

        val typeArgNodes = mutableListOf<LighterASTNode>()
        tree.forEachChildren(node) { child ->
            if (child.tokenType != CjNodeTypes.TYPE_ARGUMENT_LIST) return@forEachChildren
            tree.forEachChildren(child) { typeArg ->
                if (typeArg.tokenType == CjNodeTypes.TYPE_PROJECTION) {
                    val typeRef = tree.findChildByType(typeArg, CjNodeTypes.TYPE_REFERENCE)
                    if (typeRef != null) {
                        typeArgNodes.add(typeRef)
                    }
                }
            }
        }

        return typeArgNodes.map { typeRefNode ->
            convertTypeReference(typeRefNode, tree, source) { it.toSourceElement() }
        }
    }

    private fun collectTypeArgumentsFromCallee(calleeNode: LighterASTNode?): List<org.cangnova.cangjie.cfir.types.CfirTypeRef> {
        calleeNode ?: return emptyList()
        return when (calleeNode.tokenType) {
            CjNodeTypes.REFERENCE_EXPRESSION -> collectReferenceTypeArguments(calleeNode)
            CjNodeTypes.DOT_QUALIFIED_EXPRESSION,
            CjNodeTypes.SAFE_ACCESS_EXPRESSION -> {
                var selectorNode: LighterASTNode? = null
                var afterDot = false
                tree.forEachChildren(calleeNode) { child ->
                    val tt = child.tokenType
                    when {
                        tt == CjTokens.DOT || tt == CjTokens.QUEST -> afterDot = true
                        afterDot && selectorNode == null && isSemanticToken(tt) -> selectorNode = child
                    }
                }
                val selectorReference = selectorNode?.takeIf { it.tokenType == CjNodeTypes.REFERENCE_EXPRESSION }
                if (selectorReference != null) collectReferenceTypeArguments(selectorReference) else emptyList()
            }

            else -> emptyList()
        }
    }

    // ===== Control Flow =====

    private fun convertIf(node: LighterASTNode): CfirIfExpression {
        var conditionNode: LighterASTNode? = null
        var thenNode: LighterASTNode? = null
        var elseNode: LighterASTNode? = null

        tree.forEachChildren(node) { child ->
            when (child.tokenType) {
                CjNodeTypes.CONDITION -> conditionNode = findFirstExpression(child)
                CjNodeTypes.THEN -> thenNode = findFirstExpression(child)
                CjNodeTypes.ELSE -> elseNode = findFirstExpression(child)
            }
        }

        val condition = conditionNode?.let { convertExpression(it) }
            ?: buildErrorExpression(reason = "Missing if condition")
        val thenBranch = thenNode?.let { toBlock(it) } ?: buildBlock { source = node.toSource() }
        val elseBranch = elseNode?.let { convertExpression(it) }

        return buildIfExpression {
            source = node.toSource()
            this.condition = condition
            this.thenBranch = thenBranch
            this.elseBranch = elseBranch
        }
    }

    private fun convertMatch(node: LighterASTNode): CfirMatchExpression {
        var subjectNode: LighterASTNode? = null
        val entryNodes = mutableListOf<LighterASTNode>()

        tree.forEachChildren(node) { child ->
            when (child.tokenType) {
                CjNodeTypes.MATCH_ENTRY -> entryNodes.add(child)
                CjNodeTypes.CONDITION -> {
                    subjectNode = findFirstExpression(child)
                }
                else -> {
                    if (subjectNode == null && isExpressionToken(child.tokenType)) {
                        subjectNode = child
                    }
                }
            }
        }

        // subject 允许为 null（无主语 match）
        val subject = subjectNode?.let { convertExpression(it) }
        val hasSubject = subject != null

        val branches = entryNodes.map { entry -> convertMatchEntry(entry, hasSubject) }

        return buildMatchExpression {
            source = node.toSource()
            this.subject = subject
            this.branches.addAll(branches)
        }
    }

    private fun convertMatchEntry(entry: LighterASTNode, hasSubject: Boolean): CfirMatchBranch {
        // 收集所有 condition/pattern 节点（| 分隔的多个）
        val conditionNodes = mutableListOf<LighterASTNode>()
        var bodyNode: LighterASTNode? = null
        var guardNode: LighterASTNode? = null
        var isElse = false

        tree.forEachChildren(entry) { child ->
            when (child.tokenType) {
                CjNodeTypes.MATCH_CONDITION_EXPRESSION -> conditionNodes.add(child)
                CjNodeTypes.BLOCK, CjNodeTypes.CASE_BLOCK -> bodyNode = child
                CjNodeTypes.PATTERN_GUARD -> guardNode = child
                CjTokens.ELSE_KEYWORD -> isElse = true
                else -> {
                    if (isPatternToken(child.tokenType)) {
                        conditionNodes.add(child)
                    } else if (bodyNode == null && isExpressionToken(child.tokenType)) {
                        bodyNode = child
                    }
                }
            }
        }

        val pattern = when {
            isElse -> buildWildcardPattern { source = entry.toSource() }

            hasSubject -> when {
                conditionNodes.isEmpty() -> buildWildcardPattern { source = entry.toSource() }
                conditionNodes.size == 1 -> convertMatchCondition(conditionNodes.first())
                else -> buildOrPattern {
                    source = entry.toSource()
                    alternatives.addAll(conditionNodes.map { convertMatchCondition(it) })
                }
            }

            else -> {
                // 无主语：条件表达式包装成 ExpressionPattern
                val first = conditionNodes.firstOrNull()
                if (first != null) {
                    val expr = findFirstExpression(first) ?: first.takeIf { isExpressionToken(it.tokenType) }
                    if (expr != null) {
                        buildExpressionPattern {
                            source = entry.toSource()
                            expression = convertExpression(expr)
                        }
                    } else {
                        buildWildcardPattern { source = entry.toSource() }
                    }
                } else {
                    buildWildcardPattern { source = entry.toSource() }
                }
            }
        }

        val guard = guardNode?.let { guardBlock ->
            val guardExpr = findFirstExpression(guardBlock)
            guardExpr?.let { convertExpression(it) }
        }

        val body = bodyNode?.let { toBlock(it) } ?: buildBlock { source = entry.toSource() }

        return buildMatchBranch {
            source = entry.toSource()
            this.pattern = pattern
            this.guard = guard
            this.body = body
        }
    }

    private fun convertMatchCondition(node: LighterASTNode): CfirPattern {
        // 优先作为模式处理（对齐仓颉语义：case 后面是模式）
        if (isPatternToken(node.tokenType)) {
            return convertPattern(node)
        }
        // 在子节点中查找 pattern
        tree.forEachChildren(node) { child ->
            if (isPatternToken(child.tokenType)) {
                return convertPattern(child)
            }
        }
        // 回退：将表达式包装为 constPattern（如 case score < 60）
        val expr = findFirstExpression(node) ?: node.takeIf { isExpressionToken(it.tokenType) }
        if (expr != null) {
            return buildConstPattern {
                source = expr.toSource()
                expression = convertExpression(expr)
            }
        }
        return buildWildcardPattern { source = node.toSource() }
    }

    // ===== Pattern =====

    fun convertPattern(node: LighterASTNode): CfirPattern = when (node.tokenType) {
        CjNodeTypes.BINDING_PATTERN -> {
            val nameNode = tree.findChildByType(node, CjTokens.IDENTIFIER)
                ?: tree.findChildByType(node, CjNodeTypes.REFERENCE_EXPRESSION)
            val nameText = nameNode?.asText()
            buildBindingPattern {
                source = node.toSource()
                name = if (!nameText.isNullOrEmpty()) Name.identifier(nameText) else Name.special("<error>")
            }
        }
        CjNodeTypes.TYPE_PATTERN -> {
            val typeRef = tree.findChildByType(node, CjNodeTypes.TYPE_REFERENCE)
            val nameNode = tree.findChildByType(node, CjTokens.IDENTIFIER)
            buildTypePattern {
                source = node.toSource()
                    this.typeRef = convertTypeReference(typeRef, tree, this@LightTreeRawCfirExpressionBuilder.source) {
                        it.toSourceElement()
                    }
                bindingName = nameNode?.let { Name.identifier(it.asText()) }
            }
        }
        CjNodeTypes.TUPLE_PATTERN -> {
            val elements = mutableListOf<CfirPattern>()
            tree.forEachChildren(node) { child ->
                if (isPatternToken(child.tokenType)) {
                    elements.add(convertPattern(child))
                }
            }
            buildTuplePattern {
                source = node.toSource()
                this.elements.addAll(elements)
            }
        }
        CjNodeTypes.ENUM_PATTERN -> {
            val refExpr = findFirstExpression(node)
            val subPatterns = mutableListOf<CfirPattern>()
            tree.forEachChildren(node) { child ->
                if (isPatternToken(child.tokenType)) {
                    subPatterns.add(convertPattern(child))
                }
            }
            val refText = refExpr?.asText() ?: "<enum-pattern>"
            buildEnumPattern {
                source = node.toSource()
                constructorReference = buildNamedReference(
                    if (refText.startsWith("<")) Name.special(refText) else Name.identifier(refText),
                    refExpr?.toSource() ?: node.toSource(),
                )
                arguments.addAll(subPatterns)
            }
        }
        CjNodeTypes.CONSTANT_PATTERN -> {
            val expr = findFirstExpression(node)
            buildConstPattern {
                source = node.toSource()
                expression = expr?.let { convertExpression(it) }
                    ?: buildErrorExpression(node.toSourceElement(), "Missing constant pattern expression")
            }
        }
        CjNodeTypes.WILDCARD_PATTERN -> buildWildcardPattern { source = node.toSource() }
        else -> buildWildcardPattern { source = node.toSource() }
    }

    // ===== Loops =====

    private fun convertFor(node: LighterASTNode): CfirForInExpression {
        var paramNode: LighterASTNode? = null
        var rangeNode: LighterASTNode? = null
        var bodyNode: LighterASTNode? = null

        tree.forEachChildren(node) { child ->
            when (child.tokenType) {
                CjNodeTypes.VALUE_PARAMETER -> paramNode = child
                CjNodeTypes.LOOP_RANGE -> rangeNode = findFirstExpression(child)
                CjNodeTypes.BODY -> bodyNode = findFirstExpression(child)
            }
        }

        val paramName = paramNode?.let {
            val nameNode = tree.findChildByType(it, CjTokens.IDENTIFIER)
            nameNode?.asText()
        }
        val paramTypeRef = paramNode?.let {
            val typeRef = tree.findChildByType(it, CjNodeTypes.TYPE_REFERENCE)
            convertTypeReference(typeRef, tree, source) { n -> n.toSourceElement() }
        } ?: buildImplicitTypeRef()

        val variableName = if (paramName != null) Name.identifier(paramName) else Name.special("<anonymous>")
        val variable = buildSourceDeclaration(CfirPatternVariableSymbol(callableIdFor(variableName))) { symbol ->
            buildPatternVariable {
                source = (paramNode ?: node).toSource()
                this.symbol = symbol
                origin = CfirDeclarationOrigin.Source
                moduleData = baseModuleData
                attributes = CfirDeclarationAttributes.EMPTY
                isLocal = true
                status = CfirDeclarationStatusImpl.DEFAULT
                returnTypeRef = paramTypeRef
                pattern = buildBindingPattern {
                    source = (paramNode ?: node).toSource()
                    name = variableName
                    typeRef = paramTypeRef
                }
                isVar = false
            }
        }

        val iterable = rangeNode?.let { convertExpression(it) }
            ?: buildErrorExpression(reason = "Missing for-in iterable")
        val body = bodyNode?.let { toBlock(it) } ?: buildBlock { source = node.toSource() }

        return buildForInExpression {
            source = node.toSource()
            this.condition = buildLiteralExpression {
                source = node.toSource()
                kind = CfirLiteralKind.BOOLEAN
                value = true
            }
            this.isDoWhile = false
            this.variable = variable
            this.iterable = iterable
            this.body = body
        }
    }

    private fun convertWhile(node: LighterASTNode): CfirLoopExpression {
        var condNode: LighterASTNode? = null
        var bodyNode: LighterASTNode? = null

        tree.forEachChildren(node) { child ->
            when (child.tokenType) {
                CjNodeTypes.CONDITION -> condNode = findFirstExpression(child)
                CjNodeTypes.BODY -> bodyNode = findFirstExpression(child)
            }
        }

        val condition = condNode?.let { convertExpression(it) }
            ?: buildErrorExpression(reason = "Missing while condition")
        return buildLoopExpression {
            source = node.toSource()
            this.condition = condition
            this.body = bodyNode?.let { toBlock(it) } ?: buildBlock { source = node.toSource() }
            isDoWhile = false
        }
    }

    private fun convertDoWhile(node: LighterASTNode): CfirLoopExpression {
        var condNode: LighterASTNode? = null
        var bodyNode: LighterASTNode? = null

        tree.forEachChildren(node) { child ->
            when (child.tokenType) {
                CjNodeTypes.CONDITION -> condNode = findFirstExpression(child)
                CjNodeTypes.BODY -> bodyNode = findFirstExpression(child)
            }
        }

        val condition = condNode?.let { convertExpression(it) }
            ?: buildErrorExpression(reason = "Missing do-while condition")
        return buildLoopExpression {
            source = node.toSource()
            this.condition = condition
            this.body = bodyNode?.let { toBlock(it) } ?: buildBlock { source = node.toSource() }
            isDoWhile = true
        }
    }

    // ===== Jump & Exception =====

    private fun convertReturn(node: LighterASTNode): CfirReturnExpression {
        val resultExpr = findFirstExpression(node)
        return buildReturnExpression {
            source = node.toSource()
            result = resultExpr?.let { convertExpression(it) }
        }
    }

    private fun convertThrow(node: LighterASTNode): CfirThrowExpression {
        val exprNode = findFirstExpression(node)
        val exception = exprNode?.let { convertExpression(it) }
            ?: buildErrorExpression(node.toSourceElement(), "Missing thrown expression")
        return buildThrowExpression {
            source = node.toSource()
            this.exception = exception
        }
    }

    private fun convertTry(node: LighterASTNode): CfirTryExpression {
        var tryBlockNode: LighterASTNode? = null
        val catchNodes = mutableListOf<LighterASTNode>()
        var finallyNode: LighterASTNode? = null

        tree.forEachChildren(node) { child ->
            when (child.tokenType) {
                CjNodeTypes.BLOCK -> {
                    if (tryBlockNode == null) tryBlockNode = child
                }
                CjNodeTypes.CATCH -> catchNodes.add(child)
                CjNodeTypes.FINALLY -> finallyNode = child
            }
        }

        val tryBlock = tryBlockNode?.let { convertBlock(it) } ?: buildBlock { source = node.toSource() }

        val catches = catchNodes.map { catchNode ->
            var catchParamNode: LighterASTNode? = null
            var catchBodyNode: LighterASTNode? = null
            tree.forEachChildren(catchNode) { child ->
                when (child.tokenType) {
                    CjNodeTypes.CATCH_PARAMETER -> catchParamNode = child
                    CjNodeTypes.BLOCK -> catchBodyNode = child
                }
            }
            val paramName = catchParamNode?.let {
                val nameNode = tree.findChildByType(it, CjTokens.IDENTIFIER)
                nameNode?.asText()
            }
            // 对齐 PSI: CjCatchParameter.typeReference 硬编码返回 null
            val paramTypeRef = buildImplicitTypeRef()

            val catchParamName = if (paramName != null) Name.identifier(paramName) else Name.special("<error>")
            val parameter = buildSourceDeclaration(CfirValueParameterSymbol(callableIdFor(catchParamName))) { symbol ->
                buildValueParameter {
                    source = (catchParamNode ?: catchNode).toSource()
                    this.symbol = symbol
                    origin = CfirDeclarationOrigin.Source
                    moduleData = baseModuleData
                    attributes = CfirDeclarationAttributes.EMPTY
                    isLocal = false
                    isNamed = false
                    status = CfirDeclarationStatusImpl.DEFAULT
                    returnTypeRef = paramTypeRef
                    name = catchParamName
                }
            }
            val body = catchBodyNode?.let { convertBlock(it) } ?: buildBlock { source = catchNode.toSource() }
            buildCatch {
                source = catchNode.toSource()
                this.parameter = parameter
                this.body = body
            }
        }

        val finallyBlock = finallyNode?.let { fin ->
            val block = tree.findChildByType(fin, CjNodeTypes.BLOCK)
            block?.let { convertBlock(it) }
        }

        return buildTryExpression {
            source = node.toSource()
            this.tryBlock = tryBlock
            this.catches.addAll(catches)
            this.finallyBlock = finallyBlock
        }
    }

    // ===== Lambda =====

    private fun convertLambda(node: LighterASTNode): CfirAnonymousFunctionExpression {
        val valueParams = mutableListOf<org.cangnova.cangjie.cfir.declarations.CfirValueParameter>()
        var bodyNode: LighterASTNode? = null

        // LAMBDA_EXPRESSION 内部包含 FUNCTION_LITERAL
        val funcLiteral = tree.findChildByType(node, CjNodeTypes.FUNCTION_LITERAL) ?: node

        tree.forEachChildren(funcLiteral) { child ->
            when (child.tokenType) {
                CjNodeTypes.VALUE_PARAMETER_LIST -> {
                    tree.forEachChildren(child) { param ->
                        if (param.tokenType == CjNodeTypes.VALUE_PARAMETER) {
                            valueParams.add(declarationBuilder.convertValueParameter(param))
                        }
                    }
                }
                CjNodeTypes.BLOCK -> bodyNode = child
            }
        }

        val body = bodyNode?.let { convertBlock(it) }
        val hasExplicitParameterList = valueParams.isNotEmpty()

        val anonymousFunction = buildSourceDeclaration(CfirAnonymousFunctionSymbol()) { symbol ->
            buildAnonymousFunction {
                source = node.toSource()
                this.symbol = symbol
                resolvePhase = CfirResolvePhase.RAW_CFIR
                origin = CfirDeclarationOrigin.Source
                moduleData = baseModuleData
                attributes = CfirDeclarationAttributes.EMPTY
                isLocal = true
                status = CfirDeclarationStatusImpl.DEFAULT
                returnTypeRef = buildImplicitTypeRef()
                this.valueParameters.addAll(valueParams)
                this.body = body
                this.hasExplicitParameterList = hasExplicitParameterList
                isLambda = true
                typeRef = buildImplicitTypeRef()
            }
        }
        return buildAnonymousFunctionExpression {
            source = node.toSource()
            this.anonymousFunction = anonymousFunction
            isTrailingLambda = false
        }
    }

    // ===== Misc =====

    private fun convertSubscript(node: LighterASTNode): CfirSubscriptExpression {
        var receiverNode: LighterASTNode? = null
        val indexNodes = mutableListOf<LighterASTNode>()

        tree.forEachChildren(node) { child ->
            when (child.tokenType) {
                CjNodeTypes.INDICES -> {
                    tree.forEachChildren(child) { idx ->
                        if (isExpressionToken(idx.tokenType)) {
                            indexNodes.add(idx)
                        }
                    }
                }
                else -> {
                    if (receiverNode == null && isExpressionToken(child.tokenType)) {
                        receiverNode = child
                    }
                }
            }
        }

        val receiver = receiverNode?.let { convertExpression(it) }
            ?: buildErrorExpression(node.toSourceElement(), "Missing subscript receiver")
        return buildSubscriptExpression {
            source = node.toSource()
            this.receiver = receiver
            indices.addAll(indexNodes.map { convertExpression(it) })
        }
    }

    private fun convertArrayLiteral(node: LighterASTNode): CfirArrayLiteral {
        val elements = mutableListOf<CfirExpression>()
        tree.forEachChildren(node) { child ->
            if (isExpressionToken(child.tokenType)) {
                elements.add(convertExpression(child))
            }
        }
        return buildArrayLiteral {
            source = node.toSource()
            this.elements.addAll(elements)
        }
    }

    private fun convertTupleLiteral(node: LighterASTNode): CfirTupleLiteral {
        val elements = mutableListOf<CfirExpression>()
        tree.forEachChildren(node) { child ->
            if (isExpressionToken(child.tokenType)) {
                elements.add(convertExpression(child))
            }
        }
        return buildTupleLiteral {
            source = node.toSource()
            this.elements.addAll(elements)
        }
    }

    private fun convertTypeCheck(node: LighterASTNode): CfirTypeOperator {
        var argNode: LighterASTNode? = null
        var typeRefNode: LighterASTNode? = null

        tree.forEachChildren(node) { child ->
            when (child.tokenType) {
                CjNodeTypes.TYPE_REFERENCE -> typeRefNode = child
                else -> {
                    if (argNode == null && isExpressionToken(child.tokenType)) {
                        argNode = child
                    }
                }
            }
        }

        val argument = argNode?.let { convertExpression(it) }
            ?: buildErrorExpression(node.toSourceElement(), "Missing is-check operand")
        return buildTypeOperator {
            source = node.toSource()
            operation = CfirTypeOperationKind.IS
            this.argument = argument
            typeRef = convertTypeReference(typeRefNode, tree, this@LightTreeRawCfirExpressionBuilder.source) {
                it.toSourceElement()
            }
        }
    }

    private fun convertSynchronized(node: LighterASTNode): CfirExpression {
        var exprNode: LighterASTNode? = null
        var blockNode: LighterASTNode? = null
        tree.forEachChildren(node) { child ->
            when (child.tokenType) {
                CjNodeTypes.BLOCK -> blockNode = child
                else -> {
                    if (exprNode == null && isExpressionToken(child.tokenType)) {
                        exprNode = child
                    }
                }
            }
        }
        val mutex = exprNode?.let { convertExpression(it) }
            ?: buildErrorExpression(node.toSourceElement(), "Missing synchronized mutex expression")
        val body = blockNode?.let { convertBlock(it) } ?: buildBlock { source = node.toSource() }
        return buildSynchronizedExpression {
            source = node.toSource()
            monitor = mutex
            this.body = body
        }
    }

    private fun convertUnsafe(node: LighterASTNode): CfirExpression {
        val blockNode = tree.findChildByType(node, CjNodeTypes.BLOCK)
            ?: return buildErrorExpression(node.toSourceElement(), "Missing unsafe block")
        val bodyExpression = convertBlock(blockNode)
        return buildUnsafeExpression {
            source = node.toSource()
            body = bodyExpression
        }
    }

    private fun convertQuote(node: LighterASTNode): CfirExpression {
        val rawText = node.asText()
        val interpolations = mutableListOf<CfirExpression>()
        tree.forEachChildren(node) { child ->
            if (child.tokenType == CjNodeTypes.QUOTE_INTERPOLATE) {
                val expr = findFirstExpression(child)
                if (expr != null) {
                    interpolations.add(convertExpression(expr))
                }
            }
        }
        return buildQuoteExpression {
            source = node.toSource()
            this.rawText = rawText
            this.interpolations.addAll(interpolations)
        }
    }

    private fun convertMacroExpression(node: LighterASTNode): CfirExpression {
        val nameNode = tree.findChildByType(node, CjNodeTypes.REFERENCE_EXPRESSION)
        val inputNode = tree.findChildByType(node, CjNodeTypes.MACRO_INPUT)
        val attrNode = tree.findChildByType(node, CjNodeTypes.MACRO_ATTR)
        return buildMacroExpression {
            source = node.toSource()
            name = nameNode?.asText()?.let { Name.identifier(it) }
            inputText = inputNode?.asText()
            attrText = attrNode?.asText()
        }
    }

    // ===== 辅助方法 =====

    private fun toBlock(node: LighterASTNode): CfirBlock {
        if (node.tokenType == CjNodeTypes.BLOCK || node.tokenType == CjNodeTypes.CASE_BLOCK) {
            return convertBlock(node)
        }
        return buildBlock {
            source = node.toSource()
            statements.add(convertExpression(node))
        }
    }

    /** 在子节点中查找第一个表达式节点 */
    private fun findFirstExpression(node: LighterASTNode): LighterASTNode? {
        tree.forEachChildren(node) { child ->
            if (isExpressionToken(child.tokenType) || isDeclarationToken(child.tokenType)) return child
        }
        return null
    }

    private inline fun <D : CfirDeclaration, S : CfirSymbol<D>> buildSourceDeclaration(
        symbol: S,
        builder: (S) -> D,
    ): D {
        val declaration = builder(symbol)

        return declaration
    }

    private fun referenceNameFromText(text: String): Name {
        val raw = text.trim()
        if (raw.isEmpty()) return Name.identifier("<error>")
        val ltIndex = raw.indexOf('<')
        val base = if (ltIndex >= 0) raw.substring(0, ltIndex).trim() else raw
        val safe = if (base.isNotEmpty()) base else raw
        return Name.identifier(safe)
    }

    companion object {
        /** 判断 tokenType 是否为表达式类型 */
        fun isExpressionToken(tt: IElementType): Boolean = when (tt) {
            CjNodeTypes.BLOCK, CjNodeTypes.CASE_BLOCK,
            CjNodeTypes.INTEGER_CONSTANT, CjNodeTypes.FLOAT_CONSTANT,
            CjNodeTypes.RUNE_CONSTANT, CjNodeTypes.BOOLEAN_CONSTANT,
            CjNodeTypes.UNIT_CONSTANT, CjNodeTypes.STRING_TEMPLATE,
            CjNodeTypes.BINARY_EXPRESSION, CjNodeTypes.RANGE_EXPRESSION,
            CjNodeTypes.PREFIX_EXPRESSION, CjNodeTypes.POSTFIX_EXPRESSION,
            CjNodeTypes.DOT_QUALIFIED_EXPRESSION, CjNodeTypes.SAFE_ACCESS_EXPRESSION,
            CjNodeTypes.REFERENCE_EXPRESSION, CjNodeTypes.CALL_EXPRESSION,
            CjNodeTypes.SPAWN_EXPRESSION,
            CjNodeTypes.IF, CjNodeTypes.MATCH,
            CjNodeTypes.FOR, CjNodeTypes.WHILE, CjNodeTypes.DO_WHILE,
            CjNodeTypes.RETURN, CjNodeTypes.BREAK, CjNodeTypes.CONTINUE, CjNodeTypes.THROW,
            CjNodeTypes.TRY,
            CjNodeTypes.LAMBDA_EXPRESSION, CjNodeTypes.PARENTHESIZED,
            CjNodeTypes.ARRAY_ACCESS_EXPRESSION,
            CjNodeTypes.COLLECTION_LITERAL_EXPRESSION,
            CjNodeTypes.TUPLE_EXPRESSION,
            CjNodeTypes.IS_EXPRESSION,
            CjNodeTypes.SYNCHRONIZED_EXPRESSION, CjNodeTypes.UNSAFE_EXPRESSION,
            CjNodeTypes.QUOTE_EXPRESSION, CjNodeTypes.MACRO_EXPRESSION,
            CjNodeTypes.THIS_EXPRESSION, CjNodeTypes.SUPER_EXPRESSION,
            -> true
            else -> false
        }

        /** 判断 tokenType 是否为声明类型 */
        fun isDeclarationToken(tt: IElementType): Boolean = when (tt) {
            CjNodeTypes.CLASS, CjNodeTypes.INTERFACE, CjNodeTypes.STRUCT, CjNodeTypes.ENUM,
            CjNodeTypes.EXTEND,
            CjNodeTypes.FUNC, CjNodeTypes.MAIN_FUNC, CjNodeTypes.MACRO,
            CjNodeTypes.PROPERTY, CjNodeTypes.FIELD, CjNodeTypes.VARIABLE,
            CjNodeTypes.PRIMARY_CONSTRUCTOR, CjNodeTypes.SECONDARY_CONSTRUCTOR,
            CjNodeTypes.TYPEALIAS,
            CjNodeTypes.ENUM_CONSTRUCTOR,
            CjNodeTypes.FINALIZER,
            -> true
            else -> false
        }

        /** 判断 tokenType 是否为模式类型 */
        fun isPatternToken(tt: IElementType): Boolean = when (tt) {
            CjNodeTypes.BINDING_PATTERN, CjNodeTypes.TYPE_PATTERN,
            CjNodeTypes.TUPLE_PATTERN, CjNodeTypes.ENUM_PATTERN,
            CjNodeTypes.CONSTANT_PATTERN, CjNodeTypes.WILDCARD_PATTERN,
            -> true
            else -> false
        }

        /** 判断 tokenType 是否为语义节点（非空白/注释） */
        fun isSemanticToken(tt: IElementType): Boolean =
            isExpressionToken(tt) || isDeclarationToken(tt) || isPatternToken(tt)
                    || tt == CjNodeTypes.OPERATION_REFERENCE
                    || tt == CjNodeTypes.REFERENCE_EXPRESSION
    }
}
