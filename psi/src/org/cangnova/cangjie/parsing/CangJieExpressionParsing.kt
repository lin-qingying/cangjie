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

package org.cangnova.cangjie.parsing

import com.google.common.collect.ImmutableMap
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiBuilderUtil.rawTokenText
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Pair
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.cangnova.cangjie.lexer.CjSingleValueToken
import org.cangnova.cangjie.lexer.CjToken
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.lexer.CjTokens.*
import org.cangnova.cangjie.messages.CangJieParsingBundle
import org.cangnova.cangjie.messages.ParsingDiagnosticHint
import org.cangnova.cangjie.parsing.CangJieParsing.Companion.PARAMETER_NAME_RECOVERY_SET
import org.cangnova.cangjie.psi.CjNodeTypes
import org.cangnova.cangjie.psi.CjNodeTypes.*
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes.BASIC_REFERENCE_EXPRESSION

/**
 * 表示 `CangJieExpressionParsing`，承载仓颉语法解析中的语法节点、索引桩或辅助模型。
 */
open class CangJieExpressionParsing(
    builder: SemanticWhitespaceAwarePsiBuilder,
    /**
     * 保存 `cangJieParsing` 的内部状态，供仓颉语法解析实现维护节点缓存或解析上下文。
     */
    private val cangJieParsing: CangJieParsing,
    isLazy: Boolean,
) : AbstractCangJieParsing(
    builder,
    isLazy,
) {
    /** 同次解析的真实根、直接 base/tuple children、源码范围和破损状态，不保存 marker 或 AST 身份。 */
    data class ParsedExpression internal constructor(
        val root: Root,
        val startOffset: Int,
        val endOffset: Int,
        val hasBroken: Boolean,
        val isBroken: Boolean,
        val base: ParsedExpression? = null,
        val children: List<ParsedExpression> = emptyList(),
        val hasQuestSuffix: Boolean = false,
    ) {
        init {
            require(startOffset in 0..endOffset)
            require(!isBroken || hasBroken)
            when (root) {
                MemberRoot, IndexRoot, CallRoot, OptionalRoot, OptionalChainRoot, ParenRoot -> require(base != null)
                else -> Unit
            }
        }

        /** 仅原生 assignment 的 OptionalChain 外包装仍属于初始化器禁止的赋值根；Paren 不透明。 */
        internal val isAssignmentRoot: Boolean
            get() = root == AssignmentRoot || (root == OptionalChainRoot && checkNotNull(base).root == AssignmentRoot)

        /** 根标签继续服务原 Lambda/LetPattern/Assignment/Macro 调用合同，并保存原生左值分派所需种类。 */
        sealed interface Root
        data object Missing : Root
        data object Other : Root
        data object LambdaRoot : Root
        data object LetPatternRoot : Root
        data object AssignmentRoot : Root
        data object RefRoot : Root
        data object PrimitiveRoot : Root
        data object WildcardRoot : Root
        data object MemberRoot : Root
        data object IndexRoot : Root
        data object CallRoot : Root
        data object OptionalRoot : Root
        data object OptionalChainRoot : Root
        data object IncOrDecRoot : Root
        data object ParenRoot : Root
        data object TupleRoot : Root
        data object UnitRoot : Root
        data object IfRoot : Root
        class MacroRoot internal constructor(internal val offset: Int) : Root
    }

    /** 真实 syntax error production 的前后计数；不能用包含 warning/role 的诊断时钟判断 broken。 */
    private data class ExpressionStart(val offset: Int, val previousTokenEnd: Int?, val syntaxErrorCount: Long)

    private fun expressionStart(): ExpressionStart = ExpressionStart(
        builder.currentOffset, builder.lastConsumedTokenEndOffset, builder.macroParsingJournal.syntaxErrorCount,
    )

    private fun ExpressionStart.finishRoot(
        root: ParsedExpression.Root,
        base: ParsedExpression? = null,
        children: List<ParsedExpression> = emptyList(),
    ): ParsedExpression {
        val consumedEnd = builder.lastConsumedTokenEndOffset
        val end = if (consumedEnd == previousTokenEnd) offset else checkNotNull(consumedEnd)
        check(offset <= end)
        val syntaxBroken = builder.macroParsingJournal.syntaxErrorCount != syntaxErrorCount
        return ParsedExpression(root, offset, end,
            syntaxBroken || root == ParsedExpression.Missing || base?.hasBroken == true || children.any { it.hasBroken },
            root == ParsedExpression.Missing, base, children.toList())
    }

    private fun ParsedExpression.withCurrentSyntaxErrors(start: ExpressionStart): ParsedExpression =
        if (builder.macroParsingJournal.syntaxErrorCount == start.syntaxErrorCount) this
        else copy(hasBroken = true)

    /** 对位 MacroInvocationDecl 的真实输入属性；仅在本次 parser 调用栈中传递，不从 AST 后代补推。 */
    internal data class ParsedMacroInvocation(val expression: ParsedExpression, val isEnumConstructor: Boolean)

    /** 只有直接根宏接受本入口的scope/逗号refinement，后代宏保持各自的原生输入。 */
    private fun ParsedExpression.refineMacro(transform: (MacroParsingFacts) -> MacroParsingFacts) {
        val macro = root
        if (macro is ParsedExpression.MacroRoot) builder.macroParsingJournal.refine(macro.offset, transform)
    }
    /**
     * 保存 `ARROW_SET` 的内部状态，供仓颉语法解析实现维护节点缓存或解析上下文。
     */
    private val ARROW_SET = TokenSet.create(DOUBLE_ARROW)
    /**
     * 保存 `ARROW_COMMA_SET` 的内部状态，供仓颉语法解析实现维护节点缓存或解析上下文。
     */
    private val ARROW_COMMA_SET = TokenSet.create(DOUBLE_ARROW, COMMA)
    /**
     * 保存 `TRY_RESOURCE_TYPE_RECOVERY_SET` 的内部状态，供仓颉语法解析实现维护节点缓存或解析上下文。
     */
    private val TRY_RESOURCE_TYPE_RECOVERY_SET = TokenSet.create(EQ, COMMA, RPAR)
    /**
     * 保存 `VARRAY_TYPE_RECOVERY_SET` 的内部状态，供仓颉语法解析实现维护节点缓存或解析上下文。
     */
    private val VARRAY_TYPE_RECOVERY_SET = TokenSet.create(GT, EOL_OR_SEMICOLON)
    /**
     * 保存 `VARRAY_LINE_RECOVERY_SET` 的内部状态，供仓颉语法解析实现维护节点缓存或解析上下文。
     */
    private val VARRAY_LINE_RECOVERY_SET = TokenSet.create(EOL_OR_SEMICOLON)

    /**
     * 保存 `quoteExpressionParsing`，供仓颉语法解析流程读取节点结构或语义信息。
     */
    val quoteExpressionParsing = CangJieQuoteExpressionParsing(
        builder,
        this,
        true
    )

    /**
     * 提供 `create` 操作，封装仓颉语法解析节点的访问、构造或判断逻辑。
     */
    public override fun create(builder: SemanticWhitespaceAwarePsiBuilder): CangJieParsing =
        cangJieParsing.create(builder)


    /** 类型 RHS 没有表达式根；普通 RHS 保留同次解析产品，节点类型仍由原优先级协议决定。 */
    data class ParsedBinaryOperand internal constructor(val nodeType: IElementType, val expression: ParsedExpression?)

    // ==================== 运算符优先级定义 ====================

    /**
     * 运算符优先级枚举
     *
     * 定义了仓颉语言中各种运算符的优先级和结合性。枚举值的声明顺序从高到低，
     * 即先声明的优先级更高。每个优先级包含一组具有相同优先级的运算符。
     *
     * 优先级顺序（从高到低）：
     * 1. POSTFIX - 后缀运算符（++, --, ., ?.）
     * 2. PREFIX - 前缀运算符（-, !）
     * 3. EXPONENTIATION - 幂运算符（**）
     * 4. MULTIPLICATIVE - 乘法运算符（*, /, %）
     * 5. ADDITIVE - 加法运算符（+, -）
     * 6. SHIFT - 移位运算符（<<, >>）
     * 7. RANGE - 区间运算符（.., ..=）
     * 8. TYPE_AND_COMPARISON - 类型/比较运算符（as, is, <, >, <=, >=）
     * 9. EQUALITY - 相等运算符（==, !=）
     * 10. BIT_AND - 按位与运算符（&）
     * 11. BIT_XOR - 按位异或运算符（^）
     * 12. BIT_OR - 按位或运算符（|）
     * 13. CONJUNCTION - 逻辑与运算符（&&）
     * 14. DISJUNCTION - 逻辑或运算符（||）
     * 15. COALESCING - 合并运算符（??）
     * 16. FLOW - 流运算符（|>, ~>）
     * 17. ASSIGNMENT - 赋值运算符（=, +=, -=, *=, /=, %=, &=, &&=, ||=, |=, ^=, <<=, >>=, **=）
     *
     * @param operations 该优先级包含的运算符token类型
     */
    @SuppressWarnings("UnusedDeclaration")
    enum class Precedence(vararg operations: IElementType) {
        /**
         * 后缀运算符优先级
         *
         * 包含：
         * - `++` - 后缀自增
         * - `--` - 后缀自减
         * quest 链式后缀由专门的 optional chain 解析处理
         */
        POSTFIX(
            PLUSPLUS,
            MINUSMINUS,
            DOT,
        ),

        /**
         * 前缀运算符优先级
         *
         * 包含：
         * - `-` - 负号
         * - `!` - 逻辑非
         *
         * 注意：此优先级不调用标准的parseHigherPrecedence方法，
         * 而是直接在parsePrefixExpression中处理。
         */
        PREFIX(MINUS, /*PLUS,*/ EXCL) {
            context(context: ParsingContext)
            override fun parseHigherPrecedence(parser: CangJieExpressionParsing, firstOperand: (() -> ParsedExpression)?): ParsedExpression {
                throw IllegalStateException("Don't call this method")
            }
        },

        EXPONENTIATION(MULMUL) {
            context(context: ParsingContext)
            override fun parseHigherPrecedence(parser: CangJieExpressionParsing, firstOperand: (() -> ParsedExpression)?): ParsedExpression {
                return with(context) {
                    if (firstOperand == null) parser.parsePrefixExpression()
                    else parser.parsePostfixExpression(firstOperand)
                }
            }

            context(context: ParsingContext)
            override fun parseRightHandSide(
                operation: IElementType,
                parser: CangJieExpressionParsing,
            ): ParsedBinaryOperand {
                // 官方 parser 将 `**` 作为右结合二元运算解析。
                return ParsedBinaryOperand(BINARY_EXPRESSION, parser.parseBinaryExpression(this))
            }
        },

        /**
         * 乘法运算符优先级
         *
         * 包含：
         * - `*` - 乘法
         * - `/` - 除法
         * - `%` - 取模
         */
        MULTIPLICATIVE(MUL, DIV, PERC),

        /**
         * 加法运算符优先级
         *
         * 包含：
         * - `+` - 加法
         * - `-` - 减法
         */
        ADDITIVE(PLUS, MINUS),

        SHIFT(LTLT, GTGT),

        /**
         * 区间运算符优先级
         *
         * 包含：
         * - `..` - 开区间
         * - `..=` - 闭区间
         *
         * 解析为RANGE_EXPRESSION节点。
         */
        RANGE(CjTokens.RANGE, RANGEEQ) {
            context(context: ParsingContext)
            override fun parseRightHandSide(
                operation: IElementType,
                parser: CangJieExpressionParsing,

                ): ParsedBinaryOperand {
                if (operation == CjTokens.RANGE || operation == RANGEEQ) {
                    return ParsedBinaryOperand(RANGE_EXPRESSION, with(context) {
                        parser.parseRangeExpression()
                    })
                }
                return super.parseRightHandSide(operation, parser)
            }
        },

        /**
         * 类型转换、类型检查和比较运算符优先级
         *
         * 包含：
         * - `as` - 类型转换
         * - `is` - 类型检查
         * - `<`, `>`, `<=`, `>=` - 比较
         *
         * 官方 token 优先级表中这几类运算符同属一个优先级；其中 `as` / `is`
         * 的右侧解析类型引用而非表达式。
         */
        TYPE_AND_COMPARISON(AS_KEYWORD, IS_KEYWORD, LT, GT, LTEQ, GTEQ) {
            context(context: ParsingContext)
            override fun parseRightHandSide(
                operation: IElementType,
                parser: CangJieExpressionParsing,
            ): ParsedBinaryOperand {
                if (operation === AS_KEYWORD || operation === IS_KEYWORD) {
                    parser.mark().drop()
                    parser.cangJieParsing.parseTypeRefWithoutIntersections()
                    return ParsedBinaryOperand(if (operation === AS_KEYWORD) BINARY_WITH_TYPE else IS_EXPRESSION, null)
                }
                return super.parseRightHandSide(operation, parser)
            }
        },

        /**
         * 相等运算符优先级
         *
         * 包含：
         * - `==` - 等于
         * - `!=` - 不等于
         */
        EQUALITY(EQEQ, EXCLEQ),

        BIT_AND(AND),

        BIT_XOR(XOR),

        BIT_OR(OR),

        /**
         * 逻辑与运算符优先级
         *
         * 包含：
         * - `&&` - 逻辑与（短路求值）
         */
        CONJUNCTION(ANDAND),

        /**
         * 逻辑或运算符优先级
         *
         * 包含：
         * - `||` - 逻辑或（短路求值）
         */
        DISJUNCTION(OROR),

        /**
         * 合并运算符优先级
         *
         * 包含：
         * - `??` - 空值合并运算符
         *
         * 用于处理可选类型，当左侧为None时使用右侧值。
         */
        COALESCING(CjTokens.COALESCING) {
            context(context: ParsingContext)
            override fun parseRightHandSide(
                operation: IElementType,
                parser: CangJieExpressionParsing,
            ): ParsedBinaryOperand {
                return ParsedBinaryOperand(BINARY_EXPRESSION, parser.parseBinaryExpression(this))
            }
        },

        /**
         * 流运算符优先级
         *
         * 包含：
         * - `|>` - 管道运算符
         * - `~>` - 组合运算符
         */
        FLOW(PIPELINE, COMPOSITION),

        /**
         * 赋值运算符优先级（最低优先级）
         *
         * 包含：
         * - `=` - 赋值
         * - `+=` - 加法赋值
         * - `-=` - 减法赋值
         * - `*=` - 乘法赋值
         * - `/=` - 除法赋值
         * - `%=` - 取模赋值
         * - `&=` - 按位与赋值
         * - `&&=` - 逻辑与赋值
         * - `||=` - 逻辑或赋值
         * - `|=` - 按位或赋值
         * - `^=` - 按位异或赋值
         * - `<<=` - 左移赋值
         * - `>>=` - 右移赋值
         * - `**=` - 幂运算赋值
         */
        ASSIGNMENT(
            EQ,
            PLUSEQ,
            MINUSEQ,
            MULTEQ,
            DIVEQ,
            PERCEQ,
            ANDEQ,
            ANDANDEQ,
            OROREQ,
            OREQ,
            XOREQ,
            LTLTEQ,
            GTGTEQ,
            MULMULEQ,
        ),
        ;

        /**
         * 保存 `higher` 的内部状态，供仓颉语法解析实现维护节点缓存或解析上下文。
         */
        private var higher: Precedence? = null
        /**
         * 保存 `operations` 的内部状态，供仓颉语法解析实现维护节点缓存或解析上下文。
         */
        private val operations: TokenSet

        @OptIn(ExperimentalStdlibApi::class)
        companion object {
            init {
                val values: Array<Precedence> = entries.toTypedArray()
                for (precedence in values) {
                    val ordinal: Int = precedence.ordinal
                    precedence.higher = if (ordinal > 0) values[ordinal - 1] else null
                }
            }
        }

        init {
            this.operations = TokenSet.create(*operations)
        }

        /**
         * 提供 `getOperations` 操作，封装仓颉语法解析节点的访问、构造或判断逻辑。
         */
        fun getOperations(): TokenSet {
            return operations
        }

        /**
         * 提供 `parseRightHandSide` 操作，封装仓颉语法解析节点的访问、构造或判断逻辑。
         */
        context(context: ParsingContext)
        open fun parseRightHandSide(
            operation: IElementType,
            parser: CangJieExpressionParsing,

            ): ParsedBinaryOperand {
            if (parser.atStatementOnlyTokenAfterPhysicalNewline()) {
                parser.error("Expecting an expression")
                return ParsedBinaryOperand(BINARY_EXPRESSION, parser.run {
                    expressionStart().finishRoot(ParsedExpression.Missing)
                })
            }
            return ParsedBinaryOperand(BINARY_EXPRESSION, parseHigherPrecedence(parser))
        }

        /**
         * 提供 `parseHigherPrecedence` 操作，封装仓颉语法解析节点的访问、构造或判断逻辑。
         */
        context(context: ParsingContext)
        open fun parseHigherPrecedence(parser: CangJieExpressionParsing, firstOperand: (() -> ParsedExpression)? = null): ParsedExpression =
            parser.parseBinaryExpression(checkNotNull(higher), firstOperand)
    }

    // ==================== 表达式解析核心方法 ====================

    /**
     * 解析表达式
     *
     * Grammar:
     * ```
     * expression
     *   : macroExpression
     *   | synchronizedExpression
     *   | binaryExpression
     *   ;
     * ```
     */
    context(context: ParsingContext)
    fun parseExpression(kind: CjExprKind = CjExprKind.ALL): ParsedExpression {
        val start = expressionStart()
        val startOffset = start.offset
        val grammarKind = if (kind == CjExprKind.VAR_INIT) CjExprKind.ALL else kind
        val expressionContext = context.copy(
            memberScope = null,
            expressionGrammarKind = grammarKind,
            allowLetExpression = kind.isConditionKind(),
        )
        val result = with(expressionContext) {
            if (kind == CjExprKind.UNKNOWN_EXPR && at(LET_KEYWORD)) {
                parseLetExpression()
                return@with start.finishRoot(ParsedExpression.LetPatternRoot)
            } else if (kind == CjExprKind.INDEX_EXPR) {
                return@with parseIndexExpression()
            } else if (expressionContext.enableCustomAnnotation && isIfAvailableExpressionStart()) {
                parseIfAvailableExpression()
                return@with start.finishRoot(ParsedExpression.Other)
            } else if (at(SYNCHRONIZED_KEYWORD)) {
                cangJieParsing.parseSynchronizedExpression()
                return@with start.finishRoot(ParsedExpression.Other)
            } else if (!atSet(expressionContext.expressionFirst)) {
                error(CangJieParsingBundle.message("parsing.error.expecting.expression"))
                return@with start.finishRoot(ParsedExpression.Missing)
            }
            parseBinaryExpression(if (kind == CjExprKind.LET_PATTERN) Precedence.RANGE else Precedence.ASSIGNMENT)
        }
        if (kind == CjExprKind.VAR_INIT && result.isAssignmentRoot) {
            errorOnRange(startOffset, checkNotNull(builder.lastConsumedTokenEndOffset),
                CangJieParsingBundle.message("parsing.error.assignment.in.initializer"))
        }
        result.refineMacro { it.copy(scope = MacroParsingScope.Expression(kind)) }
        return result.withCurrentSyntaxErrors(start)
    }

    private fun CjExprKind.isConditionKind(): Boolean = when (this) {
        CjExprKind.IF_COND_EXPR, CjExprKind.WHILE_COND_EXPR,
        CjExprKind.EXPR_IN_IF_COND_TUPLE, CjExprKind.EXPR_IN_WHILE_COND_TUPLE -> true
        else -> false
    }

    /** 原生 INDEX_EXPR 在低层 ALL 文法解析范围两端，随后继续完整表达式运算。 */
    context(context: ParsingContext)
    private fun parseIndexExpression(): ParsedExpression {
        val start = expressionStart()
        if (atSet(Precedence.ASSIGNMENT.getOperations())) {
            errorOnCurrentToken(CangJieParsingBundle.message("parsing.error.expecting.expression"))
            builder.withNLTokenSkipping(false) { skipUntil(TokenSet.create(NL, SEMICOLON, RBRACKET)) }
            return start.finishRoot(ParsedExpression.Missing)
        }
        return parseBinaryExpression(Precedence.ASSIGNMENT) {
            with(context.copy(expressionGrammarKind = CjExprKind.ALL, allowLetExpression = false)) {
                parseIndexBase()
            }
        }
    }

    context(context: ParsingContext)
    private fun parseIndexBase(): ParsedExpression {
        val start = expressionStart()
        val range = mark()
        val hasStart = !at(RANGE) && !at(RANGEEQ)
        val first = if (hasStart) parseBinaryExpression(Precedence.SHIFT) else null
        if (!at(RANGE) && !at(RANGEEQ)) {
            range.drop()
            return checkNotNull(first)
        }
        val closed = at(RANGEEQ)
        parseOperationReference()
        val hasEnd = !at(RBRACKET) && !at(COLON)
        val children = arrayListOf<ParsedExpression>()
        if (first != null) children += first
        if (hasEnd) children += parseBinaryExpression(Precedence.SHIFT)
        if (closed && !hasEnd) errorOnCurrentToken(CangJieParsingBundle.message("parsing.error.expecting.expression"))
        if (at(COLON)) {
            if (!hasStart || !hasEnd) {
                errorOnCurrentToken(CangJieParsingBundle.message("parsing.error.index.range.step"))
                builder.withNLTokenSkipping(false) { skipUntil(TokenSet.create(NL, SEMICOLON, RBRACKET)) }
            } else {
                advance()
                children += parseBinaryExpression(Precedence.SHIFT)
            }
        }
        range.done(if (hasStart && hasEnd) RANGE_EXPRESSION else SLICE_EXPRESSION)
        return start.finishRoot(ParsedExpression.Other, children = children)
    }

    /**
     * 解析官方 `IfAvailableExpr`。
     *
     * 仅 EnableCustomAnno 的高层入口使用固定三实参 grammar；普通源码仍是宏调用。
     * INDEX_EXPR 和运算右操作数保持自己的低层宏原子入口。
     */
    context(context: ParsingContext)
    private fun parseIfAvailableExpression() {
        val expression = mark()
        advance() // @
        if (at(IDENTIFIER)) {
            advance() // IfAvailable
        } else {
            error(CangJieParsingBundle.message("parsing.error.expecting.identifier"))
        }

        expect(LPAR, CangJieParsingBundle.message("parsing.error.expecting.symbol", "("))
        if (!parseValueArgument()) {
            errorOnCurrentToken(CangJieParsingBundle.message("parsing.error.ifavailable.argument.name"))
        }
        if (!consumeIf(COMMA)) {
            finishBrokenIfAvailable(expression)
            return
        }
        parseIfAvailableLambdaOrExpression()
        if (!consumeIf(COMMA)) {
            finishBrokenIfAvailable(expression)
            return
        }
        parseIfAvailableLambdaOrExpression()
        expect(RPAR, CangJieParsingBundle.message("parsing.error.expecting.symbol", ")"))
        expression.done(IF_AVAILABLE_EXPRESSION)
    }

    /** 原生缺 comma 的失败分支先消费当前词元并结束节点，再恢复到右括号。 */
    context(context: ParsingContext)
    private fun finishBrokenIfAvailable(expression: PsiBuilder.Marker) {
        val message = CangJieParsingBundle.message("parsing.error.expecting.symbol", ",")
        if (eof()) errorOnCurrentToken(message) else errorAndAdvance(message)
        expression.done(IF_AVAILABLE_EXPRESSION)
        skipUntil(TokenSet.create(RPAR))
    }

    /** 检查最终直接 LambdaExpr 根；前缀、二元、调用及 ParenExpr 都不能冒充 lambda。 */
    context(context: ParsingContext)
    private fun parseIfAvailableLambdaOrExpression() {
        val start = builder.currentOffset
        val root = parseExpression()
        if (root.root != ParsedExpression.LambdaRoot) {
            val end = if (root.root == ParsedExpression.Missing) start else checkNotNull(builder.lastConsumedTokenEndOffset)
            errorOnRange(start, end, CangJieParsingBundle.message("parsing.error.ifavailable.literal.lambda"))
        }
    }

    /** 解析官方 annotation-lambda：`@Annotation { => body }`。 */
    context(context: ParsingContext)
    private fun parseAnnotatedLambdaExpression() {
        val annotated = mark()
        cangJieParsing.parseAnnotations()
        parseFunctionLiteral()
        annotated.done(ANNOTATED_EXPRESSION)
    }

    /** 判断当前 `@` 是否是直接修饰 lambda 的内置 annotation。 */
    context(context: ParsingContext)
    private fun isAnnotationLambdaStart(): Boolean {
        if (!cangJieParsing.isBuiltInAnnotation()) return false
        val probe = mark()
        cangJieParsing.parseAnnotations()
        val result = at(LBRACE)
        probe.rollbackTo()
        return result
    }

    /**
     * 解析二元表达式
     *
     * Grammar:
     * ```
     * binaryExpression
     *   : prefixExpression (operator prefixExpression)*
     *   ;
     * ```
     *
     * @param precedence 运算符优先级
     */
    context(context: ParsingContext)
    private fun parseBinaryExpression(precedence: Precedence, firstOperand: (() -> ParsedExpression)? = null): ParsedExpression {
        val start = expressionStart()
        var expression = mark()
        var root = precedence.parseHigherPrecedence(this, firstOperand)
        var previousAssignment: ParsedOperationToken? = null
        var previousAssignmentRight: ParsedExpression? = null

        while (!interruptedWithNewLine() && precedence.getOperations().contains(getGtTokenType())) {
            val operation = checkNotNull(getGtTokenType())
            if (context.stopAtConditionOperatorInLetInitializer &&
                (operation === ANDAND || operation === OROR)
            ) break
            if (root.root == ParsedExpression.LetPatternRoot && context.expressionGrammarKind.isConditionKind() &&
                precedence.ordinal > Precedence.RANGE.ordinal && operation != ANDAND && operation != OROR
            ) break

            if (precedence == Precedence.ASSIGNMENT) {
                val token = currentOperationToken(operation)
                checkLeftExpression(
                    if (previousAssignment == null) root else checkNotNull(previousAssignmentRight),
                    token, previousAssignment,
                )
                previousAssignment = token
            }
            parseOperationReference(operation)
            val right = precedence.parseRightHandSide(operation, this)
            if (precedence == Precedence.ASSIGNMENT) previousAssignmentRight = checkNotNull(right.expression)
            expression.done(right.nodeType)
            val left = root
            root = start.finishRoot(
                if (precedence == Precedence.ASSIGNMENT) ParsedExpression.AssignmentRoot else ParsedExpression.Other,
                children = listOfNotNull(left, right.expression),
            )
            // 对位 N ParseExprWithRightExprOrType：赋值检查与 RHS 消费后才包装整个 quest assignment。
            if (precedence == Precedence.ASSIGNMENT && left.hasQuestSuffix) {
                expression = expression.precede()
                expression.done(OPTIONAL_CHAIN_EXPRESSION)
                root = start.finishRoot(ParsedExpression.OptionalChainRoot, base = root)
            }
            expression = expression.precede()
        }
        expression.drop()
        return root.withCurrentSyntaxErrors(start)
    }

    /** 对位固定 N CheckLeftExpression：首 assignment 与重复非结合操作符是不同检查。 */
    context(context: ParsingContext)
    private fun checkLeftExpression(
        left: ParsedExpression,
        operation: ParsedOperationToken,
        previousAssignment: ParsedOperationToken?,
    ) {
        if (previousAssignment == null) {
            // tuple 子项有各自诊断；tuple 根始终返回 true，compound 的限制独立发表。
            if (!isLeftValueExpression(left, operation) && !left.hasBroken) {
                diagInvalidLeftHandExpr(left, operation)
            }
            if (left.root == ParsedExpression.TupleRoot && operation.type !== EQ) {
                errorOnCurrentToken(CangJieParsingBundle.message("parsing.error.cannot.operator.tuple", operation.text),
                    operation = operation)
            }
        } else if (!left.hasBroken) {
            errorOnCurrentToken(CangJieParsingBundle.message("parsing.error.chained.none.associative", "assignment"),
                listOf(ParsingDiagnosticHint(previousAssignment.startOffset, previousAssignment.endOffset)), operation)
        }
    }

    context(context: ParsingContext)
    private fun diagInvalidLeftHandExpr(left: ParsedExpression, operation: ParsedOperationToken) {
        if (left.isBroken) return
        errorOnCurrentToken(CangJieParsingBundle.message("parsing.error.invalid.assignment.lhs", operation.text),
            listOf(ParsingDiagnosticHint(left.startOffset, left.endOffset,
                CangJieParsingBundle.message("parsing.error.hint.cannot.assign.expression"))), operation)
    }

    /** 只按真实根分派；Paren 不透明，tuple 逐 child 报告，call 实参不参与左值资格判断。 */
    context(context: ParsingContext)
    private fun isLeftValueExpression(expression: ParsedExpression, operation: ParsedOperationToken): Boolean =
        when (expression.root) {
            ParsedExpression.RefRoot, ParsedExpression.WildcardRoot, ParsedExpression.OptionalChainRoot -> true
            ParsedExpression.TupleRoot -> {
                for (child in expression.children) {
                    if (!isLeftValueExpression(child, operation)) diagInvalidLeftHandExpr(child, operation)
                }
                true
            }
            ParsedExpression.MemberRoot, ParsedExpression.IndexRoot -> isLeftAuxExpression(checkNotNull(expression.base))
            else -> false
        }

    private fun isLeftAuxExpression(expression: ParsedExpression): Boolean = when (expression.root) {
        ParsedExpression.RefRoot, ParsedExpression.PrimitiveRoot -> true
        ParsedExpression.OptionalRoot, ParsedExpression.MemberRoot, ParsedExpression.CallRoot, ParsedExpression.IndexRoot ->
            isLeftAuxExpression(checkNotNull(expression.base))
        else -> false
    }

    /**
     * 解析前缀表达式
     *
     * Grammar:
     * ```
     * prefixExpression
     *   : prefixOperator* postfixExpression
     *   ;
     * ```
     */
    context(context: ParsingContext)
    fun parsePrefixExpression(): ParsedExpression {
        val start = expressionStart()
        builder.disableJoiningComplexTokens()
        if (atSet(Precedence.PREFIX.getOperations())) {
            val expression = mark()
            parseOperationReference()
            builder.restoreJoiningComplexTokensState()
            val operandGrammar = when (context.expressionGrammarKind) {
                CjExprKind.IF_COND_EXPR, CjExprKind.WHILE_COND_EXPR,
                CjExprKind.EXPR_IN_IF_COND_TUPLE, CjExprKind.EXPR_IN_WHILE_COND_TUPLE -> CjExprKind.ALL
                else -> context.expressionGrammarKind
            }
            val operand = with(context.copy(expressionGrammarKind = operandGrammar, allowLetExpression = operandGrammar.isConditionKind())) {
                parsePrefixExpression()
            }
            expression.done(PREFIX_EXPRESSION)
            return start.finishRoot(ParsedExpression.Other, base = operand)
        } else {
            if (at(MINUSMINUS) || at(PLUSPLUS) || at(PLUS)) {
                errorAndAdvance(
                    CangJieParsingBundle.message(
                        "parsing.error.expected.expression.or.declaration",
                        builder.tokenText ?: "<unknown>"
                    )
                )
            }
            builder.restoreJoiningComplexTokensState()
            return parsePostfixExpression().withCurrentSyntaxErrors(start)
        }
    }

    /**
     * 解析后缀表达式
     *
     * Grammar:
     * ```
     * postfixExpression
     *   : atomicExpression postfixOperation*
     *   ;
     *
     * postfixOperation
     *   : postfixOperator
     *   | arrayAccess
     *   | callSuffix
     *   | memberAccessOperation
     *   ;
     * ```
     */
    context(context: ParsingContext)
    private fun parsePostfixExpression(firstOperand: (() -> ParsedExpression)? = null): ParsedExpression {
        val start = expressionStart()
        var expression = mark()

        var root =
            if (firstOperand != null) {
                firstOperand()
            } else if (atSet(BASICTYPES)) {
                val mark = mark()
                advance()
                mark.done(BASIC_REFERENCE_EXPRESSION)
                start.finishRoot(ParsedExpression.PrimitiveRoot)
            } else {
                if (at(VARRAY_KEYWORD)) {
                    val mark = mark()
                    advance()

                    val typeArgumentList = mark()
                    if (!at(LT)) {
                        typeArgumentList.drop()
                        error(PARSE_VARRAY_TYPE_PARAMETER_MESSAGE)
                        mark.done(REFERENCE_EXPRESSION)
                        start.finishRoot(ParsedExpression.RefRoot)
                    } else {
                        var varrayTypeIsBroken = false
                        val typeArgumentsMismatch = mark()
                        advance()
                        val projection = mark()
                        val typeArgumentStartsCorrectly = atVArrayTypeArgumentStart()

                        cangJieParsing.parseTypeRef(TYPE_ARGUMENT_LIST_STOPPERS)

                        projection.done(TYPE_PROJECTION)

                        if (!typeArgumentStartsCorrectly) {
                            varrayTypeIsBroken = true
                            typeArgumentsMismatch.drop()
                            skipUntil(VARRAY_TYPE_RECOVERY_SET)
                            if (at(GT)) {
                                advance()
                            }
                        } else if (!at(COMMA)) {
                            varrayTypeIsBroken = true
                            if (at(GT)) {
                                advance()
                            } else {
                                advanceVArrayMismatchLookahead()
                            }
                            typeArgumentsMismatch.error(PARSE_VARRAY_TYPE_ARGS_MISMATCH_MESSAGE)
                            skipUntil(VARRAY_LINE_RECOVERY_SET)
                        } else {
                            typeArgumentsMismatch.drop()
                            advance()

                            if (!at(DOLLAR)) {
                                varrayTypeIsBroken = true
                                skipVArrayMismatchTrivia()
                                val mismatch = mark()
                                advanceVArrayMismatchLookahead()
                                mismatch.error(PARSE_VARRAY_TYPE_ARGS_MISMATCH_MESSAGE)
                                skipUntil(VARRAY_LINE_RECOVERY_SET)
                            } else {
                                val dollar = mark()
                                advance()

                                val tokenStartsOnNextPhysicalLine = physicalNewlineBeforeCurrentToken()
                                if (tokenStartsOnNextPhysicalLine || !at(INTEGER_LITERAL)) {
                                    varrayTypeIsBroken = true
                                    dollar.error(PARSE_EXPECT_INTEGER_LITERAL_VARRAY_MESSAGE)
                                    if (!tokenStartsOnNextPhysicalLine) {
                                        skipUntilPhysicalLineEnd()
                                    }
                                } else {
                                    dollar.drop()
                                    advance()
                                    if (!expect(GT, CangJieParsingBundle.message("parsing.error.expecting.symbol", ">"))) {
                                        varrayTypeIsBroken = true
                                    }
                                }
                            }
                        }

                        typeArgumentList.done(TYPE_ARGUMENT_LIST)

                        mark.done(REFERENCE_EXPRESSION)
                        if (!varrayTypeIsBroken && !at(LPAR) && !at(LBRACE)) {
                            error(PARSE_VARRAY_WITH_PAREN_MESSAGE)
                        }

                        start.finishRoot(ParsedExpression.RefRoot)
                    }
                } else if (atSet(BASICTYPES)) {
                    errorAndAdvance(
                        CangJieParsingBundle.message(
                            "parsing.error.expected.expression.or.declaration.keyword",
                            builder.tokenText ?: "<unknown>"
                        )
                    )
                    start.finishRoot(ParsedExpression.Missing)
                } else {
                    parseAtomicExpression()
                }
            }

        var hasOptionalChain = false

        while (true) {
            if (interruptedWithNewLine()) {
                break
            } else if (parseQuestChainSuffix(expression)) {
                hasOptionalChain = true
                expression.done(OPTIONAL_EXPRESSION)
                root = start.finishRoot(ParsedExpression.OptionalRoot, base = root)
            } else if (at(LBRACKET)) {
                parseArrayAccess()
                expression.done(ARRAY_ACCESS_EXPRESSION)
                root = start.finishRoot(ParsedExpression.IndexRoot, base = root)
            } else if (parseCallSuffix()) {
                expression.done(CALL_EXPRESSION)
                root = start.finishRoot(ParsedExpression.CallRoot, base = root)
            } else if (at(DOT)) {
                val expressionType: IElementType = DOT_QUALIFIED_EXPRESSION
                advance()
                if (root.root == ParsedExpression.Missing) {
                    expression.drop()
                    expression = mark()
                    root = parseAtomicExpression()
                    continue
                }
                val selector = parseSelectorCallExpression { name ->
                    // 成员产品在参数解析前完成；参数错误只进入随后形成的 Call。
                    start.finishRoot(ParsedExpression.MemberRoot, base = root, children = listOf(name))
                }
                expression.done(expressionType)
                root = when (selector.root) {
                    ParsedExpression.CallRoot -> start.finishRoot(ParsedExpression.CallRoot, base = checkNotNull(selector.base))
                    ParsedExpression.MemberRoot -> selector
                    else -> start.finishRoot(ParsedExpression.MemberRoot, base = root, children = listOf(selector))
                }
            } else if (atSet(Precedence.POSTFIX.getOperations())) {
                parseOperationReference()
                expression.done(POSTFIX_EXPRESSION)
                root = start.finishRoot(ParsedExpression.IncOrDecRoot, base = root)
            } else {
                break
            }

            expression = expression.precede()
        }

        if (hasOptionalChain) {
            // 对位 N IsNeedToCreateOptionalChain：assignment 前保留真正的 postfix 根供 CheckLeft。
            val assignmentFollows = Precedence.ASSIGNMENT.getOperations().contains(getGtTokenType())
            if (assignmentFollows && root.root != ParsedExpression.IncOrDecRoot) {
                expression.drop()
                root = root.copy(hasQuestSuffix = true)
            } else {
                expression.done(OPTIONAL_CHAIN_EXPRESSION)
                root = start.finishRoot(ParsedExpression.OptionalChainRoot, base = root)
            }
        } else {
            expression.drop()
        }
        return root.withCurrentSyntaxErrors(start)
    }

    /**
     * 执行 `atVArrayTypeArgumentStart` 内部辅助逻辑，支撑仓颉语法解析节点的结构解析与访问。
     */
    private fun atVArrayTypeArgumentStart(): Boolean =
        at(VARRAY_KEYWORD) || atSet(BASICTYPES) || at(IDENTIFIER) || at(LPAR) || at(QUEST) || at(THIS_KEYWORD)

    /**
     * 执行 `skipVArrayMismatchTrivia` 内部辅助逻辑，支撑仓颉语法解析节点的结构解析与访问。
     */
    private fun skipVArrayMismatchTrivia() {
        while (at(EOL_COMMENT) || at(BLOCK_COMMENT) || at(DOC_COMMENT) || at(SHEBANG_COMMENT)) {
            advance()
        }
    }

    /**
     * 执行 `advanceVArrayMismatchLookahead` 内部辅助逻辑，支撑仓颉语法解析节点的结构解析与访问。
     */
    private fun advanceVArrayMismatchLookahead() {
        skipVArrayMismatchTrivia()
        if (!eof() && !at(SEMICOLON)) {
            advance()
        }
    }

    /** 按官方 parser 的 ConsumeUntil(NL) 语义恢复到物理行尾。 */
    private fun skipUntilPhysicalLineEnd() {
        while (!eof() && !physicalNewlineBeforeCurrentToken()) {
            advance()
        }
    }

    /**
     * 解析区间表达式
     *
     * Grammar:
     * ```
     * rangeExpression
     *   : expression (":" expression)?
     *   ;
     * ```
     */
    context(context: ParsingContext)
    private fun parseRangeExpression(): ParsedExpression {
        val start = expressionStart()
        val first = parseBinaryExpression(Precedence.SHIFT)
        if (!at(COLON)) return first
        advance()
        val step = with(context.copy(expressionGrammarKind = CjExprKind.ALL)) { parseBinaryExpression(Precedence.SHIFT) }
        return start.finishRoot(ParsedExpression.Other, children = listOf(first, step))
    }


    /**
     * 解析块级表达式
     *
     * Grammar:
     * ```
     * blockLevelExpression
     *   : expression
     *   ;
     * ```
     */
    context(context: ParsingContext)
    private fun parseBlockLevelExpression() {
        parseExpression()
    }

    /**
     * 解析操作符引用
     *
     * @param type 操作符类型
     */
    context(context: ParsingContext)
    private fun parseOperationReference(type: IElementType) {
        val operationReference = mark()
        getGtTokenType()?.let { advanceGtToken(it) }
        operationReference.done(OPERATION_REFERENCE)
    }

    /**
     * 解析操作符引用
     */
    context(context: ParsingContext)
    private fun parseOperationReference() {
        val operationReference = mark()
        getGtTokenType()?.let { advanceGtToken(it) }
        operationReference.done(OPERATION_REFERENCE)
    }

    /**
     * 检查是否被换行中断
     *
     * @return 如果当前操作被换行中断则返回true
     */
    context(context: ParsingContext)
    private fun interruptedWithNewLine(): Boolean {
        return !ALLOW_NEWLINE_OPERATIONS.contains(tt()) && builder.newlineBeforeCurrentToken()
    }

    // ==================== 原子表达式解析 ====================

    /**
     * 解析原子表达式
     *
     * Grammar:
     * ```
     * atomicExpression
     *   : quoteExpression
     *   | unsafeExpression
     *   | spawnExpression
     *   | parenthesizedExpression
     *   | collectionLiteralExpression
     *   | thisExpression
     *   | superExpression
     *   | throwExpression
     *   | returnExpression
     *   | continueExpression
     *   | breakExpression
     *   | ifExpression
     *   | matchExpression
     *   | tryExpression
     *   | forExpression
     *   | whileExpression
     *   | doWhileExpression
     *   | letExpression
     *   | simpleNameExpression
     *   | functionLiteral
     *   | stringTemplate
     *   | literalConstant
     *   ;
     * ```
     *
     * @return 是否成功解析了原子表达式
     */
    context(context: ParsingContext)
    private fun parseAtomicExpression(): ParsedExpression {
        val start = expressionStart()
        var kind: ParsedExpression.Root = ParsedExpression.Other
        var ok = true

        when (getTokenId()) {
            // 官方 ParseAtom 的宏是正常原子，后缀与二元运算继续由外层优先级解析。
            // 声明宏借用外层 marker 的 backToken 协议不能进入表达式，否则只留下 MACRO_INPUT。
            AT_Id -> with(context.copy(memberScope = null, backToken = false)) {
                when {
                    isAnnotationLambdaStart() -> {
                        parseAnnotatedLambdaExpression()
                        return start.finishRoot(ParsedExpression.LambdaRoot)
                    }
                    cangJieParsing.isBuiltInAnnotation() -> ok = false
                    else -> return parseMacroInvocation(CjScopeKind.UNKNOWN_SCOPE, DeclarationParsingMode.ALL).expression
                }
            }
            QUOTE_KEYWORD_Id -> parseQuoteExpression()
            UNSAFE_KEYWORD_Id -> parseUnsafeExpression()
            SPAWN_KEYWORD_Id -> parseSpawnExpression()
            LPAR_Id -> return parseParenthesizedExpression()
            LBRACKET_Id -> parseCollectionLiteralExpression()
            THIS_KEYWORD_Id -> {
                parseThisExpression()
                kind = ParsedExpression.RefRoot
            }
            SUPER_KEYWORD_Id -> {
                parseSuperExpression()
                kind = ParsedExpression.RefRoot
            }
            THROW_KEYWORD_Id -> parseThrow()
            PERFORM_KEYWORD_Id -> parsePerform()
            RESUME_KEYWORD_Id -> parseResume()
            RETURN_KEYWORD_Id -> parseReturn()
            CONTINUE_KEYWORD_Id -> parseJump(CONTINUE)
            BREAK_KEYWORD_Id -> parseJump(BREAK)
            IF_KEYWORD_Id -> {
                parseIf()
                kind = ParsedExpression.IfRoot
            }
            MATCH_KEYWORD_Id -> parseMatch()
            TRY_KEYWORD_Id -> parseTry()
            FOR_KEYWORD_Id -> parseFor()
            WHILE_KEYWORD_Id -> parseWhile()
            DO_KEYWORD_Id -> parseDoWhile()
            LET_KEYWORD_Id -> if (context.allowLetExpression) {
                parseLetExpression()
                return start.finishRoot(ParsedExpression.LetPatternRoot)
            } else {
                error(CangJieParsingBundle.message("parsing.error.let.expression.only.in.conditions"))
                ok = false
            }

            IDENTIFIER_Id -> {
                parseSimpleNameExpression()
                kind = ParsedExpression.RefRoot
            }
            UNDERLINE_Id -> {
                parseWildcardExpression()
                kind = ParsedExpression.WildcardRoot
            }
            LBRACE_Id -> {
                parseFunctionLiteral()
                return start.finishRoot(ParsedExpression.LambdaRoot)
            }
            OPEN_QUOTE_Id -> parseStringTemplate()
            TRUE_KEYWORD_Id, FALSE_KEYWORD_Id -> parseOneTokenExpression(BOOLEAN_CONSTANT)
            INTEGER_LITERAL_Id -> parseOneTokenExpression(INTEGER_CONSTANT)
            RUNE_LITERAL_Id -> parseOneTokenExpression(RUNE_CONSTANT)
            CHARACTER_BYTE_LITERAL_Id -> parseOneTokenExpression(CHARACTER_BYTE_CONSTANT)
            FLOAT_LITERAL_Id -> parseOneTokenExpression(FLOAT_CONSTANT)
            // 上下文关键字（软关键字 token，如 common/specific/public）在原子位置按标识符解析，
            // 对齐官方 ParseAtom.cpp `Seeing(IDENTIFIER) || SeeingContextualKeyword()`；at(IDENTIFIER) 负责重映射
            else -> if (at(IDENTIFIER)) {
                parseSimpleNameExpression()
                kind = ParsedExpression.RefRoot
            } else ok = false
        }
        if (!ok) {
            errorWithRecovery(
                "Expecting an expression",
                TokenSet.orSet(
                    EXPRESSION_FOLLOW,
                    TokenSet.create(LONG_TEMPLATE_ENTRY_END),
                ),
            )
        }
        return start.finishRoot(if (ok) kind else ParsedExpression.Missing)
    }

    /**
     * 解析通配表达式 `_`。
     *
     * 官方把 `_` 作为独立 token 在原子表达式位置解析成 `WildcardExpr`
     * （`Parse/ParseAtom.cpp` 的 `Seeing(WILDCARD) -> ParseWildcardExpr`），由后续语义决定
     * 它只在赋值目标等允许丢弃的位置合法。本仓库沿用同一形式：`_` 只在原子位置被接受，
     * 语义层按丢弃目标处理，因此这里同样产出一个引用形态的表达式节点，名称即 `_`。
     */
    context(context: ParsingContext)
    private fun parseWildcardExpression() {
        assert(_at(UNDERLINE))
        val wildcard = mark()
        advance()
        wildcard.done(REFERENCE_EXPRESSION)
    }

    /**
     * 解析单token表达式
     *
     * @param type 表达式节点类型
     */
    context(context: ParsingContext)
    private fun parseOneTokenExpression(type: IElementType) {
        val mark = mark()
        advance()
        mark.done(type)
    }

    /**
     * effect 语法里的 `with` 只在 `resume` 局部语境中承担关键字职责，
     * 不提升为全局关键字，避免把无关 PSI 路径绑到 effect 语法上。
     */
    private fun atIdentifierText(expectedText: String): Boolean {
        return _at(IDENTIFIER) && builder.tokenText == expectedText
    }

    /**
     * 解析简单名称表达式
     *
     * Grammar:
     * ```
     * simpleNameExpression
     *   : IDENTIFIER typeArgumentList?
     *   ;
     * ```
     */
    context(context: ParsingContext)
    fun parseSimpleNameExpression() {
        val simpleName = mark()
        expect(IDENTIFIER, CangJieParsingBundle.message("parsing.error.expecting", "an identifier"))

        if (at(LT) && context.parseTypeArguments) {
            val typeArgumentList = mark()
            if (cangJieParsing.tryParseTypeArgumentList(TYPE_ARGUMENT_LIST_STOPPERS)) {
                typeArgumentList.done(TYPE_ARGUMENT_LIST)
            } else {
                typeArgumentList.rollbackTo()
            }
        }

        simpleName.done(REFERENCE_EXPRESSION)
    }

    /**
     * 解析引用表达式
     *
     * Grammar:
     * ```
     * referenceExpression
     *   : simpleNameExpression ("." simpleNameExpression)*
     *   ;
     * ```
     */
    context(context: ParsingContext)
    fun parseReferenceExpression() {
        var reference = mark()

        parseSimpleNameExpression()

        while (at(DOT)) {
            advance()
            parseSimpleNameExpression()
            reference.done(DOT_QUALIFIED_EXPRESSION)
            reference = reference.precede()
        }
        reference.drop()
    }

    // ==================== 括号和元组表达式 ====================

    /**
     * 解析括号表达式
     *
     * Grammar:
     * ```
     * parenthesizedExpression
     *   : "(" ")"                              // unit
     *   | "(" expression ("," expression)+ ")"  // tuple
     *   | "(" expression ")"                    // parenthesized
     *   ;
     * ```
     */
    context(context: ParsingContext)
    private fun parseParenthesizedExpression(): ParsedExpression {
        val start = expressionStart()
        assert(_at(LPAR))

        var isUnit = false
        var isTuple = false
        var first: ParsedExpression? = null
        val children = arrayListOf<ParsedExpression>()
        val firstKind = when (context.expressionGrammarKind) {
            CjExprKind.IF_COND_EXPR -> CjExprKind.EXPR_IN_IF_COND_TUPLE
            CjExprKind.WHILE_COND_EXPR -> CjExprKind.EXPR_IN_WHILE_COND_TUPLE
            else -> CjExprKind.EXPR_IN_TUPLE
        }

        val mark = mark()
        builder.disableNewlines()
        advance()
        if (!at(RPAR) && context.isParseOperator) {

            val parsedFirst = with(context.copy(stopAtConditionOperatorInLetInitializer = false)) {
                parseExpression(firstKind)
            }
            first = parsedFirst
            children += parsedFirst

        } else {
            isUnit = true
        }
        while (at(COMMA)) {
            if (!isTuple) first?.refineMacro { it.copy(parent = CjMacroFragmentParent.TUPLE_LITERAL) }
            isTuple = true
            advance()

            val child = with(context.copy(stopAtConditionOperatorInLetInitializer = false)) {
                parseExpression(CjExprKind.EXPR_IN_TUPLE)
            }
            child.refineMacro { it.copy(parent = CjMacroFragmentParent.TUPLE_LITERAL) }
            children += child

        }
        expect(RPAR, CangJieParsingBundle.message("parsing.error.expecting.symbol", ")"))
        builder.restoreNewlinesState()

        return when {
            isUnit -> {
                mark.done(UNIT_CONSTANT)
                start.finishRoot(ParsedExpression.UnitRoot, children = children)
            }
            isTuple -> {
                mark.done(TUPLE_EXPRESSION)
                start.finishRoot(ParsedExpression.TupleRoot, children = children)
            }
            else -> {
                mark.done(PARENTHESIZED)
                start.finishRoot(ParsedExpression.ParenRoot, base = checkNotNull(first))
            }
        }
    }

    /**
     * 解析元组字面量表达式
     *
     * Grammar:
     * ```
     * tupleLiteralExpression
     *   : TUPLE_LITERAL ")"
     *   | TUPLE_LITERAL expression ("," expression)* ")"
     *   ;
     * ```
     */
    context(context: ParsingContext)
    fun parseTupleLiteralExpression(): ParsedExpression {
        val start = expressionStart()
        val children = arrayListOf<ParsedExpression>()
        assert(_at(TUPLE_LITERAL))
        val tuple = mark()
        advance()
        if (at(RPAR)) {
            advance()
        } else {
            while (true) {
                if (at(RPAR)) {
                    break
                }

                val child = parseExpression(CjExprKind.EXPR_IN_TUPLE)
                child.refineMacro { it.copy(parent = CjMacroFragmentParent.TUPLE_LITERAL) }
                children += child

                if (at(RPAR)) {
                    break
                }
                expect(COMMA, CangJieParsingBundle.message("parsing.error.expecting.symbol", ","))
            }
            expect(RPAR, CangJieParsingBundle.message("parsing.error.expecting.symbol", ")"))
        }
        tuple.done(TUPLE_EXPRESSION)
        return start.finishRoot(ParsedExpression.TupleRoot, children = children)
    }

    // ==================== this 和 super 表达式 ====================

    /**
     * 解析this表达式
     *
     * Grammar:
     * ```
     * thisExpression
     *   : "this"
     *   ;
     * ```
     */
    context(context: ParsingContext)
    private fun parseThisExpression() {
        assert(_at(THIS_KEYWORD))
        val mark = mark()
        val thisReference = mark()
        advance()
        thisReference.done(REFERENCE_EXPRESSION)
        mark.done(THIS_EXPRESSION)
    }

    /**
     * 解析super表达式
     *
     * Grammar:
     * ```
     * superExpression
     *   : "super"
     *   ;
     * ```
     */
    context(context: ParsingContext)
    private fun parseSuperExpression() {
        assert(_at(SUPER_KEYWORD))
        val mark = mark()
        val superReference = mark()
        advance()
        superReference.done(REFERENCE_EXPRESSION)
        mark.done(SUPER_EXPRESSION)
    }

    // ==================== 集合字面量和数组访问 ====================

    /**
     * 解析集合字面量表达式
     *
     * Grammar:
     * ```
     * collectionLiteralExpression
     *   : "[" (expression ("," expression)*)? "]"
     *   ;
     * ```
     */
    context(context: ParsingContext)
    private fun parseCollectionLiteralExpression() {
        parseAsCollectionLiteralExpression(
            COLLECTION_LITERAL_EXPRESSION,
            true,
            "Expecting an element",
            CjExprKind.EXPR_IN_ARRAY,
            CjMacroFragmentParent.ARRAY_LITERAL,
        )
    }

    /**
     * 解析为集合字面量表达式
     *
     * @param nodeType 节点类型
     * @param canBeEmpty 是否可以为空
     * @param missingElementErrorMessage 缺失元素时的错误消息
     */
    context(context: ParsingContext)
    private fun parseAsCollectionLiteralExpression(
        nodeType: IElementType,
        canBeEmpty: Boolean,
        missingElementErrorMessage: String,
        elementKind: CjExprKind,
        macroParent: CjMacroFragmentParent,
    ) {
        assert(_at(LBRACKET))
        val innerExpressions = mark()
        builder.disableNewlines()
        advance()
        if (!canBeEmpty && at(RBRACKET)) {
            error(missingElementErrorMessage)
        } else {
            parseInnerExpressions(missingElementErrorMessage, elementKind, macroParent)
        }
        expect(RBRACKET, CangJieParsingBundle.message("parsing.error.expecting.symbol", "]"))
        builder.restoreNewlinesState()
        innerExpressions.done(nodeType)
    }

    /**
     * 解析内部表达式列表
     *
     * @param missingElementErrorMessage 缺失元素时的错误消息
     */
    context(context: ParsingContext)
    private fun parseInnerExpressions(
        missingElementErrorMessage: String,
        elementKind: CjExprKind,
        macroParent: CjMacroFragmentParent,
    ) {
        while (true) {
            if (at(COMMA)) errorAndAdvance(missingElementErrorMessage)
            if (at(RBRACKET)) {
                break
            }

            val expression = parseExpression(elementKind)
            expression.refineMacro { it.copy(parent = macroParent) }

            if (!at(COMMA)) break
            advance()
        }
    }

    /**
     * 解析数组访问
     *
     * Grammar:
     * ```
     * arrayAccess
     *   : "[" expression ("," expression)* "]"
     *   ;
     * ```
     */
    context(context: ParsingContext)
    private fun parseArrayAccess() {
        parseAsCollectionLiteralExpression(
            INDICES,
            false,
            "Expecting an index element",
            CjExprKind.INDEX_EXPR,
            CjMacroFragmentParent.OTHER,
        )
    }

    // ==================== 字符串模板解析 ====================

    /**
     * 解析字符串模板
     *
     * Grammar:
     * ```
     * stringTemplate
     *   : OPEN_QUOTE stringTemplateElement* CLOSING_QUOTE
     *   ;
     * ```
     */
    context(context: ParsingContext)
    fun parseStringTemplate() {
        assert(_at(OPEN_QUOTE))
        val template = mark()
        advance()
        while (!eof()) {
            if (at(CLOSING_QUOTE) || at(DANGLING_NEWLINE)) {
                break
            }

            parseStringTemplateElement()

        }
        if (at(DANGLING_NEWLINE)) {
            errorAndAdvance(CangJieParsingBundle.message("parsing.error.expecting.symbol", "\""))
        } else {
            expect(CLOSING_QUOTE, CangJieParsingBundle.message("parsing.error.expecting.symbol", "\""))
        }
        template.done(STRING_TEMPLATE)
    }

    /**
     * 解析字符串模板元素
     *
     * Grammar:
     * ```
     * stringTemplateElement
     *   : REGULAR_STRING_PART
     *   | ESCAPE_SEQUENCE
     *   | SHORT_TEMPLATE_ENTRY_START (IDENTIFIER | "this")
     *   | LONG_TEMPLATE_ENTRY_START statements LONG_TEMPLATE_ENTRY_END
     *   ;
     * ```
     */
    context(context: ParsingContext)
    private fun parseStringTemplateElement() {
        if (at(REGULAR_STRING_PART)) {
            val mark = mark()
            advance()
            mark.done(LITERAL_STRING_TEMPLATE_ENTRY)
        } else if (at(ESCAPE_SEQUENCE)) {
            val mark = mark()
            advance()
            mark.done(ESCAPE_STRING_TEMPLATE_ENTRY)
        } else if (at(SHORT_TEMPLATE_ENTRY_START)) {
            if (context.processStringInterpolation) {
                val entry = mark()
                advance()
                if (at(THIS_KEYWORD)) {
                    val thisExpression = mark()
                    val reference = mark()
                    advance()
                    reference.done(REFERENCE_EXPRESSION)
                    thisExpression.done(THIS_EXPRESSION)
                } else {
                    val keyword: CjToken? = KEYWORD_TEXTS.get(builder.tokenText)
                    if (keyword != null) {
                        builder.remapCurrentToken(keyword)
                        errorAndAdvance(CangJieParsingBundle.message("parsing.error.keyword.cannot.be.reference"))
                    } else {
                        val reference = mark()
                        expect(IDENTIFIER, CangJieParsingBundle.message("parsing.error.expecting", "a name"))
                        reference.done(REFERENCE_EXPRESSION)
                    }
                }
                entry.done(SHORT_STRING_TEMPLATE_ENTRY)
            } else {
                // 当不处理插值时，将其作为普通字符串部分消耗
                val mark = mark()
                advance()
                // 跳过标识符或 this
                if (at(THIS_KEYWORD) || at(IDENTIFIER)) {
                    advance()
                }
                mark.done(LITERAL_STRING_TEMPLATE_ENTRY)
            }
        } else if (at(LONG_TEMPLATE_ENTRY_START)) {
            if (context.processStringInterpolation) {
                val longTemplateEntry = mark()
                advance()
                while (!eof()) {
                    val offset = builder.currentOffset
                    with(context.copy(memberScope = null, statementScope = CjScopeKind.FUNC_BODY)) {
                        parseStatementsByStringTemplate()
                    }
                    if (_at(LONG_TEMPLATE_ENTRY_END)) {
                        advance()
                        break
                    } else {
                        error(CangJieParsingBundle.message("parsing.error.expecting.symbol", "}"))
                        if (offset == builder.currentOffset) {
                            advance()
                        }
                    }
                }
                longTemplateEntry.done(LONG_STRING_TEMPLATE_ENTRY)
            } else {
                // 当不处理插值时，将其作为普通字符串部分消耗
                val mark = mark()
                advance()
                // 跳过所有内容直到遇到结束符
                var braceCount = 1
                while (!eof() && braceCount > 0) {
                    if (at(LBRACE)) {
                        braceCount++
                    } else if (at(LONG_TEMPLATE_ENTRY_END)) {
                        braceCount--
                    }
                    if (braceCount > 0) {
                        advance()
                    }
                }
                if (at(LONG_TEMPLATE_ENTRY_END)) {
                    advance()
                }
                mark.done(LITERAL_STRING_TEMPLATE_ENTRY)
            }
        } else {
            errorAndAdvance(CangJieParsingBundle.message("parsing.error.unexpected.token.string.template"))
        }
    }

    // ==================== 控制流表达式 ====================

    /**
     * 解析if表达式
     *
     * Grammar:
     * ```
     * ifExpression
     *   : "if" condition thenBranch (SEMI? "else" elseBranch)?
     *   ;
     *
     * thenBranch
     *   : block
     *   ;
     *
     * elseBranch
     *   : block
     *   | ifExpression
     *   ;
     * ```
     */
    context(context: ParsingContext)
    private fun parseIf() {
        assert(_at(IF_KEYWORD))
        val marker = mark()
        advance()

        if (!parseCondition(CjExprKind.IF_COND_EXPR)) {
            consumeUntilIfExpressionEnd()
            marker.done(IF)
            return
        }

        val thenBranch = mark()
        if (!at(ELSE_KEYWORD) && !at(SEMICOLON)) {
            parseControlStructureBody()
        }
        if (at(SEMICOLON) && lookahead(1) === ELSE_KEYWORD) {
            advance()
        }
        thenBranch.done(THEN)

        if (at(ELSE_KEYWORD) && lookahead(1) !== ARROW) {
            advance()
            val elseBranch = mark()
            if (!at(SEMICOLON)) {
                if (!at(LBRACE) && !at(IF_KEYWORD)) {
                    error(CangJieParsingBundle.message("parsing.error.code.block.or.if"))
                } else if (at(IF_KEYWORD)) {

                    parseBlockLevelExpression()

                } else {
                    parseControlStructureBody()
                }
            }
            elseBranch.done(ELSE)
        }

        marker.done(IF)
    }

    /**
     * 解析match表达式
     *
     * Grammar:
     * ```
     * matchExpression
     *   : "match" condition? "{" matchEntry* "}"
     *   ;
     * ```
     */
    context(context: ParsingContext)
    private fun parseMatch() {
        assert(_at(MATCH_KEYWORD))
        val match = mark()
        advance()

        builder.disableNewlines()
        val matchContext = if (at(LPAR)) {
            parseCondition()
            // 有主语 match 的 case 条件是模式；不能继承外层无主语 match 的表达式条件上下文。
            context.copy(isExpression = false)
        } else {
            context.copy(isExpression = true)
        }

        builder.restoreNewlinesState()

        builder.enableNewlines()
        if (expect(LBRACE, CangJieParsingBundle.message("parsing.error.expecting.symbol", "{"))) {
            while (!eof() && !at(RBRACE)) {
                if (!at(CASE_KEYWORD)) {
                    errorAndAdvance(CangJieParsingBundle.message("parsing.error.expecting.keyword", "case"))
                } else {
                    with(matchContext) {
                        parseMatchEntry()
                    }
                }
            }
            expect(RBRACE, CangJieParsingBundle.message("parsing.error.expecting.symbol", "}"))
        }
        builder.restoreNewlinesState()
        match.done(MATCH)
    }

    /**
     * 解析match条目
     *
     * Grammar:
     * ```
     * matchEntry
     *   : "case" casePattern ("|" casePattern)* "=>" caseBody
     *   ;
     * ```
     */
    context(context: ParsingContext)
    private fun parseMatchEntry() {
        val entry = mark()

        if (at(CASE_KEYWORD)) {
            advance()

            parseCasePattern()
            while (at(OR)) {
                advance()
                parseCasePattern()
            }
            expect(DOUBLE_ARROW, CangJieParsingBundle.message("parsing.error.expecting.symbol", "=>"))
            parseCaseBody()
        } else {
            error(CangJieParsingBundle.message("parsing.error.expecting.keyword", "case"))
        }

        entry.done(MATCH_ENTRY)
    }

    /**
     * 解析case体
     *
     * Grammar:
     * ```
     * caseBody
     *   : statements
     *   ;
     * ```
     */
    context(context: ParsingContext)
    private fun parseCaseBody() {
        val body = mark()
        if (!at(SEMICOLON)) {
            if (at(RBRACE) || at(CASE_KEYWORD)) {
                error(CangJieParsingBundle.message("parsing.error.match.case.cannot.be.empty"))
                body.drop()
                return
            } else {
                with(context.copy(memberScope = null, statementScope = CjScopeKind.FUNC_BODY)) {
                    parseStatements(CASE_KEYWORD)
                }
            }
        }
        body.done(CASE_BLOCK)
    }

    /**
     * 解析try表达式
     *
     * Grammar:
     * ```
     * tryExpression
     *   : "try" ("(" tryResourceList ")")? block catchBlock* finallyBlock?
     *   ;
     *
     * tryResourceList
     *   : tryResource ("," tryResource)*
     *   ;
     *
     * tryResource
     *   : IDENTIFIER (":" type)? "=" expression
     *   ;
     *
     * catchBlock
     *   : "catch" "(" ("_" | IDENTIFIER) ":" type ("|" type)* ")" block
     *   ;
     *
     * finallyBlock
     *   : "finally" block
     *   ;
     * ```
     */
    context(context: ParsingContext)
    private fun parseTry() {
        assert(_at(TRY_KEYWORD))
        val tryExpression = mark()
        advance()

        var isTryWithResources = false
        if (at(LPAR)) {
            isTryWithResources = true
            advance()

            val resourceList = mark()
            do {
                if (at(COMMA)) advance()
                val resource = mark()

                val valueParameter = mark()
                expect(IDENTIFIER, CangJieParsingBundle.message("parsing.error.expecting", "resource name"))
                if (at(COLON)) {
                    advance()
                    cangJieParsing.parseTypeRef(TRY_RESOURCE_TYPE_RECOVERY_SET)
                }
                valueParameter.done(VALUE_PARAMETER)

                if (expect(
                        EQ,
                        CangJieParsingBundle.message("parsing.error.expecting.symbol", "="),
                        TRY_CATCH_RECOVERY_TOKEN_SET
                    )
                ) {

                    parseExpression(CjExprKind.VAR_INIT)

                }

                resource.done(TRY_RESOURCE)
            } while (at(COMMA))

            resourceList.done(TRY_RESOURCE_LIST)
            expect(RPAR, CangJieParsingBundle.message("parsing.error.expecting.symbol", ")"))
        }

        cangJieParsing.parseBlock()
        var catchHandleOrFinally = false
        while (at(CATCH_KEYWORD) || at(HANDLE_KEYWORD)) {
            catchHandleOrFinally = true
            if (at(HANDLE_KEYWORD)) {
                parseHandleClause()
                continue
            }

            val catchBlock = mark()
            advance()
            val parameter = mark()
            if (expect(
                    LPAR,
                    CangJieParsingBundle.message("parsing.error.expecting.symbol", "("),
                    TRY_CATCH_RECOVERY_TOKEN_SET
                )
            ) {
                if (!atSet(TRY_CATCH_RECOVERY_TOKEN_SET)) {
                    if (at(UNDERLINE)) {
                        advance()
                        if (at(COLON)) {
                            advance()
                            parseTypeReferencesByOr()
                        }
                    } else {
                        expect(
                            IDENTIFIER,
                            CangJieParsingBundle.message("parsing.error.expecting", "exception variable name")
                        )

                        if (expect(COLON, CangJieParsingBundle.message("parsing.error.expecting.symbol", ":"))) {
                            parseTypeReferencesByOr()
                        }
                    }

                    expect(
                        RPAR,
                        CangJieParsingBundle.message("parsing.error.expecting.symbol", ")"),
                        TRY_CATCH_RECOVERY_TOKEN_SET
                    )
                } else {
                    error(CangJieParsingBundle.message("parsing.error.exception.variable.declaration"))
                }
            }

            parameter.done(CATCH_PARAMETER)
            if (at(LBRACE)) {
                cangJieParsing.parseBlock()
            } else {
                error(CangJieParsingBundle.message("parsing.error.block"))
            }
            catchBlock.done(CATCH)
        }
        if (at(FINALLY_KEYWORD)) {
            catchHandleOrFinally = true
            val finallyBlock = mark()
            advance()
            cangJieParsing.parseBlock()
            finallyBlock.done(FINALLY)
        }
        if (!catchHandleOrFinally && !isTryWithResources) {
            error(CangJieParsingBundle.message("parsing.error.catch.handle.or.finally"))
        }
        tryExpression.done(TRY)
    }

    /**
     * `handle` 与 `catch` 并列挂在 try 下面。
     * 不把它做成独立表达式，保持和官方 TryExpr.handlers 相同的层次结构。
     */
    context(context: ParsingContext)
    private fun parseHandleClause() {
        assert(_at(HANDLE_KEYWORD))
        val handleClause = mark()
        advance()

        val recoverySet = TokenSet.orSet(
            TRY_CATCH_RECOVERY_TOKEN_SET,
            TokenSet.create(IDENTIFIER, UNDERLINE, RPAR),
        )
        val hasLeftParen = expect(
            LPAR,
            CangJieParsingBundle.message("parsing.error.handle.pattern.parenthesized"),
            recoverySet,
        )

        if (!at(RPAR) && !atSet(TRY_CATCH_RECOVERY_TOKEN_SET)) {
            parseCommandTypePattern()
        }

        if (hasLeftParen || !at(LBRACE)) {
            expect(
                RPAR,
                CangJieParsingBundle.message("parsing.error.expecting.symbol", ")"),
                TRY_CATCH_RECOVERY_TOKEN_SET,
            )
        }

        if (at(LBRACE)) {
            cangJieParsing.parseBlock()
        } else {
            error(CangJieParsingBundle.message("parsing.error.block"))
        }

        handleClause.done(HANDLE)
    }

    /**
     * 执行 `parseCommandTypePattern` 内部辅助逻辑，支撑仓颉语法解析节点的结构解析与访问。
     */
    context(context: ParsingContext)
    private fun parseCommandTypePattern() {
        val commandPattern = mark()
        when {
            at(UNDERLINE) -> {
                advance()
                expect(
                    COLON,
                    CangJieParsingBundle.message("parsing.error.expected.colon.in.effect.pattern"),
                    TokenSet.create(IDENTIFIER, RPAR, LBRACE),
                )
                if (!at(RPAR) && !at(LBRACE)) {
                    parseTypeReferencesByOr()
                }
            }

            at(IDENTIFIER) -> {
                advance()
                expect(
                    COLON,
                    CangJieParsingBundle.message("parsing.error.expected.colon.in.effect.pattern"),
                    TokenSet.create(IDENTIFIER, RPAR, LBRACE),
                )
                if (!at(RPAR) && !at(LBRACE)) {
                    parseTypeReferencesByOr()
                }
            }

            else -> {
                errorWithRecovery(
                    CangJieParsingBundle.message("parsing.error.expected.wildcard.or.effect.pattern"),
                    TRY_CATCH_RECOVERY_TOKEN_SET,
                )
            }
        }
        commandPattern.done(COMMAND_TYPE_PATTERN)
    }

    /**
     * 解析由|分隔的类型引用列表
     *
     * Grammar:
     * ```
     * typeReferencesByOr
     *   : type ("|" type)*
     *   ;
     * ```
     */
    context(context: ParsingContext)
    private fun parseTypeReferencesByOr() {
        do {
            if (at(OR)) advance()
            cangJieParsing.parseTypeRef()
        } while (at(OR))
    }

    // ==================== 循环语句 ====================

    /**
     * 解析for循环表达式
     *
     * Grammar:
     * ```
     * forExpression
     *   : "for" "(" modifiers? pattern "in" expression patternGuard? ")" loopBody
     *   ;
     * ```
     */
    context(context: ParsingContext)
    private fun parseFor() {
        assert(_at(FOR_KEYWORD))
        val loop = mark()
        advance()
        if (expect(LPAR, CangJieParsingBundle.message("parsing.error.loop.range"), EXPRESSION_FIRST)) {
            builder.disableNewlines()
            if (!at(RPAR)) {
                if (!at(IN_KEYWORD)) {
                    cangJieParsing.parseModifierList(IN_KEYWORD_R_PAR_COLON_SET)
                }

                PatternParser(PatternParseContext.DEFAULT).parsePattern()

                if (expect(
                        IN_KEYWORD,
                        CangJieParsingBundle.message("parsing.error.expecting.in"),
                        L_PAR_L_BRACE_R_PAR_SET
                    )
                ) {
                    val range = mark()

                    parseExpression()

                    range.done(LOOP_RANGE)
                }

                if (at(WHERE_KEYWORD)) {
                    parsePatternGuard()
                }
            } else {
                error(CangJieParsingBundle.message("parsing.error.variable.name"))
            }
            expectNoAdvance(RPAR, CangJieParsingBundle.message("parsing.error.expecting.symbol", ")"))
            builder.restoreNewlinesState()
        }
        parseLoopBody()
        loop.done(FOR)
    }

    /**
     * 解析while循环表达式
     *
     * Grammar:
     * ```
     * whileExpression
     *   : "while" condition loopBody
     *   ;
     * ```
     */
    context(context: ParsingContext)
    private fun parseWhile() {
        assert(_at(WHILE_KEYWORD))
        val loop = mark()
        advance()
        parseCondition(CjExprKind.WHILE_COND_EXPR)
        parseLoopBody()
        loop.done(WHILE)
    }

    /**
     * 解析do-while循环表达式
     *
     * Grammar:
     * ```
     * doWhileExpression
     *   : "do" loopBody "while" condition
     *   ;
     * ```
     */
    context(context: ParsingContext)
    private fun parseDoWhile() {
        assert(_at(DO_KEYWORD))
        val loop = mark()
        advance()
        if (!at(WHILE_KEYWORD)) {
            parseLoopBody()
        }
        if (expect(WHILE_KEYWORD, CangJieParsingBundle.message("parsing.error.while.post.condition"))) {
            parseCondition()
        }
        loop.done(DO_WHILE)
    }

    /**
     * 解析循环体
     *
     * Grammar:
     * ```
     * loopBody
     *   : block
     *   ;
     * ```
     */
    context(context: ParsingContext)
    private fun parseLoopBody() {
        val body = mark()
        if (!at(SEMICOLON)) {
            parseControlStructureBody()
        }
        body.done(BODY)
    }

    /**
     * 解析条件表达式
     *
     * Grammar:
     * ```
     * condition
     *   : "(" expression ")"
     *   ;
     * ```
     */
    context(context: ParsingContext)
    private fun parseCondition(kind: CjExprKind = CjExprKind.ALL): Boolean {
        builder.disableNewlines()
        val hasLeftParen = expect(
                LPAR,
                "Expecting a condition in parentheses '(...)'",
                EXPRESSION_FIRST,
            )
        if (hasLeftParen) {
            val condition = mark()

            with(context.copy(allowLetExpression = true)) {
                parseExpression(kind)
            }

            condition.done(CONDITION)
            expect(RPAR, CangJieParsingBundle.message("parsing.error.expecting.symbol", ")"))
        }
        builder.restoreNewlinesState()
        return hasLeftParen
    }

    /**
     * 官方 parser 在 `if` 后缺少左括号时消费到当前 if 表达式的代码块结束，
     * 避免把条件残片继续当作 then-body 解析并产生恢复噪声。
     */
    context(context: ParsingContext)
    private fun consumeUntilIfExpressionEnd() {
        while (!eof() && !at(LBRACE)) {
            advance()
        }
        if (!at(LBRACE)) return

        var braceDepth = 0
        do {
            when {
                at(LBRACE) -> braceDepth++
                at(RBRACE) -> braceDepth--
            }
            advance()
        } while (!eof() && braceDepth > 0)

        if (at(SEMICOLON)) {
            advance()
        }
    }

    // ==================== 跳转语句 ====================

    /**
     * 解析throw表达式
     *
     * Grammar:
     * ```
     * throwExpression
     *   : "throw" expression
     *   ;
     * ```
     */
    context(context: ParsingContext)
    private fun parseThrow() {
        assert(_at(THROW_KEYWORD))
        val marker = mark()
        advance()
        parseExpression()
        marker.done(THROW)
    }

    /**
     * 执行 `parsePerform` 内部辅助逻辑，支撑仓颉语法解析节点的结构解析与访问。
     */
    context(context: ParsingContext)
    private fun parsePerform() {
        assert(_at(PERFORM_KEYWORD))
        val marker = mark()
        advance()
        parseExpression()
        marker.done(PERFORM)
    }

    /**
     * 执行 `parseResume` 内部辅助逻辑，支撑仓颉语法解析节点的结构解析与访问。
     */
    context(context: ParsingContext)
    private fun parseResume() {
        assert(_at(RESUME_KEYWORD))
        val marker = mark()
        advance()
        when {
            atIdentifierText("with") -> {
                advance()
                parseExpression()
            }

            at(THROWING_KEYWORD) -> {
                advance()
                parseExpression()
            }
        }
        marker.done(RESUME)
    }

    /**
     * 解析return表达式
     *
     * Grammar:
     * ```
     * returnExpression
     *   : "return" expression?
     *   ;
     * ```
     */
    context(context: ParsingContext)
    private fun parseReturn() {
        assert(_at(RETURN_KEYWORD))
        val returnExpression = mark()
        advance()
        if (atSet(EXPRESSION_FIRST) && !at(EOL_OR_SEMICOLON)) {
            parseExpression()
        }
        returnExpression.done(RETURN)
    }

    /**
     * 解析跳转表达式
     *
     * Grammar:
     * ```
     * jumpExpression
     *   : "break"
     *   | "continue"
     *   ;
     * ```
     *
     * @param type 跳转类型(BREAK或CONTINUE)
     */
    context(context: ParsingContext)
    private fun parseJump(type: IElementType) {
        assert(_at(BREAK_KEYWORD) || _at(CONTINUE_KEYWORD))
        val marker = mark()
        advance()
        marker.done(type)
    }

    // ==================== let 表达式 ====================

    /**
     * 解析let表达式
     *
     * Grammar:
     * ```
     * letExpression
     *   : "let" deconstructPattern "<-" expression
     *   ;
     * ```
     */
    context(context: ParsingContext)
    private fun parseLetExpression() {
        assert(_at(LET_KEYWORD))
        val expr = mark()

        advance()

        parseDeconstructPattern()

        if (at(LEFT_ARROW)) {
            advance()
        } else {
            error(CangJieParsingBundle.message("parsing.error.left.arrow.in.let"))
        }

        with(
            context.copy(
                stopAtConditionOperatorInLetInitializer = true,
                suppressLiteralTrailingLambdaInLetCondition = true,
            )
        ) {
            parseExpression(CjExprKind.LET_PATTERN)
        }

        expr.done(LET_EXPRESSION)
    }

    /**
     * 解析解构模式
     */
    context(context: ParsingContext)
    private fun parseDeconstructPattern() {

        do {
            parseCasePattern()
        } while (expect(OR))

    }

    // ==================== 模式解析 ====================

    /**
     * 模式解析上下文
     *
     * 用于控制在不同场景下允许解析哪些类型的模式
     *
     * Grammar:
     * ```
     * pattern
     *   : constantPattern
     *   | wildcardPattern
     *   | bindingPattern
     *   | tuplePattern
     *   | typePattern
     *   | enumPattern
     *   ;
     *
     * constantPattern
     *   : INTEGER_LITERAL
     *   | RUNE_LITERAL
     *   | CHARACTER_BYTE_LITERAL
     *   | FLOAT_LITERAL
     *   | TRUE_KEYWORD
     *   | FALSE_KEYWORD
     *   | stringTemplate
     *   ;
     *
     * wildcardPattern
     *   : "_"
     *   ;
     *
     * bindingPattern
     *   : IDENTIFIER
     *   ;
     *
     * tuplePattern
     *   : "(" pattern ("," pattern)+ ")"
     *   ;
     *
     * typePattern
     *   : (IDENTIFIER | "_") ":" type
     *   ;
     *
     * enumPattern
     *   : qualifiedName ("(" expression ("," expression)* ")")?
     *   ;
     * ```
     *
     * @property allowedPatterns 允许的模式类型集合
     * @property isVariableDeclaration 是否为变量声明
     * @property nestingLevel 嵌套层级
     */
    data class PatternParseContext(
        /**
         * 保存 `allowedPatterns`，供仓颉语法解析流程读取节点结构或语义信息。
         */
        val allowedPatterns: Set<PatternType> = PatternType.ALL_PATTERNS,
        /**
         * 保存 `isVariableDeclaration`，供仓颉语法解析流程读取节点结构或语义信息。
         */
        val isVariableDeclaration: Boolean = false,
        /**
         * 保存 `nestingLevel`，供仓颉语法解析流程读取节点结构或语义信息。
         */
        val nestingLevel: Int = 0,
    ) {
        companion object {
            val DEFAULT = PatternParseContext()
            val VARIABLE_DECL = PatternParseContext(
                allowedPatterns = setOf(
                    PatternType.WILDCARD,
                    PatternType.BINDING,
                    PatternType.TUPLE,
                    PatternType.ENUM
                ),
                isVariableDeclaration = true
            )
        }

        /**
         * 提供 `withNestingLevel` 操作，封装仓颉语法解析节点的访问、构造或判断逻辑。
         */
        fun withNestingLevel(level: Int): PatternParseContext {
            return copy(nestingLevel = level)
        }

        /**
         * 提供 `isPatternAllowed` 操作，封装仓颉语法解析节点的访问、构造或判断逻辑。
         */
        fun isPatternAllowed(pattern: PatternType): Boolean {
            return allowedPatterns.contains(pattern)
        }
    }

    inner class PatternParser(
        /**
         * 保存 `context` 的内部状态，供仓颉语法解析实现维护节点缓存或解析上下文。
         */
        private val context: PatternParseContext
    ) {
        context(context: ParsingContext)
        fun parsePattern(): IElementType? {
            val constantPattern = mark()

            return when (getTokenId()) {
                UNDERLINE_Id -> {
                    constantPattern.drop()
                    parseUnderlinePattern()
                }

                LPAR_Id -> {
                    constantPattern.drop()
                    parseParenthesizedPattern()
                }

                INTEGER_LITERAL_Id -> {
                    parseOneTokenExpression(INTEGER_CONSTANT)
                    doneConstantPattern(constantPattern)
                }

                RUNE_LITERAL_Id -> {
                    parseOneTokenExpression(RUNE_CONSTANT)
                    doneConstantPattern(constantPattern)
                }

                CHARACTER_BYTE_LITERAL_Id -> {
                    parseOneTokenExpression(CHARACTER_BYTE_CONSTANT)
                    doneConstantPattern(constantPattern)
                }

                TRUE_KEYWORD_Id, FALSE_KEYWORD_Id -> {
                    parseOneTokenExpression(BOOLEAN_CONSTANT)
                    doneConstantPattern(constantPattern)
                }

                FLOAT_LITERAL_Id -> {
                    parseOneTokenExpression(FLOAT_CONSTANT)
                    doneConstantPattern(constantPattern)
                }

                OPEN_QUOTE_Id -> {
                    parseStringTemplate()
                    doneConstantPattern(constantPattern)
                }

                 IDENTIFIER_Id -> {
                    constantPattern.drop()
                    parseIdentifierPattern()
                }

                MINUS_Id -> {
                    parsePrefixExpression()
                    doneConstantPattern(constantPattern)
                }

                else -> when{
//                    具有软关键字的判断，由于上面已经做了IDENTIFIER的判断，这里做一个最小更改，后期可以尝试统一判断优化
                    at(IDENTIFIER) ->{
                        constantPattern.drop()
                        parseIdentifierPattern()
                    }
                    else -> {
                        error(CangJieParsingBundle.message("parsing.error.pattern.expression"))
                        constantPattern.drop()
                        null
                    }
                }
            }
        }

        context(parsingcontext: ParsingContext)
        fun parseIdentifierPattern(): IElementType? {
            assert(_at(IDENTIFIER))
            var mark = mark()
            var recognized = if (context.isVariableDeclaration) {
                RecognizedPattern.BINDING
            } else {
                RecognizedPattern.VAR_OR_ENUM
            }

            if (lookahead(1) == DOT || lookahead(1) == LT) {

                parseReferenceExpression()

                recognized = RecognizedPattern.ENUM

                if (at(LPAR)) {
                    parseEnumPatternArguments()
                }
            } else {

                parseReferenceExpression()


                if (at(COLON) && (!context.isVariableDeclaration || context.nestingLevel > 0)) {
                    advance()
                    recognized = RecognizedPattern.TYPE
                    cangJieParsing.parseTypeRef()
                } else if (at(LPAR)) {
                    mark.rollbackTo()
                    mark = mark()

                    parseReferenceExpression()

                    recognized = RecognizedPattern.ENUM
                    parseEnumPatternArguments()
                }
            }

            return validateAndDonePattern(mark, recognized.toPatternType(), recognized.toNodeType())
        }

        context(parsingcontext: ParsingContext)
        fun parseUnderlinePattern(): IElementType? {
            val pattern = mark()
            assert(_at(UNDERLINE))
            advance()

            return if (at(COLON) && !context.isVariableDeclaration) {
                advance()
                cangJieParsing.parseTypeRef()
                validateAndDonePattern(pattern, PatternType.TYPE, TYPE_PATTERN)
            } else {
                validateAndDonePattern(pattern, PatternType.WILDCARD, WILDCARD_PATTERN)
            }
        }

        context(parsingcontext: ParsingContext)
        fun parseParenthesizedPattern(): IElementType? {
            assert(_at(LPAR))

            var isUnit = false
            var isTuple = false

            val mark = mark()
            builder.disableNewlines()
            advance()

            if (at(RPAR)) {
                isUnit = true
            } else {
                isTuple = true
                val nestedContext = context.withNestingLevel(context.nestingLevel + 1)
                val nestedParser = PatternParser(nestedContext)
                nestedParser.parseTuplePatternElements()
            }
            expect(RPAR, CangJieParsingBundle.message("parsing.error.expecting.symbol", ")"))

            val recognized = when {
                isUnit -> {
                    mark().done(UNIT_CONSTANT)
                    doneConstantPattern(mark)
                }

                isTuple -> {
                    validateAndDonePattern(mark, PatternType.TUPLE, TUPLE_PATTERN)
                }

                else -> { mark.drop(); null }
            }

            builder.restoreNewlinesState()
            return recognized
        }

        context(parsingcontext: ParsingContext)
        private fun parseTuplePatternElements() {
            parsePattern()
            if (at(COMMA)) {
                advance()
                parsePattern()
                while (at(COMMA)) {
                    advance()
                    parsePattern()
                }
            } else {
                error(CangJieParsingBundle.message("parsing.error.tuple.expression"))
            }
        }

        context(parsingcontext: ParsingContext)
        private fun parseEnumPatternArguments() {
            advance()
            parsePattern()
            while (at(COMMA)) {
                advance()
                parsePattern()
            }
            expect(RPAR, CangJieParsingBundle.message("parsing.error.expecting.symbol", ")"))
        }

        private fun validateAndDonePattern(
            marker: PsiBuilder.Marker,
            patternType: PatternType,
            nodeType: IElementType
        ): IElementType? {
            if (context.isPatternAllowed(patternType) || context.nestingLevel > 0) {
                marker.done(nodeType)
                return nodeType
            } else {
                marker.error(
                    CangJieParsingBundle.message(
                        "parsing.error.pattern.not.supported",
                        patternType.displayName
                    )
                )
                return null
            }
        }

        private fun doneConstantPattern(constantPattern: PsiBuilder.Marker): IElementType? =
            validateAndDonePattern(constantPattern, PatternType.CONSTANT, CONSTANT_PATTERN)
    }

    /**
     * 解析模式(入口方法)
     *
     * @param context 模式解析上下文
     */
    context(parsingcontext: ParsingContext)
    fun parsePattern(context: PatternParseContext = PatternParseContext.DEFAULT): IElementType? {
        val parser = PatternParser(context)
        return parser.parsePattern()
    }

    /**
     * 解析case模式
     */
    context(context: ParsingContext)
    private fun parseCasePattern() {
        val parser = PatternParser(PatternParseContext.DEFAULT)

        if (context.isExpression) {
            if (at(UNDERLINE)) {
                parser.parseUnderlinePattern()
            } else {
                val expr = mark()
                parseExpression()
                expr.done(MATCH_CONDITION_EXPRESSION)
            }
        } else {
            parser.parsePattern()
        }

        if (at(WHERE_KEYWORD)) {
            parsePatternGuard()
        }
    }

    /**
     * 解析模式守卫
     *
     * Grammar:
     * ```
     * patternGuard
     *   : "where" expression
     *   ;
     * ```
     */
    context(context: ParsingContext)
    fun parsePatternGuard() {
        assert(_at(WHERE_KEYWORD))
        val marker = mark()

        advance()

        parseExpression()

        marker.done(PATTERN_GUARD)
    }

    // ==================== 函数字面量和 Lambda ====================

    /**
     * 解析函数字面量
     *
     * Grammar:
     * ```
     * functionLiteral
     *   : "{" "=>" statements "}"
     *   | "{" parameterList "=>" statements "}"
     *   | "{" statements "}"  // block
     *   ;
     * ```
     */
    context(context: ParsingContext)
    fun parseFunctionLiteral() {
        val bodyContext = context.copy(memberScope = null, statementScope = CjScopeKind.FUNC_BODY, backToken = false)
        parseFunctionLiteral(cangJieParsing.parserSeed(bodyContext))
    }

    /** lazy 根保留初次入口 seed，禁折叠等本次操作参数不回写入口的语义身份。 */
    context(context: ParsingContext)
    internal fun parseFunctionLiteral(seed: CjParserSeed) {
        val bodyContext = context.copy(memberScope = null, statementScope = CjScopeKind.FUNC_BODY, backToken = false)
        val journal = builder.macroParsingJournal
        val diagnosticScope = journal.reserveBodyDiagnosticScope(builder.currentOffset)
        journal.withDiagnosticScope(diagnosticScope) {
            with(bodyContext) {
                assert(_at(LBRACE))
                val openingBrace = builder.currentOffset
                var bodySeed = seed.copy(diagnosticScope = diagnosticScope)
                val literalExpression = mark()
                val literal = mark()
                builder.enableNewlines()
                advance()
                var paramsFound = false
                val token = tt()
                if (token === DOUBLE_ARROW) {
                    mark().done(VALUE_PARAMETER_LIST)
                    advance()
                    paramsFound = true
                } else if (token?.equal(IDENTIFIER_RECOVERY_SET) == true || token === COLON || token === LPAR) {
                    val rollbackMarker = mark()
                    val nextToken = lookahead(1)
                    val preferParamsToExpressions =
                        (nextToken === COMMA || nextToken === COLON)
                    parseFunctionLiteralParameterList()

                    paramsFound = if (preferParamsToExpressions) {
                        rollbackOrDrop(
                            rollbackMarker,
                            DOUBLE_ARROW,
                            "An -> is expected",
                            RBRACE,
                        )
                    } else {
                        rollbackOrDropAt(rollbackMarker, DOUBLE_ARROW)
                    }
                } else if (context.isDoubleArrow) {
                    error(CangJieParsingBundle.message("parsing.error.lambda.arrow.expected", builder.tokenText ?: ""))
                }

                if (!paramsFound && context.preferBlock) {
                    literal.drop()
                    parseStatements()
                    expect(RBRACE, CangJieParsingBundle.message("parsing.error.expecting.symbol", "}"))
                    literalExpression.done(BLOCK)
                    cangJieParsing.recordBodySeed(literalExpression, openingBrace, bodySeed)
                    builder.restoreNewlinesState()
                    return@withDiagnosticScope
                }

                if (context.collapse && isLazy) {
                    bodySeed = bodySeed.copy(roleRegionSlot = cangJieParsing.recordUnparsedRoleRegion(openingBrace))
                    cangJieParsing.advanceBalancedBlock()
                    literal.done(FUNCTION_LITERAL)
                    literalExpression.collapse(LAMBDA_EXPRESSION)
                } else {
                    val body = mark()
                    parseStatements()

                    body.done(BLOCK)
                    body.setCustomEdgeTokenBinders(PRECEDING_ALL_COMMENTS_BINDER, TRAILING_ALL_COMMENTS_BINDER)

                    expect(RBRACE, CangJieParsingBundle.message("parsing.error.expecting.symbol", "}"))
                    literal.done(FUNCTION_LITERAL)
                    literalExpression.done(LAMBDA_EXPRESSION)
                }
                cangJieParsing.recordBodySeed(literalExpression, openingBrace, bodySeed)
                builder.restoreNewlinesState()
            }
        }
    }

    /**
     * 解析函数字面量参数列表
     *
     * Grammar:
     * ```
     * functionLiteralParameterList
     *   : parameter ("," parameter)*
     *   ;
     *
     * parameter
     *   : IDENTIFIER (":" type)?
     *   ;
     * ```
     */
    context(context: ParsingContext)
    private fun parseFunctionLiteralParameterList() {
        val parameterList = mark()

        while (!eof()) {
            if (at(DOUBLE_ARROW)) {
                break
            }
            val parameter = mark()

            if (at(COLON)) {
                error(CangJieParsingBundle.message("parsing.error.parameter.name"))
            } else {
                expect(
                    IDENTIFIER_RECOVERY_SET,
                    "Expecting parameter name",
                    ARROW_SET,
                )
            }

            if (at(COLON)) {
                advance()
                cangJieParsing.parseTypeRef(ARROW_COMMA_SET)
            }
            parameter.done(VALUE_PARAMETER)

            if (at(DOUBLE_ARROW)) {
                break
            } else if (at(COMMA)) {
                advance()
            } else {
                error(CangJieParsingBundle.message("parsing.error.arrow.or.comma"))
                break
            }
        }

        parameterList.done(VALUE_PARAMETER_LIST)
    }

    /**
     * 解析带注解的Lambda表达式
     *
     * @return 是否成功解析
     */
    context(context: ParsingContext)
    private fun parseAnnotatedLambda(): Boolean {
        if (!at(LBRACE)) {
            return false
        }

        parseFunctionLiteral()

        return true
    }

    /**
     * 解析带闭包的调用
     *
     * Grammar:
     * ```
     * callWithClosure
     *   : lambdaArgument+
     *   ;
     * ```
     *
     * @return 是否成功解析
     */
    context(context: ParsingContext)
    protected fun parseCallWithClosure(): Boolean {
        var success = false

        while (true) {
            val argument = mark()

            with(context.copy(collapse = true, isDoubleArrow = false)) {
                if (!parseAnnotatedLambda()) {
                    argument.drop()
                    break
                }
            }

            argument.done(LAMBDA_ARGUMENT)
            success = true
        }

        return success
    }

    // ==================== 函数调用和后缀表达式 ====================

    /**
     * 解析调用后缀
     *
     * Grammar:
     * ```
     * callSuffix
     *   : valueArgumentList
     *   | lambdaArgument+
     *   ;
     * ```
     *
     * @return 是否成功解析了调用后缀
     */
    context(context: ParsingContext)
    private fun parseCallSuffix(): Boolean {
        if ((context.suppressLiteralTrailingLambdaInLetCondition || context.allowLetExpression) &&
            at(LBRACE) &&
            previousSignificantTokenIsLiteral()
        ) {
            return false
        }
        if (parseCallWithClosure()) {
        } else if (at(LPAR)) {
            parseValueArgumentList()
        } else {
            return false
        }
        return true
    }

    /**
     * `?.`、`?[`、`?(` 是 quest 后缀触发的 optional chain。
     * 这里仅消费 `?` 并校验后继 token，OPTIONAL_EXPR / OPTIONAL_CHAIN_EXPR 由主循环统一收树。
     */
    context(context: ParsingContext)
    private fun parseQuestChainSuffix(expression: PsiBuilder.Marker): Boolean {
        if (!at(QUEST)) return false

        val nextToken = lookahead(1)
        if (nextToken === QUEST) {
            return false
        }
        if (nextToken !== DOT && nextToken !== LBRACKET && nextToken !== LPAR && nextToken !== LBRACE) {
            error("`?` 后需要 `.`、`(`、`[`、`{` 或 `?`")
            return false
        }

        advance()
        return true
    }

    /**
     * 解析选择器调用表达式
     *
     * Grammar:
     * ```
     * selectorCallExpression
     *   : simpleNameExpression callSuffix?
     *   ;
     * ```
     */
    context(context: ParsingContext)
    private fun parseSelectorCallExpression(completeMember: (ParsedExpression) -> ParsedExpression): ParsedExpression {
        val start = expressionStart()
        val mark = mark()

        if (at(TILDE)) {
            val unexpected = mark()
            advance()
            unexpected.error(CangJieParsingBundle.message("parsing.error.expecting.identifier"))
        }

        // 官方 ParseMemberAccess 只接受标识符及可选类型实参；宏/字面量只能作为接收者。
        if (!at(IDENTIFIER)) {
            errorWithRecovery(CangJieParsingBundle.message("parsing.error.expecting.identifier"), EXPRESSION_FOLLOW)
            mark.drop()
            return start.finishRoot(ParsedExpression.Missing)
        }
        parseSimpleNameExpression()
        val name = start.finishRoot(ParsedExpression.RefRoot)
        val member = completeMember(name)
        if (!builder.newlineBeforeCurrentToken() && parseCallSuffix()) {
            mark.done(CALL_EXPRESSION)
            return start.finishRoot(ParsedExpression.CallRoot, base = member)
        }
        mark.drop()
        return member
    }

    // ==================== 值参数列表 ====================

    /**
     * 解析值参数
     *
     * Grammar:
     * ```
     * valueArgument
     *   : (IDENTIFIER ":")? expression
     *   ;
     * ```
     */
    context(context: ParsingContext)
    private fun parseValueArgument(kind: CjExprKind = CjExprKind.EXPR_IN_CALLSUFFIX): Boolean {
        val argument = mark()
        val hasName = at(IDENTIFIER) && lookahead(1) === COLON
        if (hasName) {
            val argName = mark()
            val reference = mark()
            advance()
            reference.done(REFERENCE_EXPRESSION)
            argName.done(VALUE_ARGUMENT_NAME)
            advance()
        } else if (at(INOUT_KEYWORD)) {
            advance()
        }
        parseExpression(kind).refineMacro { it.copy(parent = CjMacroFragmentParent.FUNCTION_ARGUMENT) }
        argument.done(VALUE_ARGUMENT)
        return hasName
    }

    /**
     * 解析值参数列表(使用默认括号)
     */
    context(context: ParsingContext)
    fun parseValueArgumentList() {
        parseValueArgumentList(LPAR, RPAR)
    }

    /**
     * 解析值参数列表
     *
     * @param start 开始token
     * @param end 结束token
     */
    context(context: ParsingContext)
    fun parseValueArgumentList(
        start: CjToken = LPAR,
        end: CjToken = RPAR,
        argumentKind: CjExprKind = CjExprKind.EXPR_IN_CALLSUFFIX,
    ) {
        parseValueArgumentList(Pair.create(TokenSet.create(start), end), argumentKind)
    }

    /**
     * 解析值参数列表
     *
     * Grammar:
     * ```
     * valueArgumentList
     *   : "(" (valueArgument ("," valueArgument)*)? ")"
     *   ;
     * ```
     *
     * @param struct 参数列表的起止符号对
     */
    context(context: ParsingContext)
    fun parseValueArgumentList(
        struct: Pair<TokenSet, CjToken> = Pair(
            TokenSet.create(LPAR), RPAR
        ),
        argumentKind: CjExprKind = CjExprKind.EXPR_IN_CALLSUFFIX,
    ) {
        val list = mark()

        val sturctStart = struct.first
        val sturctEnd = struct.second

        builder.disableNewlines()
        if (expectSafeCall(sturctStart, "Expecting an argument list", EXPRESSION_FOLLOW)) {
            if (!at(sturctEnd)) {
                while (true) {
                    while (at(COMMA)) errorAndAdvance(CangJieParsingBundle.message("parsing.error.expecting.element.argument"))
                    parseValueArgument(argumentKind)
                    if (at(COLON) && lookahead(1) === IDENTIFIER) {
                        errorAndAdvance(CangJieParsingBundle.message("parsing.error.unexpected.type.specification"), 2)
                    }
                    if (!at(COMMA)) {
                        if (physicalNewlineBeforeCurrentToken()) {
                            break
                        } else if (atSet(EXPRESSION_FIRST)) {
                            error(CangJieParsingBundle.message("parsing.error.expecting.symbol", ","))
                            continue
                        } else {
                            break
                        }
                    }
                    advance()
                    if (at(RPAR)) {
                        break
                    }
                }
            }
            expect(
                sturctEnd,
                CangJieParsingBundle.message("parsing.error.expecting.symbol", tokenPresentation(sturctEnd)),
                EXPRESSION_FOLLOW
            )
        }
        builder.restoreNewlinesState()
        list.done(VALUE_ARGUMENT_LIST)
    }

    private fun physicalNewlineBeforeCurrentToken(): Boolean {
        if (eof()) return true

        for (i in 1..builder.currentOffset) {
            val previousToken = builder.rawLookup(-i)
            if (
                previousToken === BLOCK_COMMENT ||
                previousToken === DOC_COMMENT ||
                previousToken === EOL_COMMENT ||
                previousToken === SHEBANG_COMMENT
            ) {
                continue
            }
            if (previousToken === NL) return true
            if (previousToken !== TokenType.WHITE_SPACE) {
                break
            }

            val previousTokenStart = builder.rawTokenTypeStart(-i)
            val previousTokenEnd = builder.rawTokenTypeStart(-i + 1).coerceAtMost(builder.originalText.length)
            if (previousTokenStart < 0 || previousTokenEnd < previousTokenStart) {
                break
            }
            for (offset in previousTokenStart until previousTokenEnd) {
                if (builder.originalText[offset] == '\n') {
                    return true
                }
            }
        }

        return false
    }

    private fun previousSignificantTokenIsLiteral(): Boolean {
        for (i in 1..builder.currentOffset) {
            val previousToken = builder.rawLookup(-i) ?: return false
            if (
                previousToken === TokenType.WHITE_SPACE ||
                previousToken === NL ||
                previousToken === BLOCK_COMMENT ||
                previousToken === DOC_COMMENT ||
                previousToken === EOL_COMMENT ||
                previousToken === SHEBANG_COMMENT
            ) {
                continue
            }
            return previousToken === INTEGER_LITERAL ||
                previousToken === RUNE_LITERAL ||
                previousToken === CHARACTER_BYTE_LITERAL ||
                previousToken === FLOAT_LITERAL ||
                previousToken === TRUE_KEYWORD ||
                previousToken === FALSE_KEYWORD
        }
        return false
    }

    private fun tokenPresentation(token: CjToken): String =
        (token as? CjSingleValueToken)?.value ?: token.name

    private fun atStatementOnlyTokenAfterPhysicalNewline(): Boolean =
        physicalNewlineBeforeCurrentToken() && atSet(STATEMENT_FIRST) && !atSet(EXPRESSION_FIRST)

    // ==================== 语句解析 ====================

    /**
     * 解析语句序列(带结束符)
     *
     * Grammar:
     * ```
     * statements
     *   : SEMI* statement (SEMI+ statement)* SEMI*
     *   ;
     * ```
     *
     * @param type 结束符类型
     */
    context(context: ParsingContext)
    fun parseStatements(type: IElementType) {
        while (at(SEMICOLON)) advance()
        while (!eof() && !at(RBRACE) && !at(type)) {
            if (!atSet(STATEMENT_FIRST)) {
                errorAndAdvance(CangJieParsingBundle.message("parsing.error.expecting.element.element"))
            }
            if (atSet(STATEMENT_FIRST)) {

                parseStatement()

            }
            if (at(SEMICOLON)) {
                while (at(SEMICOLON)) advance()
            } else if (at(RBRACE)) {
                break
            } else if (at(type)) {
                break
            } else if (!physicalNewlineBeforeCurrentToken()) {
                recoverStatementSeparator(type)
            }
        }
    }

    /**
     * 解析语句序列(默认)
     *
     * Grammar:
     * ```
     * statements
     *   : SEMI* statement (SEMI+ statement)* SEMI*
     *   ;
     * ```
     */
    context(context: ParsingContext)
    fun parseStatements() {
        while (at(SEMICOLON)) advance()
        while (!eof() && !at(RBRACE)) {
            if (!atSet(STATEMENT_FIRST)) {
                errorAndAdvance(CangJieParsingBundle.message("parsing.error.expecting.element.element"))
            }

            if (atSet(STATEMENT_FIRST)) {

                parseStatement()

            }

            if (at(SEMICOLON)) {
                while (at(SEMICOLON)) advance()
            } else if (at(RBRACE)) {
                break
            } else if (!physicalNewlineBeforeCurrentToken()) {
                recoverStatementSeparator(RBRACE)
            }
        }
    }

    /**
     * 解析字符串模板中的语句序列
     */
    context(context: ParsingContext)
    private fun parseStatementsByStringTemplate() {
        while (at(SEMICOLON)) advance()
        while (!eof() && !at(LONG_TEMPLATE_ENTRY_END)) {
            if (!atSet(STATEMENT_FIRST)) {
                errorAndAdvance(CangJieParsingBundle.message("parsing.error.expecting.element.element"))
            }

            if (atSet(STATEMENT_FIRST)) {

                parseStatement()

            }

            if (at(SEMICOLON)) {
                while (at(SEMICOLON)) advance()
            } else if (at(LONG_TEMPLATE_ENTRY_END)) {
                break
            } else if (!physicalNewlineBeforeCurrentToken()) {
                recoverStatementSeparator(LONG_TEMPLATE_ENTRY_END)
            }
        }
    }

    /**
     * 同行缺少语句分隔符时，按完整括号组恢复到下一语句边界。
     * 对位官方 ConsumeUntilDeclOrNL/SkipPairedBrackets；被丢弃的 `{...}` 不能重新解析成 lambda，
     * 否则 PSI 惰性入口与 LightTree 会为恢复垃圾产生不同诊断。
     */
    context(context: ParsingContext)
    private fun recoverStatementSeparator(endToken: IElementType) {
        if (eof()) return
        val errorMarker = mark()
        while (!eof() && !at(endToken) && !at(RBRACE) &&
            !physicalNewlineBeforeCurrentToken() && !atSet(STATEMENT_NEW_LINE_QUICK_RECOVERY_SET)
        ) {
            consumeRecoveryToken()
        }
        if (context.shouldReportError) {
            errorMarker.error(CangJieParsingBundle.message("parsing.error.unexpected.tokens.same.line"))
        } else {
            errorMarker.drop()
        }
        while (at(SEMICOLON)) advance()
    }

    /**
     * 解析单个语句
     *
     * Grammar:
     * ```
     * statement
     *   : declaration
     *   | expression
     *   ;
     * ```
     */
    context(context: ParsingContext)
    fun parseStatement() {
        with(context.copy(memberScope = null)) {
            // 只有宏展开后的注解重解析才专有化；普通源码保留真实语句宏入口。
            if (context.enableCustomAnnotation && isIfAvailableExpressionStart()) {
                parseBlockLevelExpression()
                return
            }
            if (parseStatementMacro(DeclarationParsingMode.LOCAL)) return
            if (!parseLocalDeclaration(false)) {
                if (!atSet(context.expressionFirst)) {
                    errorAndAdvance(CangJieParsingBundle.message("parsing.error.expecting.element.statement"))
                } else {
                    parseBlockLevelExpression()
                }
            }
        }
    }

    /**
     * 根据作用域解析语句
     *
     * @param scope 声明解析模式
     */
    context(context: ParsingContext)
    fun parseStatementByScope(scope: DeclarationParsingMode) {
        with(if (scope == DeclarationParsingMode.LOCAL) context.copy(memberScope = null) else context) {
            if (context.enableCustomAnnotation && isIfAvailableExpressionStart()) {
                parseBlockLevelExpression()
                return
            }
            if (scope == DeclarationParsingMode.LOCAL && parseStatementMacro(scope)) return
            if (!parseDeclaration(scope, false)) {
                if (!atSet(EXPRESSION_FIRST)) {
                    errorAndAdvance(CangJieParsingBundle.message("parsing.error.expecting.element.statement"))
                } else {

                    parseBlockLevelExpression()

                }
            }
        }
    }

    /** 判断当前语句是否以官方 `@IfAvailable` 特殊表达式开头。 */
    private fun isIfAvailableExpressionStart(): Boolean =
        at(AT) && rawTokenText(builder, 1).toString() == "IfAvailable"

    /**
     * 官方 ParseExprOrDecl 的普通语句宏先按声明入口解析输入，再继续同一表达式的运算链。
     * 首操作数只传到最左侧 atom；右操作数和宏输入内的表达式仍使用自己的语法入口。
     */
    context(context: ParsingContext)
    private fun parseStatementMacro(scope: DeclarationParsingMode): Boolean {
        if (context.disableMacroParsing || context.enableCustomAnnotation || !cangJieParsing.isMacroCallStart()) return false
        with(context.copy(backToken = false, expressionGrammarKind = CjExprKind.ALL)) {
            parseBinaryExpression(Precedence.ASSIGNMENT) {
            parseMacroInvocation(context.statementScope, scope).expression
            }
        }
        return true
    }

    // ==================== 代码块解析 ====================

    /**
     * 解析代码块(带结束符)
     *
     * Grammar:
     * ```
     * block
     *   : "{" statements "}"
     *   ;
     * ```
     *
     * @param type 结束符类型
     */
    context(context: ParsingContext)
    fun parseBlock(type: IElementType) {
        while (!at(type) && !eof() && !at(RBRACE)) {

            parseBlockLevelExpression()

        }
    }

    /**
     * 解析代码块
     *
     * Grammar:
     * ```
     * block
     *   : "{" statements "}"
     *   ;
     * ```
     */
    context(context: ParsingContext)
    fun parseBlock() {
        cangJieParsing.parseBlock(collapse = false, scope = CjScopeKind.FUNC_BODY)
    }

    /**
     * 解析控制结构或主体
     */
    context(context: ParsingContext)
    private fun parseControlStructureOrBody() {
        with(context.copy(preferBlock = true)) {
            if (!parseAnnotatedLambda()) {

                parseBlockLevelExpression()

            }
        }
    }

    /**
     * 解析控制结构主体
     *
     * Grammar:
     * ```
     * controlStructureBody
     *   : block
     *   ;
     * ```
     */
    context(context: ParsingContext)
    private fun parseControlStructureBody() {
        if (at(LBRACE)) {
            cangJieParsing.parseBlock(collapse = false, scope = CjScopeKind.FUNC_BODY)
        } else {
            error(CangJieParsingBundle.message("parsing.error.expecting.symbol", "{"))
        }
    }

    // ==================== 声明解析 ====================

    /**
     * 解析声明
     *
     * Grammar:
     * ```
     * declaration
     *   : modifiers? declarationRest
     *   ;
     * ```
     *
     * @param scope 声明解析模式
     * @param rollbackIfDefinitelyNotExpression 如果明确不是表达式是否回滚
     * @param rollbackMacro 是否回滚宏
     * @return 是否成功解析声明
     */
    context(context: ParsingContext)
    fun parseDeclaration(
        scope: DeclarationParsingMode,
        rollbackIfDefinitelyNotExpression: Boolean,
        rollbackMacro: Boolean = false,
    ): Boolean {
        val decl: PsiBuilder.Marker = mark()
        val detector: CangJieParsing.ModifierDetector = CangJieParsing.ModifierDetector()

        // 局部声明与文件/成员声明共享注解前缀解析；不是声明时由 decl 一并回滚。
        cangJieParsing.parseAnnotations()
        cangJieParsing.parseModifierList(detector, TokenSet.EMPTY, rollbackMacro)
        val declType: IElementType? = parseDeclarationRest(detector, rollbackIfDefinitelyNotExpression, scope)

        return if (declType == ANNOTATION) {
            decl.rollbackTo()
            parseLocalDeclaration(rollbackIfDefinitelyNotExpression, true)
        } else if (declType == INVALID_DECLARATION) {
            decl.error(CangJieParsingBundle.message("parsing.error.invalid.declaration.scope"))

//            errorAndAdvance(
//                CangJieParsingBundle.message("parsing.error.invalid.declaration.scope")
//            )
//            decl.drop()


            true
        } else if (declType != null) {
            closeDeclarationWithCommentBinders(
                decl,
                declType,
                declType !== VARIABLE,
            )
            true
        } else {
            decl.rollbackTo()
            false
        }
    }

    /**
     * 解析局部声明
     *
     * @param rollbackIfDefinitelyNotExpression 如果明确不是表达式是否回滚
     * @param rollbackMacro 是否回滚宏
     * @return 是否成功解析声明
     */
    context(context: ParsingContext)
    private fun parseLocalDeclaration(
        rollbackIfDefinitelyNotExpression: Boolean,
        rollbackMacro: Boolean = false,
    ): Boolean {
        return parseDeclaration(DeclarationParsingMode.LOCAL, rollbackIfDefinitelyNotExpression, rollbackMacro)
    }

    /**
     * 解析声明的剩余部分
     *
     * @param detector 修饰符检测器
     * @param failIfDefinitelyNotExpression 如果明确不是表达式是否失败
     * @param scope 声明解析模式
     * @return 声明类型,如果解析失败则返回null
     */
    context(context: ParsingContext)
    private fun parseDeclarationRest(
        detector: CangJieParsing.ModifierDetector,
        failIfDefinitelyNotExpression: Boolean,
        scope: DeclarationParsingMode,
    ): IElementType? {
        val keyword = tt()
        if (failIfDefinitelyNotExpression) {
            if (keyword != FUNC_KEYWORD) return null
            return cangJieParsing.parseFunction(detector = detector)
        }

        return cangJieParsing.parseCommonDeclaration(
            detector,
            NameParsingMode.REQUIRED,
            scope,
        )
    }

    // ==================== 特殊表达式 ====================

    /**
     * 解析unsafe表达式
     *
     * Grammar:
     * ```
     * unsafeExpression
     *   : "unsafe" block
     *   ;
     * ```
     */
    context(context: ParsingContext)
    fun parseUnsafeExpression() {
        assert(_at(UNSAFE_KEYWORD))
        val unsafe = mark()

        advance()

        cangJieParsing.parseBlock()
        unsafe.done(UNSAFE_EXPRESSION)
    }

    /**
     * 解析spawn表达式
     *
     * Grammar:
     * ```
     * spawnExpression
     *   : "spawn" valueArgumentList? lambdaArgument+
     *   ;
     * ```
     */
    context(context: ParsingContext)
    fun parseSpawnExpression() {
        assert(_at(SPAWN_KEYWORD))
        val spawn = mark()
        advance()

        if (at(LPAR)) {
            parseValueArgumentList()
        }
        parseCallWithClosure()

        spawn.done(SPAWN_EXPRESSION)
    }

    // ==================== 引用(Quote)表达式 ====================

    /**
     * 解析quote表达式
     *
     * Grammar:
     * ```
     * quoteExpression
     *   : "quote" "(" quoteParameters ")" quoteBody
     *   ;
     * ```
     */
    context(context: ParsingContext)
    fun parseQuoteExpression() {
        assert(_at(QUOTE_KEYWORD))

        val quoteExpression = mark()
        builder.withNLTokenSkipping(false) {
            val quoteKeyword = mark()
            advance()

            if (at(NL)) {
                error(
                    quoteKeyword,
                    CangJieParsingBundle.message("parsing.error.unexpected.line.break.after.quote"),
                )
                while (at(NL)) advance()
                if (at(LPAR)) {
                    advance()
                    parseQuoteParameters()
                    expect(RPAR, CangJieParsingBundle.message("parsing.error.expecting.symbol", ")"))
                }
            } else {
                quoteKeyword.drop()
                expect(LPAR, CangJieParsingBundle.message("parsing.error.expecting.symbol", "(")) {
                    parseQuoteParameters()
                }
                expect(RPAR, CangJieParsingBundle.message("parsing.error.expecting.symbol", ")"))
            }
        }
        quoteExpression.done(QUOTE_EXPRESSION)

//        quoteExpressionParsing.parseQuoteExpression()
    }

    /**
     * 解析quote参数
     *
     * Grammar:
     * ```
     * quoteParameters
     *   : (quoteToken | quoteInterpolate)+
     *   ;
     * ```
     */
    context(context: ParsingContext)
    private fun parseQuoteParameters() {
        val quoteParameters = mark()
        var lparCount = 0
        while (!eof()) {
            if (at(RPAR) && lparCount == 0) break

            if (isMacroEscapeStart()) {
                parseQuoteEscapeToken()
            } else if (at(DOLLAR) && lookahead(1) == LPAR) {
                parseQuoteInterpolate()
            } else if (at(FIELD_IDENTIFIER)) {
                parseQuoteIdentifierInterpolate()
            } else {
                if (at(LPAR)) {
                    lparCount++
                } else if (at(RPAR)) {
                    if (lparCount == 0) break
                    lparCount--
                } else if (at(DOLLAR)) {
                    errorAndAdvance(CangJieParsingBundle.message("parsing.error.expected.identifier.after.dollar"))
                    continue
                }
                advance()
            }
        }
        quoteParameters.done(QUOTE_PARAMETERS)
    }

    /** 原生 lexer 同样保留独立反斜杠词元；escape 只在 quote/macro scanner 中解释。 */
    private fun isMacroEscapeStart(): Boolean =
        (_at(TokenType.BAD_CHARACTER) || _at(CjTokens.UNKNOWN_TOKEN_START)) && builder.tokenText == "\\"

    context(context: ParsingContext)
    private fun consumeMacroEscapePrefix() {
        check(isMacroEscapeStart())
        advance()
    }

    /** 对位 ParseQuoteEscapeToken；转义目标不参与括号计数，也不建立插值或宏调用。 */
    context(context: ParsingContext)
    private fun parseQuoteEscapeToken() {
        if (lookahead(1) == LBRACKET || lookahead(1) == RBRACKET) {
            errorOnRange(builder.currentOffset, builder.currentOffset,
                CangJieParsingBundle.message("parsing.error.unknown.token.start", "\\"),
                category = org.cangnova.cangjie.messages.ParsingDiagnostic.Category.LEX,
                rangeKind = org.cangnova.cangjie.messages.ParsingDiagnostic.RangeKind.POINT)
        }
        consumeMacroEscapePrefix()
        if (!eof()) advance()
    }

    /**
     * 解析quote插值
     *
     * Grammar:
     * ```
     * quoteInterpolate
     *   : "$" "(" expression ")"
     *   ;
     * ```
     */
    context(context: ParsingContext)
    private fun parseQuoteInterpolate() {
        assert(_at(DOLLAR) && lookahead(1) == LPAR)
        val mark = mark()

        advance()
        advance()
        builder.withNLTokenSkipping(true) {
            parseExpression()
        }

        if (at(RPAR)) {
            advance()
        } else {
            error("expected ')' after '$'")
        }

        mark.done(QUOTE_INTERPOLATE)
    }

    /**
     * 解析 quote 的 `$identifier` 简单插值。
     *
     * Grammar:
     * ```
     * quoteInterpolate
     *   : "$" "(" expression ")"
     *   | "$" identifier
     *   ;
     * ```
     */
    context(context: ParsingContext)
    private fun parseQuoteIdentifierInterpolate() {
        assert(_at(FIELD_IDENTIFIER))
        val interpolate = mark()
        val reference = mark()
        advance()
        reference.done(REFERENCE_EXPRESSION)
        interpolate.done(QUOTE_INTERPOLATE)
    }

    // ==================== 宏表达式 ====================

    /**
     * 解析宏表达式
     *
     * Grammar:
     * ```
     * macroExpression
     *   : "@" IDENTIFIER macroAttr? macroInput
     *   ;
     * ```
     *
     * @return 宏表达式类型,如果解析失败则返回null
     */
    context(context: ParsingContext)
    fun parseMacroExpression(
        scope: CjScopeKind,
        declarationParsingMode: DeclarationParsingMode,
        declarationPrefix: CjMacroDeclarationPrefix = CjMacroDeclarationPrefix.EMPTY,
    ): IElementType? {
        val result = parseMacroInvocation(scope, declarationParsingMode, declarationPrefix = declarationPrefix)
        return if (result.expression.root != ParsedExpression.Missing && context.backToken) MACRO_EXPRESSION else null
    }

    /** 原生调用登记与声明外层marker的闭合返回值分离；有效节点带错误也仍保留调用身份。 */
    context(context: ParsingContext)
    internal fun parseMacroInvocation(
        scope: CjScopeKind,
        declarationParsingMode: DeclarationParsingMode,
        parsingScope: MacroParsingScope = MacroParsingScope.Declaration(scope),
        declarationPrefix: CjMacroDeclarationPrefix = CjMacroDeclarationPrefix.EMPTY,
    ): ParsedMacroInvocation {
        assert(_at(AT))

        val start = expressionStart()
        val macroExpression = mark()
        val occurrence = builder.currentOffset
        advance()

        if (declarationPrefix.sourceModifiers.isNotEmpty()) {
            val first = declarationPrefix.sourceModifiers.minWith(compareBy(
                { it.sourceRange.source.nativePosition(it.sourceRange.startOffset).first },
                { it.sourceRange.source.nativePosition(it.sourceRange.startOffset).second },
            ))
            val hintEnd = if (eof()) checkNotNull(builder.lastConsumedTokenEndOffset)
                else builder.currentOffset + checkNotNull(builder.tokenText).length
            errorOnSourceRange(first.origin, first.sourceRange,
                CangJieParsingBundle.message("parsing.error.expected.no.modifier.before", "macro call", first.kind.token().value),
                listOf(ParsingDiagnosticHint(occurrence, hintEnd,
                    CangJieParsingBundle.message("parsing.error.hint.before", "macro call"))),
            )
        }

        if (!parseMacroReferenceExpression()) {
            error("expected identifier after '@'")
            macroExpression.drop()

            return ParsedMacroInvocation(start.finishRoot(ParsedExpression.Missing), false)
        }

        val root = if (context.recordMacroInvocations) {
            builder.macroParsingJournal.register(
                occurrence,
                // v1.1.3 fresh 宏节点只保存前导 annotation 向量，modifiers 仅传给输入声明。
                MacroParsingFacts(parsingScope, CjMacroFragmentParent.OTHER, context.primaryDeclaration?.identifier,
                    CjMacroDeclarationPrefix(emptyList(), declarationPrefix.annotations)),
            )
            ParsedExpression.MacroRoot(occurrence)
        } else ParsedExpression.Other

        if (at(LBRACKET)) {
            parseMacroAttrExpression()
        }

        val input = mark()
        var isEnumConstructorInput = false
        if (at(LPAR)) {
            val hasParenthesis = parseMacroInputExprWithParens()
            isEnumConstructorInput = hasParenthesis &&
                (scope == CjScopeKind.ENUM_CONSTRUCTOR || scope == CjScopeKind.ENUM_BODY)
        } else {
            val decl = mark()

            val modifiterDetector = CangJieParsing.ModifierDetector(
                CjMacroDeclarationPrefix(declarationPrefix.sourceModifiers, emptyList()),
            )

            cangJieParsing.parseAnnotations(modifiterDetector)
            cangJieParsing.parseModifierList(
                modifiterDetector, TokenSet.EMPTY,
                parseConstBeforePrimaryConstructor = true,
            )

            // 宏原子显式使用 UNKNOWN_SCOPE；宿主语句作用域不能覆盖实际传给 ParseDecl 的输入。
            val inputContext = context.copy(
                statementScope = scope,
                memberScope = context.memberScope?.takeIf {
                    it.declarationScope == scope ||
                        scope == CjScopeKind.ENUM_CONSTRUCTOR && it.kind == CjMemberParsingContext.Kind.ENUM
                },
            )
            val declType = with(inputContext) {
                if (at(AT)) {
                    val nested = with(inputContext.copy(backToken = true)) {
                        parseMacroInvocation(scope, declarationParsingMode,
                            declarationPrefix = modifiterDetector.declarationPrefix())
                    }
                    isEnumConstructorInput = nested.isEnumConstructor
                    if (nested.expression.root == ParsedExpression.Missing) null else MACRO_EXPRESSION
                } else {
                    parseMacroInputExprWithoutParens(modifiterDetector, scope, declarationParsingMode).also {
                        isEnumConstructorInput = it == ENUM_CONSTRUCTOR
                    }
                }
            }

            if (declType == null) {
                error("Macro call has no input")
                decl.drop()
            } else {
                closeDeclarationWithCommentBinders(decl, declType, true)
            }
        }
        input.done(MACRO_INPUT)
        if (context.backToken) {
            macroExpression.drop()
        } else {
            macroExpression.done(MACRO_EXPRESSION)
        }
        return ParsedMacroInvocation(start.finishRoot(root), isEnumConstructorInput)
    }

    /**
     * 解析 macro callee 的完整限定名。
     *
     * PSI 宏节点沿用单个 REFERENCE_EXPRESSION 作为 callee 容器；这里必须消费
     * `@pkg.Name` 的点号链，否则顶层 declaration macro 的真实声明会落到宏输入之外。
     */
    private fun parseMacroReferenceExpression(): Boolean {
        val simpleName = mark()
        if (!at(IDENTIFIER)) {
            simpleName.drop()
            return false
        }

        advance()
        while (at(DOT) && lookahead(1) == IDENTIFIER) {
            advance()
            advance()
        }
        simpleName.done(REFERENCE_EXPRESSION)
        return true
    }

    /**
     * 解析宏输入表达式(不带括号)
     *
     * @param modifiterDetector 修饰符检测器
     * @return 声明类型,如果解析失败则返回null
     */
    context(context: ParsingContext)
    private fun parseMacroInputExprWithoutParens(
        modifiterDetector: CangJieParsing.ModifierDetector,
        scope: CjScopeKind,
        declarationParsingMode: DeclarationParsingMode,
    ): IElementType? {
        // 官方 ParseMacroCallDeclInput 调用真实声明文法；检查时点属于该声明，不属于宏包装。
        if (declarationParsingMode == DeclarationParsingMode.ENUM_CONSTRUCTOR && at(IDENTIFIER)) {
            cangJieParsing.parseEnumConstructorRest(modifiterDetector)
            return ENUM_CONSTRUCTOR
        }

        var declType = parseMacroInputExprWithoutParensDeclaration(modifiterDetector, scope, declarationParsingMode)

        if (declType == null) {
            if (at(IDENTIFIER)) {
                // 宏声明输入与普通成员共享主构造器签名解析，包含自身泛型。
                val primaryGrammar = cangJieParsing.primaryParameterGrammar()
                if (cangJieParsing.seeingPrimaryConstructor()) {
                    // 宏 invocation 的 input 不经过名义 body 的直接成员错名收尾。
                    cangJieParsing.parsePrimaryInitFunc(checkNotNull(primaryGrammar), modifiterDetector)
                    declType = PRIMARY_CONSTRUCTOR
                } else {
                    if (context.memberScope != null) {
                        val invalidInput = mark()
                        cangJieParsing.parseInvalidMemberDeclaration()
                        val inputEnd = mark()
                        if (context.shouldReportError) {
                            invalidInput.errorBefore(
                                CangJieParsingBundle.message("parsing.error.macro.input.declaration.expected"), inputEnd,
                            )
                        } else {
                            invalidInput.drop()
                        }
                        inputEnd.drop()
                        declType = INVALID_DECLARATION
                    } else {
                        advance()
                    }
                }
            }
        }

        return declType
    }

    /**
     * 解析宏输入表达式(不带括号的声明)
     *
     * Grammar:
     * ```
     * macroInputExprWithoutParensDeclaration
     *   : macroExpression
     *   | functionDefinition
     *   | classDefinition
     *   | variableDeclaration
     *   | propertyDefinition
     *   | initDefinition
     *   ;
     * ```
     *
     * @param modifiterDetector 修饰符检测器
     * @return 声明类型,如果解析失败则返回null
     */
    context(context: ParsingContext)
    private fun parseMacroInputExprWithoutParensDeclaration(
        modifiterDetector: CangJieParsing.ModifierDetector,
        scope: CjScopeKind,
        declarationParsingMode: DeclarationParsingMode,
    ): IElementType? {
        return when (getTokenId()) {
            FUNC_KEYWORD_Id -> cangJieParsing.parseFunction(detector = modifiterDetector)
            EXTEND_KEYWORD_Id, ENUM_KEYWORD_Id, STRUCT_KEYWORD_Id, INTERFACE_KEYWORD_Id, CLASS_KEYWORD_Id ->
                cangJieParsing.parseClass(
                    modifiterDetector,
                    enclosingDeclarationKeyword = if (declarationParsingMode == DeclarationParsingMode.LOCAL) FUNC_KEYWORD else null,
                )

            LET_KEYWORD_Id, VAR_KEYWORD_Id, CONST_KEYWORD_Id -> cangJieParsing.parseVariable(modifiterDetector)

            PROP_KEYWORD_Id -> cangJieParsing.parseProperty(detector = modifiterDetector)
            INIT_KEYWORD_Id -> {
                cangJieParsing.parseInitFunc(modifiterDetector)
                SECONDARY_CONSTRUCTOR
            }

            TILED_Id -> cangJieParsing.parseMacroFinalizer(scope, modifiterDetector)

            else -> null
        }
    }

    /**
     * 解析宏输入表达式(带括号)
     *
     * Grammar:
     * ```
     * macroInputExprWithParens
     *   : "(" quoteTokens ")"
     *   ;
     * ```
     */
    context(context: ParsingContext)
    private fun parseMacroInputExprWithParens(): Boolean =
        with(context.copy(recordMacroInvocations = false)) {
            cangJieParsing.withQuotedFileSyntax { parseMacroInputTokensWithParens() }
        }

    context(context: ParsingContext)
    private fun parseMacroInputTokensWithParens(): Boolean =
        builder.withNLTokenSkipping(false) {
            assert(_at(LPAR))
            advance()
            val tokens = mark()
            val succeeded = parseMacroCallTokens(LPAR, RPAR, "parsing.error.macro.input.escape")
            tokens.done(CjNodeTypes.QUOTE_TOKENS)
            if (succeeded) expect(RPAR, CangJieParsingBundle.message("parsing.error.expecting.symbol", ")"))
            // hasParenthesis 对位原生 scanner 成功标志；缺右括号保留自己的恢复诊断。
            succeeded
        }

    /** 宏属性与圆括号 input 共用原生 token scanner，来源事实仍保持 token-only。 */
    context(context: ParsingContext)
    private fun parseMacroAttrExpression() {
        with(context.copy(recordMacroInvocations = false)) {
            cangJieParsing.withQuotedFileSyntax { parseMacroAttributeTokens() }
        }
    }

    context(context: ParsingContext)
    private fun parseMacroAttributeTokens() {
        builder.withNLTokenSkipping(false) {
            val attr = mark()
            if (at(LBRACKET)) {
                advance()
                val tokens = mark()
                val succeeded = parseMacroCallTokens(LBRACKET, RBRACKET, "parsing.error.macro.attribute.escape")
                tokens.done(CjNodeTypes.QUOTE_TOKENS)
                if (succeeded) expect(RBRACKET, CangJieParsingBundle.message("parsing.error.expecting.symbol", "]"))
            } else {
                error(CangJieParsingBundle.message("parsing.error.expecting.symbol", "["))
            }
            attr.done(MACRO_ATTR)
        }
    }

    /**
     * 对位 ParseMacroCallTokens。普通 AT、美元符号和声明关键字均是 token；
     * 子宏的发现属于 construction forest，不在原始 payload 内递归调用声明文法。
     */
    context(context: ParsingContext)
    private fun parseMacroCallTokens(left: IElementType, right: IElementType, escapeDiagnostic: String): Boolean {
        var depth = 0
        while (!eof()) {
            if (isMacroEscapeStart()) {
                if (!parseMacroCallEscapeTokens(left, right, escapeDiagnostic)) return false
            } else {
                if (at(left)) {
                    depth++
                } else if (at(right)) {
                    if (depth == 0) return true
                    depth--
                }
                advance()
            }
        }
        return true
    }

    /**
     * 对位 ParseMacroCallEscapeTokens：只允许本 input 的同类定界符及 AT。
     * 非法输入仅消费连续的 '['、'('、'.' 起点，随后结束 scanner，不吞掉整个平衡组。
     */
    context(context: ParsingContext)
    private fun parseMacroCallEscapeTokens(left: IElementType, right: IElementType, diagnosticKey: String): Boolean {
        consumeMacroEscapePrefix()
        if (at(left) || at(right) || at(AT)) {
            advance()
            return true
        }
        val message = CangJieParsingBundle.message(diagnosticKey)
        errorOnCurrentToken(message)
        while (at(LBRACKET) || at(LPAR) || at(DOT)) advance()
        return false
    }

    // ==================== 标签引用 ====================

    /**
     * 解析标签引用(不允许@前有空格)
     *
     * Grammar:
     * ```
     * labelReference
     *   : "@" IDENTIFIER  // no whitespace before @
     *   ;
     * ```
     */
    context(context: ParsingContext)
    private fun parseLabelReferenceWithNoWhitespace() {
        if (at(AT) && !builder.newlineBeforeCurrentToken()) {
            if (WHITE_SPACE_OR_COMMENT_BIT_SET.contains(builder.rawLookup(-1))) {
                error("There should be no space or comments before '@' in label reference")
            }
            parseLabelReference()
        }
    }

    /**
     * 解析标签引用
     *
     * Grammar:
     * ```
     * labelReference
     *   : "@" IDENTIFIER
     *   ;
     * ```
     */
    context(context: ParsingContext)
    private fun parseLabelReference() {
        assert(_at(AT))
        val labelWrap = mark()
        val mark = mark()
        if (builder.rawLookup(1) !== IDENTIFIER) {
            errorAndAdvance("Label must be named")
            labelWrap.drop()
            mark.drop()
            return
        }
        advance()
        advance()
        mark.done(LABEL)
        labelWrap.done(LABEL_QUALIFIER)
    }

    // ==================== 辅助方法 ====================

    /**
     * 回滚或丢弃标记
     *
     * @param rollbackMarker 标记
     * @param expected 期望的token
     * @param expectMessage 期望消息
     * @param validForDrop 有效丢弃类型
     * @return 是否成功
     */
    context(context: ParsingContext)
    private fun rollbackOrDrop(
        rollbackMarker: PsiBuilder.Marker,
        expected: CjToken,
        expectMessage: String,
        validForDrop: IElementType,
    ): Boolean {
        if (at(expected)) {
            advance()
            rollbackMarker.drop()
            return true
        } else if (at(validForDrop)) {
            rollbackMarker.drop()
            expect(expected, expectMessage)
            return true
        }

        rollbackMarker.rollbackTo()
        return false
    }

    /**
     * 在指定位置回滚或丢弃标记
     *
     * @param rollbackMarker 标记
     * @param dropAt 丢弃位置类型
     * @return 是否成功
     */
    context(context: ParsingContext)
    private fun rollbackOrDropAt(
        rollbackMarker: PsiBuilder.Marker,
        dropAt: IElementType
    ): Boolean {
        if (at(dropAt)) {
            advance()
            rollbackMarker.drop()
            return true
        }
        rollbackMarker.rollbackTo()
        return false
    }

    // ==================== 伴生对象和常量 ====================

    @OptIn(ExperimentalStdlibApi::class)
    companion object {
        var ALL_OPERATIONS: TokenSet? = null
        private const val PARSE_VARRAY_TYPE_PARAMETER_MESSAGE =
            "expected type parameters after 'VArray' keyword"
        private const val PARSE_VARRAY_TYPE_ARGS_MISMATCH_MESSAGE =
            "expected VArray type arguments between '<' and '>' of 'VArray' type"
        private const val PARSE_EXPECT_INTEGER_LITERAL_VARRAY_MESSAGE =
            "expected an integer literal than or equal to 0 after '$' to specificate the size of 'VArray' type"
        private const val PARSE_VARRAY_WITH_PAREN_MESSAGE =
            "expected '(' or '{' after 'VArray' for 'VArray' constructor"

        @JvmField
        val IDENTIFIER_RECOVERY_SET = TokenSet.create(
            IDENTIFIER,
            UNDERLINE,
        )

        private fun doneOrDrop(
            marker: PsiBuilder.Marker,
            type: IElementType,
            condition: Boolean,
        ) {
            if (condition) {
                marker.done(type)
            } else {
                marker.drop()
            }
        }

        @JvmStatic
        val EXPRESSION_FOLLOW = TokenSet.create(
            EOL_OR_SEMICOLON,
            ARROW,
            DOUBLE_ARROW,
            COMPOSITION,
            COMMA,
            RBRACE,
            RPAR,
            RBRACKET,
        )

        val ALLOW_NEWLINE_OPERATIONS = TokenSet.create(
            DOT,
            COLON,
            AS_KEYWORD,
            ANDAND,
            OROR,
            COALESCING,
        )

        val EXPRESSION_FIRST = TokenSet.orSet(
            TokenSet.create(
                AT,
                MINUS,
                PLUS,
                MINUSMINUS,
                PLUSPLUS,
                EXCL,
                LPAR,
                TRUE_KEYWORD,
                FALSE_KEYWORD,
                OPEN_QUOTE,
                INTEGER_LITERAL,
                RUNE_LITERAL,
                CHARACTER_BYTE_LITERAL,
                FLOAT_LITERAL,
                LBRACE,
                FUNC_KEYWORD,
                THIS_KEYWORD,
                SUPER_KEYWORD,
                IF_KEYWORD,
                MATCH_KEYWORD,
                TRY_KEYWORD,
                THROW_KEYWORD,
                PERFORM_KEYWORD,
                RESUME_KEYWORD,
                RETURN_KEYWORD,
                CONTINUE_KEYWORD,
                BREAK_KEYWORD,
                FOR_KEYWORD,
                WHILE_KEYWORD,
                DO_KEYWORD,
                VARRAY_KEYWORD,
                IDENTIFIER,
                UNDERLINE,
                LBRACKET,
                UNSAFE_KEYWORD,
                SPAWN_KEYWORD,
                SYNCHRONIZED_KEYWORD,
                QUOTE_KEYWORD,
            ),
            BASICTYPES,
        )

        val EXPRESSION_FIRST_WITH_LET = TokenSet.orSet(
            EXPRESSION_FIRST,
            TokenSet.create(LET_KEYWORD)
        )

        private val TYPE_ARGUMENT_LIST_STOPPERS = TokenSet.create(
            INTEGER_LITERAL,
            FLOAT_LITERAL,
            RUNE_LITERAL,
            CHARACTER_BYTE_LITERAL,
            OPEN_QUOTE,
            PACKAGE_KEYWORD,
            AS_KEYWORD,
            COALESCING,
            INTERFACE_KEYWORD,
            CLASS_KEYWORD,
            THIS_KEYWORD,
            LET_KEYWORD,
            VAR_KEYWORD,
            CONST_KEYWORD,
            FUNC_KEYWORD,
            FOR_KEYWORD,
            TRUE_KEYWORD,
            FALSE_KEYWORD,
            IS_KEYWORD,
            THROW_KEYWORD,
            RETURN_KEYWORD,
            BREAK_KEYWORD,
            CONTINUE_KEYWORD,
            IF_KEYWORD,
            TRY_KEYWORD,
            ELSE_KEYWORD,
            WHILE_KEYWORD,
            DO_KEYWORD,
            MATCH_KEYWORD,
            RBRACKET,
            RBRACE,
            RPAR,
            PLUSPLUS,
            MINUSMINUS,
            PLUS,
            MINUS,
            EXCL,
            DIV,
            PERC,
            LTEQ,
            EQEQ,
            EXCLEQ,
            ANDAND,
            OROR,
            SEMICOLON,
            RANGE,
            EQ,
            MULTEQ,
            DIVEQ,
            PERCEQ,
            PLUSEQ,
            MINUSEQ,
            COLON,
        )

        private val TRY_CATCH_RECOVERY_TOKEN_SET =
            TokenSet.create(LBRACE, RBRACE, FINALLY_KEYWORD, CATCH_KEYWORD, HANDLE_KEYWORD)

        private val IN_KEYWORD_R_PAR_COLON_SET = TokenSet.create(IN_KEYWORD, RPAR, COLON)

        private val IN_KEYWORD_L_BRACE_SET = TokenSet.create(IN_KEYWORD, LBRACE)

        private val IN_KEYWORD_L_BRACE_RECOVERY_SET =
            TokenSet.orSet(IN_KEYWORD_L_BRACE_SET, PARAMETER_NAME_RECOVERY_SET)

        private val COLON_IN_KEYWORD_SET = TokenSet.create(COLON, IN_KEYWORD)
        private val IN_KEYWORD_SET = TokenSet.create(IN_KEYWORD)

        private val L_PAR_L_BRACE_R_PAR_SET = TokenSet.create(LPAR, LBRACE, RPAR)

        private val MATCH_CONDITION_RECOVERY_SET = TokenSet.create(
            RBRACE,
            IN_KEYWORD,
            IS_KEYWORD,
        )

        @SuppressWarnings("WeakerAccess")
        val STATEMENT_FIRST = TokenSet.orSet(
            EXPRESSION_FIRST,
            TokenSet.create(
                // 与通用声明分派及原生 SeeingDecl 对齐；非法作用域也须先完整解析声明。
                FUNC_KEYWORD,
                MACRO_KEYWORD,
                LET_KEYWORD,
                CONST_KEYWORD,
                VAR_KEYWORD,
                INTERFACE_KEYWORD,
                CLASS_KEYWORD,
                STRUCT_KEYWORD,
                ENUM_KEYWORD,
                EXTEND_KEYWORD,
                TYPE_KEYWORD,
                MAIN_KEYWORD,
                FOREIGN_KEYWORD,
                SPAWN_KEYWORD,
                SYNCHRONIZED_KEYWORD,
            ),
            MODIFIER_KEYWORDS,
            BASICTYPES,
            SPECIAL_MODIFIER_KEYWORDS,
            TokenSet.create(AT, ATEXCL),
        )

        val STATEMENT_NEW_LINE_QUICK_RECOVERY_SET = TokenSet.orSet(
            TokenSet.andSet(
                STATEMENT_FIRST,
                TokenSet.andNot(KEYWORDS, TokenSet.create(IN_KEYWORD)),
            ),
            TokenSet.create(EOL_OR_SEMICOLON),
        )

        val logger = Logger.getInstance(
            CangJieExpressionParsing::class.java,
        )

        private val KEYWORD_TEXTS: ImmutableMap<String, CjToken> =
            tokenSetToMap(KEYWORDS)

        private fun tokenSetToMap(tokens: TokenSet): ImmutableMap<String, CjToken> {
            val builder: ImmutableMap.Builder<String, CjToken> = ImmutableMap.builder<String, CjToken>()
            for (token in tokens.types) {
                builder.put(token.toString(), token as CjToken)
            }
            return builder.build()
        }

        init {
            val operations: MutableSet<IElementType> = HashSet()
            val values: Array<Precedence> = Precedence.entries.toTypedArray()
            for (precedence in values) {
                operations.addAll(listOf(*precedence.getOperations().types))
            }
            ALL_OPERATIONS = TokenSet.create(*operations.toTypedArray<IElementType>())
        }

        init {
            val operations: Array<IElementType> = OPERATIONS.types
            val opSet: MutableSet<IElementType> = HashSet(listOf(*operations))
            val usedOperations: Array<IElementType> = ALL_OPERATIONS?.types ?: emptyArray()
            val usedSet: MutableSet<IElementType> = HashSet(listOf(*usedOperations))

            if (opSet.size > usedSet.size) {
                opSet.removeAll(usedSet)
                assert(false) { opSet }
            }
            assert(usedSet.size == opSet.size) { "Either some ops are unused, or something a non-op is used" }

            usedSet.removeAll(opSet)

            assert(usedSet.isEmpty()) { usedSet.toString() }
        }
    }
}

