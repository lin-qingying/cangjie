/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.cfir.lightTree

import com.intellij.lang.LighterASTNode
import com.intellij.psi.tree.IElementType
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.cangnova.cangjie.cfir.CfirFunctionTarget
import org.cangnova.cangjie.cfir.builder.*
import org.cangnova.cangjie.cfir.builder.macro.MacroPayloadTokenizer
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.builder.buildAnonymousFunction
import org.cangnova.cangjie.cfir.declarations.builder.buildFieldVariable
import org.cangnova.cangjie.cfir.declarations.builder.buildPatternVariable
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.expressions.builder.*
import org.cangnova.cangjie.cfir.patterns.CfirCatchPattern
import org.cangnova.cangjie.cfir.patterns.CfirCommandTypePattern
import org.cangnova.cangjie.cfir.patterns.CfirPattern
import org.cangnova.cangjie.cfir.patterns.builder.*
import org.cangnova.cangjie.cfir.references.builder.buildSuperReference
import org.cangnova.cangjie.cfir.references.builder.buildThisReference
import org.cangnova.cangjie.cfir.resolve.providers.macro.*
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirAnonymousFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFieldVariableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPatternVariableSymbol
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind
import org.cangnova.cangjie.cfir.types.builder.buildBasicTypeRef
import org.cangnova.cangjie.cfir.types.isExposedBuiltinClassifier
import org.cangnova.cangjie.descriptors.Modality
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.OperatorNameConventions
import org.cangnova.cangjie.psi.CjNodeTypes
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes.BASIC_REFERENCE_EXPRESSION
import org.cangnova.cangjie.source.CjFakeSourceElementKind
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.source.CjSourceElementOffsetStrategy
import org.cangnova.cangjie.source.fakeElement
import org.cangnova.cangjie.cfir.expressions.builder.buildErrorExpression as buildErrorExpressionNode

/**
 * LightTree → Raw CFIR 表达式构建器（对齐 PsiRawCfirBuilder 的表达式转换部分）。
 *
 * 通过 `when(node.tokenType)` 手动分发代替 PSI Visitor 模式，
 * 遍历 LightTree 子节点构建 CFIR 表达式。
 *
 * @property declarationBuilder 声明转换器，用于 block 内声明和 pattern binding 等场景。
 */
class LightTreeRawCfirExpressionBuilder(
    session: CfirSession,
    tree: FlyweightCapableTreeStructure<LighterASTNode>,
    source: CharSequence,
    context: Context<LighterASTNode>,
    /** 声明转换器，用于 block 内声明和 pattern binding 等场景。 */
    private val declarationBuilder: LightTreeRawCfirDeclarationBuilder,
) : AbstractLightTreeRawCfirBuilder(session, tree, source, context) {

    // ===== 公共 API =====

    /** 从 LightTree 表达式节点构建 raw CFIR 表达式。 */
    override fun buildExpression(expression: LighterASTNode): CfirExpression =
        convertExpression(expression)

    /** 按 LightTree token type 分派到具体表达式转换函数。 */
    fun convertExpression(node: LighterASTNode): CfirExpression = when (node.tokenType) {
        CjNodeTypes.BLOCK, CjNodeTypes.CASE_BLOCK -> convertBlock(node)

        // 字面量
        CjNodeTypes.INTEGER_CONSTANT -> convertLiteral(node, CfirLiteralKind.INT)
        CjNodeTypes.FLOAT_CONSTANT -> convertLiteral(node, CfirLiteralKind.FLOAT)
        CjNodeTypes.RUNE_CONSTANT -> convertLiteral(node, CfirLiteralKind.RUNE)
        CjNodeTypes.CHARACTER_BYTE_CONSTANT -> buildLiteralExpression {
            source = node.toSource()
            kind = CfirLiteralKind.BYTE
            value = byteLiteralCodePointOrNull(node.asText())
        }
        CjNodeTypes.BOOLEAN_CONSTANT -> convertBooleanLiteral(node)
        CjNodeTypes.UNIT_CONSTANT -> buildLiteralExpression {
            source = node.toSource(); kind = CfirLiteralKind.UNIT; value = null
        }
        CjNodeTypes.STRING_TEMPLATE -> convertStringTemplate(node)

        // 二元/一元
        CjNodeTypes.BINARY_WITH_TYPE -> convertTypeOperator(node)
        CjNodeTypes.BINARY_EXPRESSION -> convertBinary(node)
        CjNodeTypes.RANGE_EXPRESSION -> convertRange(node)
        CjNodeTypes.SLICE_EXPRESSION -> convertSlice(node)
        CjNodeTypes.PREFIX_EXPRESSION -> convertPrefix(node)
        CjNodeTypes.POSTFIX_EXPRESSION -> convertPostfix(node)

        // 访问
        CjNodeTypes.OPTIONAL_EXPRESSION -> convertOptionalExpression(node)
        CjNodeTypes.OPTIONAL_CHAIN_EXPRESSION -> convertOptionalChainExpression(node)
        CjNodeTypes.DOT_QUALIFIED_EXPRESSION -> convertDotQualified(node)
        CjNodeTypes.REFERENCE_EXPRESSION, BASIC_REFERENCE_EXPRESSION -> convertNameReference(node)

        // 调用
        CjNodeTypes.CALL_EXPRESSION -> convertCall(node)
        CjNodeTypes.SPAWN_EXPRESSION -> convertSpawn(node)

        // 控制流
        CjNodeTypes.IF -> convertIf(node)
        CjNodeTypes.MATCH -> convertMatch(node)
        CjNodeTypes.FOR -> convertFor(node)
        CjNodeTypes.WHILE -> convertWhile(node)
        CjNodeTypes.DO_WHILE -> convertDoWhile(node)
        CjNodeTypes.LET_EXPRESSION -> convertLetPatternExpression(node)

        // 跳转 / 异常
        CjNodeTypes.RETURN -> convertReturn(node)
        CjNodeTypes.BREAK -> buildBreakExpression(node.toSource())
        CjNodeTypes.CONTINUE -> buildContinueExpression(node.toSource())
        CjNodeTypes.THROW -> convertThrow(node)
        CjNodeTypes.PERFORM -> convertPerform(node)
        CjNodeTypes.RESUME -> convertResume(node)
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

    /** 将 LightTree 声明或表达式节点包装成可放入 block 的 CFIR statement。 */
    private inline fun LighterASTNode.toCfirStatement(errorReasonLazy: () -> String): CfirStatement {
        val cfir = when {
            isDeclarationToken(tokenType) -> declarationBuilder.convertDeclaration(this)
            isExpressionToken(tokenType) -> convertExpression(this)
            else -> buildErrorExpressionNode {
                source = toSource()
                diagnostic = ConeSimpleDiagnostic(errorReasonLazy())
            }
        }

        return when (cfir) {
            is CfirStatement -> cfir
            else -> buildErrorExpressionNode {
                source = toSource()
                diagnostic = ConeSimpleDiagnostic(errorReasonLazy())
                nonExpressionElement = cfir
            }
        }
    }

    /** 转换 block/case block，并展开无 annotation 的普通嵌套 block。 */
    fun convertBlock(node: LighterASTNode): CfirBlock {
        val statements = withLocalContext {
            val stmts = mutableListOf<CfirStatement>()
            tree.forEachChildren(node) { child ->
                val tt = child.tokenType
                if (isDeclarationToken(tt) || isExpressionToken(tt)) {
                    val cfirStatement = child.toCfirStatement { "Statement expected: ${child.asText()}" }
                    val isForLoopBlock =
                        cfirStatement is CfirBlock && cfirStatement.source?.kind == CjFakeSourceElementKind.DesugaredForLoop
                    if (cfirStatement !is CfirBlock || isForLoopBlock || cfirStatement.annotations.isNotEmpty()) {
                        stmts.add(cfirStatement)
                    } else {
                        stmts.addAll(cfirStatement.statements)
                    }
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

    /** 转换基础 literal 表达式。 */
    private fun convertLiteral(node: LighterASTNode, kind: CfirLiteralKind): CfirLiteralExpression {
        val text = node.asText()
        return buildLiteralExpression {
            source = node.toSource()
            this.kind = kind
            this.value = text
        }
    }

    /** 转换布尔 literal。 */
    private fun convertBooleanLiteral(node: LighterASTNode): CfirLiteralExpression {
        val text = node.asText()
        return buildLiteralExpression {
            source = node.toSource()
            kind = CfirLiteralKind.BOOLEAN
            value = text == "true"
        }
    }

    /** 转换字符串模板；有插值时构造 string interpolation。 */
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

    /** 转换二元表达式、赋值表达式和可重载二元运算。 */
    private fun convertBinary(node: LighterASTNode): CfirExpression {
        var left: LighterASTNode? = null
        var right: LighterASTNode? = null
        var opToken: IElementType? = null
        var opNode: LighterASTNode? = null

        tree.forEachChildren(node) { child ->
            when (child.tokenType) {
                CjNodeTypes.OPERATION_REFERENCE -> {
                    // OPERATION_REFERENCE 内部包含实际操作符 Token
                    tree.forEachChildren(child) { opChild ->
                        opToken = opChild.tokenType
                        opNode = opChild
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
                if (leftExpr is CfirTupleLiteral) {
                    return desugarDestructuringAssignment(node, leftExpr, rightExpr)
                }
                return buildAssignment {
                    source = node.toSource()
                    lValue = leftExpr
                    rValue = rightExpr
                }
            }
            val operation = op.toCompoundAssignName() ?: Name.identifier("<error>")
            return buildAugmentedAssignment {
                source = node.toSource()
                this.operation = operation
                operationSource = opNode?.toSource()
                leftArgument = leftExpr
                rightArgument = rightExpr
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

    /**
     * 把元组解构赋值脱糖为「临时绑定 + 逐元素赋值」的 block。
     *
     * `(a, b) = rhs` 展开为：
     * ```
     * let <destructuring-0> = rhs
     * a = <destructuring-0>[0]
     * b = <destructuring-0>[1]
     * ```
     *
     * 引入临时绑定是为了让右值只求值一次；每条合成赋值的 source 锚定在对应左值元素上，
     * 使左值可写性、初始化、const 求值等诊断落在元素本身而不是整条赋值上。
     *
     * 脱糖在建树期完成，因此所有下游消费方（赋值合法性检查、初始化流分析、const 求值、
     * DFA）看到的都是普通赋值，无需各自重新实现元组左值语义。
     */
    private fun desugarDestructuringAssignment(
        node: LighterASTNode,
        targets: CfirTupleLiteral,
        rValue: CfirExpression,
    ): CfirExpression {
        val fakeSource = node.toSource()
            .fakeElement(CjFakeSourceElementKind.DesugaredDestructuringAssignment)
        val statements = mutableListOf<CfirStatement>()
        expandDestructuringTargets(targets, rValue, fakeSource, statements, nextTemporaryId = 0)
        return buildBlock {
            source = fakeSource
            this.statements.addAll(statements)
        }
    }

    /**
     * 逐层展开解构目标，把生成的临时绑定与赋值语句追加到 [out]。
     *
     * 嵌套元组各自分配临时绑定；名字必须逐层唯一，否则内层绑定会遮蔽外层，
     * 导致外层后续元素的下标读取解析到错误的绑定上。返回下一个可用的临时绑定编号。
     */
    private fun expandDestructuringTargets(
        targets: CfirTupleLiteral,
        rValue: CfirExpression,
        fakeSource: CjSourceElement,
        out: MutableList<CfirStatement>,
        nextTemporaryId: Int,
    ): Int {
        val temporaryName = Name.special("<destructuring-$nextTemporaryId>")
        var temporaryId = nextTemporaryId + 1
        out.add(buildDestructuringTemporary(temporaryName, rValue, fakeSource))

        targets.elements.forEachIndexed { index, target ->
            val elementRead = buildSubscriptExpression {
                source = fakeSource
                receiver = buildNamedAccessExpression {
                    source = fakeSource
                    calleeReference = buildNamedReference(temporaryName, fakeSource)
                }
                indices.add(
                    buildLiteralExpression {
                        source = fakeSource
                        kind = CfirLiteralKind.INT
                        value = index.toString()
                    }
                )
            }

            if (target is CfirTupleLiteral) {
                temporaryId = expandDestructuringTargets(target, elementRead, fakeSource, out, temporaryId)
            } else {
                out.add(
                    buildAssignment {
                        // 锚点落在左值元素上，而不是整条 `(a, b) = rhs`
                        source = target.source ?: fakeSource
                        this.lValue = target
                        this.rValue = elementRead
                    }
                )
            }
        }
        return temporaryId
    }

    /**
     * 构造承载解构右值的不可变合成局部绑定。
     *
     * 本仓库把所有变量声明统一建模为模式变量，简单名字是退化的 binding pattern，
     * 因此临时绑定同样构造为 [CfirPatternVariable]，
     * 使其能像普通局部变量一样在 body resolve 阶段进入作用域并被后续下标读取引用。
     */
    private fun buildDestructuringTemporary(
        name: Name,
        rValue: CfirExpression,
        fakeSource: CjSourceElement,
    ): CfirPatternVariable {
        val temporaryStatus = declarationBuilder.cloneDeclarationStatus(CfirDeclarationStatusImpl.DEFAULT)
        return buildSourceDeclaration(CfirPatternVariableSymbol(callableIdFor(name))) { symbol ->
            buildPatternVariable {
                resolvePhase = CfirResolvePhase.RAW_CFIR
                source = fakeSource
                this.symbol = symbol
                origin = CfirDeclarationOrigin.Source
                moduleData = baseModuleData

                attributes = CfirDeclarationAttributes.EMPTY
                isLocal = true
                status = temporaryStatus
                returnTypeRef = buildImplicitTypeRef()
                pattern = buildBindingPattern {
                    source = fakeSource
                    this.name = name
                    bindingVariable = declarationBuilder.createPatternBindingVariable(
                        source = fakeSource,
                        name = name,
                        status = temporaryStatus,
                        isLocal = true,
                        isVar = false,
                        returnTypeRef = buildImplicitTypeRef(),
                    )
                }
                initializer = rValue
                isVar = false
            }
        }
    }

    /** 转换 range 表达式，保留起点、终点、步长与闭区间标志。 */
    private fun convertRange(node: LighterASTNode): CfirRangeExpression {
        var left: LighterASTNode? = null
        var right: LighterASTNode? = null
        var step: LighterASTNode? = null
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
                    else if (right == null) right = child
                    else step = child
                }
            }
        }

        val start = left?.let { convertExpression(it) }
            ?: buildErrorExpression(reason = "Missing range start")
        val end = right?.let { convertExpression(it) }
            ?: buildErrorExpression(reason = "Missing range end")
        val stepExpression = step?.let { convertExpression(it) }

        return buildRangeExpression {
            source = node.toSource()
            this.start = start
            this.end = end
            this.step = stepExpression
            this.isInclusive = isInclusive
        }
    }

    /**
     * 转换下标中的半范围表达式，例如 `array[2..]`、`array[..2]` 与 `array[..]`。
     *
     * LightTree 为这类节点使用独立的 `SLICE_EXPRESSION`，而 CFIR 统一以
     * `CfirRangeExpression` 表示范围索引。缺少的端点必须保留为 error expression，
     * 这样范围类型推断与后续下标语义仍能沿用完整的 Range 管线。
     */
    private fun convertSlice(node: LighterASTNode): CfirRangeExpression {
        var startNode: LighterASTNode? = null
        var endNode: LighterASTNode? = null
        var rangeOperatorSeen = false

        tree.forEachChildren(node) { child ->
            when (child.tokenType) {
                CjNodeTypes.OPERATION_REFERENCE -> rangeOperatorSeen = true
                else -> if (isExpressionToken(child.tokenType)) {
                    if (!rangeOperatorSeen && startNode == null) {
                        startNode = child
                    } else if (rangeOperatorSeen && endNode == null) {
                        endNode = child
                    }
                }
            }
        }

        val start = startNode?.let { convertExpression(it) }
            ?: buildErrorExpression(reason = "Missing range start")
        val end = endNode?.let { convertExpression(it) }
            ?: buildErrorExpression(reason = "Missing range end")

        return buildRangeExpression {
            source = node.toSource()
            this.start = start
            this.end = end
            step = null
            isInclusive = false
        }
    }

    /** 转换前缀一元表达式。 */
    private fun convertPrefix(node: LighterASTNode): CfirExpression {
        var opToken: IElementType? = null
        var opNode: LighterASTNode? = null
        var operandNode: LighterASTNode? = null

        tree.forEachChildren(node) { child ->
            when (child.tokenType) {
                CjNodeTypes.OPERATION_REFERENCE -> {
                    tree.forEachChildren(child) { opChild ->
                        opToken = opChild.tokenType
                        opNode = opChild
                    }
                }
                else -> if (isExpressionToken(child.tokenType)) { operandNode = child }
            }
        }

        val base = operandNode?.let { convertExpression(it) }
            ?: return buildErrorExpression(node.toSourceElement(), "Missing prefix operand")
        val opName = opToken?.toPrefixUnaryName() ?: Name.identifier("<prefix>")
        if (opToken == CjTokens.PLUSPLUS || opToken == CjTokens.MINUSMINUS) {
            return buildIncrementDecrementExpression {
                source = node.toSource()
                isPrefix = true
                operationName = opName
                expression = base
                operationSource = opNode?.toSource()
            }
        }
        return buildFunctionCall {
            source = node.toSource()
            calleeReference = buildNamedReference(opName, node.toSource())
            argumentList = buildArgumentList()
            explicitReceiver = base
            origin = CfirFunctionCallOrigin.Operator
        }
    }

    /** 转换后缀一元表达式。 */
    private fun convertPostfix(node: LighterASTNode): CfirExpression {
        var opToken: IElementType? = null
        var opNode: LighterASTNode? = null
        var operandNode: LighterASTNode? = null

        tree.forEachChildren(node) { child ->
            when (child.tokenType) {
                CjNodeTypes.OPERATION_REFERENCE -> {
                    tree.forEachChildren(child) { opChild ->
                        opToken = opChild.tokenType
                        opNode = opChild
                    }
                }
                else -> if (isExpressionToken(child.tokenType)) { operandNode = child }
            }
        }

        val base = operandNode?.let { convertExpression(it) }
            ?: return buildErrorExpression(node.toSourceElement(), "Missing postfix operand")
        val opName = opToken?.toPostfixUnaryName() ?: Name.identifier("<postfix>")
        if (opToken == CjTokens.PLUSPLUS || opToken == CjTokens.MINUSMINUS) {
            return buildIncrementDecrementExpression {
                source = node.toSource()
                isPrefix = false
                operationName = opName
                expression = base
                operationSource = opNode?.toSource()
            }
        }
        return buildFunctionCall {
            source = node.toSource()
            calleeReference = buildNamedReference(opName, node.toSource())
            argumentList = buildArgumentList()
            explicitReceiver = base
            origin = CfirFunctionCallOrigin.Operator
        }
    }

    /**
     * optional 后缀包装节点。
     *
     * LightTree 这里只承接 parser 产出的 `OPTIONAL_EXPRESSION`，
     * 不再把 `?.` / `?[` / `?(` 视为独立安全访问表达式。
     */
    private fun convertOptionalExpression(node: LighterASTNode): CfirExpression {
        val baseExpression = findFirstExpression(node)
            ?: return buildErrorExpression(node.toSourceElement(), "Malformed optional expression: missing base expression")
        return buildOptionalExpression {
            source = node.toSource()
            expression = convertExpression(baseExpression)
        }
    }

    /**
     * optional chain 根节点。
     *
     * 链内部仍保留普通的 qualified access / call / subscript 结构，
     * optional 语义由外层节点统一承接。
     */
    private fun convertOptionalChainExpression(node: LighterASTNode): CfirExpression {
        val chainExpression = findFirstExpression(node)
            ?: return buildErrorExpression(node.toSourceElement(), "Malformed optional chain expression: missing chain body")
        return buildOptionalChainExpression {
            source = node.toSource()
            expression = convertExpression(chainExpression)
        }
    }

    // ===== Call & Access =====

    /** 转换普通调用表达式与尾随 lambda 调用。 */
    private fun convertCall(node: LighterASTNode): CfirExpression {
          var calleeNode: LighterASTNode? = null
          val argNodes = mutableListOf<LighterASTNode>()
          val typeArgNodes = mutableListOf<LighterASTNode>()
          val lambdaArgNodes = mutableListOf<LighterASTNode>()
          var hasValueArgumentList = false
          var valueArgumentListNode: LighterASTNode? = null

        tree.forEachChildren(node) { child ->
            when (child.tokenType) {
                CjNodeTypes.VALUE_ARGUMENT_LIST -> {
                    hasValueArgumentList = true
                    valueArgumentListNode = child
                    argNodes.addAll(valueArgumentNodes(child))
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

          var effectiveCalleeNode = calleeNode
          val effectiveArgNodes = argNodes.toMutableList()
          val effectiveTypeArgNodes = typeArgNodes.toMutableList()
          var callSourceNodeWithoutTrailingLambda: LighterASTNode? = null
          var effectiveValueArgumentListNode = valueArgumentListNode

          /**
           * 对齐 Kotlin FIR light-tree raw builder：
           *
           * 尾随 lambda 语法 `f(1) { ... }` 在 light-tree 中会表现为
           * outer CALL_EXPRESSION(callee = inner CALL_EXPRESSION, lambdaArgument = ...)，
           * 但语义上它仍然是一条对 `f` 的调用。
           *
           * 因此当“外层只有尾随 lambda、内层持有普通实参”时，要把这两层 CALL_EXPRESSION
           * 扁平化成同一个 function call，避免后续把 `f(1)` 错当成 callee 名称。
           */
          if (lambdaArgNodes.isNotEmpty() && argNodes.isEmpty() && calleeNode?.tokenType == CjNodeTypes.CALL_EXPRESSION) {
              val nestedCallNode = calleeNode!!
              var nestedCalleeNode: LighterASTNode? = null
              val nestedArgNodes = mutableListOf<LighterASTNode>()
              val nestedTypeArgNodes = mutableListOf<LighterASTNode>()
              var nestedValueArgumentListNode: LighterASTNode? = null
              var nestedHasTypeArgumentList = false

              tree.forEachChildren(nestedCallNode) { child ->
                  when (child.tokenType) {
                      CjNodeTypes.VALUE_ARGUMENT_LIST -> {
                          nestedValueArgumentListNode = child
                          nestedArgNodes.addAll(valueArgumentNodes(child))
                      }

                      CjNodeTypes.TYPE_ARGUMENT_LIST -> {
                          nestedHasTypeArgumentList = true
                          tree.forEachChildren(child) { typeArg ->
                              if (typeArg.tokenType == CjNodeTypes.TYPE_PROJECTION) {
                                  val typeRef = tree.findChildByType(typeArg, CjNodeTypes.TYPE_REFERENCE)
                                  if (typeRef != null) nestedTypeArgNodes.add(typeRef)
                              }
                          }
                      }

                      else -> {
                          if (nestedCalleeNode == null && isExpressionToken(child.tokenType)) {
                              nestedCalleeNode = child
                          }
                      }
                  }
              }

              // `x { ... }` 也可能被 parser 包成退化的 inner CALL_EXPRESSION。
              // 该包装只提供 callee 身份，不拥有调用后缀，不能成为调用 source 的 owner。
              if (nestedCalleeNode != null) {
                  effectiveCalleeNode = nestedCalleeNode
              }

              val nestedOwnsCallSuffix = nestedValueArgumentListNode != null || nestedHasTypeArgumentList
              if (nestedOwnsCallSuffix) {
                  effectiveValueArgumentListNode = nestedValueArgumentListNode
                  effectiveArgNodes.clear()
                  effectiveArgNodes.addAll(nestedArgNodes)
                  effectiveTypeArgNodes.clear()
                  effectiveTypeArgNodes.addAll(nestedTypeArgNodes)

                  // inner call 已经精确覆盖普通调用后缀；其 source 天然不包含外层尾随 lambda。
                  // 不得再用外层 lambda 的 offset 重写这个内层节点的 source。
                  callSourceNodeWithoutTrailingLambda = nestedCallNode
              }
          }

          val directTypeArgs = effectiveTypeArgNodes.map { typeRefNode ->
              convertTypeReference(typeRefNode, tree, source) { it.toSourceElement() }
          }
          val typeArgs = if (directTypeArgs.isNotEmpty()) {
              directTypeArgs
          } else {
              collectTypeArgumentsFromCallee(effectiveCalleeNode)
          }
          val varraySizeLiteral = extractVArraySizeLiteral(node, effectiveCalleeNode)

          tryBuildTypeConversion(
              node,
              effectiveCalleeNode,
              effectiveArgNodes,
              typeArgs,
              lambdaArgNodes
          )?.let { return it }

          // flatten 后的 effectiveArgNodes 是普通实参的规范集合；直接交给共享 converter，
          // 避免重新依赖某一层 VALUE_ARGUMENT_LIST 而丢失内层调用实参。
          val callArguments = convertValueArguments(effectiveArgNodes).toMutableList()
          val lambdaArgs = lambdaArgNodes.mapNotNull { lambdaArg ->
              val lambdaExpr = findLambdaExpression(lambdaArg)
              lambdaExpr?.let {
                  convertLambda(it).also { anonymousFunctionExpression ->
                      anonymousFunctionExpression.replaceIsTrailingLambda(true)
                  }
              }
          }
          callArguments.addAll(lambdaArgs)

          val (receiver, reference) = resolveCalleeReference(effectiveCalleeNode)
          val callSource = callSourceNodeWithoutTrailingLambda?.toSource()
              ?: node.callSourceWithoutTrailingLambda(lambdaArgNodes)

          if (!hasValueArgumentList && lambdaArgs.isEmpty() && typeArgs.isNotEmpty()) {
              return buildNamedAccessExpression {
                  source = callSource
                  calleeReference = reference
                  explicitReceiver = receiver
                  this.typeArguments.addAll(typeArgs)
              }
          }

           return buildFunctionCall {
               source = callSource
               calleeReference = reference
               argumentList = buildArgumentList {
                   source = effectiveValueArgumentListNode?.toSource()
                   arguments.addAll(callArguments)
               }
               explicitReceiver = receiver
               typeArguments.addAll(typeArgs)
               origin = callOriginFor(effectiveCalleeNode)
               hasTrailingLambda = lambdaArgNodes.isNotEmpty()
               this.varraySizeLiteral = varraySizeLiteral
           }
       }

    /**
     * LightTree 路径与 PSI 路径保持同一 source 语义：尾随 lambda 是实参，
     * 不是调用主体 source 的一部分。
     */
    private fun LighterASTNode.callSourceWithoutTrailingLambda(
        lambdaArgNodes: List<LighterASTNode>,
    ): CjSourceElement {
        val callSource = toSource()
        val firstLambda = lambdaArgNodes.firstOrNull() ?: return callSource
        val startOffset = callSource.startOffset
        val callEndOffset = callSource.endOffset
        var endOffset = tree.getStartOffset(firstLambda)

        check(endOffset in startOffset..callEndOffset) {
            "Trailing lambda must be a suffix of its call source owner: call=[$startOffset, $callEndOffset], lambda=$endOffset"
        }

        while (endOffset > startOffset && endOffset - 1 < source.length && source[endOffset - 1].isWhitespace()) {
            endOffset--
        }
        if (endOffset <= startOffset) return callSource

        return callSource.fakeElement(
            CjFakeSourceElementKind.SyntheticCall,
            CjSourceElementOffsetStrategy.Custom.Initialized(startOffset, endOffset),
        )
    }

    /**
     * 转换标准 [CjNodeTypes.VALUE_ARGUMENT_LIST] 的直接实参子节点。
     *
     * 普通调用与 annotation 必须共用该入口，避免不同 builder 对 named argument
     * 的名称节点和值表达式采用不同遍历规则。
     */
    internal fun convertValueArguments(valueArgumentListNode: LighterASTNode): List<CfirExpression> {
        return convertValueArguments(valueArgumentNodes(valueArgumentListNode))
    }

    /** 转换已经按调用扁平化规则选定的 value argument 节点集合。 */
    private fun convertValueArguments(valueArgumentNodes: List<LighterASTNode>): List<CfirExpression> =
        valueArgumentNodes.mapNotNull(::convertValueArgument)

    /** 读取 value-argument-list 的直接实参节点，作为收集与转换共用的结构边界。 */
    private fun valueArgumentNodes(valueArgumentListNode: LighterASTNode): List<LighterASTNode> {
        check(valueArgumentListNode.tokenType == CjNodeTypes.VALUE_ARGUMENT_LIST) {
            "Value arguments must be read from a VALUE_ARGUMENT_LIST node."
        }
        return tree.getChildrenByType(valueArgumentListNode, CjNodeTypes.VALUE_ARGUMENT)
    }

    /**
     * 转换单个标准 value argument，保留 named、inout 及各层 source 语义。
     *
     * 值表达式只允许来自 [CjNodeTypes.VALUE_ARGUMENT] 的直接表达式子节点；
     * [CjNodeTypes.VALUE_ARGUMENT_NAME] 仅用于构造参数名，绝不参与值表达式查找。
     */
    internal fun convertValueArgument(valueArgumentNode: LighterASTNode): CfirExpression? {
        check(valueArgumentNode.tokenType == CjNodeTypes.VALUE_ARGUMENT) {
            "A value argument must be converted from a VALUE_ARGUMENT node."
        }
        val expressionNode = findFirstExpression(valueArgumentNode) ?: return null
        val convertedExpression = convertExpression(expressionNode)
        val isInout = tree.findChildByType(valueArgumentNode, CjTokens.INOUT_KEYWORD) != null
        val wrapped = if (isInout) {
            buildInoutArgumentExpression {
                source = expressionNode.toSource()
                expression = convertedExpression
            }
        } else {
            convertedExpression
        }
        val nameNode = tree.findChildByType(valueArgumentNode, CjNodeTypes.VALUE_ARGUMENT_NAME)
            ?: return wrapped
        val referenceNode = tree.findChildByType(nameNode, CjNodeTypes.REFERENCE_EXPRESSION)
            ?: error("Named value argument must contain a reference expression")
        return buildNamedArgumentExpression {
            source = valueArgumentNode.toSource()
            argumentName = Name.identifier(referenceNode.asText())
            nameSource = referenceNode.toSource()
            expression = wrapped
        }
    }

    /** 尝试把基础类型调用直接构造成类型转换表达式。 */
    private fun tryBuildTypeConversion(
        callNode: LighterASTNode,
        calleeNode: LighterASTNode?,
        argNodes: List<LighterASTNode>,
        typeArgs: List<CfirTypeRef>,
        lambdaArgNodes: List<LighterASTNode>,
    ): CfirExpression? {
        val targetKind = calleeNode?.primitiveTypeConversionKindOrNull() ?: return null
        if (typeArgs.isNotEmpty() || lambdaArgNodes.isNotEmpty()) {
            return buildErrorExpression(callNode.toSourceElement(), "Malformed primitive type conversion")
        }

        val valueArgumentNode = argNodes.singleOrNull()
            ?: return buildErrorExpression(callNode.toSourceElement(), "Malformed primitive type conversion")
        if (tree.findChildByType(valueArgumentNode, CjNodeTypes.VALUE_ARGUMENT_NAME) != null) {
            return buildErrorExpression(callNode.toSourceElement(), "Malformed primitive type conversion")
        }
        val argumentNode = findFirstExpression(valueArgumentNode)
            ?: return buildErrorExpression(
                valueArgumentNode.toSourceElement(),
                "Missing primitive type conversion argument"
            )

        return buildTypeConversion {
            source = callNode.toSource()
            argument = convertExpression(argumentNode)
            targetTypeRef = buildBasicTypeRef {
                source = calleeNode.toSource()
                name = Name.identifier(targetKind.typeName)
            }
        }
    }

    /** 识别可由调用语法触发的基础类型转换目标。 */
    private fun LighterASTNode.primitiveTypeConversionKindOrNull(): PrimitiveTypeKind? {
        if (tokenType != BASIC_REFERENCE_EXPRESSION) return null
        val rawName = referenceNameFromText(asText()).asString()
        return PrimitiveTypeKind.entries.firstOrNull {
            it.isExposedBuiltinClassifier && it.typeName == rawName
        }
    }

    /** 把 callee 节点拆成显式 receiver 与待解析的具名引用。 */
    private fun resolveCalleeReference(calleeNode: LighterASTNode?): Pair<CfirExpression?, org.cangnova.cangjie.cfir.references.CfirNamedReference> {
        if (calleeNode == null) {
            return null to buildNamedReference(Name.identifier("<error>"))
        }
        return when (calleeNode.tokenType) {
            CjNodeTypes.REFERENCE_EXPRESSION, BASIC_REFERENCE_EXPRESSION -> {
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
                if (selectorNode?.tokenType == CjNodeTypes.CALL_EXPRESSION) {
                    return convertExpression(calleeNode) to buildNamedReference(
                        OperatorNameConventions.INVOKE,
                        calleeNode.toSource(),
                    )
                }

                val recv = receiverNode?.let { convertExpression(it) }
                val refNode = when (selectorNode?.tokenType) {
                    CjNodeTypes.REFERENCE_EXPRESSION, BASIC_REFERENCE_EXPRESSION -> selectorNode
                    else -> null
                }
                val refName = refNode?.asText() ?: "<error>"
                recv to buildNamedReference(referenceNameFromText(refName), refNode?.toSource() ?: calleeNode.toSource())
            }
            else -> convertExpression(calleeNode) to buildNamedReference(
                OperatorNameConventions.INVOKE,
                calleeNode.toSource(),
            )
        }
    }

    /**
     * LightTree 路径与 PSI 路径要共享同一套 delegation 语义入口，
     * 不能因为没有完整 PSI，就把 `this(...)` / `super(...)` 重新降格成普通 Regular call。
     */
    private fun callOriginFor(calleeNode: LighterASTNode?): CfirFunctionCallOrigin = when (calleeNode?.tokenType) {
        CjNodeTypes.THIS_EXPRESSION -> CfirFunctionCallOrigin.ConstructorDelegationThis
        CjNodeTypes.SUPER_EXPRESSION -> CfirFunctionCallOrigin.ConstructorDelegationSuper
        else -> if (calleeNode.isMockIntrinsicCallee()) {
            CfirFunctionCallOrigin.MockIntrinsic
        } else {
            CfirFunctionCallOrigin.Regular
        }
    }

    /** 判断 callee 是否为 mock intrinsic 调用入口。 */
    private fun LighterASTNode?.isMockIntrinsicCallee(): Boolean {
        val rawName = when (this?.tokenType) {
            CjNodeTypes.REFERENCE_EXPRESSION -> asText()
            CjNodeTypes.DOT_QUALIFIED_EXPRESSION -> {
                var selectorNode: LighterASTNode? = null
                var afterDot = false
                tree.forEachChildren(this) { child ->
                    when {
                        child.tokenType == CjTokens.DOT -> afterDot = true
                        afterDot && selectorNode == null && isSemanticToken(child.tokenType) -> selectorNode = child
                    }
                }
                selectorNode?.asText()
            }
            else -> null
        } ?: return false

        val name = referenceNameFromText(rawName).asString()
        return name == "createMock" || name == "createSpy"
    }

    /** 转换 spawn 表达式。 */
    private fun convertSpawn(node: LighterASTNode): CfirExpression {
        val lambdaNode = findLambdaExpression(node)
        val bodyNode = lambdaNode?.let(::findLambdaBodyBlock)
        val body = bodyNode?.let { convertBlock(it) }
            ?: buildBlock { source = (lambdaNode ?: node).toSource() }
        val threadContextArgument = findFirstValueArgument(node)?.let(::convertValueArgument)
        return buildSpawnExpression {
            source = node.toSource()
            this.body = body
            this.threadContextArgument = threadContextArgument
        }
    }

    /** 转换点访问或安全访问表达式。 */
    private fun convertDotQualified(node: LighterASTNode): CfirExpression {
        var receiverNode: LighterASTNode? = null
        var selectorNode: LighterASTNode? = null
        var afterDot = false

        tree.forEachChildren(node) { child ->
            val tt = child.tokenType
            when {
                tt == CjTokens.DOT -> afterDot = true
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
            val typeArgNodes = mutableListOf<LighterASTNode>()
            val lambdaArgNodes = mutableListOf<LighterASTNode>()
            var valueArgumentListNode: LighterASTNode? = null

            tree.forEachChildren(selector) { child ->
                when (child.tokenType) {
                    CjNodeTypes.VALUE_ARGUMENT_LIST -> {
                        valueArgumentListNode = child
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

            val ref = if (
                calleeRef?.tokenType == CjNodeTypes.REFERENCE_EXPRESSION ||
                calleeRef?.tokenType == BASIC_REFERENCE_EXPRESSION
            ) {
                buildNamedReference(referenceNameFromText(calleeRef!!.asText()), calleeRef!!.toSource())
            } else {
                buildNamedReference(referenceNameFromText(calleeRef?.asText() ?: "<error>"), calleeRef?.toSource() ?: selector.toSource())
            }
            val callArguments = valueArgumentListNode
                ?.let(::convertValueArguments)
                .orEmpty()
                .toMutableList()
            val directTypeArgs = typeArgNodes.map { typeRefNode ->
                convertTypeReference(typeRefNode, tree, source) { it.toSourceElement() }
            }
            val typeArgs = if (directTypeArgs.isNotEmpty()) {
                directTypeArgs
            } else {
                collectTypeArgumentsFromCallee(calleeRef)
              }
              val lambdaArgs = lambdaArgNodes.mapNotNull { lambdaArg ->
                  val lambdaExpr = findLambdaExpression(lambdaArg)
                  lambdaExpr?.let {
                      convertLambda(it).also { anonymousFunctionExpression ->
                          anonymousFunctionExpression.replaceIsTrailingLambda(true)
                      }
                  }
              }
            callArguments.addAll(lambdaArgs)

            return buildFunctionCall {
                source = node.toSource()
                calleeReference = ref
                argumentList = buildArgumentList {
                    source = valueArgumentListNode?.toSource()
                    arguments.addAll(callArguments)
                }
                explicitReceiver = receiver
                typeArguments.addAll(typeArgs)
                origin = CfirFunctionCallOrigin.Regular
                hasTrailingLambda = lambdaArgNodes.isNotEmpty()
            }
        }

        // selector 为简单名称引用
        if (selector.tokenType == CjNodeTypes.REFERENCE_EXPRESSION || selector.tokenType == BASIC_REFERENCE_EXPRESSION) {
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

    /** 转换裸名称引用和带类型实参的名称访问。 */
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

    /** 从名称引用节点收集类型实参。 */
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

    /** 从 callee 节点收集调用类型实参。 */
    private fun collectTypeArgumentsFromCallee(calleeNode: LighterASTNode?): List<org.cangnova.cangjie.cfir.types.CfirTypeRef> {
        calleeNode ?: return emptyList()
        return when (calleeNode.tokenType) {
            CjNodeTypes.REFERENCE_EXPRESSION -> collectReferenceTypeArguments(calleeNode)
            CjNodeTypes.DOT_QUALIFIED_EXPRESSION -> {
                var selectorNode: LighterASTNode? = null
                var afterDot = false
                tree.forEachChildren(calleeNode) { child ->
                    val tt = child.tokenType
                    when {
                        tt == CjTokens.DOT -> afterDot = true
                        afterDot && selectorNode == null && isSemanticToken(tt) -> selectorNode = child
                    }
                }
                val selectorReference = selectorNode?.takeIf { it.tokenType == CjNodeTypes.REFERENCE_EXPRESSION }
                if (selectorReference != null) collectReferenceTypeArguments(selectorReference) else emptyList()
            }

            else -> emptyList()
        }
    }

    /** 提取 `VArray` 调用携带的大小字面量。 */
    private fun extractVArraySizeLiteral(
        callNode: LighterASTNode,
        calleeNode: LighterASTNode?,
    ): String? {
        if (!calleeNode.isDirectVArrayCallee()) return null
        return findVArraySizeLiteral(callNode) ?: findVArraySizeLiteral(calleeNode)
    }

    /** 判断 callee 是否为直接 `VArray` 名称。 */
    private fun LighterASTNode?.isDirectVArrayCallee(): Boolean {
        if (this?.tokenType != CjNodeTypes.REFERENCE_EXPRESSION && this?.tokenType != BASIC_REFERENCE_EXPRESSION) return false
        return referenceNameFromText(asText()).asString() == "VArray"
    }

    /** 查找节点中的 VArray 大小字面量。 */
    private fun findVArraySizeLiteral(node: LighterASTNode?): String? {
        node ?: return null
        if (node.tokenType == CjNodeTypes.TYPE_ARGUMENT_LIST) {
            tree.findChildByType(node, CjTokens.INTEGER_LITERAL)?.let { return it.asText() }
        }
        tree.forEachChildren(node) { child ->
            findVArraySizeLiteral(child)?.let { return it }
        }
        return null
    }

    // ===== Control Flow =====

    /** 转换 if 表达式，支持 let-pattern condition。 */
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

    /** 转换 let-pattern 条件表达式。 */
    private fun convertLetPatternExpression(node: LighterASTNode): CfirLetPatternExpression {
        val patternNodes = mutableListOf<LighterASTNode>()
        var initializerNode: LighterASTNode? = null

        tree.forEachChildren(node) { child ->
            when {
                isPatternToken(child.tokenType) -> patternNodes += child
                initializerNode == null && isExpressionToken(child.tokenType) -> initializerNode = child
            }
        }

        val status = declarationBuilder.cloneDeclarationStatus(CfirDeclarationStatusImpl.DEFAULT)
        val convertedPatterns = patternNodes.map {
            convertPattern(
                node = it,
                ownerStatus = status,
                ownerIsLocal = true,
                ownerIsVar = false,
            )
        }
        val pattern = when (convertedPatterns.size) {
            0 -> buildWildcardPattern { source = node.toSource() }
            1 -> convertedPatterns.single()
            else -> buildOrPattern {
                source = node.toSource()
                alternatives.addAll(convertedPatterns)
            }
        }
        val initializer = initializerNode?.let { convertExpression(it) }
            ?: buildErrorExpression(reason = "Missing let-pattern initializer")

        return buildLetPatternExpression {
            source = node.toSource()
            this.initializer = initializer
            this.pattern = pattern
        }
    }

    /** 转换 match 表达式。 */
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

    /** 转换单个 match entry。 */
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

    /** 将 match condition 转换为 CFIR pattern。 */
    private fun convertMatchCondition(node: LighterASTNode): CfirPattern {
        // 优先作为模式处理（对齐仓颉语义：case 后面是模式）
        if (isPatternToken(node.tokenType)) {
            return convertPattern(node)
        }
        findFirstPatternNode(node)?.let { return convertPattern(it) }
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

    /**
     * LightTree 中 match condition 可能额外包一层语法容器；递归查找第一个真实 pattern，
     * 避免把 `case x: T` 回退成表达式 pattern。
     */
    private fun findFirstPatternNode(node: LighterASTNode): LighterASTNode? {
        tree.forEachChildren(node) { child ->
            if (isPatternToken(child.tokenType)) return child
            findFirstPatternNode(child)?.let { return it }
        }
        return null
    }

    // ===== Pattern =====

    /** 转换 LightTree pattern，并为具名绑定创建 binding variable。 */
    fun convertPattern(
        node: LighterASTNode,
        ownerStatus: CfirDeclarationStatus = declarationBuilder.cloneDeclarationStatus(CfirDeclarationStatusImpl.DEFAULT),
        ownerIsLocal: Boolean = true,
        ownerIsVar: Boolean = false,
    ): CfirPattern = when (node.tokenType) {
        CjNodeTypes.BINDING_PATTERN -> {
            val nameNode = tree.findChildByType(node, CjTokens.IDENTIFIER)
                ?: tree.findChildByType(node, CjNodeTypes.REFERENCE_EXPRESSION)
            val nameText = nameNode?.asText()
            buildBindingPattern {
                source = node.toSource()
                name = if (!nameText.isNullOrEmpty()) Name.identifier(nameText) else Name.special("<error>")
                bindingVariable = declarationBuilder.createPatternBindingVariable(
                    source = node.toSource(),
                    name = name,
                    status = ownerStatus,
                    isLocal = ownerIsLocal,
                    isVar = ownerIsVar,
                    returnTypeRef = buildImplicitTypeRef(),
                )
            }
        }
        CjNodeTypes.VAR_OR_ENUM_PATTERN -> {
            val nameNode = tree.findChildByType(node, CjTokens.IDENTIFIER)
                ?: tree.findChildByType(node, CjNodeTypes.REFERENCE_EXPRESSION)
            val nameText = nameNode?.asText()
            buildVarOrEnumPattern {
                source = node.toSource()
                name = if (!nameText.isNullOrEmpty()) Name.identifier(nameText) else Name.special("<error>")
                bindingVariable = declarationBuilder.createPatternBindingVariable(
                    source = node.toSource(),
                    name = name,
                    status = ownerStatus,
                    isLocal = ownerIsLocal,
                    isVar = ownerIsVar,
                    returnTypeRef = buildImplicitTypeRef(),
                )
            }
        }
        CjNodeTypes.TYPE_PATTERN -> {
            val typeRef = tree.findChildByType(node, CjNodeTypes.TYPE_REFERENCE)
            var nameNode: LighterASTNode? = null
            tree.forEachChildren(node) { child ->
                if (child.tokenType == CjTokens.COLON || child.tokenType == CjNodeTypes.TYPE_REFERENCE) return@forEachChildren
                if (nameNode == null && child.isTypePatternBindingNameNode()) {
                    nameNode = child
                }
            }
            val bindingName = nameNode
                ?.asText()
                ?.takeUnless { it == "_" }
                ?.let(Name::identifier)
            buildTypePattern {
                source = node.toSource()
                this.typeRef = convertTypeReference(typeRef, tree, this@LightTreeRawCfirExpressionBuilder.source) {
                    it.toSourceElement()
                }
                this.bindingName = bindingName
                bindingVariable = bindingName?.let { name ->
                    declarationBuilder.createPatternBindingVariable(
                        source = (nameNode ?: node).toSource(),
                        name = name,
                        status = ownerStatus,
                        isLocal = ownerIsLocal,
                        isVar = ownerIsVar,
                        returnTypeRef = this.typeRef,
                    )
                }
            }
        }
        CjNodeTypes.TUPLE_PATTERN -> {
            val elements = mutableListOf<CfirPattern>()
            tree.forEachChildren(node) { child ->
                if (isPatternToken(child.tokenType)) {
                    elements.add(convertPattern(child, ownerStatus, ownerIsLocal, ownerIsVar))
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
                    subPatterns.add(convertPattern(child, ownerStatus, ownerIsLocal, ownerIsVar))
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

    /** 转换 for-in 循环。 */
    private fun convertFor(node: LighterASTNode): CfirForInExpression {
        var patternNode: LighterASTNode? = null
        var rangeNode: LighterASTNode? = null
        var guardNode: LighterASTNode? = null
        var bodyNode: LighterASTNode? = null

        tree.forEachChildren(node) { child ->
            when (child.tokenType) {
                CjNodeTypes.LOOP_RANGE -> rangeNode = findFirstExpression(child)
                CjNodeTypes.PATTERN_GUARD -> guardNode = findFirstExpression(child)
                CjNodeTypes.BODY -> bodyNode = findFirstExpression(child)
                else -> if (isPatternToken(child.tokenType)) {
                    patternNode = child
                }
            }
        }

        val loopStatus = declarationBuilder.cloneDeclarationStatus(CfirDeclarationStatusImpl.DEFAULT)
        val loopPattern = patternNode?.let {
            convertPattern(
                node = it,
                ownerStatus = loopStatus,
                ownerIsLocal = true,
                ownerIsVar = false,
            )
        } ?: buildWildcardPattern {
            source = node.toSource()
        }
        val variable = buildSourceDeclaration(CfirPatternVariableSymbol(callableIdFor(Name.special("<pattern-variable>")))) { symbol ->
                buildPatternVariable {
                    resolvePhase = CfirResolvePhase.RAW_CFIR
                    source = (patternNode ?: node).toSource()
                    this.symbol = symbol
                    origin = CfirDeclarationOrigin.Source
                    moduleData = baseModuleData
                attributes = CfirDeclarationAttributes.EMPTY
                isLocal = true
                status = loopStatus
                returnTypeRef = buildImplicitTypeRef()
                pattern = loopPattern
                isVar = false
            }
        }

        val iterable = rangeNode?.let { convertExpression(it) }
            ?: buildErrorExpression(reason = "Missing for-in iterable")
        val patternGuard = guardNode?.let { convertExpression(it) }

        val loop = buildForInExpression {
            source = node.toSource()
            this.condition = buildLiteralExpression {
                source = node.toSource()
                kind = CfirLiteralKind.BOOLEAN
                value = true
            }
            this.isDoWhile = false
            this.variable = variable
            this.iterable = iterable
            this.patternGuard = patternGuard
            this.body = bodyNode?.let { toBlock(it) } ?: buildBlock { source = node.toSource() }
        }
        return loop
    }

    /** 转换 while 循环。 */
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

    /** 转换 do-while 循环。 */
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

    /** 转换 return 表达式并绑定当前函数 target。 */
    private fun convertReturn(node: LighterASTNode): CfirReturnExpression {
        val resultExpr = findFirstExpression(node)
        return buildReturnExpressionWithCurrentFunctionTarget(
            source = node.toSource(),
            result = resultExpr?.let { convertExpression(it) },
        )
    }

    /** 转换 throw 表达式。 */
    private fun convertThrow(node: LighterASTNode): CfirThrowExpression {
        val exprNode = findFirstExpression(node)
        val exception = exprNode?.let { convertExpression(it) }
            ?: buildErrorExpression(node.toSourceElement(), "Missing thrown expression")
        return buildThrowExpression {
            source = node.toSource()
            this.exception = exception
        }
    }

    /** 转换 effect perform 表达式。 */
    private fun convertPerform(node: LighterASTNode): CfirPerformExpression {
        val exprNode = findFirstExpression(node)
        val performedExpression = exprNode?.let { convertExpression(it) }
            ?: buildErrorExpression(node.toSourceElement(), "Missing performed expression")
        return buildPerformExpression {
            source = node.toSource()
            expression = performedExpression
        }
    }

    /** 转换 effect resume 表达式。 */
    private fun convertResume(node: LighterASTNode): CfirResumeExpression {
        var payloadNode: LighterASTNode? = null
        var isThrowing = false
        var isWith = false

        tree.forEachChildren(node) { child ->
            when {
                child.tokenType == CjTokens.THROWING_KEYWORD -> isThrowing = true
                child.tokenType == CjTokens.IDENTIFIER && child.asText() == "with" -> isWith = true
                payloadNode == null && isExpressionToken(child.tokenType) -> payloadNode = child
            }
        }

        return buildResumeExpression {
            source = node.toSource()
            if (isThrowing) {
                throwingExpression = payloadNode?.let(::convertExpression)
            } else if (isWith) {
                withExpression = payloadNode?.let(::convertExpression)
            }
        }
    }

    /** 转换 try handle 分支中的 command type pattern。 */
    private fun convertCommandTypePattern(node: LighterASTNode): CfirCommandTypePattern {
        val bindingName = tree.findChildByType(node, CjTokens.IDENTIFIER)?.asText()?.let(Name::identifier)
        val isWildcard = tree.findChildByType(node, CjTokens.UNDERLINE) != null
        val typeRefs = tree.getChildrenByType(node, CjNodeTypes.TYPE_REFERENCE).map { typeRefNode ->
            convertTypeReference(typeRefNode, tree, source) { it.toSourceElement() }
        }
        return buildCommandTypePattern {
            source = node.toSource()
            this.bindingName = bindingName
            this.isWildcard = isWildcard
            this.typeRefs.addAll(typeRefs)
        }
    }

    /** 转换 try-with-resource 资源声明。 */
    private fun convertTryResource(node: LighterASTNode): CfirFieldVariable {
        val parameterNode = tree.findChildByType(node, CjNodeTypes.VALUE_PARAMETER)
        val nameText = parameterNode?.let { tree.findChildByType(it, CjTokens.IDENTIFIER)?.asText() }
        val resourceName = nameText?.let(Name::identifier) ?: Name.special("<error>")
        val resourceTypeRef = parameterNode
            ?.let { tree.findChildByType(it, CjNodeTypes.TYPE_REFERENCE) }
            ?.let { convertTypeReference(it, tree, source) { typeRefNode -> typeRefNode.toSourceElement() } }
            ?: buildImplicitTypeRef()
        var initializerNode: LighterASTNode? = null
        tree.forEachChildren(node) { child ->
            if (child === parameterNode) return@forEachChildren
            if (isExpressionToken(child.tokenType)) {
                initializerNode = child
            }
        }
        val resourceStatus = declarationBuilder.cloneDeclarationStatus(CfirDeclarationStatusImpl(Visibilities.Local, Modality.FINAL))
        return buildSourceDeclaration(CfirFieldVariableSymbol(callableIdFor(resourceName))) { symbol ->
            buildFieldVariable {
                resolvePhase = CfirResolvePhase.RAW_CFIR
                source = node.toSource()
                this.symbol = symbol
                origin = CfirDeclarationOrigin.Source
                moduleData = baseModuleData
                attributes = CfirDeclarationAttributes.EMPTY
                isLocal = true
                dispatchReceiverType = null
                status = resourceStatus
                returnTypeRef = resourceTypeRef
                name = resourceName
                initializer = initializerNode?.let(::convertExpression)
                isVar = false
            }
        }
    }

    /** 转换 try 表达式，包括 resource、handle、catch 与 finally。 */
    private fun convertTry(node: LighterASTNode): CfirTryExpression {
        var tryBlockNode: LighterASTNode? = null
        var resourceListNode: LighterASTNode? = null
        val handleNodes = mutableListOf<LighterASTNode>()
        val catchNodes = mutableListOf<LighterASTNode>()
        var finallyNode: LighterASTNode? = null

        tree.forEachChildren(node) { child ->
            when (child.tokenType) {
                CjNodeTypes.TRY_RESOURCE_LIST -> resourceListNode = child
                CjNodeTypes.BLOCK -> {
                    if (tryBlockNode == null) tryBlockNode = child
                }
                CjNodeTypes.HANDLE -> handleNodes.add(child)
                CjNodeTypes.CATCH -> catchNodes.add(child)
                CjNodeTypes.FINALLY -> finallyNode = child
            }
        }

        val resources = resourceListNode?.let { resourceList ->
            tree.getChildrenByType(resourceList, CjNodeTypes.TRY_RESOURCE).map { resourceNode ->
                convertTryResource(resourceNode)
            }
        } ?: emptyList()
        val tryBlock = tryBlockNode?.let { convertBlock(it) } ?: buildBlock { source = node.toSource() }
        val handlers = handleNodes.map { handleNode ->
            val commandPatternNode = tree.findChildByType(handleNode, CjNodeTypes.COMMAND_TYPE_PATTERN)
            val bodyNode = tree.findChildByType(handleNode, CjNodeTypes.BLOCK)
            buildHandleClause {
                source = handleNode.toSource()
                commandPattern = commandPatternNode?.let(::convertCommandTypePattern) ?: buildCommandTypePattern {
                    source = handleNode.toSource()
                    bindingName = null
                    isWildcard = false
                }
                body = bodyNode?.let(::convertBlock) ?: buildBlock { source = handleNode.toSource() }
            }
        }

        val catches = catchNodes.map { catchNode ->
            var catchParamNode: LighterASTNode? = null
            var catchBodyNode: LighterASTNode? = null
            tree.forEachChildren(catchNode) { child ->
                when (child.tokenType) {
                    CjNodeTypes.CATCH_PARAMETER -> catchParamNode = child
                    CjNodeTypes.BLOCK -> catchBodyNode = child
                }
            }
            val body = catchBodyNode?.let { convertBlock(it) } ?: buildBlock { source = catchNode.toSource() }
            buildCatch {
                source = catchNode.toSource()
                pattern = convertCatchPattern(catchNode, catchParamNode)
                this.body = body
            }
        }

        val finallyBlock = finallyNode?.let { fin ->
            val block = tree.findChildByType(fin, CjNodeTypes.BLOCK)
            block?.let { convertBlock(it) }
        }

        return buildTryExpression {
            source = node.toSource()
            this.resources.addAll(resources)
            this.tryBlock = tryBlock
            this.handlers.addAll(handlers)
            this.catches.addAll(catches)
            this.finallyBlock = finallyBlock
        }
    }

    // ===== Lambda =====

    /** 转换 lambda 表达式及其匿名函数声明。 */
    private fun convertLambda(node: LighterASTNode): CfirAnonymousFunctionExpression {
        val functionSymbol = CfirAnonymousFunctionSymbol()
        val valueParams = mutableListOf<org.cangnova.cangjie.cfir.declarations.CfirValueParameter>()

        // LAMBDA_EXPRESSION 内部包含 FUNCTION_LITERAL
        val funcLiteral = lambdaFunctionLiteral(node)
        val bodyNode = findLambdaBodyBlock(node)

        tree.forEachChildren(funcLiteral) { child ->
            when (child.tokenType) {
                CjNodeTypes.VALUE_PARAMETER_LIST -> {
                    tree.forEachChildren(child) { param ->
                        if (param.tokenType == CjNodeTypes.VALUE_PARAMETER) {
                            val valueParameter = declarationBuilder.convertValueParameter(
                                param,
                                functionSymbol,
                                requiresExplicitType = false,
                            )
                            if (tree.findChildByType(param, CjNodeTypes.TYPE_REFERENCE) == null) {
                                valueParameter.isLambdaParameterTypeOmitted = true
                            }
                            valueParams.add(valueParameter)
                        }
                    }
                }
            }
        }

        val functionTarget = CfirFunctionTarget(labelName = null, isLambda = true)
        val body = withContainerSymbol(functionSymbol) {
            withFunctionTarget(functionTarget) {
                bodyNode?.let { convertBlock(it) }
            }
        }
        val hasExplicitParameterList = valueParams.isNotEmpty()

        val anonymousFunction = buildSourceDeclaration(functionSymbol) { symbol ->
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
        }.also { bindFunctionTarget(functionTarget, it) }
        return buildAnonymousFunctionExpression {
            source = node.toSource()
            this.anonymousFunction = anonymousFunction
            isTrailingLambda = false
        }
    }

    /** LightTree 的 lambda body 位于 FUNCTION_LITERAL 下，spawn 与普通 lambda 必须共用同一抽取规则。 */
    private fun findLambdaBodyBlock(lambdaNode: LighterASTNode): LighterASTNode? =
        tree.findChildByType(lambdaFunctionLiteral(lambdaNode), CjNodeTypes.BLOCK)

    /** LightTree 中尾随 lambda 会包在 LAMBDA_ARGUMENT 下，这里统一还原语义上的 LAMBDA_EXPRESSION。 */
    private fun findLambdaExpression(node: LighterASTNode): LighterASTNode? {
        if (node.tokenType == CjNodeTypes.LAMBDA_EXPRESSION) return node
        tree.forEachChildren(node) { child ->
            when (child.tokenType) {
                CjNodeTypes.LAMBDA_EXPRESSION -> return child
                CjNodeTypes.LAMBDA_ARGUMENT -> tree.findChildByType(child, CjNodeTypes.LAMBDA_EXPRESSION)?.let { return it }
            }
        }
        return null
    }

    /** spawn 的线程上下文参数位于可选 VALUE_ARGUMENT_LIST 的第一个 VALUE_ARGUMENT。 */
    private fun findFirstValueArgument(node: LighterASTNode): LighterASTNode? {
        tree.findChildByType(node, CjNodeTypes.VALUE_ARGUMENT_LIST)?.let { argumentList ->
            tree.forEachChildren(argumentList) { argument ->
                if (argument.tokenType == CjNodeTypes.VALUE_ARGUMENT) return argument
            }
        }
        return null
    }

    private fun lambdaFunctionLiteral(lambdaNode: LighterASTNode): LighterASTNode =
        tree.findChildByType(lambdaNode, CjNodeTypes.FUNCTION_LITERAL) ?: lambdaNode

    /** 类型模式绑定名是冒号前的直接名称节点，LightTree 可能使用 BASIC_REFERENCE_EXPRESSION 包装普通标识符。 */
    private fun LighterASTNode.isTypePatternBindingNameNode(): Boolean =
        tokenType == CjNodeTypes.REFERENCE_EXPRESSION ||
                tokenType == BASIC_REFERENCE_EXPRESSION ||
                tokenType == CjTokens.IDENTIFIER

    // ===== Misc =====

    /** 转换数组或下标访问表达式。 */
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

    /** 转换数组字面量。 */
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

    /** 转换 tuple 字面量。 */
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

    /** 转换 `is` 类型检查表达式。 */
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

    /** 转换 catch clause 的绑定与类型 pattern。 */
    private fun convertCatchPattern(
        catchNode: LighterASTNode,
        parameterNode: LighterASTNode?,
    ): CfirCatchPattern {
        val nameText = parameterNode
            ?.let { tree.findChildByType(it, CjTokens.IDENTIFIER) }
            ?.asText()
        val bindingName = nameText?.let(Name::identifier)
        val typeRefs = parameterNode
            ?.let { tree.getChildrenByType(it, CjNodeTypes.TYPE_REFERENCE) }
            ?.map { typeRefNode ->
                convertTypeReference(typeRefNode, tree, source) { it.toSourceElement() }
            }
            .orEmpty()
        val bindingStatus = declarationBuilder.cloneDeclarationStatus(
            CfirDeclarationStatusImpl(Visibilities.Local, Modality.FINAL),
        )

        return buildCatchPattern {
            source = (parameterNode ?: catchNode).toSource()
            this.bindingName = bindingName
            isWildcard = bindingName == null
            this.typeRefs.addAll(typeRefs)
            bindingVariable = bindingName?.let { name ->
                declarationBuilder.createPatternBindingVariable(
                    source = parameterNode?.toSource(),
                    name = name,
                    status = bindingStatus,
                    isLocal = true,
                    isVar = false,
                    returnTypeRef = buildImplicitTypeRef(),
                )
            }
        }
    }

    /** 转换 `as` 等带类型 RHS 的类型操作表达式。 */
    private fun convertTypeOperator(node: LighterASTNode): CfirTypeOperator {
        var argNode: LighterASTNode? = null
        var typeRefNode: LighterASTNode? = null
        var operationToken: IElementType? = null

        tree.forEachChildren(node) { child ->
            when (child.tokenType) {
                CjNodeTypes.OPERATION_REFERENCE -> {
                    tree.forEachChildren(child) { opChild ->
                        operationToken = opChild.tokenType
                    }
                }

                CjNodeTypes.TYPE_REFERENCE -> typeRefNode = child
                else -> {
                    if (argNode == null && isExpressionToken(child.tokenType)) {
                        argNode = child
                    }
                }
            }
        }

        val operation = when (operationToken) {
            CjTokens.AS_KEYWORD -> CfirTypeOperationKind.AS
            else -> error("Unexpected binary type operator: $operationToken")
        }
        val argument = argNode?.let { convertExpression(it) }
            ?: buildErrorExpression(node.toSourceElement(), "Missing as-cast operand")
        return buildTypeOperator {
            source = node.toSource()
            this.operation = operation
            this.argument = argument
            typeRef = convertTypeReference(typeRefNode, tree, this@LightTreeRawCfirExpressionBuilder.source) {
                it.toSourceElement()
            }
        }
    }

    /** 转换 synchronized 表达式。 */
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

    /** 转换 unsafe 表达式。 */
    private fun convertUnsafe(node: LighterASTNode): CfirExpression {
        val blockNode = tree.findChildByType(node, CjNodeTypes.BLOCK)
            ?: return buildErrorExpression(node.toSourceElement(), "Missing unsafe block")
        val bodyExpression = convertBlock(blockNode)
        return buildUnsafeExpression {
            source = node.toSource()
            body = bodyExpression
        }
    }

    /** 转换 quote 表达式并保留插值表达式列表。 */
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

    /** 转换表达式位置的 macro call，并收集 [MacroSurfaceExpr]。 */
    private fun convertMacroExpression(node: LighterASTNode): CfirExpression {
        val nameNode = tree.findChildByType(node, CjNodeTypes.REFERENCE_EXPRESSION)
        val inputNode = tree.findChildByType(node, CjNodeTypes.MACRO_INPUT)
        val attrNode = tree.findChildByType(node, CjNodeTypes.MACRO_ATTR)

        val text = node.asText()
        val isForced = text.startsWith("@!")
        val nameStr = nameNode?.asText()
        val name = nameStr?.let { Name.identifier(it) }
        val surfaceId = MacroSurfaceIdGenerator.next()
        val sourceElement = node.toSource()
        val carrier = buildErrorExpressionNode {
            source = sourceElement
            diagnostic = ConeSimpleDiagnostic(
                "Macro expression `$text` is a construction-only surface and must be replaced before final provider registration.",
            )
        }
        val qualifiedName = name?.let {
            if (context.packageFqName.isRoot) {
                org.cangnova.cangjie.name.FqName.topLevel(it)
            } else {
                context.packageFqName.child(it)
            }
        }
        declarationBuilder.collectedMacroSurfaces += MacroSurfaceExpr(
            surfaceId = surfaceId,
            qualifiedName = qualifiedName,
            kind = if (isForced) MacroSurface.Kind.FORCED else MacroSurface.Kind.PLAIN,
            hasParenthesis = inputNode != null || text.hasMacroInputParentheses(),
            attrTokens = MacroPayloadTokenizer.tokenize(
                attrNode?.asText(),
                attrNode?.startOffset ?: 0,
            ).toMacroSurfaceTokens(),
            inputTokens = MacroPayloadTokenizer.tokenize(
                inputNode?.asText(),
                inputNode?.startOffset ?: 0,
            ).toMacroSurfaceTokens(),
            sourceRange = MacroSurfaceSourceRange(
                source = node.toSource(),
                startOffset = node.startOffset,
                endOffset = node.endOffset,
            ),
            scopeContext = MacroSurfaceScopeContext(
                packageFqName = context.packageFqName,
                enclosingClassFqName = null,
                enclosingFunctionName = null,
            ),
            modifiers = emptyList(),
            carriedAnnotations = emptyList(),
            capturedRawSyntax = text,
            containerContext = MacroSurfaceContainerContext(
                outerDeclarationKind = MacroSurfaceContainerContext.OuterDeclarationKind.NONE,
                isInsidePrimaryConstructor = false,
                isInsideEnumBody = false,
                isInsideBlock = false,
            ),
            replaceHandle = CfirReplaceHandle(handleId = surfaceId, carrier = carrier),
        )

        return carrier
    }

    /** 在 raw 文本中判断 macro input 是否显式包含括号。 */
    private fun String.hasMacroInputParentheses(): Boolean {
        val open = indexOf('(')
        return open >= 0 && indexOf(')', startIndex = open + 1) >= 0
    }

    // ===== 辅助方法 =====

    /** 将任意表达式节点包装成 block。 */
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

    /** 使用 [symbol] 创建 source declaration，并保持符号与声明的类型关系。 */
    private inline fun <D : CfirDeclaration, S : CfirBasedSymbol<D>> buildSourceDeclaration(
        symbol: S,
        builder: (S) -> D,
    ): D {
        val declaration = builder(symbol)

        return declaration
    }

    /** 将引用文本转换为 [Name]，非法文本使用 special name。 */
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
            CjNodeTypes.RUNE_CONSTANT, CjNodeTypes.CHARACTER_BYTE_CONSTANT,
            CjNodeTypes.BOOLEAN_CONSTANT,
            CjNodeTypes.UNIT_CONSTANT, CjNodeTypes.STRING_TEMPLATE,
            CjNodeTypes.BINARY_WITH_TYPE, CjNodeTypes.BINARY_EXPRESSION, CjNodeTypes.RANGE_EXPRESSION,
            CjNodeTypes.SLICE_EXPRESSION,
            CjNodeTypes.PREFIX_EXPRESSION, CjNodeTypes.POSTFIX_EXPRESSION,
            CjNodeTypes.OPTIONAL_EXPRESSION, CjNodeTypes.OPTIONAL_CHAIN_EXPRESSION,
            CjNodeTypes.DOT_QUALIFIED_EXPRESSION,
            CjNodeTypes.REFERENCE_EXPRESSION, BASIC_REFERENCE_EXPRESSION, CjNodeTypes.CALL_EXPRESSION,
            CjNodeTypes.SPAWN_EXPRESSION,
            CjNodeTypes.IF, CjNodeTypes.MATCH,
            CjNodeTypes.FOR, CjNodeTypes.WHILE, CjNodeTypes.DO_WHILE,
            CjNodeTypes.LET_EXPRESSION,
            CjNodeTypes.RETURN, CjNodeTypes.BREAK, CjNodeTypes.CONTINUE, CjNodeTypes.THROW,
            CjNodeTypes.PERFORM, CjNodeTypes.RESUME,
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
            CjNodeTypes.VAR_OR_ENUM_PATTERN, CjNodeTypes.TUPLE_PATTERN, CjNodeTypes.ENUM_PATTERN,
            CjNodeTypes.CONSTANT_PATTERN, CjNodeTypes.WILDCARD_PATTERN,
            -> true
            else -> false
        }

        /** 判断 tokenType 是否为语义节点（非空白/注释） */
        fun isSemanticToken(tt: IElementType): Boolean =
            isExpressionToken(tt) || isDeclarationToken(tt) || isPatternToken(tt)
                    || tt == CjNodeTypes.OPERATION_REFERENCE
                    || tt == CjNodeTypes.REFERENCE_EXPRESSION
                    || tt == BASIC_REFERENCE_EXPRESSION
    }
}

    /** 将 raw-cfir-common 的 macro payload token 映射为 providers 层 macro surface token。 */
    private fun List<org.cangnova.cangjie.cfir.builder.macro.MacroPayloadToken>.toMacroSurfaceTokens(): List<MacroSurfaceToken> {
    return map { token ->
        MacroSurfaceToken(
            text = token.text,
            startOffset = token.startOffset,
            endOffset = token.endOffset,
            kindName = token.kindName,
        )
    }
}