/** 判断当前元素类型是否与给定 token 完全相同（引用比较）。 */
private fun IElementType.equal(token: IElementType): Boolean {
    return token === this
}

/** 判断当前元素类型是否属于给定 token 集合。 */
private fun IElementType.equal(tokenSet: TokenSet): Boolean {
    return tokenSet.contains(this)
}

/**
 * 模式解析过程中已识别的模式语法类别。
 *
 * 解析阶段先按语法形态归类为该枚举，再由 [toPatternType] 映射为
 * 语义层的 [PatternType]、由 [toNodeType] 映射为具体 AST 节点类型；
 * 两步分离使同一语法类别可以在不同上下文落成不同节点。
 */
private enum class RecognizedPattern {
    VAR_OR_ENUM,
    BINDING,
    TYPE,
    ENUM;

    /**
     * 提供 `toPatternType` 操作，封装仓颉语法解析节点的访问、构造或判断逻辑。
     */
    fun toPatternType(): PatternType = when (this) {
        VAR_OR_ENUM -> PatternType.VAR_OR_ENUM
        BINDING -> PatternType.BINDING
        TYPE -> PatternType.TYPE
        ENUM -> PatternType.ENUM
    }

    /**
     * 提供 `toNodeType` 操作，封装仓颉语法解析节点的访问、构造或判断逻辑。
     */
    fun toNodeType(): IElementType = when (this) {
        VAR_OR_ENUM -> CjNodeTypes.VAR_OR_ENUM_PATTERN
        BINDING -> BINDING_PATTERN
        TYPE -> TYPE_PATTERN
        ENUM -> ENUM_PATTERN
    }
}

/**
 * 语义层的模式分类，供诊断与错误恢复按官方模式族分组处理。
 *
 * 与 [RecognizedPattern] 的区别：本枚举是完整分类（含 tuple/constant 等
 * 非延迟形态），而 [RecognizedPattern] 只覆盖语法识别的中间类别。
 *
 * @property displayName 诊断信息中的模式族显示名。
 */
enum class PatternType(val displayName: String) {
    WILDCARD("Wildcard patterns"),
    VAR_OR_ENUM("Deferred binding-or-enum patterns"),
    BINDING("Binding patterns"),
    TYPE("Type patterns"),
    TUPLE("Tuple patterns"),
    ENUM("Enum patterns"),
    CONSTANT("Constant patterns");

    companion object {
        val ALL_PATTERNS = PatternType.entries.toSet()
    }
}

/** TokenSet 的并集运算符，等价于 `TokenSet.orSet(this, set)`，便于解析器内组合 token 集合。 */
operator fun TokenSet.plus(set: TokenSet): TokenSet {
    return TokenSet.orSet(this, set)
}
