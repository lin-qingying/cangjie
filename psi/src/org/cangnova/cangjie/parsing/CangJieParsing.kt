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

import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiBuilderUtil.rawTokenText
import com.intellij.lang.WhitespacesBinders
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.cangnova.cangjie.CjSourceKind
import org.cangnova.cangjie.CjmpDeclarationRole
import org.cangnova.cangjie.CjmpFileRoleError
import org.cangnova.cangjie.CjmpFileRoleMode
import org.cangnova.cangjie.CjmpFileRoleTransition
import org.cangnova.cangjie.CjmpRoleEventKind
import org.cangnova.cangjie.CjmpRoleModifiers
import org.cangnova.cangjie.hasCjmpDefaultImplementation
import org.cangnova.cangjie.annotations.BuiltInAnnotationKind
import org.cangnova.cangjie.annotations.BuiltInAnnotationDescriptor
import org.cangnova.cangjie.annotations.BuiltInAnnotationRegistry
import org.cangnova.cangjie.lexer.CjKeywordToken
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.lexer.CjTokens.*
import org.cangnova.cangjie.messages.CangJieParsingBundle
import org.cangnova.cangjie.messages.CjParsingDiagnosticOrder
import org.cangnova.cangjie.messages.ParsingDiagnosticHint
import org.cangnova.cangjie.psi.CjRelativeErrorHint
import org.cangnova.cangjie.messages.ParsingDiagnostic
import org.cangnova.cangjie.psi.CallingConvention
import org.cangnova.cangjie.psi.CjBuiltInAnnotation
import org.cangnova.cangjie.psi.CjPsiUtil
import org.cangnova.cangjie.psi.CjNodeTypes.*
import org.cangnova.cangjie.psi.OverflowStrategy
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes.ANNOTATION
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes.ANNOTATIONS
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes.CONSTRUCTOR_CALLEE


/**
 * 表示 `CangJieParsing`，承载仓颉语法解析中的语法节点、索引桩或辅助模型。
 */
class CangJieParsing private constructor(
    builder: SemanticWhitespaceAwarePsiBuilder, isTopLevel: Boolean, isLazy: Boolean,
    initialLanguageModuleName: String,
    initialSourceKind: CjSourceKind = CjSourceKind.SOURCE,
    private val cjmpContext: CjmpParsingContext = CjmpParsingContext.SyntaxOnly,
    private val initialSettings: CjParserSettings = CjParserSettings(),
) : AbstractCangJieParsing(
    builder, isLazy
) {
    /** 官方仅在类体函数直接返回类型的解析期间允许 `This`。 */
    private var thisTypeAllowed: Boolean = false
    /** 当前源码的语言模块；完整文件按源码 prefixPaths 更新，片段由解析入口确定来源。 */
    internal val languageModuleName: String
        get() = fileState.languageModuleName

    /**
     * 被解析代码所属的源文件种类。
     *
     * 这是"解析模式"的**唯一载体**：声明文件（`.cj.d`）中的体允许缺失，相关诊断由
     * [reportMissingBody] 统一抑制（对齐官方 `Parser::parseDeclFile`）。
     *
     * 为什么不放进 [AbstractCangJieParsing.ParsingContext]：后者承载的是"用哪套文法"
     * （局部开关，会被 `with(...)` 整体替换），而"是不是声明文件"是**文件级恒定属性**。
     * 放进前者会被任何局部 `with` 静默清除，且属于分类维度错配。
     *
     * 取值只能来自被解析代码的归属文件（文件级解析来自文件本身，片段重解析来自片段的来源文件）。
     */
    val sourceKind: CjSourceKind
        get() = fileState.sourceKind

    /** 文件状态由根 journal 唯一持有，截断视图和子 parser 不保留另一份可变副本。 */
    private val fileState: CjParserFileState
        get() = builder.macroParsingJournal.fileState

    init {
        builder.macroParsingJournal.initializeFileState(initialSourceKind, initialLanguageModuleName, cjmpContext, initialSettings)
    }

    /**
     * 当前 nominal body 的直接声明解析事实；不进入共享宏 parser seed。
     * 这些事实只由当前文法接收路径记录，供 body 完成时执行官方短路顺序。
     */
    private class CjmpNominalParseFacts(
        val kind: CjMemberParsingContext.Kind,
        val declarationName: String?,
        val isCommon: Boolean,
        val isSpecific: Boolean,
        val modifierRulesEnabled: Boolean,
    ) {
        val directMembers: MutableList<CjmpDirectMemberFacts> = mutableListOf()
        var hasModifierMismatch: Boolean = false
        var hasDirectConstructor: Boolean = false
    }

    /** 一个直接成员的 modifier 与语法体事实，不从 PSI/CFIR 后代反向推断。 */
    private data class CjmpDirectMemberFacts(
        val startOffset: Int,
        val endOffset: Int,
        val declarationType: IElementType,
        val declarationName: String?,
        val isCommon: Boolean,
        val isSpecific: Boolean,
        val sourceCommon: Boolean,
        val sourceSpecific: Boolean,
        val sourcePlatform: Boolean,
        val hasFunctionBody: Boolean,
        val hasPropertyAccessor: Boolean,
        val hasInitializer: Boolean,
    ) {
        /**
         * 只根据 parser 已消费的直接成员语法事实复现官方 HasDefault；
         * 不能依赖构建后才存在的 CFIR 状态。
         */
        val hasDefault: Boolean
            get() = hasCjmpDefaultImplementation(isSpecific) {
                when (declarationType) {
                    FUNC, SECONDARY_CONSTRUCTOR, FINALIZER -> hasFunctionBody
                    PROPERTY -> hasPropertyAccessor
                    FIELD, VARIABLE -> hasInitializer
                    else -> false
                }
            }
    }

    /** 注解文法完成后的不可变身份和范围；包头、getter/setter 使用同一份采集协议。 */
    private data class CjmpParsedAnnotationFacts(
        val sourceName: String,
        val kind: BuiltInAnnotationKind?,
        val startOffset: Int,
        val endOffset: Int,
        val sourceFact: CjAnnotationSourceFact,
    )

    /** 一份 callable 声明的 CJMP 规则输入；声明签名完整后再按官方整体条件检查。 */
    private class CjmpParameterRuleFacts(
        val declarationKind: String,
        val isCommon: Boolean,
        val isSpecific: Boolean,
        val enabled: Boolean,
        val rejectSpecificDefaultValues: Boolean = false,
        val usesPlatformRole: Boolean = false,
    ) {
        val parameters: MutableList<CjmpParameterFacts> = mutableListOf()
        val specificParameterDefaultValues: MutableList<CjmpParameterDefaultValue> = mutableListOf()
        var genericDeclarationUnsupported: Boolean = false
    }

    private data class CjmpParameterDefaultValue(val startOffset: Int, val endOffset: Int)

    private data class CjmpParameterFacts(
        val startOffset: Int,
        val endOffset: Int,
        val name: String,
        val isCommon: Boolean,
        val isSpecific: Boolean,
    )

    /** lazy 根复用文件配置与当前角色，但始终创建独立的可变解析事务。 */
    internal fun restoreSeed(seed: CjParserSeed) {
        builder.macroParsingJournal.initializeFileSeed(seed)
    }

    internal fun parserSeed(context: ParsingContext): CjParserSeed = CjParserSeed(
        sourceKind, languageModuleName, cjmpContext, parsedFileFacts.cjmpRole, fileState.quotedSyntaxDepth, context,
        sourceContext = builder.macroParsingJournal.sourceContext,
        lexicalInput = builder.macroParsingJournal.lexicalInput,
        diagnosticScope = builder.macroParsingJournal.diagnosticScope,
        settings = builder.macroParsingJournal.parserSettings,
    )

    internal fun recordBodySeed(marker: PsiBuilder.Marker, openingBrace: Int, seed: CjParserSeed) {
        builder.macroParsingJournal.recordSeed(marker, openingBrace, seed)
    }

    /** 惰性体未解析其声明，不能将缺失的角色事件当作空事件序列。 */
    internal fun recordUnparsedRoleRegion(openingBrace: Int): CjmpParsedRoleEventId? =
        if (fileState.quotedSyntaxDepth == 0) {
            builder.macroParsingJournal.registerRoleEvent(openingBrace, CjmpRoleEventKind.Unknown)
        } else null

    /** quote 中的声明形状仅用于 token 结构，不是宿主文件的真实声明。 */
    internal fun <T> withQuotedFileSyntax(action: () -> T): T {
        val entryDepth = fileState.quotedSyntaxDepth
        builder.macroParsingJournal.updateFileState(fileState.copy(quotedSyntaxDepth = entryDepth + 1))
        try {
            return action()
        } finally {
            builder.macroParsingJournal.updateFileState(fileState.copy(quotedSyntaxDepth = entryDepth))
        }
    }

    internal val parsedFileFacts: ParsingFileFacts
        get() = ParsingFileFacts(sourceKind, fileState.role)

    companion object {
        val PARAMETER_NAME_RECOVERY_SET = TokenSet.create(COLON, EQ, COMMA, RPAR)

        // 官方 Modifier::operator< 按 TokenKind 排序；非成员参数选择首个修饰符时不按源码顺序。
        private val PARAMETER_MODIFIERS_IN_TOKEN_ORDER = listOf(
            CONST_KEYWORD, STATIC_KEYWORD, PUBLIC_KEYWORD, PRIVATE_KEYWORD, INTERNAL_KEYWORD,
            PROTECTED_KEYWORD, OVERRIDE_KEYWORD, REDEF_KEYWORD, ABSTRACT_KEYWORD, SEALED_KEYWORD,
            OPEN_KEYWORD, FOREIGN_KEYWORD, INOUT_KEYWORD, MUT_KEYWORD, UNSAFE_KEYWORD,
            OPERATOR_KEYWORD, COMMON_KEYWORD, SPECIFIC_KEYWORD,
        )
        private val PARAMETER_MODIFIER_KEYWORDS = TokenSet.create(*PARAMETER_MODIFIERS_IN_TOKEN_ORDER.toTypedArray())
        private val PROPERTY_ACCESSOR_MODIFIER_KEYWORDS = TokenSet.orSet(
            MODIFIER_KEYWORDS,
            TokenSet.create(INOUT_KEYWORD),
        )
        private val PARAMETER_ACCESS_MODIFIERS = listOf(PUBLIC_KEYWORD, PRIVATE_KEYWORD, INTERNAL_KEYWORD, PROTECTED_KEYWORD)

        /**
         * 上下文关键字（本仓库软关键字修饰符）后随这些 token 时按标识符处理，不作修饰符。
         *
         * 对齐官方 `SeeingKeywordAndOperater`（ParserUtils.cpp）：后随 token 落在官方 Tokens.inc 的
         * `DOT..EQUAL` 区间即判为标识符；区间内的 `~`（析构函数）与 `@`（宏调用）除外，故不入此集合。
         */
        private val CONTEXTUAL_KEYWORD_AS_IDENTIFIER_FOLLOW = TokenSet.create(
            DOT, COMMA, LPAR, RPAR, LBRACKET, RBRACKET, LBRACE, RBRACE,
            MULMUL, MUL, PERC, DIV, PLUS, MINUS, PLUSPLUS, MINUSMINUS,
            ANDAND, OROR, COALESCING, PIPELINE, COMPOSITION, EXCL, AND, OR, XOR, LTLT, GTGT,
            COLON, SEMICOLON, EQ, PLUSEQ, MINUSEQ, MULTEQ, MULMULEQ, DIVEQ, PERCEQ,
            ANDANDEQ, OROREQ, ANDEQ, OREQ, XOREQ, LTLTEQ, GTGTEQ,
            ARROW, LEFT_ARROW, DOUBLE_ARROW, RANGE, RANGEEQ, ELLIPSIS, HASH, QUEST,
            LT, GT, LTEQ, GTEQ, IS_KEYWORD, AS_KEYWORD, EXCLEQ, EQEQ,
        )
        private val GT_COMMA_COLON_SET = TokenSet.create(GT, COMMA, COLON)
        private val LOG = Logger.getInstance(CangJieParsing::class.java)
        private val TOP_LEVEL_DECLARATION_FIRST = TokenSet.create(
            INTERFACE_KEYWORD, CLASS_KEYWORD, FUNC_KEYWORD, LET_KEYWORD, VAR_KEYWORD, CONST_KEYWORD, PACKAGE_KEYWORD
        )
        private val TOP_LEVEL_DECLARATION_FIRST_SEMICOLON_SET =
            TokenSet.orSet(TOP_LEVEL_DECLARATION_FIRST, TokenSet.create(SEMICOLON))
        private val LT_EQ_SEMICOLON_TOP_LEVEL_DECLARATION_FIRST_SET =
            TokenSet.orSet(TokenSet.create(LT, EQ, SEMICOLON), TOP_LEVEL_DECLARATION_FIRST)

        private val CLASS_NAME_RECOVERY_SET =
            TokenSet.orSet(TokenSet.create(LT, LPAR, LTCOLON, LBRACE), TOP_LEVEL_DECLARATION_FIRST)
        private val TYPE_PARAMETER_GT_RECOVERY_SET = TokenSet.create(WHERE_KEYWORD, LPAR, COLON, LBRACE, GT)
        private val PACKAGE_NAME_RECOVERY_SET = TokenSet.create(DOT, DOUBLE_COLON, EOL_OR_SEMICOLON)
        private val IMPORT_RECOVERY_SET = TokenSet.create(AS_KEYWORD, DOT, DOUBLE_COLON, EOL_OR_SEMICOLON)
        private val TYPE_REF_FIRST = TokenSet.create(LBRACKET, IDENTIFIER, LPAR, HASH)
        private val TYPE_REF_RECOVERY_SET = TokenSet.orSet(
            TOP_LEVEL_DECLARATION_FIRST,
            TokenSet.create(EQ, COMMA, GT, RBRACKET, DOT, RPAR, RBRACE, LBRACE, SEMICOLON),
        )
        private val LBRACE_RBRACE_TYPE_REF_FIRST_SET = TokenSet.orSet(TokenSet.create(LBRACE, RBRACE), TYPE_REF_FIRST)
        private val LTCOLON_COMMA_LBRACE_RBRACE_TYPE_REF_FIRST_SET =
            TokenSet.orSet(TokenSet.create(LTCOLON, COMMA, LBRACE, RBRACE), TYPE_REF_FIRST)
        private val RECEIVER_TYPE_TERMINATORS = TokenSet.create(DOT)
        private val VALUE_PARAMETER_FIRST = TokenSet.orSet(
            TokenSet.create(IDENTIFIER, LBRACKET, LET_KEYWORD, CONST_KEYWORD, VAR_KEYWORD),
            TokenSet.andNot(MODIFIER_KEYWORDS, TokenSet.create(FUNC_KEYWORD))
        )
        private const val PARSE_VARRAY_TYPE_PARAMETER_MESSAGE =
            "expected type parameters after 'VArray' keyword"
        private const val PARSE_VARRAY_TYPE_ARGS_MISMATCH_MESSAGE =
            "expected VArray type arguments between '<' and '>' of 'VArray' type"
        private const val PARSE_EXPECT_INTEGER_LITERAL_VARRAY_MESSAGE =
            "expected an integer literal than or equal to 0 after '$' to specificate the size of 'VArray' type"
        private val LAMBDA_VALUE_PARAMETER_FIRST = TokenSet.orSet(
            TokenSet.create(IDENTIFIER, LBRACKET), TokenSet.andNot(MODIFIER_KEYWORDS, TokenSet.create(FUNC_KEYWORD))
        )

        private val BLOCK_DOC_COMMENT_SET = TokenSet.create(BLOCK_COMMENT, DOC_COMMENT)
        private val SEMICOLON_SET = TokenSet.create(SEMICOLON)
        private val COMMA_COLON_GT_SET = TokenSet.create(COMMA, COLON, GT)
        private val IDENTIFIER_RBRACKET_LBRACKET_SET = TokenSet.create(IDENTIFIER, RBRACKET, LBRACKET)
        private val LBRACE_RBRACE_SET = TokenSet.create(LBRACE, RBRACE)
        private val COMMA_SEMICOLON_RBRACE_SET = TokenSet.create(COMMA, SEMICOLON, RBRACE)
        private val VALUE_ARGS_RECOVERY_SET = TokenSet.create(LBRACE, SEMICOLON, RPAR, EOL_OR_SEMICOLON, RBRACE)
        private val PROPERTY_NAME_FOLLOW_SET =
            TokenSet.create(COLON, EQ, LBRACE, RBRACE, SEMICOLON, LET_KEYWORD, VAR_KEYWORD, FUNC_KEYWORD, CLASS_KEYWORD)
        private val PROPERTY_NAME_FOLLOW_MULTI_DECLARATION_RECOVERY_SET =
            TokenSet.orSet(PROPERTY_NAME_FOLLOW_SET, PARAMETER_NAME_RECOVERY_SET)
        private val PROPERTY_NAME_FOLLOW_FUNCTION_OR_PROPERTY_RECOVERY_SET =
            TokenSet.orSet(PROPERTY_NAME_FOLLOW_SET, LBRACE_RBRACE_SET, TOP_LEVEL_DECLARATION_FIRST)
        private val IDENTIFIER_EQ_COLON_SEMICOLON_SET = TokenSet.create(IDENTIFIER, EQ, COLON, SEMICOLON)
        private val COMMA_RPAR_COLON_EQ_SET = TokenSet.create(COMMA, RPAR, COLON, EQ)
        private val ACCESSOR_FIRST_OR_PROPERTY_END =
            TokenSet.orSet(MODIFIER_KEYWORDS, TokenSet.create(GET_KEYWORD, SET_KEYWORD, EOL_OR_SEMICOLON, RBRACE))
        private val RPAR_IDENTIFIER_COLON_LBRACE_EQ_SET = TokenSet.create(RPAR, IDENTIFIER, COLON, LBRACE, EQ)
        private val COMMA_COLON_RPAR_SET = TokenSet.create(COMMA, COLON, RPAR)
        private val RPAR_COLON_LBRACE_EQ_SET = TokenSet.create(RPAR, COLON, LBRACE, EQ)
        private val LBRACKET_LBRACE_RBRACE_LPAR_SET = TokenSet.create(LBRACKET, LBRACE, RBRACE, LPAR)
        private val CONSTRUCTOR_HEADER_RECOVERY_SET =
            TokenSet.orSet(LBRACKET_LBRACE_RBRACE_LPAR_SET, TokenSet.create(WHERE_KEYWORD))
        private val FUNCTION_NAME_FOLLOW_SET = TokenSet.create(LT, LPAR, RPAR, COLON, EQ)
        private val FUNCTION_NAME_RECOVERY_SET =
            TokenSet.orSet(TokenSet.create(LT, LPAR, RPAR, COLON, EQ), LBRACE_RBRACE_SET, TOP_LEVEL_DECLARATION_FIRST)
        private val VALUE_PARAMETERS_FOLLOW_SET = TokenSet.create(EQ, LBRACE, RBRACE, SEMICOLON, RPAR)
        private val ACCESSOR_PARAMETERS_FOLLOW_SET = TokenSet.create(LBRACE, RBRACE, SEMICOLON, RPAR)
        private val LPAR_VALUE_PARAMETERS_FOLLOW_SET =
            TokenSet.orSet(TokenSet.create(LPAR), VALUE_PARAMETERS_FOLLOW_SET)
        private val LPAR_LBRACE_COLON_CONSTRUCTOR_KEYWORD_SET = TokenSet.create(LPAR, LBRACE, COLON, INIT_KEYWORD)
        private val definitelyOutOfReceiverSet =
            TokenSet.orSet(TokenSet.create(EQ, COLON, LBRACE, RBRACE), TOP_LEVEL_DECLARATION_FIRST)
        private val EOL_OR_SEMICOLON_RBRACE_SET = TokenSet.create(EOL_OR_SEMICOLON, RBRACE)
        private val CLASS_INTERFACE_SET = TokenSet.create(CLASS_KEYWORD, INTERFACE_KEYWORD)
        private val CLASS_INTERFACE_STRUCT_ENUM_EXTEND_SET =
            TokenSet.create(CLASS_KEYWORD, INTERFACE_KEYWORD, STRUCT_KEYWORD, ENUM_KEYWORD, EXTEND_KEYWORD)

        private val IDENTIFIER_DOT_MUL_SET = TokenSet.create(IDENTIFIER, DOT, MUL)

        // 包的访问修饰符
        private val PACKAGE_ACCESS_MODIFIER_SET = TokenSet.create(PUBLIC_KEYWORD, INTERNAL_KEYWORD, PROTECTED_KEYWORD)

        // 导入语句访问修饰符
        private val IMPORT_ACCESS_MODIFIER_SET =
            TokenSet.create(PUBLIC_KEYWORD, INTERNAL_KEYWORD, PROTECTED_KEYWORD, PRIVATE_KEYWORD)

        private val DOT_MUL_SET = TokenSet.create(DOT, MUL)
        private val DECLARATION_FIRST =
            TokenSet.orSet(TOP_LEVEL_DECLARATION_FIRST, TokenSet.create(INIT_KEYWORD, GET_KEYWORD, SET_KEYWORD))
        private val MUT_PROP_SET = TokenSet.create(MUT_KEYWORD, PROP_KEYWORD)


        private fun createForByClause(
            builder: SemanticWhitespaceAwarePsiBuilder,
            isLazy: Boolean,
            languageModuleName: String,
            sourceKind: CjSourceKind = CjSourceKind.SOURCE,
            cjmpContext: CjmpParsingContext = CjmpParsingContext.SyntaxOnly,
            settings: CjParserSettings = CjParserSettings(),
        ): CangJieParsing {
            return CangJieParsing(
                SemanticWhitespaceAwarePsiBuilderForByClause(builder), false, isLazy, languageModuleName, sourceKind, cjmpContext, settings,
            )
        }

        fun createForTopLevel(
            builder: SemanticWhitespaceAwarePsiBuilder,
            languageModuleName: String = "",
            sourceKind: CjSourceKind = CjSourceKind.SOURCE,
            cjmpContext: CjmpParsingContext = CjmpParsingContext.SyntaxOnly,
            settings: CjParserSettings = CjParserSettings(),
        ): CangJieParsing {
            return CangJieParsing(builder, true, true, languageModuleName, sourceKind, cjmpContext, settings)
        }

        fun createForTopLevelNonLazy(
            builder: SemanticWhitespaceAwarePsiBuilder,
            languageModuleName: String = "",
            sourceKind: CjSourceKind = CjSourceKind.SOURCE,
            cjmpContext: CjmpParsingContext = CjmpParsingContext.SyntaxOnly,
            settings: CjParserSettings = CjParserSettings(),
        ): CangJieParsing {
            return CangJieParsing(builder, true, false, languageModuleName, sourceKind, cjmpContext, settings)
        }

    }

    /**
     * 保存 `expressionParsing`，供仓颉语法解析流程读取节点结构或语义信息。
     */
    val expressionParsing: CangJieExpressionParsing =
        if (isTopLevel) CangJieExpressionParsing(builder, this, isLazy) else object :
            CangJieExpressionParsing(builder, this@CangJieParsing, isLazy) {
            public override fun create(builder: SemanticWhitespaceAwarePsiBuilder): CangJieParsing {
                return CangJieParsing.createForByClause(
                    builder, super.isLazy, this@CangJieParsing.languageModuleName, sourceKind, cjmpContext, initialSettings,
                )
            }
        }


    /** 只有 scoped macro 根产生该结果；由平台完成范围平衡后附在真实 contents 上。 */
    internal var macroFragmentInfo: CjMacroFragmentParseInfo? = null
        private set

    /** 普通声明与宏首声明共享同一 modifier/annotation 检测器。 */
    public class ModifierDetector(prefix: CjMacroDeclarationPrefix = CjMacroDeclarationPrefix.EMPTY) : ((IElementType?) -> Unit) {
        private val inheritedPrefix = prefix
        private val inheritedModifiers = prefix.modifiers
        /**
         * 保存 `modifiers` 的内部状态，供仓颉语法解析实现维护节点缓存或解析上下文。
         */
        private val modifiers = LinkedHashSet(prefix.modifiers)

        /** 原始 token 的源码顺序；与文件角色检查清空后的有效属性分开保存。 */
        internal val parsedModifierOccurrences = mutableListOf<CjmpModifierOccurrence>()

        /** 真实语言内建注解身份；限定名和强制 custom 注解不会填充此集合。 */
        private val annotationKinds = LinkedHashSet(prefix.annotationKinds)
        private val parsedAnnotations = mutableListOf<CjAnnotationSourceFact>()
        internal var declarationAnchor: Int? = null
        internal var declarationKeyword: CjKeywordToken? = null
        internal var declarationNameRange: Pair<Int, Int>? = null

        /** 原生 set 保留先到的同 kind 对象；源码位置排序是另一项独立操作。 */
        internal val sourceModifierOccurrences: List<CjmpModifierOccurrence>
            get() = (inheritedPrefix.sourceModifiers.map { fact ->
                CjmpModifierOccurrence(fact.kind.token(), fact.sourceRange.startOffset, fact.sourceRange.endOffset, fact)
            } + parsedModifierOccurrences).distinctBy { it.token }

        internal fun declarationPrefix(): CjMacroDeclarationPrefix = CjMacroDeclarationPrefix(
            sourceModifierOccurrences.map { it.sourceFact }, inheritedPrefix.annotations + parsedAnnotations,
        )

        /** 本次声明解析时捕获的名字与 body 事实。 */
        internal var cjmpDeclarationName: String? = null
        internal var cjmpHasFunctionBody: Boolean = false
        internal var cjmpHasPropertyAccessor: Boolean = false
        internal var cjmpHasInitializer: Boolean = false
        internal var cjmpRulesShortCircuited: Boolean = false
        internal val isPlatformDetected: Boolean get() = has(CjModifierKind.PLATFORM)
        internal var cjmpGenericDeclarationUnsupported: Boolean = false
        internal var cjmpGenericDeclarationStartOffset: Int? = null
        /** 文法 owner 的实际检查结果；声明收尾只读取它，不再次更新文件角色。 */
        internal var cjmpFileRoleAccepted: Boolean = true
        /** 名字破损的 property 在官方检查入口之前返回，不执行后续修饰符检查。 */
        internal var cjmpSkipModifierChecks: Boolean = false
        /** secondary 的 COMMON 在 CheckDeclModifiers 前已置位，不随返回空属性而撤销。 */
        internal var predefinedCommonAttribute: Boolean = false

        /** foreign 集合为每条声明克隆完整前缀；可变源码列表不在兄弟声明之间共享。 */
        internal fun copyDeclarationPrefix(): ModifierDetector =
            ModifierDetector(inheritedPrefix).also {
                it.modifiers.addAll(modifiers)
                it.parsedModifierOccurrences.addAll(parsedModifierOccurrences)
                it.parsedAnnotations.addAll(parsedAnnotations)
                it.annotationKinds.addAll(annotationKinds)
            }

        internal fun recordAnnotation(fact: CjAnnotationSourceFact) {
            fact.kind?.let(annotationKinds::add)
            parsedAnnotations.add(fact)
        }

        internal fun hasJavaMirrorAnnotation(): Boolean =
            BuiltInAnnotationKind.JAVA_MIRROR in annotationKinds ||
                BuiltInAnnotationKind.JAVA_IMPL in annotationKinds

        internal fun hasFrozenAnnotation(): Boolean = BuiltInAnnotationKind.FROZEN in annotationKinds

        /**
         * 保存 `count`，供仓颉语法解析流程读取节点结构或语义信息。
         */
        val count: Int get() = modifiers.size

        /** 是否存在任何实际修饰符，包括不参与角色标记的语法 token。 */
        internal val hasParsedModifiers: Boolean get() = modifiers.isNotEmpty() || parsedModifierOccurrences.isNotEmpty()

        /** 前缀和本地 token 使用同一原始身份；重复 token 只占集合中的一项。 */
        internal val rawModifierKinds: Set<CjModifierKind>
            get() = inheritedModifiers + parsedModifierOccurrences.mapNotNull { it.token.cjModifierKindOrNull() }

        /**
         * 提供 `has` 操作，封装仓颉语法解析节点的访问、构造或判断逻辑。
         */
        fun has(kind: CjModifierKind): Boolean = modifiers.contains(kind)

        /** 官方 CheckDeclModifiers 在文件角色不合法时不发布任何有效修饰符属性。 */
        internal fun clear() = modifiers.clear()

        internal fun setReturnedModifierKinds(kinds: Set<CjModifierKind>) {
            modifiers.clear()
            modifiers.addAll(kinds)
        }

        internal fun restorePredefinedAttributes() {
            if (predefinedCommonAttribute) modifiers.add(CjModifierKind.COMMON)
        }

        /** GetModifierAttrs 即使报告非法目标或冲突也返回属性；CONST/INOUT 不进入 AttributePack。 */
        internal fun freezeDeclarationModifiers(
            rootIdentity: java.util.UUID,
            context: CjmpParsingContext,
            sourceToken: CjSourceTokenId,
            declarationKind: CjmpModifierDeclarationKind,
            scope: CjScopeKind,
        ): CjParsedDeclarationModifiers {
            val attributes = modifiers.filterTo(linkedSetOf()) {
                it != CjModifierKind.CONST && it != CjModifierKind.INOUT && it != CjModifierKind.PLATFORM
            }
            if (CjModifierKind.PLATFORM in modifiers &&
                (context as? CjmpParsingContext.Compilation)?.platformModifierSyntaxAvailable == true
            ) attributes.add(CjModifierKind.SPECIFIC)
            return CjParsedDeclarationModifiers(rootIdentity, context, attributes, CjModifierKind.CONST in rawModifierKinds,
                sourceToken, declarationKind, scope)
        }

        internal fun recordParsedModifier(occurrence: CjmpModifierOccurrence): CjmpModifierOccurrence? {
            val previous = sourceModifierOccurrences.firstOrNull { it.token == occurrence.token }
            parsedModifierOccurrences += occurrence
            return previous
        }

        /**
         * 保存 `isAbstractDetected`，供仓颉语法解析流程读取节点结构或语义信息。
         */
        val isAbstractDetected: Boolean get() = has(CjModifierKind.ABSTRACT)
        /**
         * 保存 `isMutDetected`，供仓颉语法解析流程读取节点结构或语义信息。
         */
        val isMutDetected: Boolean get() = has(CjModifierKind.MUT)
        /**
         * 保存 `isPublicDetected`，供仓颉语法解析流程读取节点结构或语义信息。
         */
        val isPublicDetected: Boolean get() = has(CjModifierKind.PUBLIC)
        /**
         * 保存 `isPrivateDetected`，供仓颉语法解析流程读取节点结构或语义信息。
         */
        val isPrivateDetected: Boolean get() = has(CjModifierKind.PRIVATE)
        /**
         * 保存 `isProtectedDetected`，供仓颉语法解析流程读取节点结构或语义信息。
         */
        val isProtectedDetected: Boolean get() = has(CjModifierKind.PROTECTED)
        /**
         * 保存 `isOperatorDetected`，供仓颉语法解析流程读取节点结构或语义信息。
         */
        val isOperatorDetected: Boolean get() = has(CjModifierKind.OPERATOR)
        /**
         * 保存 `isForeignDetected`，供仓颉语法解析流程读取节点结构或语义信息。
         */
        val isForeignDetected: Boolean get() = has(CjModifierKind.FOREIGN)
        /**
         * 保存 `isConstDetected`，供仓颉语法解析流程读取节点结构或语义信息。
         */
        val isConstDetected: Boolean get() = has(CjModifierKind.CONST)
        /**
         * 保存 `isUnsafeDetected`，供仓颉语法解析流程读取节点结构或语义信息。
         */
        val isUnsafeDetected: Boolean get() = has(CjModifierKind.UNSAFE)
        /**
         * 保存 `isSealedDetected`，供仓颉语法解析流程读取节点结构或语义信息。
         */
        val isSealedDetected: Boolean get() = has(CjModifierKind.SEALED)
        /**
         * 保存 `isRedefDetected`，供仓颉语法解析流程读取节点结构或语义信息。
         */
        val isRedefDetected: Boolean get() = has(CjModifierKind.REDEF)
        /**
         * 保存 `isOpenDetected`，供仓颉语法解析流程读取节点结构或语义信息。
         */
        val isOpenDetected: Boolean get() = has(CjModifierKind.OPEN)
        /**
         * 保存 `isStaticDetected`，供仓颉语法解析流程读取节点结构或语义信息。
         */
        val isStaticDetected: Boolean get() = has(CjModifierKind.STATIC)
        /**
         * 保存 `isCommonDetected`，供仓颉语法解析流程读取节点结构或语义信息。
         */
        val isCommonDetected: Boolean get() = has(CjModifierKind.COMMON)
        /**
         * 保存 `isSpecificDetected`，供仓颉语法解析流程读取节点结构或语义信息。
         */
        val isSpecificDetected: Boolean get() = has(CjModifierKind.SPECIFIC) || has(CjModifierKind.PLATFORM)

        /**
         * 提供 `getSize` 操作，封装仓颉语法解析节点的访问、构造或判断逻辑。
         */
        @Deprecated("Use count property", ReplaceWith("count"))
        fun getSize(): Int = count

        /**
         * 实现 `invoke` 的仓颉语法解析协议回调，保持与 IntelliJ PSI 访问契约一致。
         */
        override fun invoke(item: IElementType?) {
            item?.cjModifierKindOrNull()?.let(modifiers::add)
        }
    }


    /**
     * 保存 `lastDotAfterReceiverNotLParPattern` 的内部状态，供仓颉语法解析实现维护节点缓存或解析上下文。
     */
    private val lastDotAfterReceiverNotLParPattern = LastBefore(
        AtSet(RECEIVER_TYPE_TERMINATORS), object : AbstractTokenStreamPredicate() {
            override fun matching(topLevel: Boolean): Boolean {
                if (topLevel && (atSet(definitelyOutOfReceiverSet) || at(LPAR))) {
                    return true
                }
                if (topLevel && at(IDENTIFIER)) {
                    val lookahead = lookahead(1)
                    return lookahead != LT && lookahead != DOT && lookahead != QUEST
                }
                return false
            }
        })

    /**
     * 保存 `lastDotAfterReceiverLParPattern` 的内部状态，供仓颉语法解析实现维护节点缓存或解析上下文。
     */
    private val lastDotAfterReceiverLParPattern = FirstBefore(
        AtSet(RECEIVER_TYPE_TERMINATORS), object : AbstractTokenStreamPredicate() {
            override fun matching(topLevel: Boolean): Boolean {
                if (topLevel && atSet(definitelyOutOfReceiverSet)) {
                    return true
                }
                return topLevel && !at(QUEST) && !at(LPAR) && !at(RPAR)
            }
        })


    /**
     * 解析类型引用
     *
     * Grammar:
     * ```
     * typeReference
     *   : userType
     *   | functionType
     *   | basicType
     *   | optionalType
     *   | tupleType
     *   ;
     * ```
     */
    context(parseContext: ParsingContext)
    fun parseTypeRef() {
        parseTypeRef(TokenSet.EMPTY, false)
    }

    /**
     * 解析不带交集类型的类型引用
     *
     * Grammar:
     * ```
     * typeReference
     *   : userType
     *   | functionType
     *   | basicType
     *   | optionalType
     *   | tupleType
     *   ;
     * ```
     */
    context(parseContext: ParsingContext)
    fun parseTypeRefWithoutIntersections() {
        parseTypeRef(TokenSet.EMPTY)
    }


    /**
     * 提供 `create` 操作，封装仓颉语法解析节点的访问、构造或判断逻辑。
     */
    public override fun create(builder: SemanticWhitespaceAwarePsiBuilder): CangJieParsing {
        return CangJieParsing(builder, true, true, languageModuleName, sourceKind, cjmpContext)
    }


    /**
     * 解析构造函数代码块
     *
     * Grammar:
     * ```
     * initBlock
     *   : "{" statement* "}"
     *   ;
     * ```
     */

    context(parseContext: ParsingContext)
    private fun parseInitFunctionBlock() {
        parseBlock(collapse = false, scope = CjScopeKind.FUNC_BODY)
    }

    /**
     * 解析代码块
     *
     * Grammar:
     * ```
     * block
     *   : "{" statement* "}"
     *   ;
     * ```
     *
     * @param collapse 是否折叠代码块以提升解析性能
     */
    context(parseContext: ParsingContext)
    fun parseBlock(collapse: Boolean = true, scope: CjScopeKind = CjScopeKind.FUNC_BODY,
        diagnosticOwner: CjParsingScopeOwner? = null) {
        val lazyBlock = mark()
        val openingBrace = builder.currentOffset
        val journal = builder.macroParsingJournal
        val owner = diagnosticOwner ?: if (at(LBRACE)) CjParsingScopeOwner(CjParsingScopeOwner.Kind.BLOCK,
            journal.sourceOrigin(openingBrace, openingBrace + 1)) else parseContext.diagnosticScopeOwner
        val bodyContext = parseContext.copy(memberScope = null, statementScope = scope, backToken = false,
            diagnosticScopeOwner = owner)
        val diagnosticScope = if (at(LBRACE)) journal.reserveBodyDiagnosticScope(openingBrace) else journal.diagnosticScope
        var seed = parserSeed(bodyContext).copy(diagnosticScope = diagnosticScope)

        journal.withDiagnosticScope(diagnosticScope) {
            builder.enableNewlines()
            val hasOpeningBrace = expect(LBRACE, "Expecting '{'  ")
            val canCollapse = collapse && hasOpeningBrace && isLazy

            if (canCollapse) {
                seed = seed.copy(roleRegionSlot = recordUnparsedRoleRegion(openingBrace))
                advanceBalancedBlock()
            } else {
                with(bodyContext) { expressionParsing.parseStatements() }
                expect(RBRACE, "Expecting '}'")
            }

            builder.restoreNewlinesState()
            if (canCollapse) lazyBlock.collapse(BLOCK) else lazyBlock.done(BLOCK)
            if (hasOpeningBrace) recordBodySeed(lazyBlock, openingBrace, seed)
        }
    }


    /**
     * 解析块表达式
     *
     * 解析一个代码块作为表达式，不进行折叠。
     *
     * Grammar:
     * ```
     * blockExpression
     *   : block
     *   ;
     * ```
     */
    context(parseContext: ParsingContext)
    fun parseBlockExpression(diagnosticOwner: CjParsingScopeOwner? = null) {
        parseBlock(false, parseContext.statementScope, diagnosticOwner)
    }

    /**
     * 解析包名
     *
     * Grammar:
     * ```
     * packageName
     *   : simpleName ("." simpleName)*
     *   ;
     * ```
     */

//    context(parseContext: ParsingContext)
//    private fun parsePackageName() {
//        var qualifiedExpression = mark()
//        var simpleName = true
//
//        while (true) {
//            if (builder.newlineBeforeCurrentToken()) {
//                errorWithRecovery(
//                    "Package name must be a '.'-separated identifier list placed on a single line",
//                    PACKAGE_NAME_RECOVERY_SET
//                )
//                break
//            }
//
//            if (at(DOT)) {
//                advance() // DOT
//                qualifiedExpression.error(CangJieParsingBundle.message("parsing.error.package.name.separator"))
//                qualifiedExpression = mark()
//                continue
//            }
//
//            val nsName = mark()
//            val simpleNameFound = expect(
//                IDENTIFIER, "Package name must be a '.'-separated identifier list", PACKAGE_NAME_RECOVERY_SET
//            )
//            if (simpleNameFound) {
//                nsName.done(REFERENCE_EXPRESSION)
//            } else {
//                nsName.drop()
//            }
//
//            if (!simpleName) {
//                val precedingMarker = qualifiedExpression.precede()
//                qualifiedExpression.done(DOT_QUALIFIED_EXPRESSION)
//                qualifiedExpression = precedingMarker
//            }
//
//            if (at(DOT)) {
//                advance() // DOT
//
//                if (simpleName && !simpleNameFound) {
//                    qualifiedExpression.drop()
//                    qualifiedExpression = mark()
//                } else {
//                    simpleName = false
//                }
//            } else {
//                break
//            }
//        }
//
//        qualifiedExpression.drop()
//    }


    /**
     * 执行 `parsePackageName` 内部辅助逻辑，支撑仓颉语法解析节点的结构解析与访问。
     */
    context(parseContext: ParsingContext)
    private fun parsePackageName() {
        // 对齐官方 prefixPaths：多段源码包名覆盖输入模块，单段包保留 seed，组织名仍是首段。
        // 先解析第一个标识符
        if (builder.newlineBeforeCurrentToken()) {
            errorWithRecovery(
                "Package name must be a '.'-separated identifier list placed on a single line",
                PACKAGE_NAME_RECOVERY_SET
            )
            return
        }

        // 第一个片段
        val firstNameText = builder.tokenText
        val firstName = mark()
        val firstFound = expect(
            IDENTIFIER,
            "Package name must be a '.'-separated identifier list",
            PACKAGE_NAME_RECOVERY_SET
        )
        if (firstFound) {
            firstName.done(REFERENCE_EXPRESSION)
        } else {
            firstName.drop()
            return
        }

        // 单段 std 没有 prefixPaths，不能成为标准库内置注解的模块依据。
        if (!at(DOT) && !at(DOUBLE_COLON)) return
        builder.macroParsingJournal.updateFileState(fileState.copy(languageModuleName = checkNotNull(firstNameText)))

        // 有多段，构建左递归 DOT_QUALIFIED_EXPRESSION 树
        // 初始：wrap 第一个 REFERENCE_EXPRESSION
        var lhs = firstName.precede()
        // 此时 lhs 包裹着已 done 的 firstName

        var allowOrganizationSeparator = true
        while (at(DOT) || (allowOrganizationSeparator && at(DOUBLE_COLON))) {
            advance() // DOT or the one organization DOUBLE_COLON

            if (builder.newlineBeforeCurrentToken()) {
                lhs.done(DOT_QUALIFIED_EXPRESSION)
                errorWithRecovery(
                    "Package name must be a '.'-separated identifier list placed on a single line",
                    PACKAGE_NAME_RECOVERY_SET
                )
                return
            }

            val nextName = mark()
            val nextFound = expect(
                IDENTIFIER,
                "Package name must be a '.'-separated identifier list",
                PACKAGE_NAME_RECOVERY_SET
            )
            if (nextFound) {
                nextName.done(REFERENCE_EXPRESSION)
            } else {
                nextName.drop()
                lhs.done(DOT_QUALIFIED_EXPRESSION)
                return
            }

            // 把 lhs 收进新的 DOT_QUALIFIED_EXPRESSION，再向外扩展
            val newLhs = lhs.precede()
            lhs.done(DOT_QUALIFIED_EXPRESSION)
            lhs = newLhs
            allowOrganizationSeparator = false
        }

        if (at(DOUBLE_COLON)) {
            error("Organization separator is only allowed after the first identifier")
            advance()
        }

        // 最外层多套了一层，drop 掉
        lhs.drop()
    }

    /**
     * 执行 `parsePreamble` 内部辅助逻辑，支撑仓颉语法解析节点的结构解析与访问。
     */
    context(parseContext: ParsingContext)
    private fun parsePreamble() {
        parseFeaturesDirectiveIfPresent()
        // 解析包声明
        parsePackageDirective()
        // 解析导入列表
        parseImportDirectives()
    }

    /**
     * 解析官方文件前导 `features` directive。
     *
     * 官方 grammar 规定它位于 package header 之前，且仅允许在该 directive
     * 上承载前导注解（当前语义 checker 再验证 `@NonProduct`）。没有看到
     * `features` 时必须完整回滚，不能吞掉后续 package 的注解。
     */
    context(parseContext: ParsingContext)
    private fun parseFeaturesDirectiveIfPresent(): Boolean {
        val directive = mark()
        with(parseContext.copy(allowPackageDirectiveAnnotations = true)) {
            if (isAnnotationStart()) parseAnnotations()
        }
        if (!at(FEATURES_KEYWORD)) {
            directive.rollbackTo()
            return false
        }

        advance() // features
        val featureSet = mark()
        expect(LBRACE, CangJieParsingBundle.message("parsing.error.expecting.symbol", "{"))
        while (!eof() && !at(RBRACE)) {
            val featureId = mark()
            if (!at(IDENTIFIER)) {
                error(CangJieParsingBundle.message("parsing.error.expecting", "a feature identifier"))
                featureId.drop()
                while (!eof() && !at(COMMA) && !at(RBRACE)) advance()
            } else {
                advance()
                while (at(DOT)) {
                    advance()
                    if (at(IDENTIFIER)) {
                        advance()
                    } else {
                        error(CangJieParsingBundle.message("parsing.error.expecting", "a feature identifier"))
                        break
                    }
                }
                featureId.done(FEATURE_ID)
            }
            if (at(COMMA)) {
                advance()
            } else if (!at(RBRACE) && !eof()) {
                error(CangJieParsingBundle.message("parsing.error.expecting.symbol", ","))
                while (!eof() && !at(COMMA) && !at(RBRACE)) advance()
            }
        }
        expect(RBRACE, CangJieParsingBundle.message("parsing.error.expecting.symbol", "}"))
        featureSet.done(FEATURES_SET)
        directive.done(FEATURES_DIRECTIVE)
        return true
    }

    /**
     * 执行 `parsePackageDirective` 内部辅助逻辑，支撑仓颉语法解析节点的结构解析与访问。
     */
    context(parseContext: ParsingContext)
    private fun parsePackageDirective() {
        // 对位 ParseFile 的 ParserScope：先收集完整前缀，非包头时连同 journal 一起撤销。
        val packageDirective = mark()
        val detector = ModifierDetector()
        val annotations = mutableListOf<CjmpParsedAnnotationFacts>()
        parseAnnotations(detector, annotations::add)
        parseModifierList(detector, TokenSet.EMPTY)
        if (!at(PACKAGE_KEYWORD) && !(at(MACRO_KEYWORD) && lookahead(1) == PACKAGE_KEYWORD)) {
            packageDirective.rollbackTo()
            // 没有包声明，直接插入空的 PACKAGE_DIRECTIVE 占位
            val emptyPackage = mark()
            emptyPackage.done(PACKAGE_DIRECTIVE)
            emptyPackage.setCustomEdgeTokenBinders(BindFirstShebangWithWhitespaceOnly, null)
            return
        }

        if (at(MACRO_KEYWORD)) {
            advance() // MACRO_KEYWORD
        }

        if (at(PACKAGE_KEYWORD)) {
            val declarationAnchor = builder.currentOffset
            advance() // PACKAGE_KEYWORD
            parsePackageName()
            checkCjmpDeclarationFileRole(detector, PACKAGE_DIRECTIVE, declarationAnchor)
            // 官方包头只报告首个前导注解，随后清空该前缀，不转交下一条声明。
            annotations.firstOrNull()?.let {
                reportUnexpectedAnnotation(it, PACKAGE_KEYWORD.value, declarationAnchor, PACKAGE_KEYWORD.value.length)
            }
            consumeIf(SEMICOLON)
        } else {
            error(CangJieParsingBundle.message("parsing.error.expecting.keyword", "package"))
        }

        packageDirective.done(PACKAGE_DIRECTIVE)
    }
    /**
     * 解析文件前导部分
     *
     * Grammar:
     * ```
     * preamble
     *   : fileAnnotationList? packageDirective? importDirective*
     *   ;
     * ```
     */
//    context(parseContext: ParsingContext)
//    private fun parsePreamble() {
//        val firstEntry = mark()
//
//        /*
//         * TODO fileAnnotationList Ko
//         * 文档注释 : fileAnnotations*
//         */
//
//        /**
//         * 解析包声明
//         *
//         * Grammar:
//         * ```
//         * packageDirective
//         *   : modifier* "package" qualifiedName (";")?
//         *   ;
//         * ```
//         */
//        var packageDirective = mark()
//
//        // 是否有修饰符
//        var isPackageAccessModifier = false
//        if (atSet(PACKAGE_ACCESS_MODIFIER_SET) && (lookahead(1) == MACRO_KEYWORD || lookahead(1) == PACKAGE_KEYWORD)) {
//            advance() // 修饰符
//            isPackageAccessModifier = true
//        }
//
//        if (at(MACRO_KEYWORD)) {
//            advance() // MARCO_KEYWORD 宏声明
//            isPackageAccessModifier = true
//        }
//
//        if (at(PACKAGE_KEYWORD)) {
//            if (at(PACKAGE_KEYWORD)) {
//                advance() // PACKAGE_KEYWORD
//            } else if (isPackageAccessModifier) {
//                error(CangJieParsingBundle.message("parsing.error.expecting.keyword", "package"))
//            }
//
//
//            parsePackageName()
//
//            firstEntry.drop()
//
//            consumeIf(SEMICOLON)
//
//            packageDirective.done(PACKAGE_DIRECTIVE)
//        } else {
//            // 忽略 package 指令时不应报错，将位置回滚
//            firstEntry.rollbackTo()
//
//            // TODO 解析文件注解列表
//            // parseFileAnnotationList(FILE_ANNOTATIONS_WHEN_PACKAGE_OMITTED)
//            packageDirective = mark()
//            packageDirective.done(PACKAGE_DIRECTIVE)
//            packageDirective.setCustomEdgeTokenBinders(
//                BindFirstShebangWithWhitespaceOnly, null
//            )
//
//            // TODO 仓颉包中必须有包名，但单文件可无
//        }
//
//        parseImportDirectives()
//    }


    /**
     * 解析导入指令列表
     *
     * Grammar:
     * ```
     * importList
     *   : importDirective*
     *   ;
     * ```
     */
    context(parseContext: ParsingContext)
    private fun parseImportDirectives() {
        val importList = mark()

//        if (!(at(IMPORT_KEYWORD) || atSet(IMPORT_ACCESS_MODIFIER_SET) && lookahead(1) == IMPORT_KEYWORD)) {
//            // 允许注释绑定到首个声明
//            importList.setCustomEdgeTokenBinders(DoNotBindAnything, null)
//        }

        while (at(IMPORT_KEYWORD) || atSet(IMPORT_ACCESS_MODIFIER_SET) && lookahead(1) == IMPORT_KEYWORD || isWhenAnnotation()) {
            if (!parseImportDirective()) {
                break
            }
        }

        importList.done(IMPORT_LIST)
    }

    /**
     * 提供 `isWhenAnnotation` 操作，封装仓颉语法解析节点的访问、构造或判断逻辑。
     */
    fun isWhenAnnotation(): Boolean {
        if (at(AT) && CjBuiltInAnnotation.fromName(
                rawTokenText(builder, 1).toString()
            ) == CjBuiltInAnnotation.WHEN
        ) {
            return true
        }
        return false
    }

    /**
     * 解析 @When 条件编译注解
     *
     * @When 注解用于条件编译,可以出现在 import 语句之前或其他声明之前。
     *
     * Grammar:
     * ```
     * whenAnnotation
     *   : "@" "When" "[" expression "]"
     *   ;
     * ```
     *
     * 示例:
     * ```cangjie
     * @When[DEBUG]
     * import some.debug.Module
     * ```
     */
    context(parseContext: ParsingContext)
    private fun parseWhenAnnotation(


        isCreateAnnotations: Boolean = false

    ) {
        assert(at(AT))

        val annotationMark = mark()


        val mark = mark()
        advance() // 消耗 @

        // 解析 When 标识符
        if (!at(IDENTIFIER)) {
            error(CangJieParsingBundle.message("parsing.error.expecting.identifier"))
            mark.done(ANNOTATION)
            return
        }

        val annotationName = builder.tokenText
        if (annotationName != "When") {
            error(CangJieParsingBundle.message("parsing.error.expecting.annotation", "When"))
            mark.done(ANNOTATION)
            return
        }

        parseConstructorCallee()// 消耗 When


        expect(LBRACKET, CangJieParsingBundle.message("parsing.error.expecting.symbol", "["))

        // 使用专用节点类型封装 When 条件
        val whenConditionMark = mark()
        expressionParsing.parseExpression()
        whenConditionMark.done(ANNOTATION_WHEN_CONDITION)

        expect(RBRACKET, CangJieParsingBundle.message("parsing.error.expecting.symbol", "]"))


//        if (at(LBRACKET)) {
//            expressionParsing.parseValueArgumentList(LBRACKET, RBRACKET)
//        }

        mark.done(ANNOTATION)

        if (isCreateAnnotations) {
            annotationMark.done(ANNOTATIONS)
        } else {
            annotationMark.drop()
        }
    }

    /**
     * 执行 `parseAttributeAnnotation` 内部辅助逻辑，支撑仓颉语法解析节点的结构解析与访问。
     */
    context(parseContext: ParsingContext)
    private fun parseAttributeAnnotation(


        isCreateAnnotations: Boolean = false

    ) {
        assert(at(AT))

        val annotationMark = mark()


        val mark = mark()
        advance() // 消耗 @

        // 解析 Attribute 标识符
        if (!at(IDENTIFIER)) {
            error(CangJieParsingBundle.message("parsing.error.expecting.identifier"))
            mark.done(ANNOTATION)
            return
        }

        val annotationName = builder.tokenText
        if (annotationName != "Attribute") {
            error(CangJieParsingBundle.message("parsing.error.expecting.annotation", "Attribute"))
            mark.done(ANNOTATION)
            return
        }

        parseConstructorCallee()// 消耗 Attribute


        expect(LBRACKET, CangJieParsingBundle.message("parsing.error.expecting.symbol", "["))

        val attr = mark()


        // 处理标识符或字符串，使用逗号分隔，允许多个连续逗号
        // 例如: @Attribute[State, aaa,,,,,,sdfsd,,,,,,,ssss]
        builder.disableNewlines()

        while (true) {
            // 跳过多余的逗号
            while (at(COMMA)) {
                advance()
            }

            // 如果到达右括号，结束解析
            if (at(RBRACKET)) {
                break
            }

            // 解析标识符或字符串
            if (at(IDENTIFIER)) {
                advance()
            } else if (at(OPEN_QUOTE)) {
                with(parseContext.copy(processStringInterpolation = false)) {
                    expressionParsing.parseStringTemplate()
                }

            } else {
                errorAndAdvance(CangJieParsingBundle.message("parsing.error.expecting.identifier.or.string"))

            }

            // 跳过逗号
            while (at(COMMA)) {
                advance()
            }

            // 如果到达右括号，结束解析
            if (at(RBRACKET)) {
                break
            }
            if (eof()) {
                break
            }
        }

        builder.enableNewlines()
        attr.done(ANNTATION_ATTR_ATTRIBUTE)
        expect(RBRACKET, CangJieParsingBundle.message("parsing.error.expecting.symbol", "]"))

        mark.done(ANNOTATION)

        if (isCreateAnnotations) {
            annotationMark.done(ANNOTATIONS)
        } else {
            annotationMark.drop()
        }
    }

    /**
     * 关闭导入指令并处理换行错误
     *
     * @param importDirective 导入指令标记
     * @param importAlias 导入别名标记
     * @param errorMessage 错误消息
     * @return 是否处理了错误
     */
    context(parseContext: ParsingContext)
    private fun closeImportWithErrorIfNewline(
        importDirective: PsiBuilder.Marker?, importAlias: PsiBuilder.Marker?, errorMessage: String
    ): Boolean {
        if (builder.newlineBeforeCurrentToken()) {
            importAlias?.done(IMPORT_ALIAS)
            error(errorMessage)
            importDirective?.done(IMPORT_DIRECTIVE)
            return true
        }
        return false
    }

    /**
     * 解析导入指令项
     *
     * Grammar:
     * ```
     * importDirectiveItem
     *   : qualifiedName ("as" simpleName)?
     *   | qualifiedName "." "*"
     *   ;
     * ```
     *
     * @param isTopLevel 是否为顶层导入
     * @return 是否成功解析
     */

    /**
     * 解析单个导入项
     *
     * 支持:
     * - a.b.c
     * - a.b.*
     * - a.b as AB
     */
    /** 组织分隔符只允许出现在最外层首段后，包路径始终是独立表达式。 */
    context(parseContext: ParsingContext)
    private fun parseImportPath(allowOrganization: Boolean): Boolean {
        if (allowOrganization && at(IDENTIFIER) && lookahead(1) == DOUBLE_COLON) {
            val organization = mark()
            advance()
            organization.done(REFERENCE_EXPRESSION)
            advance() // ::
            if (at(LBRACE)) return true
        }
        if (!at(IDENTIFIER)) {
            error("Expecting qualified name")
            return false
        }
        var qualified = mark()
        var reference = mark()
        advance()
        reference.done(REFERENCE_EXPRESSION)
        while (at(DOT) && lookahead(1) != MUL && lookahead(1) != LBRACE) {
            advance()
            reference = mark()
            val hasName = !atImportDeclarationBoundary() && at(IDENTIFIER)
            if (hasName) {
                advance()
                reference.done(REFERENCE_EXPRESSION)
            } else {
                reference.drop()
                error("Expecting identifier")
            }
            val preceding = qualified.precede()
            qualified.done(DOT_QUALIFIED_EXPRESSION)
            qualified = preceding
            if (!hasName) break
        }
        qualified.drop()
        return false
    }

    context(parseContext: ParsingContext)
    private fun parseImportItemSuffix() {
        if (at(DOT) && lookahead(1) == MUL) {
            advance()
            advance()
            if (at(AS_KEYWORD)) {
                val invalidAlias = mark()
                advance()
                if (at(IDENTIFIER)) advance()
                invalidAlias.error(CangJieParsingBundle.message("parsing.error.aliases.not.allowed.for.all.imports"))
            }
        } else if (at(AS_KEYWORD)) {
            val alias = mark()
            advance()
            if (!atImportDeclarationBoundary() && at(IDENTIFIER)) advance() else error("Expecting identifier")
            alias.done(IMPORT_ALIAS)
        }
    }

    /** 跳过非法项尾部时配平非法嵌套花括号，不消耗外层组的结束符。 */
    context(parseContext: ParsingContext)
    private fun recoverImportItem() {
        val invalid = mark()
        var depth = 0
        while (!eof()) {
            if (depth == 0 && (at(COMMA) || at(RBRACE) || at(SEMICOLON) || builder.newlineBeforeCurrentToken())) break
            if (at(LBRACE)) depth++
            if (at(RBRACE)) depth--
            advance()
        }
        invalid.error("Unexpected token in import item")
    }

    context(parseContext: ParsingContext)
    private fun parseImportItem() {
        val item = mark()
        parseImportPath(allowOrganization = false)
        parseImportItemSuffix()
        if (!eof() && !at(COMMA) && !at(RBRACE) && !at(SEMICOLON) && !builder.newlineBeforeCurrentToken()) {
            recoverImportItem()
        }
        item.done(IMPORT_ITEM)
    }

    /** 在不重映射软关键字的前提下识别下一条声明，保留合法的软关键字路径。 */
    context(parseContext: ParsingContext)
    private fun atImportDeclarationBoundary(): Boolean {
        if (!builder.newlineBeforeCurrentToken()) return false
        fun startsDeclaration(token: IElementType?): Boolean =
            token == PACKAGE_KEYWORD || token == IMPORT_KEYWORD ||
                (token as? org.cangnova.cangjie.lexer.CjToken)?.tokenId in declarationParsers
        // 软关键字可能尚未被 lexer 重映射。沿用 modifier parser 的 atSet 识别协议，
        // 在事务式前瞻结束后回到原位，不能只检查 lookAhead 返回的原始 IDENTIFIER。
        val preview = mark()
        return try {
            while (!startsDeclaration(tt()) && (atSet(MODIFIER_KEYWORDS) || atSet(SPECIAL_MODIFIER_KEYWORDS))) advance()
            startsDeclaration(tt())
        } finally {
            preview.rollbackTo()
        }
    }

    /** 空组非法；逗号后遇到右括号是尾逗号，不创建空导入项。 */
    context(parseContext: ParsingContext)
    private fun parseImportItemList() {
        if (at(RBRACE)) {
            error("Expecting import item")
            return
        }
        while (!eof() && !at(RBRACE) && !at(SEMICOLON)) {
            if (at(COMMA)) {
                errorAndAdvance("Expecting import item")
                continue
            }
            // 保留下一条顶层声明，缺失右括号由调用方报告。
            if (builder.newlineBeforeCurrentToken()) {
                if (atImportDeclarationBoundary() || !at(IDENTIFIER)) break
            }
            val position = builder.currentOffset
            parseImportItem()
            if (at(COMMA)) {
                advance()
                if (at(RBRACE)) break
            } else {
                if (!at(RBRACE) && !eof()) error("Expecting ',' or '}'")
                break
            }
            if (builder.currentOffset == position) break
        }
    }

    /** 语句体只包含一个单项或一个真正的花括号分组。 */
    context(parseContext: ParsingContext)
    private fun parseImportDirective(): Boolean {
        assert(_at(IMPORT_KEYWORD) || _atSet(IMPORT_ACCESS_MODIFIER_SET) || isWhenAnnotation())
        val directive = mark()
        if (isWhenAnnotation()) parseWhenAnnotation(true)
        if (_atSet(IMPORT_ACCESS_MODIFIER_SET)) advance()
        if (!at(IMPORT_KEYWORD)) {
            error(CangJieParsingBundle.message("parsing.error.expecting.keyword", "import"))
            directive.rollbackTo()
            return false
        }
        advance()
        if (closeImportWithErrorIfNewline(directive, null, "Expecting qualified name")) return true

        val body = mark()
        val unprefixedGroup = at(LBRACE)
        val organizationOnlyGroup = !unprefixedGroup && parseImportPath(allowOrganization = true)
        val grouped = unprefixedGroup || organizationOnlyGroup || (at(DOT) && lookahead(1) == LBRACE)
        if (grouped) {
            if (at(DOT)) advance()
            advance() // {
            parseImportItemList()
            expect(RBRACE, "Expecting '}'")
            if (at(AS_KEYWORD)) {
                val alias = mark()
                advance()
                if (at(IDENTIFIER)) advance()
                alias.error("Aliases are only allowed for individual imports")
            }
            body.done(IMPORT_GROUP)
        } else {
            parseImportItemSuffix()
            if (!eof() && !at(SEMICOLON) && !builder.newlineBeforeCurrentToken()) recoverImportItem()
            body.done(IMPORT_ITEM)
        }
        consumeIf(SEMICOLON)
        directive.done(IMPORT_DIRECTIVE)
        directive.setCustomEdgeTokenBinders(null, TrailingCommentsBinder)
        return true
    }




    /**
     * 提供 `parseOnlyAnnotationFile` 操作，封装仓颉语法解析节点的访问、构造或判断逻辑。
     */
    fun parseOnlyAnnotationFile() {

        with(ParsingContext.ANNOTATION_ONLY.copy(statementScope = CjScopeKind.TOPLEVEL)) {
            val fileMarker = mark()
            val fileErrorStart = mark()
            val fileErrorEnd = mark()

            // 处理开头 package
            parsePreamble()

            // 处理声明式语句
            while (!eof()) {
                parseTopLevelDeclaration()
            }

            checkUnclosedBlockComment()
            finishCjmpFileError(fileErrorStart, fileErrorEnd)
            fileMarker.done(CJ_FILE)
        }

    }

    /**
     * 解析官方宏展开后的声明片段。
     *
     * 官方 `ReplaceEachMacro` 在重新解析宏返回 token 时启用
     * `EnableCustomAnno`：展开结果中的 `@!Name` 必须进入 custom annotation
     * 集合，而不能再次被当成待执行的声明宏。这个入口与普通源码文件解析
     * 保持同一声明语法，只改变该次重解析的 annotation 分派模式。
     */
    fun parseMacroExpansionFile() {
        with(
            ParsingContext.DEFAULT.copy(
                disableMacroParsing = true,
                enableCustomAnnotation = true,
                statementScope = CjScopeKind.TOPLEVEL,
            )
        ) {
            val fileMarker = mark()

            val fileErrorStart = mark()
            val fileErrorEnd = mark()
            parsePreamble()
            while (!eof()) {
                parseTopLevelDeclaration()
            }

            checkUnclosedBlockComment()
            finishCjmpFileError(fileErrorStart, fileErrorEnd)
            fileMarker.done(CJ_FILE)
        }
    }

    /**
     * 解析仓颉源文件
     *
     * 文件解析的主入口方法。解析完整的仓颉源文件，包括包声明、导入语句和顶级声明。
     *
     * Grammar:
     * ```
     * cangJieFile
     *   : preamble topLevelDeclaration*
     *   ;
     * ```
     */
    fun parseFile() {

        with(ParsingContext.DEFAULT.copy(statementScope = CjScopeKind.TOPLEVEL)) {
            val fileMarker = mark()
            val fileErrorStart = mark()
            val fileErrorEnd = mark()

            // 处理开头 package
            parsePreamble()

            // 处理声明式语句
            while (!eof()) {
                parseTopLevelDeclaration()
            }

            checkUnclosedBlockComment()
            finishCjmpFileError(fileErrorStart, fileErrorEnd)
            fileMarker.done(CJ_FILE)
        }

    }


    /** extend 仅在单个角色修饰符且启用相应编译模式时参与文件角色检查。 */
    internal fun cjmpExtendRoleCheckApplies(detector: ModifierDetector): Boolean {
        if (detector.count != 1 || !(detector.isCommonDetected || detector.isSpecificDetected)) return false
        if (detector.parsedModifierOccurrences.any { it.token !in setOf(COMMON_KEYWORD, SPECIFIC_KEYWORD, PLATFORM_KEYWORD) }) return false
        // 官方集合按原始 token 去重；platform 和 specific 不能先归一为同一成员。
        if (detector.parsedModifierOccurrences.map { it.token }.distinct().size > 1) return false
        val context = cjmpContext
        return context !is CjmpParsingContext.Compilation || (context.compileCommon || context.compileSpecific)
    }

    /** 文件角色检查保留首个错误；普通声明失败时清空有效修饰符。 */
    internal fun checkCjmpFileRole(detector: ModifierDetector, isExtend: Boolean = false, anchor: Int = builder.currentOffset): Boolean {
        if (fileState.quotedSyntaxDepth > 0) return true
        if (!detector.hasParsedModifiers) return true
        val tokens = detector.rawModifierKinds
        val roleModifiers = CjmpRoleModifiers(
            CjModifierKind.COMMON in tokens, CjModifierKind.SPECIFIC in tokens, CjModifierKind.PLATFORM in tokens,
        )
        val event = when {
            isExtend -> CjmpRoleEventKind.Extend(roleModifiers, tokens.size)
            else -> CjmpRoleEventKind.Check(roleModifiers)
        }
        builder.macroParsingJournal.registerRoleEvent(anchor, event)
        if (isExtend && !cjmpExtendRoleCheckApplies(detector)) return true
        val mode = when (val context = cjmpContext) {
            CjmpParsingContext.SyntaxOnly -> CjmpFileRoleMode.SyntaxOnly
            is CjmpParsingContext.Compilation -> CjmpFileRoleMode.Compilation(
                context.enabled, context.compileCommon, context.compileSpecific, context.mixedCommonSpecificDeclarations,
            )
        }
        val result = CjmpFileRoleTransition.check(
            mode, fileState.role, CjmpDeclarationRole(detector.isCommonDetected, detector.isSpecificDetected),
        )
        val error = result.error?.let {
            CjPendingFileRoleDiagnostic(it, builder.macroParsingJournal.nextDiagnosticOrder())
        }
        builder.macroParsingJournal.updateFileState(fileState.copy(
            role = result.role,
            error = fileState.error ?: error,
        ))
        if (error != null) {
            // extend 的唯一角色修饰符即使检查失败也保留为声明属性。
            if (!isExtend) detector.clear()
            return false
        }
        return true
    }

    /** 记录该声明是否实际经过文件角色检查，而不是推断声明的最终角色。 */
    internal fun cjmpDeclarationRoleCheck(accepted: Boolean, hasModifiers: Boolean, checked: Boolean = true): CjmpDeclarationRoleCheck = when {
        fileState.quotedSyntaxDepth > 0 -> CjmpDeclarationRoleCheck.NOT_CHECKED
        cjmpContext !is CjmpParsingContext.Compilation || !cjmpContext.enabled -> CjmpDeclarationRoleCheck.NOT_CHECKED
        !hasModifiers -> CjmpDeclarationRoleCheck.NOT_CHECKED
        !checked -> CjmpDeclarationRoleCheck.NOT_CHECKED
        accepted -> CjmpDeclarationRoleCheck.ACCEPTED
        else -> CjmpDeclarationRoleCheck.REJECTED
    }

    /**
     * 每个真实声明文法在官方规定的时点调用一次；锚点在消费关键字前保存。
     * journal 只保存结果及事件，包装层不通过声明结束位置或最终属性重新推导检查。
     */
    context(parseContext: ParsingContext)
    internal fun checkCjmpDeclarationFileRole(
        detector: ModifierDetector?,
        declarationType: IElementType,
        anchor: Int,
    ): CjmpDeclarationRoleCheck {
        val modifiers = detector ?: ModifierDetector()
        modifiers.declarationAnchor = anchor
        builder.macroParsingJournal.registerDeclarationPrefix(anchor, modifiers.declarationPrefix())
        val hasModifiers = modifiers.hasParsedModifiers
        val isExtend = declarationType == EXTEND
        val scope = parseContext.statementScope
        val hasRules = isExtend || CjmpModifierSyntaxRules.hasFileRoleCheckRules(
            cjmpModifierDeclarationKind(declarationType), scope,
        )
        val checkApplies = hasRules && (!isExtend || cjmpExtendRoleCheckApplies(modifiers))
        val accepted = !hasRules || checkCjmpFileRole(modifiers, isExtend, anchor)
        // 已知作用域的缺表/空表返回空属性集；只有 UNKNOWN_SCOPE 直接保留输入属性。
        if ((!hasRules && scope != CjScopeKind.UNKNOWN_SCOPE) || isExtend && !checkApplies) modifiers.clear()
        detector?.cjmpFileRoleAccepted = accepted
        checkDeclarationModifierSyntax(modifiers, declarationType, accepted)
        val result = cjmpDeclarationRoleCheck(accepted, hasModifiers, checkApplies)
        builder.macroParsingJournal.registerDeclarationRoleCheck(anchor, result)
        modifiers.restorePredefinedAttributes()
        builder.macroParsingJournal.registerDeclarationModifiers(anchor,
            modifiers.freezeDeclarationModifiers(builder.macroParsingJournal.rootIdentity, cjmpContext,
                builder.macroParsingJournal.sourceTokenAt(anchor), cjmpModifierDeclarationKind(declarationType), scope))
        return result
    }

    /** 四项 common/specific 声明规则只属于启用中的编译调用，不由 PSI 文件角色推测。 */
    private fun cjmpModifierRulesEnabled(): Boolean {
        if (fileState.quotedSyntaxDepth > 0) return false
        val context = cjmpContext as? CjmpParsingContext.Compilation ?: return false
        return context.enabled && (context.compileCommon || context.compileSpecific)
    }

    private fun cjmpPlatformModifierSyntaxAvailable(): Boolean =
        (cjmpContext as? CjmpParsingContext.Compilation)?.platformModifierSyntaxAvailable == true

    private fun cjmpPrimaryConstructorRoleModifiersAvailable(): Boolean =
        (cjmpContext as? CjmpParsingContext.Compilation)?.primaryConstructorRoleModifiersAvailable == true

    /** alpha.65 的 generic/default 语法规则只在实际 CJMP 编译调用中生效。 */
    private fun cjmpGenericDeclarationsAvailable(): Boolean =
        (cjmpContext as? CjmpParsingContext.Compilation)?.genericDeclarationsAvailable != false

    private fun cjmpParameterDefaultValueRulesAvailable(): Boolean =
        (cjmpContext as? CjmpParsingContext.Compilation)?.parameterDefaultValueRulesAvailable != false

    /** 解析声明自己的类型参数表，并在历史版本禁止泛型时由共享 parser 产生语法错误。 */
    context(parseContext: ParsingContext)
    private fun parseCjmpTypeParameterList(
        recoverySet: TokenSet,
        detector: ModifierDetector?,
        cjmpParameterFacts: CjmpParameterRuleFacts? = null,
    ): Boolean {
        if (!at(LT)) return false
        val startOffset = builder.currentOffset
        parseTypeParameterList(recoverySet)
        val hasSupportedCjmpRole = detector?.isCommonDetected == true ||
            detector?.isSpecificDetected == true &&
            (detector.isPlatformDetected || cjmpModifierSyntaxAvailable())
        if (parseContext.shouldReportError && cjmpModifierRulesEnabled() &&
            !cjmpGenericDeclarationsAvailable() &&
            hasSupportedCjmpRole
        ) {
            cjmpParameterFacts?.genericDeclarationUnsupported = true
            val declarationDetector = checkNotNull(detector)
            declarationDetector.cjmpGenericDeclarationUnsupported = true
            declarationDetector.cjmpRulesShortCircuited = true
            declarationDetector.cjmpGenericDeclarationStartOffset = declarationDetector.parsedModifierOccurrences
                .filter { it.token == COMMON_KEYWORD || it.token == PLATFORM_KEYWORD || it.token == SPECIFIC_KEYWORD }
                .minOfOrNull(CjmpModifierOccurrence::startOffset) ?: startOffset
        }
        return true
    }

    /** alpha.65 官方把 parse_cjmp_generic_decl 锚在完整声明节点，待其 body/签名结束后发布范围。 */
    context(parseContext: ParsingContext)
    private fun reportUnsupportedGenericDeclaration(detector: ModifierDetector?) {
        if (detector?.cjmpGenericDeclarationUnsupported != true ||
            !(detector.isCommonDetected || detector.isSpecificDetected) ||
            !parseContext.shouldReportError
        ) return
        val role = when {
            detector.isCommonDetected -> COMMON_KEYWORD.value
            detector.isPlatformDetected -> PLATFORM_KEYWORD.value
            else -> SPECIFIC_KEYWORD.value
        }
        errorOnRange(
            checkNotNull(detector.cjmpGenericDeclarationStartOffset),
            lastSignificantTokenEndOffset(),
            CangJieParsingBundle.message("parsing.error.cjmp.generic.declaration.unsupported", role),
        )
    }

    /** 有独立历史语法规则的 callable 参数信息；语义 matcher 不从 parser detector 反查角色。 */
    private fun cjmpCallableParameterFacts(
        detector: ModifierDetector?,
        declarationType: IElementType,
    ): CjmpParameterRuleFacts? {
        val isCommon = detector?.isCommonDetected == true
        val isSpecific = detector?.isSpecificDetected == true
        val isSupportedSpecificRole = isSpecific && (detector?.isPlatformDetected == true || cjmpModifierSyntaxAvailable())
        if (!isCommon && !isSupportedSpecificRole) return null
        require(declarationType == FUNC || declarationType == SECONDARY_CONSTRUCTOR)
        val declarationKind = if (declarationType == FUNC) {
            CangJieParsingBundle.message("parsing.scope.func")
        } else {
            CangJieParsingBundle.message("parsing.scope.constructor")
        }
        return CjmpParameterRuleFacts(
            declarationKind = declarationKind,
            isCommon = isCommon,
            isSpecific = isSupportedSpecificRole,
            enabled = false,
            rejectSpecificDefaultValues = isSupportedSpecificRole && cjmpModifierRulesEnabled() &&
                !(declarationType == SECONDARY_CONSTRUCTOR && detector?.isStaticDetected == true) &&
                !cjmpParameterDefaultValueRulesAvailable(),
            usesPlatformRole = detector?.isPlatformDetected == true,
        )
    }

    /** 官方 CJMP callable 检查仅处理单参数列表；generic 失败会短路该声明的其余 CJMP 检查。 */
    context(parseContext: ParsingContext)
    private fun reportUnsupportedSpecificParameterDefaults(
        facts: CjmpParameterRuleFacts?,
        parameterListCount: Int,
    ) {
        if (parameterListCount != 1 || facts?.rejectSpecificDefaultValues != true ||
            facts.genericDeclarationUnsupported || !cjmpModifierRulesEnabled()
        ) return

        for (defaultValue in facts.specificParameterDefaultValues) {
            errorOnRange(
                defaultValue.startOffset,
                defaultValue.endOffset,
                CangJieParsingBundle.message(
                    if (facts.usesPlatformRole) {
                        "parsing.error.cjmp.platform.parameter.default.unsupported"
                    } else {
                        "parsing.error.cjmp.specific.parameter.default.unsupported"
                    },
                    facts.declarationKind,
                ),
            )
        }
    }

    /** 语法 token 是否属于当前版本；不等同于编译调用是否显式启用了该语言特性。 */
    private fun cjmpModifierSyntaxAvailable(): Boolean = when (val context = cjmpContext) {
        CjmpParsingContext.SyntaxOnly -> true
        is CjmpParsingContext.Compilation -> context.modifierSyntaxAvailable
    }

    /** 在真实 CheckDeclModifiers 文法时点完成全部准入、冲突与非错误报告。 */
    context(parseContext: ParsingContext)
    private fun checkDeclarationModifierSyntax(
        detector: ModifierDetector,
        declarationType: IElementType,
        declarationRulesEnabled: Boolean,
    ) {
        if (fileState.quotedSyntaxDepth > 0 || detector.cjmpSkipModifierChecks) return
        val sourceModifiers = detector.sourceModifierOccurrences.sortedWith(compareBy(
            { it.sourceFact.sourceRange.source.nativePosition(it.sourceFact.sourceRange.startOffset).first },
            { it.sourceFact.sourceRange.source.nativePosition(it.sourceFact.sourceRange.startOffset).second },
        ))
        val unsupportedModifiers = sourceModifiers.filter { modifier ->
            when (modifier.token) {
                COMMON_KEYWORD -> !cjmpModifierSyntaxAvailable() && !cjmpPlatformModifierSyntaxAvailable()
                SPECIFIC_KEYWORD -> !cjmpModifierSyntaxAvailable()
                PLATFORM_KEYWORD -> cjmpContext != CjmpParsingContext.SyntaxOnly && !cjmpPlatformModifierSyntaxAvailable()
                else -> false
            }
        }
        if (unsupportedModifiers.isNotEmpty()) {
            for (modifier in unsupportedModifiers) {
                errorOnSourceRange(
                    modifier.sourceFact.origin, modifier.sourceFact.sourceRange,
                    CangJieParsingBundle.message(
                        "parsing.error.cjmp.modifier.unsupported.language.version",
                        modifier.token.value,
                    ),
                )
            }
            return
        }
        // extend 不使用普通修饰符冲突表；原生只接收一个角色 token，其余前缀报首个 token。
        if (declarationType == EXTEND) {
            val raw = detector.rawModifierKinds
            val hasSingleRole = raw.size == 1 && raw.any {
                it == CjModifierKind.COMMON || it == CjModifierKind.SPECIFIC || it == CjModifierKind.PLATFORM
            }
            val hasCompilationRole = (cjmpContext as? CjmpParsingContext.Compilation)?.let {
                it.compileCommon || it.compileSpecific
            } ?: true
            if (raw.isNotEmpty() && !(hasSingleRole && hasCompilationRole)) {
                val first = PARAMETER_MODIFIERS_IN_TOKEN_ORDER.firstNotNullOfOrNull { token ->
                    sourceModifiers.firstOrNull {
                        it.token == token || token == SPECIFIC_KEYWORD && it.token == PLATFORM_KEYWORD
                    }
                }
                val modifier = checkNotNull(first) { "Every raw modifier must retain its actual source occurrence." }
                val anchor = checkNotNull(detector.declarationAnchor)
                errorOnSourceRange(
                    modifier.sourceFact.origin, modifier.sourceFact.sourceRange,
                    CangJieParsingBundle.message("parsing.error.expected.no.modifier.before", "extend declaration", modifier.token.value),
                    hints = listOf(ParsingDiagnosticHint(anchor, anchor + EXTEND_KEYWORD.value.length,
                        CangJieParsingBundle.message("parsing.error.hint.before", "extend declaration"))),
                )
            }
            return
        }
        // libast/宏原子的 UNKNOWN_SCOPE 直接发布属性，不进入目标及冲突规则表。
        if (parseContext.statementScope == CjScopeKind.UNKNOWN_SCOPE ||
            parseContext.statementScope == CjScopeKind.ENUM_CONSTRUCTOR && declarationType != ENUM_CONSTRUCTOR
        ) return
        if (!declarationRulesEnabled) return
        val declaration = cjmpModifierDeclarationKind(declarationType)
        val scope = parseContext.statementScope
        val rules = CjmpModifierSyntaxRules.scopeRules(declaration, scope) ?: return
        if (rules.isEmpty()) {
            sourceModifiers.firstOrNull()?.let { modifier ->
                reportIllegalModifierInScope(modifier, detector, declarationType)
            }
            return
        }
        val checked = CjmpModifierSyntaxRules.check(
            declaration, scope,
            if (cjmpContext == CjmpParsingContext.SyntaxOnly) sourceModifiers.filterNot { it.token == PLATFORM_KEYWORD } else sourceModifiers,
            parseContext.ignoreScopeCheck,
        )
        for (violation in checked.violations) {
            val modifier = violation.modifier
            if (violation is CjmpModifierSyntaxViolation.IllegalTarget) {
                reportIllegalModifierInScope(modifier, detector, declarationType)
                continue
            }
            val message = when (violation) {
                is CjmpModifierSyntaxViolation.IllegalTarget ->
                    kotlin.error("Illegal modifier must be reported with its actual scope")
                is CjmpModifierSyntaxViolation.Conflict -> CangJieParsingBundle.message(
                    "parsing.error.modifier.conflict", modifier.token.value, violation.earlierModifier.token.value,
                    nativeModifierDeclarationDescription(declarationType),
                )
            }
            val hints = if (violation is CjmpModifierSyntaxViolation.Conflict) listOf(
                sourceHint(violation.earlierModifier.sourceFact.origin, violation.earlierModifier.sourceFact.sourceRange,
                    CangJieParsingBundle.message("parsing.error.hint.conflicted.modifier")),
                modifierConflictTarget(detector, declarationType),
            ) else emptyList()
            errorOnSourceRange(modifier.sourceFact.origin, modifier.sourceFact.sourceRange, message, hints)
        }
        detector.setReturnedModifierKinds(checked.returnedKinds)
        for (redundant in CjmpModifierSyntaxRules.redundantModifiers(declaration, scope, checked.validModifiers,
            parseContext.ignoreScopeCheck)) {
            val modifier = redundant.modifier
            val kind = checkNotNull(modifier.token.cjModifierKindOrNull())
            val warning: org.cangnova.cangjie.messages.ParsingWarning.RedundantModifier
            val hint: ParsingDiagnosticHint
            val mainHint: String
            when (val reason = redundant.reason) {
                is CjmpModifierSyntaxRules.Redundancy.Explicit -> {
                    val higher = reason.higher
                    warning = org.cangnova.cangjie.messages.ParsingWarning.RedundantModifier(kind,
                        org.cangnova.cangjie.messages.ParsingWarning.Redundancy.Explicit(checkNotNull(higher.token.cjModifierKindOrNull())))
                    hint = sourceHint(higher.sourceFact.origin, higher.sourceFact.sourceRange)
                    mainHint = "'${higher.token.value}' implies '${modifier.token.value}'"
                }
                CjmpModifierSyntaxRules.Redundancy.InterfaceMember -> {
                    val origin = if (declarationType == INTERFACE) {
                        val anchor = checkNotNull(detector.declarationAnchor)
                        builder.macroParsingJournal.sourceOrigin(anchor, anchor + INTERFACE_KEYWORD.value.length)
                    } else parseContext.memberScope?.takeIf { it.kind == CjMemberParsingContext.Kind.INTERFACE }?.declarationOrigin
                        ?: continue
                    warning = org.cangnova.cangjie.messages.ParsingWarning.RedundantModifier(kind,
                        org.cangnova.cangjie.messages.ParsingWarning.Redundancy.InterfaceMember)
                    hint = sourceHint(origin, origin.range)
                    mainHint = "interface member implies '${modifier.token.value}'"
                }
            }
            warningOnSourceRange(modifier.sourceFact.origin, modifier.sourceFact.sourceRange, warning,
                "redundant modifier: '${modifier.token.value}'", mainHint, listOf(hint),
                inheritedCarrier = detector.declarationAnchor)
        }
    }

    private fun nativeModifierDeclarationDescription(type: IElementType): String = when (type) {
        CLASS -> "class declaration"
        STRUCT -> "struct declaration"
        INTERFACE -> "interface declaration"
        ENUM -> "enum declaration"
        FUNC -> "function declaration"
        PROPERTY -> "property declaration"
        FIELD, VARIABLE -> "variable declaration"
        SECONDARY_CONSTRUCTOR -> "constructor"
        PRIMARY_CONSTRUCTOR -> "primary constructor"
        FINALIZER -> "finalizer"
        TYPEALIAS -> "type alias declaration"
        MAIN_FUNC -> "main function"
        MACRO -> "macro declaration"
        PACKAGE_DIRECTIVE -> "package declaration"
        else -> error("No native modifier declaration kind for $type")
    }

    /** DiagIllegalModifierInScope 的 owner 与 scope 是两条独立 hint，不能只输出泛化目标错误。 */
    context(parseContext: ParsingContext)
    private fun reportIllegalModifierInScope(modifier: CjmpModifierOccurrence, detector: ModifierDetector, type: IElementType,
        declarationBegin: Int = checkNotNull(detector.declarationAnchor)) {
        val description = nativeModifierDeclarationDescription(type)
        val anchor = declarationBegin
        val width = when (type) {
            CLASS -> 5; STRUCT -> 6; INTERFACE -> 9; ENUM -> 4
            FUNC, SECONDARY_CONSTRUCTOR, FINALIZER -> 4
            PROPERTY -> 4; FIELD, VARIABLE -> 3
            TYPEALIAS -> 4; MAIN_FUNC -> 4; MACRO -> 5; PACKAGE_DIRECTIVE -> 7
            PRIMARY_CONSTRUCTOR -> checkNotNull(detector.cjmpDeclarationName).length
            else -> kotlin.error("No native modifier hint range for $type")
        }
        val owner = parseContext.diagnosticScopeOwner
        val needsScope = type in setOf(FUNC, SECONDARY_CONSTRUCTOR, FINALIZER, FIELD, VARIABLE, PROPERTY)
        val scopeName = owner?.kind?.nameText ?: "'top-level' scope"
        val scopeBody = owner?.kind?.bodyText ?: "'top-level' scope"
        val hints = buildList {
            add(ParsingDiagnosticHint(anchor, anchor + width,
                CangJieParsingBundle.message("parsing.error.hint.on", description + if (needsScope && owner == null) " in $scopeName" else "")))
            if (owner != null) add(sourceHint(owner.origin, owner.origin.range,
                CangJieParsingBundle.message("parsing.error.hint.in", scopeName)))
        }
        errorOnSourceRange(modifier.sourceFact.origin, modifier.sourceFact.sourceRange,
            CangJieParsingBundle.message("parsing.error.modifier.in.scope", modifier.token.value, description,
                if (needsScope) " in $scopeBody" else ""), hints)
    }

    /** DiagConflictedModifier 的第二个 hint 由真实声明 owner 提供，不跟随被诊断 modifier 的文件。 */
    private fun modifierConflictTarget(detector: ModifierDetector, type: IElementType): ParsingDiagnosticHint {
        val anchor = checkNotNull(detector.declarationAnchor)
        val (width, label) = when (type) {
            CLASS -> CLASS_KEYWORD.value.length to "class declaration"
            STRUCT -> STRUCT_KEYWORD.value.length to "struct declaration"
            INTERFACE -> INTERFACE_KEYWORD.value.length to "interface declaration"
            ENUM -> ENUM_KEYWORD.value.length to "enum declaration"
            FUNC -> FUNC_KEYWORD.value.length to "function declaration"
            PROPERTY -> PROP_KEYWORD.value.length to "property declaration"
            // 官方 secondary 是 FuncDecl；PRIMARY_CTOR_DECL 才使用专有 constructor hint。
            SECONDARY_CONSTRUCTOR -> INIT_KEYWORD.value.length to "function declaration"
            PRIMARY_CONSTRUCTOR -> checkNotNull(detector.cjmpDeclarationName).length to "constructor"
            FIELD, VARIABLE -> {
                val keyword = checkNotNull(detector.declarationKeyword)
                if (keyword == CONST_KEYWORD) {
                    val range = checkNotNull(detector.declarationNameRange)
                    return ParsingDiagnosticHint(range.first, range.second,
                        CangJieParsingBundle.message("parsing.error.hint.on", "variable declaration"))
                }
                keyword.value.length to "variable declaration"
            }
            else -> error("No native conflict target for declaration $type")
        }
        return ParsingDiagnosticHint(anchor, anchor + width, CangJieParsingBundle.message("parsing.error.hint.on", label))
    }

    private fun cjmpModifierDeclarationKind(declarationType: IElementType): CjmpModifierDeclarationKind = when (declarationType) {
        CLASS -> CjmpModifierDeclarationKind.CLASS
        STRUCT -> CjmpModifierDeclarationKind.STRUCT
        ENUM -> CjmpModifierDeclarationKind.ENUM
        INTERFACE -> CjmpModifierDeclarationKind.INTERFACE
        EXTEND -> CjmpModifierDeclarationKind.EXTEND
        FUNC -> CjmpModifierDeclarationKind.FUNCTION
        FIELD, VARIABLE -> CjmpModifierDeclarationKind.VARIABLE
        PROPERTY -> CjmpModifierDeclarationKind.PROPERTY
        PRIMARY_CONSTRUCTOR -> CjmpModifierDeclarationKind.PRIMARY_CONSTRUCTOR
        SECONDARY_CONSTRUCTOR -> CjmpModifierDeclarationKind.SECONDARY_CONSTRUCTOR
        ENUM_CONSTRUCTOR -> CjmpModifierDeclarationKind.ENUM_CONSTRUCTOR
        FINALIZER -> CjmpModifierDeclarationKind.FINALIZER
        MAIN_FUNC -> CjmpModifierDeclarationKind.MAIN
        TYPEALIAS -> CjmpModifierDeclarationKind.TYPE_ALIAS
        MACRO -> CjmpModifierDeclarationKind.MACRO
        PACKAGE_DIRECTIVE -> CjmpModifierDeclarationKind.PACKAGE
        else -> CjmpModifierDeclarationKind.OTHER
    }

    /** 参数角色比较有独立的历史版本区间，不能借用成员或混合文件 feature gate。 */
    private fun cjmpParameterRoleMatchingEnabled(): Boolean =
        cjmpModifierRulesEnabled() &&
            (cjmpContext as? CjmpParsingContext.Compilation)?.parameterRoleMatching == true

    /** 显式 abstract 是语言特性规则；SyntaxOnly 使用 parser 默认语言版本。 */
    private fun cjmpAbstractRuleEnabled(): Boolean =
        fileState.quotedSyntaxDepth == 0 && when (val context = cjmpContext) {
            CjmpParsingContext.SyntaxOnly -> true
            is CjmpParsingContext.Compilation -> context.enabled
        }

    /** 返回当前语法位置之前最后一个非 trivia token 的结束偏移。 */
    private fun lastSignificantTokenEndOffset(): Int {
        var rawIndex = -1
        while (CjTokens.WHITE_SPACE_OR_COMMENT_BIT_SET.contains(builder.rawLookup(rawIndex))) rawIndex--
        return builder.rawTokenTypeStart(rawIndex + 1)
    }

    /** 当前 token 之后的下一个逻辑 token 起点，遵循同一 builder 的空白和换行跳过状态。 */
    internal fun nextLogicalTokenStartOffset(): Int {
        var rawIndex = 1
        while (true) {
            val tokenType = builder.rawLookup(rawIndex) ?: return builder.originalText.length
            val skippedWhitespace = CjTokens.WHITE_SPACE_OR_COMMENT_BIT_SET.contains(tokenType)
            val skippedNewline = tokenType === NL && builder.isNLTokenSkippingEnabled
            if (!skippedWhitespace && !skippedNewline) return builder.rawTokenTypeStart(rawIndex)
            rawIndex++
        }
    }

    private fun cjmpNominalKindName(kind: CjMemberParsingContext.Kind): String = when (kind) {
        CjMemberParsingContext.Kind.CLASS -> CangJieParsingBundle.message("parsing.scope.class")
        CjMemberParsingContext.Kind.STRUCT -> CangJieParsingBundle.message("parsing.scope.struct")
        CjMemberParsingContext.Kind.INTERFACE -> CangJieParsingBundle.message("parsing.scope.interface")
        CjMemberParsingContext.Kind.ENUM -> CangJieParsingBundle.message("parsing.scope.enum")
        CjMemberParsingContext.Kind.EXTEND -> CangJieParsingBundle.message("parsing.scope.extend")
    }

    private fun cjmpMemberKindName(declarationType: IElementType): String? = when (declarationType) {
        CLASS -> CangJieParsingBundle.message("parsing.scope.class")
        STRUCT -> CangJieParsingBundle.message("parsing.scope.struct")
        INTERFACE -> CangJieParsingBundle.message("parsing.scope.interface")
        ENUM -> CangJieParsingBundle.message("parsing.scope.enum")
        EXTEND -> CangJieParsingBundle.message("parsing.scope.extend")
        FUNC -> CangJieParsingBundle.message("parsing.scope.func")
        PROPERTY -> CangJieParsingBundle.message("parsing.scope.property")
        FIELD, VARIABLE -> CangJieParsingBundle.message("parsing.scope.variable")
        PRIMARY_CONSTRUCTOR -> CangJieParsingBundle.message("parsing.scope.primary.constructor")
        SECONDARY_CONSTRUCTOR -> CangJieParsingBundle.message("parsing.scope.constructor")
        FINALIZER -> CangJieParsingBundle.message("parsing.scope.func")
        else -> null
    }

    private fun cjmpMemberDescription(member: CjmpDirectMemberFacts): String? {
        val kind = cjmpMemberKindName(member.declarationType) ?: return null
        val name = member.declarationName?.let(CjPsiUtil::unquoteIdentifier).orEmpty()
        return if (name.isEmpty()) kind else "$kind $name"
    }

    /** 对位官方 CheckCJMPModifiersBetween；alpha.20 起 common 可落在 specific 外层。 */
    context(parseContext: ParsingContext)
    private fun checkCjmpMemberMismatch(
        member: CjmpDirectMemberFacts,
        outerKind: CjMemberParsingContext.Kind,
        outerIsCommon: Boolean,
        outerIsSpecific: Boolean,
    ): Boolean {
        val description = cjmpMemberDescription(member) ?: return false
        return reportCjmpRoleMismatch(
            memberDescription = description,
            memberIsCommon = member.isCommon,
            memberIsSpecific = member.isSpecific,
            outerKindName = cjmpNominalKindName(outerKind),
            outerIsCommon = outerIsCommon,
            outerIsSpecific = outerIsSpecific,
            allowCommonMemberInSpecificOuter =
                (cjmpContext as? CjmpParsingContext.Compilation)?.commonMemberInSpecificOuter == true,
            startOffset = member.startOffset,
            endOffset = member.endOffset,
        )
    }

    /** 声明及其主构造器成员参数共用官方 common/specific 双谓词与完整源码锚点。 */
    context(parseContext: ParsingContext)
    private fun reportCjmpRoleMismatch(
        memberDescription: String,
        memberIsCommon: Boolean,
        memberIsSpecific: Boolean,
        outerKindName: String,
        outerIsCommon: Boolean,
        outerIsSpecific: Boolean,
        allowCommonMemberInSpecificOuter: Boolean = false,
        startOffset: Int,
        endOffset: Int,
    ): Boolean {
        val memberRole = when {
            memberIsCommon && !outerIsCommon &&
                !(allowCommonMemberInSpecificOuter && outerIsSpecific) -> COMMON_KEYWORD.value
            memberIsSpecific && !outerIsSpecific -> SPECIFIC_KEYWORD.value
            else -> return false
        }
        errorOnRange(
            startOffset,
            endOffset,
            CangJieParsingBundle.message(
                "parsing.error.cjmp.outdecl.mismatch",
                memberDescription,
                memberRole,
                outerKindName,
                memberRole,
            ),
        )
        return true
    }

    /** body 完成后才知道是否允许 specific 实现/ common 构造器检查。 */
    context(parseContext: ParsingContext)
    private fun finishCjmpNominalRules(facts: CjmpNominalParseFacts, nameRange: Pair<Int, Int>?) {
        if (!facts.modifierRulesEnabled || facts.hasModifierMismatch) return
        if (facts.kind == CjMemberParsingContext.Kind.INTERFACE && facts.isSpecific) {
            for (member in facts.directMembers) {
                if (member.hasDefault) continue
                val name = member.declarationName?.let(CjPsiUtil::unquoteIdentifier).orEmpty()
                val interfaceName = facts.declarationName?.let(CjPsiUtil::unquoteIdentifier).orEmpty()
                errorOnRange(
                member.startOffset,
                member.endOffset,
                    CangJieParsingBundle.message(
                        "parsing.error.cjmp.specific.member.implementation", name, interfaceName,
                    ),
                )
            }
        }
        if (facts.isCommon && facts.kind in setOf(CjMemberParsingContext.Kind.CLASS, CjMemberParsingContext.Kind.STRUCT) &&
            !facts.hasDirectConstructor && nameRange != null
        ) {
            val declarationName = facts.declarationName?.let(CjPsiUtil::unquoteIdentifier).orEmpty()
            errorOnRange(
                nameRange.first,
                nameRange.second,
                CangJieParsingBundle.message(
                    "parsing.error.cjmp.common.ctor.required",
                    cjmpNominalKindName(facts.kind),
                    declarationName,
                ),
            )
        }
    }

    /** 延迟完成文件起点的零宽错误，错误节点不包裹 package 或任何声明。 */
    private fun finishCjmpFileError(start: PsiBuilder.Marker, end: PsiBuilder.Marker) {
        val diagnostic = fileState.error
        if (diagnostic == null) {
            start.drop()
        } else {
            start.doneBefore(CjmpFileRoleErrorElementType(diagnostic.roleError, diagnostic.order), end)
            start.setCustomEdgeTokenBinders(WhitespacesBinders.GREEDY_LEFT_BINDER, WhitespacesBinders.GREEDY_LEFT_BINDER)
        }
        end.drop()
    }

    /**
     * 检查未关闭的块注释
     *
     * 检查文件末尾是否有未关闭的块注释，并报告错误。
     */
    context(parseContext: ParsingContext)
    private fun checkUnclosedBlockComment() {
        if (BLOCK_DOC_COMMENT_SET.contains(builder.rawLookup(-1))) {
            val startOffset = builder.rawTokenTypeStart(-1)
            val endOffset = builder.rawTokenTypeStart(0)
            val tokenChars = builder.originalText.subSequence(startOffset, endOffset)

            if (!(tokenChars.length > 2 && tokenChars.subSequence(tokenChars.length - 2, tokenChars.length)
                    .toString() == "*/")
            ) {
                errorOnRange(startOffset, endOffset, CangJieParsingBundle.message("parsing.error.unclosed.comment"),
                    category = ParsingDiagnostic.Category.LEX, rangeKind = ParsingDiagnostic.RangeKind.TOKEN)
            }
        }
    }


    /**
     * 解析顶层声明（无参数版本）
     *
     * 调用带参数的版本，默认不解析宏。
     */
    context(parseContext: ParsingContext)
    private fun parseTopLevelDeclaration() {
        parseTopLevelDeclaration(true)
    }


    /**
     * 解析顶层声明
     *
     * Grammar:
     * ```
     * topLevelDeclaration
     *   : classDeclaration
     *   | interfaceDeclaration
     *   | structDeclaration
     *   | enumDeclaration
     *   | extendDeclaration
     *   | functionDeclaration
     *   | variableDeclaration
     *   | typeAliasDeclaration
     *   | foreignDeclaration
     *   | macroDeclaration
     *   ;
     * ```
     *
     * @param parseMacro 是否解析宏表达式
     */
    context(parseContext: ParsingContext)
    private fun parseTopLevelDeclaration(parseMacro: Boolean) {
        if (at(SEMICOLON)) {
            advance() // SEMICOLON
            return
        }

        val decl = mark()

        // 如果有导入语句
        if (at(PUBLIC_KEYWORD) && lookahead(1) == IMPORT_KEYWORD) {
            // error("imports are only allowed in the beginning of file");
            parseImportDirectives()
            decl.drop()
            return
        }

        // 顶层 `@Macro decl` 由 macro expression 包裹真实声明，PSI 与 LightTree
        // 必须产出同构的 MACRO_EXPRESSION，后续 raw builder 才能建立 construction surface。
        if (parseMacro && !parseContext.disableMacroParsing && isMacroCallStart()) {
            expressionParsing.parseMacroExpression(CjScopeKind.TOPLEVEL, DeclarationParsingMode.TOPLEVEL)
            decl.drop()
            return
        }

        val detector = ModifierDetector()
        parseAnnotations(detector)

        parseModifierList(detector, TokenSet.EMPTY, parseMacro, stopBeforeForeignCollection = true)
        // foreign 集合属于文件 driver；集合内的 TOPLEVEL ParseDecl 不再次分派集合。
        val declType = if (at(FOREIGN_KEYWORD)) parseForeign(detector)
            else parseCommonDeclaration(detector, NameParsingMode.REQUIRED, DeclarationParsingMode.TOPLEVEL)


        if (declType == null) {
            errorAndAdvance(CangJieParsingBundle.message("parsing.error.expecting.element", "a top level declaration"))
            // decl.error("Expecting a top level declaration")
            decl.drop()
        } else {
            closeDeclarationWithCommentBinders(decl, declType, true)
        }
    }


    /**
     * 尝试解析修饰符
     *
     * @param tokenConsumer 修饰符令牌消费者
     * @param noModifiersBefore 禁止修饰符前的令牌集合
     * @param modifierKeywords 修饰符关键字集合
     * @return 是否成功解析修饰符
     */
    context(parseContext: ParsingContext)
    private fun tryParseModifier(
        tokenConsumer: ((IElementType) -> Unit)?,
        noModifiersBefore: TokenSet,
        modifierKeywords: TokenSet,
        parseConstBeforePrimaryConstructor: Boolean = false,
        parsedModifier: ((IElementType, PsiBuilder.Marker) -> Unit)? = null,
        parseConstBeforePropertyAccessor: Boolean = false,
        stopBeforeForeignCollection: Boolean = false,
    ): Boolean {
        val marker = mark()

        // 无编译语境的语法树需要保留历史拼写；正式编译仍由版本门决定其文法。
        if ((cjmpContext == CjmpParsingContext.SyntaxOnly || cjmpPlatformModifierSyntaxAvailable()) &&
            modifierKeywords.contains(SPECIFIC_KEYWORD) &&
            at(IDENTIFIER) && builder.tokenText?.toString() == PLATFORM_KEYWORD.value
        ) {
            val next = lookahead(1)
            if (next != null && (noModifiersBefore.contains(next) || CONTEXTUAL_KEYWORD_AS_IDENTIFIER_FOLLOW.contains(next))) {
                marker.rollbackTo()
                return false
            }
            // 语法采集不赋予历史修饰符当前编译角色；其原始拼写仍由 occurrence 保留。
            if (cjmpContext != CjmpParsingContext.SyntaxOnly) tokenConsumer?.invoke(PLATFORM_KEYWORD)
            advance()
            marker.collapse(PLATFORM_KEYWORD)
            parsedModifier?.invoke(PLATFORM_KEYWORD, marker)
            return true
        }

        if (atSet(modifierKeywords)) {
            // SDK 的延迟 remapper 可以把上下文关键字再次表示为 IDENTIFIER；
            // 文法已选中的 modifier 种类必须由同一词元的真实拼写保留，不能强制转换重读结果。
            val modifier = when (val current = tt()) {
                IDENTIFIER -> contextualKeywordFromTokenText()
                else -> current
            }
            check(modifier != null && modifierKeywords.contains(modifier))
            val lookahead = lookahead(1)

            if (at(FUNC_KEYWORD) && lookahead != INTERFACE_KEYWORD) {
                marker.rollbackTo()
                return false
            }

            if (isContextualKeywordUsedAsIdentifier(modifier, lookahead)) {
                marker.rollbackTo()
                return false
            }

            if (lookahead == null || !noModifiersBefore.contains(lookahead)) {
                tokenConsumer?.invoke(modifier)
                advance() // MODIFIER
                marker.collapse(modifier)
                parsedModifier?.invoke(modifier, marker)
                return true
            }
        } else if (isConstModifierStart(parseConstBeforePrimaryConstructor, parseConstBeforePropertyAccessor)) {
            // const 既可以是变量声明关键字，也可以修饰 const func/init/主构造器。
            advance() // MODIFIER
            tokenConsumer?.invoke(CONST_KEYWORD)
            marker.collapse(CONST_KEYWORD)
            parsedModifier?.invoke(CONST_KEYWORD, marker)
            return true
        } else if (at(UNSAFE_KEYWORD) && lookahead(1) != LBRACE) {
            advance()
            tokenConsumer?.invoke(UNSAFE_KEYWORD)
            marker.collapse(UNSAFE_KEYWORD)
            parsedModifier?.invoke(UNSAFE_KEYWORD, marker)
            return true
        } else if (at(FOREIGN_KEYWORD) && (!stopBeforeForeignCollection || lookahead(1) != LBRACE)) {
            advance()
            tokenConsumer?.invoke(FOREIGN_KEYWORD)
            marker.collapse(FOREIGN_KEYWORD)
            parsedModifier?.invoke(FOREIGN_KEYWORD, marker)
            return true
        }

        marker.rollbackTo()
        return false
    }

    /**
     * 当前软关键字修饰符（官方上下文关键字，如 `public`/`open`/`common`）在 [next] 之前是否实为标识符。
     *
     * 例：`enum E { | common | specific }` 中的 `common` 后随 `|`，是构造器名而非修饰符。
     */
    private fun isContextualKeywordUsedAsIdentifier(current: IElementType, next: IElementType?): Boolean {
        return current is CjKeywordToken && current.isSoft &&
                next != null && CONTEXTUAL_KEYWORD_AS_IDENTIFIER_FOLLOW.contains(next)
    }

    /**
     * 判断当前 `const` 是声明修饰符，还是变量/字段声明关键字。
     *
     * `const` 同时具有两种语法身份：
     * - `const value = 1` 中它开始一个变量或字段声明；
     * - `const common let a` 中它作为 modifier 修饰变量声明；
     * - `const override func` 中它与其他 modifier 共同修饰函数。
     *
     * 官方 parser 会连续收集完整 modifier 序列，不要求 `const` 必须紧邻
     * `func`/`init`/`let`/`var`。因此这里先越过后续可识别的声明 modifier，再由最终声明起始
     * token 决定 `const` 的身份。这保证 modifier 顺序无关，同时不会吞掉合法的
     * `const value` 字段/变量以及 `const TypeName(...)` 主构造器。
     */
    context(parseContext: ParsingContext)
    private fun isConstModifierStart(
        parseConstBeforePrimaryConstructor: Boolean,
        parseConstBeforePropertyAccessor: Boolean,
    ): Boolean {
        if (!at(CONST_KEYWORD)) return false
        // 属性体已经进入 modifier* accessor 文法；其中 const 没有变量声明身份。
        if (parseConstBeforePropertyAccessor) return true

        var declarationStartOffset = 1
        var hasFollowingConstModifier = false
        while (true) {
            val modifier = declarationModifierAtLookahead(declarationStartOffset) ?: break
            if (modifier == CONST_KEYWORD) hasFollowingConstModifier = true
            declarationStartOffset++
        }

        val declarationStart = lookahead(declarationStartOffset)
        return when {
            declarationStart in setOf(FUNC_KEYWORD, INIT_KEYWORD, LET_KEYWORD, VAR_KEYWORD) -> true
            declarationStart in setOf(GET_KEYWORD, SET_KEYWORD) && parseConstBeforePropertyAccessor -> true
            declarationStart == IDENTIFIER || declarationStart is CjKeywordToken && declarationStart.isSoft ->
                hasFollowingConstModifier ||
                    parseConstBeforePrimaryConstructor && lookahead(declarationStartOffset + 1) in setOf(LPAR, LT)
            else -> false
        }
    }

    /**
     * 判断 token 是否可作为声明前缀中的后续 modifier。
     *
     * `const`/`foreign`/`unsafe` 因同时拥有其他语法身份，没有放入
     * [MODIFIER_KEYWORDS]；但在 modifier 序列的前瞻中必须与普通 modifier 统一处理。
     */
    private fun isDeclarationModifierToken(token: IElementType?): Boolean {
        if (token == null) return false
        return MODIFIER_KEYWORDS.contains(token) ||
                token == CONST_KEYWORD ||
                token == FOREIGN_KEYWORD ||
                token == UNSAFE_KEYWORD ||
                token == INOUT_KEYWORD
    }

    /** 后续 modifier 与当前位置共用上下文词元分类；终止符前的上下文名字不能被前瞻吃掉。 */
    private fun declarationModifierAtLookahead(steps: Int): IElementType? {
        val token = lookahead(steps) ?: return null
        val grammar = if (token == IDENTIFIER) contextualKeywordFromText(builder.lookAheadTokenText(steps)) else token
        if (!isDeclarationModifierToken(grammar)) return null
        if (isContextualKeywordUsedAsIdentifier(checkNotNull(grammar), lookahead(steps + 1))) return null
        return grammar
    }

    /**
     * 解析修饰符列表主体
     *
     * @param tokenConsumer 修饰符令牌消费者
     * @param modifierKeywords 修饰符关键字集合
     * @param noModifiersBefore 禁止修饰符前的令牌集合
     * @param isParseMacro 是否解析宏
     * @return 是否为空列表
     */
    context(parseContext: ParsingContext)
    private fun doParseModifierListBody(
        tokenConsumer: ((IElementType) -> Unit)?,
        modifierKeywords: TokenSet,
        noModifiersBefore: TokenSet,
        isParseMacro: Boolean = false,
        parseConstBeforePrimaryConstructor: Boolean = false,
        parsedModifier: ((IElementType, PsiBuilder.Marker) -> Unit)? = null,
        parseConstBeforePropertyAccessor: Boolean = false,
        stopBeforeForeignCollection: Boolean = false,
    ): Boolean {
        var empty = true

        while (!eof()) {/*
            if (at(AT) && isParseMacro) {
                val beforeAnnotationMarker = mark()
                val type = parseAnnotation(null)
                if (type == null || type == MACRO_EXPRESSION) {
                    beforeAnnotationMarker.rollbackTo()
                    break
                } else {
                    tokenConsumer?.invoke(type)
                    beforeAnnotationMarker.done(type)
                }
            } else
            */
            if (!tryParseModifier(
                    tokenConsumer,
                    noModifiersBefore,
                    modifierKeywords,
                    parseConstBeforePrimaryConstructor,
                    parsedModifier,
                    parseConstBeforePropertyAccessor,
                    stopBeforeForeignCollection,
                )
            ) {
                // modifier not advanced
                break
            }
            empty = false
        }
        return empty
    }

    /**
     * 解析 Lambda 表达式
     *
     * Grammar:
     * ```
     * lambdaExpression
     *   : "{" parameter* "->" statement* "}"
     *   ;
     * ```
     */
    context(parseContext: ParsingContext)
    fun parseLambdaExpression() {
        val seed = parserSeed(parseContext)
        with(
            parseContext.copy(preferBlock = false, collapse = false, isDoubleArrow = false)
        ) {
            expressionParsing.parseFunctionLiteral(seed)
        }
    }

    /**
     * 解析注解或宏表达式
     *
     * Grammar:
     * ```
     * annotation
     *   : "@" (annotationUseSiteTarget ":" )? unescapedAnnotation
     *   ;
     *
     * unescapedAnnotation
     *   : qualifiedName typeArguments? valueArguments?
     *   ;
     *
     * macroExpression
     *   : "@" qualifiedName "[" macroArguments? "]"
     *   ;
     * ```
     *
     * @param detector 修饰符检测器
     * @return 解析结果的节点类型
     */
    context(parseContext: ParsingContext)
    fun parseAnnotation() {

        if (isBuiltInAnnotation()) {
            parseBuiltAnnotation()
            return
        }

        if (parseContext.allowPackageDirectiveAnnotations && isPackageDirectiveAnnotation()) {
            parseAnnotationWithOptionalArguments()
            return
        }

        // Platform interop annotations use the ordinary annotation grammar in
        // the official parser.  They are intentionally not language builtin
        // kinds in the common model: this branch only preserves the syntax;
        // TYPES resolves the real ClassId and publishes platform identity.
        if (isPlatformAnnotationSurface()) {
            parseAnnotationWithOptionalArguments()
            return
        }

        if (parseContext.disableMacroParsing) {
            if (!_atSet(AT, ATEXCL)) return

        } else {
            if (!_atSet(ATEXCL)) return
        }

        val mark = mark()
        if (_at(ATEXCL)) {
            builder.macroParsingJournal.register(
                builder.currentOffset,
                MacroParsingFacts(
                    scope = MacroParsingScope.Declaration(CjScopeKind.UNKNOWN_SCOPE),
                    parent = CjMacroFragmentParent.OTHER,
                    outerDeclIdent = parseContext.primaryDeclaration?.identifier,
                ),
            )
        }

        if (_at(ATEXCL) && reservedLanguageBuiltInAnnotationAtCurrentToken() != null) {
            error("Reserved built-in annotation cannot use @!")
        }
        advance() //消耗


//        注解名称

        parseConstructorCallee()

        if (at(LBRACKET)) {
            expressionParsing.parseValueArgumentList(LBRACKET, RBRACKET, CjExprKind.EXPR_IN_ANNOTATION)
        }

        mark.done(ANNOTATION)
    }

    context(parseContext: ParsingContext)
            /**
             * 解析带可选参数的注解
             *
             * 解析标准格式的注解，支持可选的参数列表（使用方括号 `[...]`）。
             * 适用于多种内置注解，如 @Deprecated, @Java, @C, @Annotation 等。
             *
             * Grammar:
             * ```
             * annotationWithOptionalArguments
             *   : "@" IDENTIFIER ("[" valueArguments "]")?
             *   ;
             * ```
             *
             * 支持的注解示例：
             * ```cangjie
             * @Deprecated                           // 无参数
             * @Deprecated["Use newMethod instead"]  // 带参数
             * @Java                                 // 无参数
             * @Annotation                           // 无参数
             * ```
             */
    fun parseAnnotationWithOptionalArguments() {
        assert(_at(AT))

        val mark = mark()
        advance() // 消耗 @

        // 解析注解名称标识符
        if (!at(IDENTIFIER)) {
            error(CangJieParsingBundle.message("parsing.error.expecting.identifier"))
            mark.done(ANNOTATION)
            return
        }

        parseConstructorCallee() // 消耗注解名称

        // 解析可选的参数列表 [...]
        if (at(LBRACKET)) {
            expressionParsing.parseValueArgumentList(LBRACKET, RBRACKET, CjExprKind.EXPR_IN_ANNOTATION)
        }

        mark.done(ANNOTATION)
    }

    /**
     * 提供 `parseAnnotations` 操作，封装仓颉语法解析节点的访问、构造或判断逻辑。
     */
    context(parseContext: ParsingContext)
    fun parseAnnotations(detector: ModifierDetector? = null) {
        parseAnnotations(detector, annotationConsumer = null)
    }

    context(parseContext: ParsingContext)
    private fun parseAnnotations(
        detector: ModifierDetector?,
        annotationConsumer: ((CjmpParsedAnnotationFacts) -> Unit)?,
    ) {
        val mark = mark()
        while (isAnnotationStart()) {
            val builtInAnnotation = if (isBuiltInAnnotation()) languageBuiltInAnnotationAtCurrentToken() else null
            val sourceName = builtInAnnotation?.sourceName ?: annotationSourceNameAtCurrentToken()
            val startOffset = builder.currentOffset
            parseAnnotation()
            val endOffset = lastSignificantTokenEndOffset()
            val sourceFact = CjAnnotationSourceFact(sourceName, builtInAnnotation?.kind,
                builder.macroParsingJournal.sourceOrigin(startOffset, endOffset))
            builder.macroParsingJournal.registerAnnotationSource(startOffset, sourceFact)
            detector?.recordAnnotation(sourceFact)
            annotationConsumer?.invoke(
                CjmpParsedAnnotationFacts(
                    sourceName = sourceName,
                    kind = builtInAnnotation?.kind,
                    startOffset = startOffset,
                    endOffset = endOffset,
                    sourceFact = sourceFact,
                ),
            )
        }
        mark.done(ANNOTATIONS)
    }

    private fun annotationSourceNameAtCurrentToken(): String =
        if (_atSet(AT, ATEXCL) && lookahead(1) == IDENTIFIER) {
            rawTokenText(builder, 1).toString()
        } else {
            builder.tokenText?.toString().orEmpty()
        }

    /**
     * 按当前解析上下文判断 annotation 起始。
     *
     * 生产解析保留普通 `@Macro` 的 macro 语义，只把 `@!` 与 builtin annotation
     * 交给 annotation parser；annotation-only 上下文才同时接受普通 `@`。
     */
    context(parseContext: ParsingContext)
    private fun isAnnotationStart(): Boolean =
        if (parseContext.disableMacroParsing) {
            _atSet(AT, ATEXCL)
        } else {
            at(ATEXCL) || isBuiltInAnnotation() || isPlatformAnnotationSurface() ||
                (parseContext.allowPackageDirectiveAnnotations && isPackageDirectiveAnnotation())
        }

    /**
     * 检查是否是内置注解
     *
     * 检查当前位置的注解是否为仓颉语言的内置注解。
     *
     * 内置注解包括:
     * - FFI相关: @C, @CallingConv
     * - 编译器指令: @Attribute, @NumericOverflow, @Intrinsic, @When, @FastNative, @ConstSafe
     * - 语义标记: @Deprecated, @Frozen
     * - 元注解: @Annotation
     * - 测试相关: @EnsurePreparedToMock
     *
     * @return 如果当前注解为内置注解返回true,否则返回false
     * @see org.cangnova.cangjie.psi.CjBuiltInAnnotation
     */
    fun isBuiltInAnnotation(): Boolean =
        _at(AT) && languageBuiltInAnnotationAtCurrentToken() != null

    /** 普通宏与内置、平台注解共享 AT 前缀，但必须保留各自的语法入口。 */
    internal fun isMacroCallStart(): Boolean =
        at(AT) && !isBuiltInAnnotation() && !isPlatformAnnotationSurface()

    /**
     * 判断当前 token 是否为官方平台 annotation 的语法表面。
     *
     * 这里不能发布平台语义身份，也不能根据短名进入 checker；它只防止
     * `@JavaMirror` 等 1.1.x 语法在 parser 中被误判成宏。真实身份仍由
     * TYPES 阶段解析出的 annotation ClassId 决定，旧语言版本也保留该语法
     * 并在语义阶段报告版本不支持。
     */
    private fun isPlatformAnnotationSurface(): Boolean {
        if (!_at(AT) || lookahead(1) != IDENTIFIER) return false
        var tokenIndex = 1
        var sourceName: String? = null
        while (lookahead(tokenIndex) == IDENTIFIER) {
            sourceName = rawTokenText(builder, tokenIndex).toString()
            if (lookahead(tokenIndex + 1) != DOT) break
            tokenIndex += 2
        }
        val finalSourceName = sourceName ?: return false
        return BuiltInAnnotationRegistry.findPlatformAnnotationsBySourceName(finalSourceName).isNotEmpty()
    }

    /** 语法分派与 @! 保留名检查共享模块约束；显式限定名称属于自定义注解。 */
    private fun languageBuiltInAnnotationAtCurrentToken(): BuiltInAnnotationDescriptor? {
        if (!_atSet(AT, ATEXCL) || lookahead(1) != IDENTIFIER || lookahead(2) == DOT) return null
        val sourceName = rawTokenText(builder, 1).toString()
        return BuiltInAnnotationRegistry.resolveLanguageBuiltIn(
            sourceName = sourceName,
            forcedCustom = _at(ATEXCL),
            moduleName = languageModuleName,
        )
    }

    /** `NonProduct` belongs to the file-header features directive only. */
    private fun isPackageDirectiveAnnotation(): Boolean {
        if (!_at(AT) || lookahead(1) != IDENTIFIER || lookahead(2) == DOT) return false
        return BuiltInAnnotationRegistry.resolvePackageDirective(
            sourceName = rawTokenText(builder, 1).toString(),
            forcedCustom = false,
        ) != null
    }

    /** `@!` 会改变 annotation provenance，但不能绕过语言 builtin 保留名。 */
    private fun reservedLanguageBuiltInAnnotationAtCurrentToken(): BuiltInAnnotationDescriptor? {
        if (!_atSet(AT, ATEXCL) || lookahead(1) != IDENTIFIER || lookahead(2) == DOT) return null
        return BuiltInAnnotationRegistry.resolveLanguageBuiltIn(
            sourceName = rawTokenText(builder, 1).toString(),
            forcedCustom = false,
            moduleName = languageModuleName,
        )
    }

    /**
     * 解析内置注解
     *
     * 解析仓颉语言的内置注解,如 @Java, @C, @When 等。
     * 内置注解的语法格式:
     * ```
     * builtInAnnotation
     *   : "@" builtInAnnotationName ("[" argumentList "]")?
     *   ;
     * ```
     *
     *
     * 示例:
     * ```cangjie
     * @Java[package="java.lang", name="String"]
     * @Deprecated[message:"Use newMethod instead"]
     * ```
     */
    context(parseContext: ParsingContext)
    private fun parseBuiltAnnotation() {
        assert(_atSet(AT))
        val descriptor = checkNotNull(
            org.cangnova.cangjie.annotations.BuiltInAnnotationRegistry.findLanguageBuiltIn(rawTokenText(builder, 1).toString())
                ?: org.cangnova.cangjie.annotations.BuiltInAnnotationRegistry.findPackageDirective(
                    rawTokenText(builder, 1).toString(),
                )
        )
        // Registry 选择语法形状；参数数量、字面量类型和目标约束由语义阶段统一检查。
        when (descriptor.argumentSyntax) {
            org.cangnova.cangjie.annotations.CangjieAnnotationArgumentSyntax.ATTRIBUTE_TOKENS -> parseAttributeAnnotation()
            org.cangnova.cangjie.annotations.CangjieAnnotationArgumentSyntax.CALLING_CONVENTION_REFERENCE -> {
                // Bare identifier form gets the dedicated PSI node. Empty, string and
                // multi-argument forms must remain ordinary value arguments so the
                // semantic owner can report the official arity/type diagnostic.
                if (lookahead(2) == LBRACKET && lookahead(3) == IDENTIFIER && lookahead(4) == RBRACKET) {
                    parseCallingConvAnnotation()
                } else {
                    parseAnnotationWithOptionalArguments()
                }
            }
            org.cangnova.cangjie.annotations.CangjieAnnotationArgumentSyntax.WHEN_CONDITION -> parseWhenAnnotation()
            org.cangnova.cangjie.annotations.CangjieAnnotationArgumentSyntax.OVERFLOW_STRATEGY ->
                parseOverflowAnnotation(checkNotNull(CjBuiltInAnnotation.fromName(descriptor.sourceName)))
            else -> parseAnnotationWithOptionalArguments()
        }
    }

    /**
     * 解析 @CallingConv 调用约定注解
     *
     * @CallingConv 注解用于指定函数的调用约定,支持 CDECL 和 STDCALL。
     * CDECL 是 clang C 编译器在不同平台上默认使用的调用约定。
     * STDCALL 是 Win32 API 使用的调用约定。
     *
     * Grammar:
     * ```
     * callingConvAnnotation
     *   : "@" "CallingConv" "[" ("CDECL" | "STDCALL") "]"
     *   ;
     * ```
     *
     * 示例:
     * ```cangjie
     * @CallingConv[CDECL]
     * foreign func rand(): Int32
     *
     * @CallingConv[STDCALL]
     * foreign func WinApiFunction(): Int32
     * ```
     */
    context(parseContext: ParsingContext)
    private fun parseCallingConvAnnotation() {
        assert(_atSet(AT))

        val mark = mark()
        advance() // 消耗 @

        // 解析 CallingConv 标识符
        if (!at(IDENTIFIER)) {
            error(CangJieParsingBundle.message("parsing.error.expecting.identifier"))
            mark.done(ANNOTATION)
            return
        }

        val annotationName = builder.tokenText
        if (annotationName != "CallingConv") {
            error(CangJieParsingBundle.message("parsing.error.expecting.annotation", "CallingConv"))
            mark.done(ANNOTATION)
            return
        }

        parseConstructorCallee()// 消耗 CallingConv

        // 解析参数: [CDECL] 或 [STDCALL]
        if (!expect(LBRACKET, CangJieParsingBundle.message("parsing.error.expecting.symbol", "["))) {
            mark.done(ANNOTATION)
            return
        }

        // 使用专用节点类型封装调用约定参数
        val callingConvMark = mark()

        // 解析调用约定标识符
        if (!at(IDENTIFIER)) {
            error(CangJieParsingBundle.message("parsing.error.expecting.calling.convention"))
            callingConvMark.drop()
            mark.done(ANNOTATION)
            return
        }

        val callingConv = builder.tokenText?.toString()
        if (callingConv != null && !CallingConvention.isValid(callingConv)) {
            error(CangJieParsingBundle.message("parsing.error.invalid.calling.convention", callingConv))
        }

        advance() // 消耗调用约定标识符

        callingConvMark.done(ANNOTATION_CALLING_CONV)

        expect(RBRACKET, CangJieParsingBundle.message("parsing.error.expecting.symbol", "]"))

        mark.done(ANNOTATION)
    }

    /**
     * 解析 @Java 注解
     *
     * @Java 注解用于与Java代码进行互操作,只接受一个字符串参数。
     *
     * Grammar:
     * ```
     * javaAnnotation
     *   : "@" "Java" ("[" stringLiteral "]")?
     *   ;
     * ```
     *
     * 示例:
     * ```cangjie
     * @Java["java.lang.String"]
     * @Java
     * ```
     */
    context(parseContext: ParsingContext)
    private fun parseJavaAnnotation(

    ) {
        assert(_atSet(AT))

        val mark = mark()
        advance() // 消耗 @

        //
        if (!at(IDENTIFIER)) {
            error(CangJieParsingBundle.message("parsing.error.expecting.identifier"))
            mark.done(ANNOTATION)
            return
        }

        val annotationName = builder.tokenText
        if (annotationName != "Java") {
            error(CangJieParsingBundle.message("parsing.error.expecting.annotation", "Java"))
            mark.done(ANNOTATION)
            return
        }

        advance() // 消耗 Java

        // 检查是否有参数
        if (!at(LBRACKET)) {
            // @Java 注解可以不带参数
            mark.done(ANNOTATION)
            return
        }

        advance() // 消耗 [

        builder.disableNewlines()

        // @Java 注解只接受一个字符串参数
        if (at(OPEN_QUOTE)) {
            with(parseContext.copy(processStringInterpolation = false)) {
                expressionParsing.parseStringTemplate()
            }
        } else {
            error(CangJieParsingBundle.message("parsing.error.expecting.string.literal"))
        }

        builder.enableNewlines()

        expect(RBRACKET, CangJieParsingBundle.message("parsing.error.expecting.symbol", "]"))

        mark.done(ANNOTATION)
    }


    /**
     * 提供 `parseConstructorCallee` 操作，封装仓颉语法解析节点的访问、构造或判断逻辑。
     */
    context(parseContext: ParsingContext)
    fun parseConstructorCallee() {
        val reference = mark()
        parseTypeRef()
        reference.done(CONSTRUCTOR_CALLEE)
    }


    /**
     * 解析溢出注解
     *
     * 解析 @OverflowThrowing, @OverflowWrapping, @OverflowSaturating 注解。
     * 这些注解可以带可选参数来指定具体的溢出策略。
     *
     * Grammar:
     * ```
     * overflowAnnotation
     *   : "@" ("OverflowThrowing" | "OverflowWrapping" | "OverflowSaturating") ("[" identifier "]")?
     *   ;
     * ```
     *
     * 示例:
     * ```cangjie
     * @OverflowThrowing
     * @OverflowThrowing[checked]
     * @OverflowWrapping[wrapping]
     * @OverflowSaturating[saturating]
     * ```
     *
     * @param builtInAnnotation 内置注解类型
     */
    context(parseContext: ParsingContext)
    private fun parseOverflowAnnotation(
        builtInAnnotation: CjBuiltInAnnotation
    ) {
        assert(_atSet(AT))

        val mark = mark()
        advance() // 消耗 @

        // 解析注解标识符
        if (!at(IDENTIFIER)) {
            error(CangJieParsingBundle.message("parsing.error.expecting.identifier"))
            mark.done(ANNOTATION)
            return
        }

        val annotationName = builder.tokenText
        if (annotationName != builtInAnnotation.annotationName) {
            error(CangJieParsingBundle.message("parsing.error.expecting.annotation", builtInAnnotation.annotationName))
            mark.done(ANNOTATION)
            return
        }

        parseConstructorCallee()// 消耗注解名称

        // 检查是否有参数
        if (at(LBRACKET)) {
            advance() // 消耗 [
            // 使用专用节点类型封装调用约定参数
            val overflow = mark()
            builder.disableNewlines()

            // 解析溢出策略标识符
            if (at(IDENTIFIER)) {
                val strategyName = builder.tokenText?.toString()
                if (strategyName != null && !OverflowStrategy.isValid(strategyName)) {
                    error(
                        CangJieParsingBundle.message(
                            "parsing.error.invalid.overflow.strategy",
                            strategyName
                        )
                    )
                }
                advance() // 消耗策略标识符
            } else {
                error(CangJieParsingBundle.message("parsing.error.expecting.overflow.strategy"))
            }

            builder.enableNewlines()
            overflow.done(ANNOTATION_OVERFLOW_STRATEGY)
            expect(RBRACKET, CangJieParsingBundle.message("parsing.error.expecting.symbol", "]"))
        }
        // 如果没有参数,从注解名称中提取策略(如 OverflowThrowing -> throwing)

        mark.done(ANNOTATION)
    }

    /**
     * 解析不需要参数的内置注解
     *
     * 解析简单的内置注解,这些注解不需要任何参数,如 @C, @FastNative, @Intrinsic 等。
     * 语法格式:
     * ```
     * nonArgAnnotation
     *   : "@" annotationName
     *   ;
     * ```
     *
     * @param builtInAnnotation 内置注解类型
     *
     * 示例:
     * ```cangjie
     * @C
     * @FastNative
     * @Intrinsic
     * ```
     */
    context(parseContext: ParsingContext)
    private fun parseBuiltNonArgAnnotation(
        builtInAnnotation: CjBuiltInAnnotation
    ) {
        assert(_atSet(AT))

        val mark = mark()
        advance() // 消耗 @

        // 解析注解标识符
        if (!at(IDENTIFIER)) {
            error(CangJieParsingBundle.message("parsing.error.expecting.identifier"))
            mark.done(ANNOTATION)
            return
        }

        val annotationName = builder.tokenText
        if (annotationName != builtInAnnotation.annotationName) {
            error(CangJieParsingBundle.message("parsing.error.expecting.annotation", builtInAnnotation.annotationName))
            mark.done(ANNOTATION)
            return
        }
        parseConstructorCallee()


        mark.done(ANNOTATION)
    }

    /**
     * (modifier )*
     *
     * 如果不为空，则将修饰符(非批注)馈送到传递的使用者
     *
     * @param noModifiersBefore 是一个令牌集，其中包含指示何时满足这些元素的元素。
     *                          必须将前一个令牌解析为标识符，而不是修饰符
     */
    context(parseContext: ParsingContext)
    fun parseModifierList(
        tokenConsumer: ((IElementType) -> Unit)?,
        noModifiersBefore: TokenSet,
        isParseMacro: Boolean = false,
        parseConstBeforePrimaryConstructor: Boolean = false,
        stopBeforeForeignCollection: Boolean = false,
    ): Boolean {
        return parseModifierListInternal(
            tokenConsumer = tokenConsumer,
            noModifiersBefore = noModifiersBefore,
            isParseMacro = isParseMacro,
            parseConstBeforePrimaryConstructor = parseConstBeforePrimaryConstructor,
            stopBeforeForeignCollection = stopBeforeForeignCollection,
        )
    }

    context(parseContext: ParsingContext)
    private fun parseModifierListInternal(
        tokenConsumer: ((IElementType) -> Unit)?,
        noModifiersBefore: TokenSet,
        isParseMacro: Boolean = false,
        parseConstBeforePrimaryConstructor: Boolean = false,
        parseConstBeforePropertyAccessor: Boolean = false,
        modifierKeywords: TokenSet = MODIFIER_KEYWORDS,
        stopBeforeForeignCollection: Boolean = false,
    ): Boolean {
        val detector = tokenConsumer as? ModifierDetector
        val parsedModifier = detector?.let { target ->
            { token: IElementType, marker: PsiBuilder.Marker ->
                val start = marker.startOffset - builder.macroParsingJournal.markerOffsetOrigin
                recordParsedModifier(target, token, start, start + (token as CjKeywordToken).value.length)
            }
        }
        return doParseModifierList(
            tokenConsumer,
            modifierKeywords,
            noModifiersBefore,
            isParseMacro,
            parseConstBeforePrimaryConstructor,
            parsedModifier,
            parseConstBeforePropertyAccessor,
            stopBeforeForeignCollection,
        )
    }

    /** 首次 token 捕获与有效属性独立；同 kind 插入失败保留旧对象，诊断指向新 token。 */
    private fun captureModifierSource(token: CjKeywordToken, start: Int, end: Int): CjModifierSourceFact {
        val fact = CjModifierSourceFact(checkNotNull(token.cjModifierKindOrNull()),
            builder.macroParsingJournal.sourceOrigin(start, end))
        check(fact.sourceRange.source.text.substring(fact.sourceRange.startOffset, fact.sourceRange.endOffset) == token.value) {
            "A captured modifier source must cover its actual keyword."
        }
        builder.macroParsingJournal.registerModifierSource(start, fact)
        return fact
    }

    context(parseContext: ParsingContext)
    private fun recordParsedModifier(detector: ModifierDetector, token: IElementType, start: Int, end: Int) {
        val keyword = token as CjKeywordToken
        val fact = captureModifierSource(keyword, start, end)
        val previous = detector.recordParsedModifier(CjmpModifierOccurrence(keyword, start, end, fact)) ?: return
        if (fileState.quotedSyntaxDepth > 0) return
        errorOnSourceRange(fact.origin, fact.sourceRange,
            CangJieParsingBundle.message("parsing.error.duplicate.modifier", keyword.value),
            listOf(sourceHint(previous.sourceFact.origin, previous.sourceFact.sourceRange,
                CangJieParsingBundle.message("parsing.error.hint.previous.one.is.here"))),
        )
    }


    /**
     * 私有的修饰符列表解析方法
     *
     * @param tokenConsumer 修饰符令牌消费者
     * @param modifierKeywords 修饰符关键字集合
     * @param noModifiersBefore 禁止修饰符前的令牌集合
     * @param isParseMacro 是否解析宏
     * @return 是否解析到修饰符
     */
    context(parseContext: ParsingContext)
    private fun doParseModifierList(
        tokenConsumer: ((IElementType) -> Unit)?,
        modifierKeywords: TokenSet,
        noModifiersBefore: TokenSet,
        isParseMacro: Boolean = false,
        parseConstBeforePrimaryConstructor: Boolean = false,
        parsedModifier: ((IElementType, PsiBuilder.Marker) -> Unit)? = null,
        parseConstBeforePropertyAccessor: Boolean = false,
        stopBeforeForeignCollection: Boolean = false,
    ): Boolean {
        val list = mark()

        val empty = doParseModifierListBody(
            tokenConsumer,
            modifierKeywords,
            noModifiersBefore,
            isParseMacro,
            parseConstBeforePrimaryConstructor,
            parsedModifier,
            parseConstBeforePropertyAccessor,
            stopBeforeForeignCollection,
        )

        // 始终创建 MODIFIER_LIST 占位符，保持与反编译 Stub 结构一致
        list.done(MODIFIER_LIST)
        return !empty
    }


    /**
     * 解析类通用声明
     *
     * 处理类内部的通用声明，包括函数、属性、变量等。
     *
     * @param tokenId 令牌 ID
     * @param classdetector 类修饰符检测器
     * @param detector 修饰符检测器
     * @return 解析结果的节点类型
     */
    context(parseContext: ParsingContext)
    private fun parseClassCommonDeclaration(
        tokenId: Int?, classdetector: ModifierDetector, detector: ModifierDetector
    ): IElementType? {
        return when (getTokenId()) {
            AT_Id -> with(parseContext.copy(backToken = true)) {
                expressionParsing.parseMacroExpression(checkNotNull(parseContext.memberScope).declarationScope,
                    DeclarationParsingMode.MEMBER, detector.declarationPrefix())
            }

            FUNC_KEYWORD_Id -> if (tokenId != null) {
                if (tokenId == INTERFACE_KEYWORD_Id) {
                    parseFunction(true, classdetector, detector, topTokenId = tokenId)
                } else {
                    parseFunction(classdetector = classdetector, detector = detector, topTokenId = tokenId)
                }
            } else {
                parseFunction(detector = detector, topTokenId = tokenId)
            }

            PROP_KEYWORD_Id -> if (tokenId != null && tokenId == INTERFACE_KEYWORD_Id) {
                parseProperty(true, classdetector, detector)
            } else {
                parseProperty(classdetector = classdetector, detector = detector)
            }

            LET_KEYWORD_Id, VAR_KEYWORD_Id, CONST_KEYWORD_Id -> parseField(detector)

            MACRO_KEYWORD_Id -> with(parseContext.copy(disableMacroParsing = false, allowParseAnnotationsInValueParameter = false)) {
                // 原生 MacroDecl 即使作用域错误仍完整解析并保留声明身份。
                parseMacro(enclosingDeclarationKeyword(tokenId), detector)
            }

            CLASS_KEYWORD_Id, INTERFACE_KEYWORD_Id, STRUCT_KEYWORD_Id, ENUM_KEYWORD_Id, EXTEND_KEYWORD_Id -> {
                parseClass(detector, enclosingDeclarationKeyword = enclosingDeclarationKeyword(tokenId))
            }

            else -> null
        }
    }

    /** 成员声明使用真实外层名义关键字；诊断中的语法角色由本地化资源渲染。 */
    private fun enclosingDeclarationKeyword(tokenId: Int?): CjKeywordToken? = when (tokenId) {
        null -> null
        CLASS_KEYWORD_Id -> CLASS_KEYWORD
        INTERFACE_KEYWORD_Id -> INTERFACE_KEYWORD
        STRUCT_KEYWORD_Id -> STRUCT_KEYWORD
        ENUM_KEYWORD_Id -> ENUM_KEYWORD
        EXTEND_KEYWORD_Id -> EXTEND_KEYWORD
        else -> kotlin.error("Unexpected nominal declaration scope: $tokenId")
    }


    /**
     * 解析类初始化器（占位方法）
     *
     * 当前未实现，返回 null。
     *
     * @return 总是返回 null
     */
    context(parseContext: ParsingContext)
    private fun parseClassInitializer(): IElementType? {
        return null
    }


    /**
     * 解析属性委托或赋值
     *
     * Grammar:
     * ```
     * propertyDelegateOrAssignment
     *   : "=" expression
     *   ;
     * ```
     *
     * @return 是否成功解析
     */
    context(parseContext: ParsingContext)
    private fun parsePropertyDelegateOrAssignment(): Boolean {
        if (at(EQ)) {
            advance() // consume EQ token

            expressionParsing.parseExpression(CjExprKind.VAR_INIT)

            return true
        }
        return false
    }


    /**
     * 寻找接收者后的最后一个点
     *
     * 用于处理接收者类型的解析。
     *
     * @return 最后一个点的位置
     */
    context(parseContext: ParsingContext)
    private fun lastDotAfterReceiver(): Int {
        val pattern = if (at(LPAR)) lastDotAfterReceiverLParPattern else lastDotAfterReceiverNotLParPattern
        pattern.reset()
        return matchTokenStreamPredicate(pattern)
    }


    /**
     * 解析接收者类型
     *
     * @param title 标题名称
     * @param nameFollow 名称跟随的令牌集合
     * @return 是否存在接收者类型
     */
    context(parseContext: ParsingContext)
    private fun parseReceiverType(
        title: String,
        nameFollow: TokenSet
    ): Boolean {
        val lastDot = lastDotAfterReceiver()
        val receiverPresent = lastDot != -1

        if (!receiverPresent) return false

        createTruncatedBuilder(lastDot).parseTypeRef()

        if (atSet(RECEIVER_TYPE_TERMINATORS)) {
            advance() // expectation
        } else {
            errorWithRecovery(CangJieParsingBundle.message("parsing.error.expecting.symbol", "."), nameFollow)
        }
        return true
    }


    /**
     * 解析类成员字段声明
     *
     * field
     *   : modifiers ("let" | "var" | "const") SimpleName (":" type) ("=" expression)?
     *   | modifiers ("let" | "var" | "const") SimpleName (":" type)? "=" expression
     *   ;
     *
     * 成员字段使用简单标识符，不支持模式匹配
     * 成员字段必须有类型声明或初始化表达式
     */
    context(parseContext: ParsingContext)
    fun parseField(
        detector: ModifierDetector
    ): IElementType {
        assert(at(LET_KEYWORD) || at(VAR_KEYWORD) || at(CONST_KEYWORD))
        // 官方成员入口也先按变量文法分流；直接名义 body 的接收层另行拒绝模式声明。
        if (seeingVariablePattern(1)) return parseVariable(detector)
        val declarationAnchor = builder.currentOffset
        detector.declarationKeyword = tt() as CjKeywordToken
        advance()

        if (!at(IDENTIFIER)) {
            error(CangJieParsingBundle.message("parsing.error.expecting.identifier"))
            skipUntil(EOL_OR_SEMICOLON_RBRACE_SET)
            return INVALID_DECLARATION
        }

        // 成员字段使用简单标识符
        detector.declarationNameRange = builder.currentOffset to (builder.currentOffset + checkNotNull(builder.tokenText).length)
        detector.cjmpDeclarationName = builder.tokenText
        parseIdentifierByTitle("field", PROPERTY_NAME_FOLLOW_SET, true)

        var hasTypeRef = false
        var hasInitializer = false

        if (at(COLON)) {
            advance() // COLON
            parseTypeRef()
            hasTypeRef = true
        }

        if (at(EQ)) {
            advance() // EQ
            expressionParsing.parseExpression(CjExprKind.VAR_INIT)
            hasInitializer = true
            detector.cjmpHasInitializer = true
        }

        // 成员字段必须有类型声明或初始化表达式
        if (!hasTypeRef && !hasInitializer) {
            error(CangJieParsingBundle.message("parsing.error.field.requires.type.or.initializer"))
        }

        // 普通成员变量与 ParseVarDecl 共用 initializer 后检查的次序。
        checkCjmpDeclarationFileRole(detector, FIELD, declarationAnchor)

        return FIELD
    }

    /**
     * 解析变量声明（支持模式匹配）
     *
     * variableDeclaration
     *   : modifiers ("let" | "var" | "const") pattern (":" type)? ("=" expression)?
     *   ;
     *
     * 用于顶层变量和局部变量，支持解构赋值如 let (a, b) = tuple
     */
    context(parseContext: ParsingContext)
    fun parseVariable(
        classdetector: ModifierDetector
    ): IElementType {
        assert(at(LET_KEYWORD) || at(VAR_KEYWORD) || at(CONST_KEYWORD))
        val declarationAnchor = builder.currentOffset
        classdetector.declarationKeyword = tt() as CjKeywordToken
        advance()

        // ParseVarOrLetOrConst 在进入 pattern 文法前分流，初始化值的形状不参与这个判定。
        val isPatternDeclaration = seeingVariablePattern()
        if (!isPatternDeclaration && !at(IDENTIFIER)) {
            // 无名字或模式的 InvalidDecl 没有 CheckDeclModifiers 调用。
            error(CangJieParsingBundle.message("parsing.error.expecting.identifier"))
            skipUntil(EOL_OR_SEMICOLON_RBRACE_SET)
            return INVALID_DECLARATION
        }
        if (isPatternDeclaration) checkCjmpDeclarationFileRole(classdetector, VARIABLE, declarationAnchor)
        else classdetector.declarationNameRange = builder.currentOffset to (builder.currentOffset + checkNotNull(builder.tokenText).length)

        // 变量声明使用模式匹配
        val patternRange = mark()
        val patternType = expressionParsing.parsePattern(
            CangJieExpressionParsing.PatternParseContext.VARIABLE_DECL
        )
        // 使用共享模式文法实际完成的根节点种类，不从首 token 或 raw CFIR 反推。
        val commonPatternKind = when (patternType) {
            TUPLE_PATTERN -> "tuple"
            ENUM_PATTERN -> "enum"
            WILDCARD_PATTERN -> "wildcard"
            else -> null
        }
        val cjmpRulesEnabled = when (val context = cjmpContext) {
            CjmpParsingContext.SyntaxOnly -> true
            is CjmpParsingContext.Compilation -> context.enabled && (context.compileCommon || context.compileSpecific)
        }
        if (classdetector.isCommonDetected && commonPatternKind != null && cjmpRulesEnabled && fileState.quotedSyntaxDepth == 0) {
            patternRange.error(CangJieParsingBundle.message("parsing.error.cjmp.pattern.declaration", commonPatternKind, "common"))
        } else {
            patternRange.drop()
        }

        if (at(COLON)) {
            advance() // COLON
            parseTypeRef()
        }

        if (at(EQ)) {
            advance() // EQ
            expressionParsing.parseExpression(CjExprKind.VAR_INIT)
            classdetector.cjmpHasInitializer = true
        }

        if (!isPatternDeclaration) checkCjmpDeclarationFileRole(classdetector, VARIABLE, declarationAnchor)

        return VARIABLE
    }

    /** 对位 ParseVarOrLetOrConst 的入口判定，不通过已完成的 pattern 或初始化值补推。 */
    private fun seeingVariablePattern(offset: Int = 0): Boolean {
        val token = if (offset == 0) tt() else lookahead(offset)
        val isIdentifier = token == IDENTIFIER || token is CjKeywordToken && token.isSoft
        return token == UNDERLINE || token == LPAR || isIdentifier && lookahead(offset + 1) in setOf(LPAR, DOT, LT)
    }

    /**
     * 解析表达式代码片段
     *
     * 用于调试和代码片段解析。
     */
    context(parseContext: ParsingContext)
    fun parseExpressionCodeFragment() {
        val marker = mark()
        expressionParsing.parseExpression()


        checkForUnexpectedSymbols()

        marker.done(EXPRESSION_CODE_FRAGMENT)
    }

    /**
     * 对位官方 ParseNodes：作用域选择文法，真实 parent 选择分隔规则；此处不决定替换数量。
     * 分隔错误保留已完成的直接 roots，未解析尾部只保留 tokens，不再制造额外声明。
     */
    context(parseContext: ParsingContext)
    fun parseMacroCodeFragment(entry: CjMacroFragmentContext) {
        check(macroFragmentInfo == null) { "A macro parser root can only be completed once." }
        require(parseContext.enableCustomAnnotation && parseContext.disableMacroParsing)
        val entryFileRole = parsedFileFacts.cjmpRole
        val fragment = mark()
        val fileErrorStart = mark()
        val fileErrorEnd = mark()
        val contents = mark()
        val scope = when (entry) {
            is CjMacroFragmentContext.Nodes -> (entry.scope as? MacroParsingScope.Declaration)?.kind
            is CjMacroFragmentContext.ParameterSlice, is CjMacroFragmentContext.Annotation -> null
        }
        var connectedByComma = entry is CjMacroFragmentContext.Nodes && entry.parent.isConnectedByComma
        var rootIndex = 0
        var previousRootStart = -1
        var prefixRootIndex: Int? = null
        var prefixRoleCheck: CjmpDeclarationRoleCheck? = null
        var hasInvalidRoot = false
        val inoutRoots = linkedSetOf<Int>()
        val parameterSequence = ParameterSequenceState()
        with(parseContext.forMacroFragment(entry)) {
            while (!eof()) {
                val hasRoot = rootIndex != 0
                if (entry is CjMacroFragmentContext.Nodes && hasRoot && connectedByComma && !consumeIf(COMMA)) {
                    errorOnCurrentToken(CangJieParsingBundle.message("parsing.error.expecting.symbol", ","))
                    break
                }
                // 参数的 comma 每轮只消费一次；不复制官方1.1.3在第三项双重消费的缺陷。
                if (entry is CjMacroFragmentContext.ParameterSlice) connectedByComma = consumeIf(COMMA)
                if (scope != null || hasRoot || entry is CjMacroFragmentContext.ParameterSlice) {
                    // 同一失败 root 不能重复使用已经看过的 NL；只有新消费的边界才是分隔符。
                    var hasSeparator = builder.currentOffset != previousRootStart && builder.newlineBeforeCurrentToken()
                    builder.withNLTokenSkipping(false) {
                        while (at(NL) || at(SEMICOLON)) {
                            hasSeparator = true
                            advance()
                        }
                    }
                    if (hasRoot && !connectedByComma && !hasSeparator) {
                        errorOnCurrentToken(CangJieParsingBundle.message("parsing.error.macro.node.separator"))
                        break
                    }
                }
                if (eof()) break
                previousRootStart = builder.currentOffset
                when (entry) {
                    is CjMacroFragmentContext.ParameterSlice -> {
                        parseValueParameter(entry.grammar, sequence = parameterSequence)
                    }
                    is CjMacroFragmentContext.Annotation -> {
                        if (!at(AT) && !at(ATEXCL)) {
                            error("Expected an annotation root in a macro annotation fragment")
                            hasInvalidRoot = true
                        } else {
                            parseAnnotation()
                        }
                    }
                    is CjMacroFragmentContext.Nodes -> {
                        if (scope != null && seeingScopedMacroDeclaration(scope)) {
                            val prefix = if (rootIndex == 0) entry.declarationPrefix else CjMacroDeclarationPrefix.EMPTY
                            val result = parseScopedMacroDeclaration(scope, prefix)
                            if (rootIndex == 0) {
                                prefixRootIndex = 0
                                prefixRoleCheck = result.second
                            }
                            if (result.first == INVALID_DECLARATION) hasInvalidRoot = true
                        } else {
                            if (entry.scope is MacroParsingScope.Expression &&
                                entry.parent == CjMacroFragmentParent.FUNCTION_ARGUMENT && consumeIf(INOUT_KEYWORD)
                            ) inoutRoots += rootIndex
                            val kind = (entry.scope as? MacroParsingScope.Expression)?.kind ?: CjExprKind.ALL
                            if (expressionParsing.parseExpression(kind).root == CangJieExpressionParsing.ParsedExpression.Missing) {
                                hasInvalidRoot = true
                            }
                        }
                    }
                }
                rootIndex++
            }
            // SDK 根覆盖完整输入；未解析叶子不成为 roots，也不追加次生诊断。
            while (!eof()) advance()
        }
        contents.done(MACRO_FRAGMENT_CONTENTS)
        finishCjmpFileError(fileErrorStart, fileErrorEnd)
        fragment.done(MACRO_CODE_FRAGMENT)
        val inheritedPrefix = if (prefixRootIndex == null) null else (entry as CjMacroFragmentContext.Nodes).declarationPrefix
        macroFragmentInfo = CjMacroFragmentParseInfo(
            inoutRoots, prefixRootIndex, hasInvalidRoot, inheritedPrefix, prefixRoleCheck, cjmpContext, entryFileRole,
        )
    }

    /** 官方 CheckIfSeeingDecl；不通过 speculative parse 失败来推断 root 类型。 */
    context(parseContext: ParsingContext)
    private fun seeingScopedMacroDeclaration(scope: CjScopeKind): Boolean {
        if (at(AT) && lookahead(1) == IDENTIFIER && rawTokenText(builder, 1).toString() == "IfAvailable") return false
        if (seeingDeclarationForRecovery()) return true
        if ((at(AT) || at(ATEXCL)) && lookahead(1) == IDENTIFIER && !isBuiltInAnnotation()) return true
        if (at(INIT_KEYWORD) || at(PROP_KEYWORD) || at(IMPORT_KEYWORD)) return true
        if (scope == CjScopeKind.CLASS_BODY || scope == CjScopeKind.STRUCT_BODY) {
            if (seeingPrimaryConstructor()) return true
        }
        return (scope == CjScopeKind.ENUM_BODY || scope == CjScopeKind.ENUM_CONSTRUCTOR) && at(IDENTIFIER)
    }

    /** shared 声明文法直接构造当前 root；不执行完整名义 body 的聚合接收步骤。 */
    context(parseContext: ParsingContext)
    private fun parseScopedMacroDeclaration(scope: CjScopeKind, prefix: CjMacroDeclarationPrefix): Pair<IElementType, CjmpDeclarationRoleCheck> {
        val declaration = mark()
        val detector = ModifierDetector(prefix)
        parseAnnotations(detector)
        parseModifierList(detector, TokenSet.EMPTY, parseConstBeforePrimaryConstructor = true)
        val declarationStart = builder.currentOffset
        val declarationAnchor = if (at(TILDE)) nextLogicalTokenStartOffset() else declarationStart
        val memberKeyword = when (scope) {
            CjScopeKind.CLASS_BODY -> CLASS_KEYWORD_Id
            CjScopeKind.STRUCT_BODY -> STRUCT_KEYWORD_Id
            CjScopeKind.INTERFACE_BODY -> INTERFACE_KEYWORD_Id
            CjScopeKind.ENUM_BODY -> ENUM_KEYWORD_Id
            CjScopeKind.EXTEND_BODY -> EXTEND_KEYWORD_Id
            else -> null
        }
        val kind = when {
            (scope == CjScopeKind.ENUM_BODY || scope == CjScopeKind.ENUM_CONSTRUCTOR) && at(IDENTIFIER) -> {
                parseEnumConstructorRest(detector)
                ENUM_CONSTRUCTOR
            }
            (scope == CjScopeKind.CLASS_BODY || scope == CjScopeKind.STRUCT_BODY) && seeingPrimaryConstructor() -> {
                // ParseDecl 保留 primary；直接 class 接收时的错名淘汰不在这里执行。
                parsePrimaryInitFunc(checkNotNull(primaryParameterGrammar()), detector)
                PRIMARY_CONSTRUCTOR
            }
            memberKeyword != null -> parseMemberDeclarationRest(memberKeyword, ModifierDetector(), detector)
            else -> {
                val mode = when (scope) {
                    CjScopeKind.TOPLEVEL -> DeclarationParsingMode.TOPLEVEL
                    CjScopeKind.UNKNOWN_SCOPE -> DeclarationParsingMode.ALL
                    else -> DeclarationParsingMode.LOCAL
                }
                parseCommonDeclaration(detector, NameParsingMode.REQUIRED, mode)
            }
        }
        if (kind == null) errorOnCurrentToken(CangJieParsingBundle.message("parsing.error.invalid.declaration.scope"))
        val rootType = kind ?: INVALID_DECLARATION
        val roleCheck = when (rootType) {
            INVALID_DECLARATION, MACRO_EXPRESSION -> CjmpDeclarationRoleCheck.NOT_CHECKED
            else -> checkNotNull(builder.macroParsingJournal.declarationRoleCheckAt(declarationAnchor)) {
                "A declaration grammar must publish its own file role check."
            }
        }
        closeDeclarationWithCommentBinders(declaration, rootType, true)
        return rootType to roleCheck
    }

    /**
     * 解析块代码片段
     *
     * 用于调试和代码片段解析。
     */
    context(parseContext: ParsingContext)
    fun parseBlockCodeFragment() {
        val marker = mark()
        val blockMarker = mark()

        if (at(PACKAGE_KEYWORD) || at(IMPORT_KEYWORD)) {
            val err = mark()
            parsePreamble()
            err.error(
                CangJieParsingBundle.message(
                    "parsing.error.not.allowed.context", "Package directive and imports", "code fragments"
                )
            )
        }


        expressionParsing.parseStatements()

        checkForUnexpectedSymbols()

        blockMarker.done(BLOCK)
        marker.done(BLOCK_CODE_FRAGMENT)
    }

    /**
     * 定义 `DeclarationParser` 接口，约束仓颉语法解析节点或服务需要暴露的结构能力。
     */
    private interface DeclarationParser {
        /**
         * 提供 `isValidInScope` 操作，封装仓颉语法解析节点的访问、构造或判断逻辑。
         */
        fun isValidInScope(scope: DeclarationParsingMode): Boolean

        /**
         * 提供 `parse` 操作，封装仓颉语法解析节点的访问、构造或判断逻辑。
         */
        context(parseContext: ParsingContext)
        fun parse(
            parser: CangJieParsing,
            detector: ModifierDetector,
            nameParsingMode: NameParsingMode,
            scope: DeclarationParsingMode
        ): IElementType?
    }

    /**
     * 表示 `ScopedDeclarationParser`，承载仓颉语法解析中的语法节点、索引桩或辅助模型。
     */
    private abstract class ScopedDeclarationParser(
        /**
         * 保存 `validScopes` 的内部状态，供仓颉语法解析实现维护节点缓存或解析上下文。
         */
        private val validScopes: Set<DeclarationParsingMode>
    ) : DeclarationParser {
        /**
         * 实现 `isValidInScope` 的仓颉语法解析协议回调，保持与 IntelliJ PSI 访问契约一致。
         */
        override fun isValidInScope(scope: DeclarationParsingMode): Boolean {
            return validScopes.contains(scope)
        }

        /**
         * 实现 `parse` 的仓颉语法解析协议回调，保持与 IntelliJ PSI 访问契约一致。
         */
        context(parseContext: ParsingContext)
        override fun parse(
            parser: CangJieParsing,
            detector: ModifierDetector,
            nameParsingMode: NameParsingMode,
            scope: DeclarationParsingMode
        ): IElementType? {
            val type = doParse(parser, detector, nameParsingMode, scope)
            if (!isValidInScope(scope)) {
                return null
            }
            return type
        }

        /**
         * 提供 `doParse` 操作，封装仓颉语法解析节点的访问、构造或判断逻辑。
         */
        context(parseContext: ParsingContext)
        protected abstract fun doParse(
            parser: CangJieParsing,
            detector: ModifierDetector,
            nameParsingMode: NameParsingMode,
            scope: DeclarationParsingMode
        ): IElementType?
    }

    /**
     * 表示 `TypeAliasParser`，承载仓颉语法解析中的语法节点、索引桩或辅助模型。
     */
    private class TypeAliasParser : ScopedDeclarationParser(
        setOf(DeclarationParsingMode.ALL, DeclarationParsingMode.TOPLEVEL)
    ) {
        /**
         * 实现 `doParse` 的仓颉语法解析协议回调，保持与 IntelliJ PSI 访问契约一致。
         */
        context(parseContext: ParsingContext)
        override fun doParse(
            parser: CangJieParsing,
            detector: ModifierDetector,
            nameParsingMode: NameParsingMode,
            scope: DeclarationParsingMode
        ): IElementType? {
            return parser.parseTypeAlias(detector)
        }
    }

    /**
     * 表示 `MainFuncParser`，承载仓颉语法解析中的语法节点、索引桩或辅助模型。
     */
    private class MainFuncParser : ScopedDeclarationParser(
        setOf(DeclarationParsingMode.ALL, DeclarationParsingMode.TOPLEVEL)
    ) {
        /**
         * 实现 `doParse` 的仓颉语法解析协议回调，保持与 IntelliJ PSI 访问契约一致。
         */
        context(parseContext: ParsingContext)
        override fun doParse(
            parser: CangJieParsing,
            detector: ModifierDetector,
            nameParsingMode: NameParsingMode,
            scope: DeclarationParsingMode
        ): IElementType? {
            return parser.parseMainFunc(detector)
        }
    }

    /**
     * 表示 `ClassParser`，承载仓颉语法解析中的语法节点、索引桩或辅助模型。
     */
    private class ClassParser : ScopedDeclarationParser(
        setOf(DeclarationParsingMode.ALL, DeclarationParsingMode.TOPLEVEL, DeclarationParsingMode.LOCAL)
    ) {
        /**
         * 实现 `doParse` 的仓颉语法解析协议回调，保持与 IntelliJ PSI 访问契约一致。
         */
        context(parseContext: ParsingContext)
        override fun doParse(
            parser: CangJieParsing,
            detector: ModifierDetector,
            nameParsingMode: NameParsingMode,
            scope: DeclarationParsingMode
        ): IElementType? {
            return parser.parseClass(
                detector,
                enclosingDeclarationKeyword = if (scope == DeclarationParsingMode.LOCAL) FUNC_KEYWORD else null,
            )
        }


    }

    /**
     * 表示 `MacroParser`，承载仓颉语法解析中的语法节点、索引桩或辅助模型。
     */
    private class MacroParser : DeclarationParser {
        /**
         * 实现 `isValidInScope` 的仓颉语法解析协议回调，保持与 IntelliJ PSI 访问契约一致。
         */
        override fun isValidInScope(scope: DeclarationParsingMode): Boolean = true

        /**
         * 实现 `parse` 的仓颉语法解析协议回调，保持与 IntelliJ PSI 访问契约一致。
         */
        context(parseContext: ParsingContext)
        override fun parse(
            parser: CangJieParsing,
            detector: ModifierDetector,
            nameParsingMode: NameParsingMode,
            scope: DeclarationParsingMode
        ): IElementType {
            return with(parseContext.copy(disableMacroParsing = false, allowParseAnnotationsInValueParameter = false)) {
                parser.parseMacro(
                    enclosingDeclarationKeyword = if (scope == DeclarationParsingMode.LOCAL) FUNC_KEYWORD else null,
                    detector = detector,
                )
            }

        }
    }

    /**
     * 表示 `FunctionParser`，承载仓颉语法解析中的语法节点、索引桩或辅助模型。
     */
    private class FunctionParser : DeclarationParser {
        /**
         * 实现 `isValidInScope` 的仓颉语法解析协议回调，保持与 IntelliJ PSI 访问契约一致。
         */
        override fun isValidInScope(scope: DeclarationParsingMode): Boolean = true

        /**
         * 实现 `parse` 的仓颉语法解析协议回调，保持与 IntelliJ PSI 访问契约一致。
         */
        context(parseContext: ParsingContext)
        override fun parse(
            parser: CangJieParsing,
            detector: ModifierDetector,
            nameParsingMode: NameParsingMode,
            scope: DeclarationParsingMode
        ): IElementType? {
            return parser.parseFunction(detector = detector)
        }
    }

    /**
     * 表示 `VariableParser`，承载仓颉语法解析中的语法节点、索引桩或辅助模型。
     */
    private class VariableParser : DeclarationParser {
        /**
         * 实现 `isValidInScope` 的仓颉语法解析协议回调，保持与 IntelliJ PSI 访问契约一致。
         */
        override fun isValidInScope(scope: DeclarationParsingMode): Boolean =
            scope != DeclarationParsingMode.MEMBER // 成员字段由 FieldParser 处理

        /**
         * 实现 `parse` 的仓颉语法解析协议回调，保持与 IntelliJ PSI 访问契约一致。
         */
        context(parseContext: ParsingContext)
        override fun parse(
            parser: CangJieParsing,
            detector: ModifierDetector,
            nameParsingMode: NameParsingMode,
            scope: DeclarationParsingMode
        ): IElementType? {
            return parser.parseVariable(detector)
        }
    }

    /**
     * 表示 `FieldParser`，承载仓颉语法解析中的语法节点、索引桩或辅助模型。
     */
    private class FieldParser : DeclarationParser {
        /**
         * 实现 `isValidInScope` 的仓颉语法解析协议回调，保持与 IntelliJ PSI 访问契约一致。
         */
        override fun isValidInScope(scope: DeclarationParsingMode): Boolean =
            scope == DeclarationParsingMode.MEMBER

        /**
         * 实现 `parse` 的仓颉语法解析协议回调，保持与 IntelliJ PSI 访问契约一致。
         */
        context(parseContext: ParsingContext)
        override fun parse(
            parser: CangJieParsing,
            detector: ModifierDetector,
            nameParsingMode: NameParsingMode,
            scope: DeclarationParsingMode
        ): IElementType? {
            return parser.parseField(detector)
        }
    }

    /**
     * 表示 `MacroExpressionParser`，承载仓颉语法解析中的语法节点、索引桩或辅助模型。
     */
    private class MacroExpressionParser(private val expressionParsing: CangJieExpressionParsing) : DeclarationParser {
        /**
         * 实现 `isValidInScope` 的仓颉语法解析协议回调，保持与 IntelliJ PSI 访问契约一致。
         */
        override fun isValidInScope(scope: DeclarationParsingMode): Boolean = true

        /**
         * 实现 `parse` 的仓颉语法解析协议回调，保持与 IntelliJ PSI 访问契约一致。
         */
        context(parseContext: ParsingContext)
        override fun parse(
            parser: CangJieParsing,
            detector: ModifierDetector,
            nameParsingMode: NameParsingMode,
            scope: DeclarationParsingMode
        ): IElementType? {


            return with(parseContext.copy(backToken = true)) {
                val nativeScope = when (scope) {
                    DeclarationParsingMode.TOPLEVEL -> CjScopeKind.TOPLEVEL
                    DeclarationParsingMode.MEMBER -> checkNotNull(parseContext.memberScope).declarationScope
                    DeclarationParsingMode.LOCAL -> parseContext.statementScope
                    DeclarationParsingMode.ENUM_CONSTRUCTOR -> CjScopeKind.ENUM_CONSTRUCTOR
                    DeclarationParsingMode.ALL -> CjScopeKind.UNKNOWN_SCOPE
                    DeclarationParsingMode.MEMBER_OR_TOPLEVEL -> kotlin.error("A macro declaration requires its actual declaration scope.")
                }
                expressionParsing.parseMacroExpression(nativeScope, scope, detector.declarationPrefix())
            }
        }
    }

    /**
     * 保存 `declarationParsers` 的内部状态，供仓颉语法解析实现维护节点缓存或解析上下文。
     */
    private val declarationParsers: Map<Int, DeclarationParser> by lazy {
        mapOf(
            TYPE_KEYWORD_Id to TypeAliasParser(),
            AT_Id to MacroExpressionParser(expressionParsing),
            MACRO_KEYWORD_Id to MacroParser(),
            FUNC_KEYWORD_Id to FunctionParser(),
            MAIN_KEYWORD_Id to MainFuncParser(),
            EXTEND_KEYWORD_Id to ClassParser(),
            ENUM_KEYWORD_Id to ClassParser(),
            STRUCT_KEYWORD_Id to ClassParser(),
            INTERFACE_KEYWORD_Id to ClassParser(),
            CLASS_KEYWORD_Id to ClassParser(),
            LET_KEYWORD_Id to VariableParser(),
            VAR_KEYWORD_Id to VariableParser(),
            CONST_KEYWORD_Id to VariableParser()
        )
    }

    /**
     * 解析通用声明
     *
     * 根据令牌类型选择合适的解析器进行声明解析。
     *
     * @param detector 修饰符检测器
     * @param nameParsingMode 名称解析模式
     * @param declarationParsingMode 声明解析模式
     * @return 解析结果的节点类型
     */
    context(parseContext: ParsingContext)
    fun parseCommonDeclaration(
        detector: ModifierDetector, nameParsingMode: NameParsingMode, declarationParsingMode: DeclarationParsingMode
    ): IElementType? {
        val tokenId = getTokenId() ?: return null
        val parser = declarationParsers[tokenId] ?: return null
//        先处理
        val type = parser.parse(this, detector, nameParsingMode, declarationParsingMode)
//        TODO 先处理还是先返回INVALID_DECLARATION？
        if (!parser.isValidInScope(declarationParsingMode)) {

            return INVALID_DECLARATION
        }


        return type

    }


    /**
     * 解析类型别名
     *
     * Grammar:
     * ```
     * typeAliasDeclaration
     *   : modifier* "type" simpleName typeParameters? "=" typeReference
     *   ;
     * ```
     *
     * @return 解析结果的节点类型
     */
    context(parseContext: ParsingContext)
    private fun parseTypeAlias(detector: ModifierDetector? = null): IElementType {
        assert(_at(TYPE_KEYWORD))

        val declarationAnchor = builder.currentOffset
        advance() // TYPE_KEYWORD
        checkCjmpDeclarationFileRole(detector, TYPEALIAS, declarationAnchor)

        expect(IDENTIFIER, "Type name expected", LT_EQ_SEMICOLON_TOP_LEVEL_DECLARATION_FIRST_SET)

        parseCjmpTypeParameterList(TYPE_PARAMETER_GT_RECOVERY_SET, detector)

        if (at(WHERE_KEYWORD)) {
            val error = mark()
            parseTypeConstraints()
            error.error(CangJieParsingBundle.message("parsing.error.not.allowed", "Type alias parameters bounds"))
        }

        expect(EQ, "Expecting '='", TOP_LEVEL_DECLARATION_FIRST_SEMICOLON_SET)

        parseTypeRef()

        consumeIf(SEMICOLON)
        reportUnsupportedGenericDeclaration(detector)

        return TYPEALIAS
    }


    /**
     * 解析属性声明
     *
     * Grammar:
     * ```
     * propertyDeclaration
     *   : modifier* "prop" simpleName ":" typeReference propertyBody?
     *   ;
     * ```
     *
     * @param isInterface 是否在接口中
     * @param classdetector 类修饰符检测器
     * @param detector 修饰符检测器
     * @return 解析结果的节点类型
     */
    context(parseContext: ParsingContext)
    fun parseProperty(
        isInterface: Boolean = false, classdetector: ModifierDetector? = null, detector: ModifierDetector? = null
    ): IElementType {
        assert(_at(PROP_KEYWORD))

        val declarationAnchor = builder.currentOffset
        // advance past 'prop' keyword
        advance()
        val propertyNameStartOffset = builder.currentOffset

        detector?.cjmpDeclarationName = builder.tokenText
        if (!parseIdentifierByTitle("property", EOL_OR_SEMICOLON_RBRACE_SET)) {
            skipUntil(EOL_OR_SEMICOLON_RBRACE_SET)
            detector?.clear()
            detector?.cjmpSkipModifierChecks = true
            builder.macroParsingJournal.registerDeclarationRoleCheck(declarationAnchor, CjmpDeclarationRoleCheck.NOT_CHECKED)
            builder.macroParsingJournal.registerDeclarationModifiers(declarationAnchor,
                CjParsedDeclarationModifiers(builder.macroParsingJournal.rootIdentity, cjmpContext, emptySet(), false,
                    builder.macroParsingJournal.sourceTokenAt(declarationAnchor), CjmpModifierDeclarationKind.PROPERTY,
                    parseContext.statementScope))
            return PROPERTY
        }

        parseByType()
        checkCjmpDeclarationFileRole(detector, PROPERTY, declarationAnchor)

        if (at(LBRACE)) {
            parsePropertyBody(detector, propertyNameStartOffset)
        } else if (!isInterface) {
            if (classdetector != null && !classdetector.isAbstractDetected) {
                // 只抑制"缺体"诊断；"无 {} 即 abstract"的判定（R4）不在这里，保持原样。
                reportMissingBody {
                    error(CangJieParsingBundle.message("parsing.error.unimplemented.abstract.property"))
                    error(CangJieParsingBundle.message("parsing.error.missing", "prop body. Expecting '{''"))
                }
            }
        }

        return PROPERTY
    }


    /**
     * 解析类型注解
     *
     * Grammar:
     * ```
     * typeAnnotation
     *   : ":" typeReference
     *   ;
     * ```
     */
    context(parseContext: ParsingContext)
    private fun parseByType() {
        if (at(COLON)) {
            advance() // COLON
            parseTypeRef()
        } else {
            error(CangJieParsingBundle.message("parsing.error.missing", "type. Expecting ':' type"))
        }
    }


    /**
     * 解析属性 getter 访问器
     *
     * Grammar:
     * ```
     * propertyGetter
     *   : "get" "(" ")" block
     *   ;
     * ```
     */
    context(parseContext: ParsingContext)
    private fun parsePropertyGet() {
        assert(_at(GET_KEYWORD))

        advance() // GET_KEYWORD

        if (at(LPAR)) {
            do {
                parseValueParameterList(CjParameterGrammar.ACCESSOR_NAME_ONLY, ACCESSOR_PARAMETERS_FOLLOW_SET)
            } while (at(LPAR))
        } else {
            error(CangJieParsingBundle.message("parsing.error.expecting.symbol", "("))
        }

        if (at(LBRACE)) {
            parseBlock()
        } else {
            reportMissingBody()
        }
    }


    /**
     * 解析属性 setter 访问器
     *
     * Grammar:
     * ```
     * propertySetter
     *   : "set" "(" identifier ")" block
     *   ;
     *
     * setter 参数只需名字，类型由 prop 声明类型决定（对齐官方 PROP_MEMBER_SETTER_BODY）。
     * ```
     *
     */
    context(parseContext: ParsingContext)
    private fun parsePropertySet() {
        assert(_at(SET_KEYWORD))

        advance() // SET_KEYWORD

        if (at(LPAR)) {
            do {
                parseValueParameterList(CjParameterGrammar.ACCESSOR_NAME_ONLY, ACCESSOR_PARAMETERS_FOLLOW_SET)
            } while (at(LPAR))
        } else {
            error(CangJieParsingBundle.message("parsing.error.expecting.symbol", "("))
        }

        if (at(LBRACE)) {
            parseBlock()
        } else {
            reportMissingBody()
        }
    }


    /**
     * 解析属性体
     *
     * Grammar:
     * ```
     * propertyBody
     *   : "{" propertyAccessor* "}"
     *   ;
     *
     * propertyAccessor
     *   : annotation* modifier* (propertyGetter | propertySetter)
     *   ;
     * ```
     *
     * @param detector 修饰符检测器
     */
    context(parseContext: ParsingContext)
    private fun parsePropertyBody(detector: ModifierDetector?, propertyNameStartOffset: Int) {
        assert(_at(LBRACE))

        val body = mark()
        advance() // LBRACE
        val previousAccessorRanges = mutableMapOf<IElementType, ParsingDiagnosticHint>()

        while (!eof() && !at(RBRACE)) {
            if (!parsePropertyAccessor(detector, propertyNameStartOffset, previousAccessorRanges)) {
                consumeUntilAny(TokenSet.create(NL, SEMICOLON))
            } else {
                while (at(SEMICOLON)) advance()
            }
        }

        expect(RBRACE, "Expecting '}'")

        body.done(PROPERTY_BODY)
    }


    context(parseContext: ParsingContext)
    private fun parsePropertyAccessor(
        detector: ModifierDetector?,
        propertyNameStartOffset: Int,
        previousAccessorRanges: MutableMap<IElementType, ParsingDiagnosticHint>,
    ): Boolean {
        val accessor = mark()
        val accessorDetector = ModifierDetector()
        val annotations = mutableListOf<CjmpParsedAnnotationFacts>()
        parseAnnotations(accessorDetector, annotations::add)
        parseModifierListInternal(
            accessorDetector,
            TokenSet.EMPTY,
            parseConstBeforePropertyAccessor = true,
            modifierKeywords = PROPERTY_ACCESSOR_MODIFIER_KEYWORDS,
        )

        val accessorKeyword = when {
            at(GET_KEYWORD) -> GET_KEYWORD
            at(SET_KEYWORD) -> SET_KEYWORD
            else -> null
        }
        val accessorKeywordStartOffset = accessorKeyword?.let { builder.currentOffset }
        val accessorNextTokenOffset = accessorKeyword?.let { nextLogicalTokenStartOffset() }
        val firstModifierOffset = accessorDetector.parsedModifierOccurrences
            .minOfOrNull { it.startOffset } ?: accessorNextTokenOffset
        if (accessorKeyword == GET_KEYWORD) {
            detector?.cjmpHasPropertyAccessor = true
            reportDuplicatedPropertyAccessor(
                GET_KEYWORD,
                checkNotNull(accessorKeywordStartOffset),
                previousAccessorRanges,
                propertyNameStartOffset,
            )
            reportPropertyAccessorModifier(
                accessorDetector,
                GET_KEYWORD,
                checkNotNull(accessorNextTokenOffset),
            )
            parsePropertyGet()
            reportPropertyAccessorAnnotations(
                annotations,
                GET_KEYWORD,
                checkNotNull(firstModifierOffset),
            )
            rememberPropertyAccessorRange(
                GET_KEYWORD,
                checkNotNull(firstModifierOffset),
                previousAccessorRanges,
            )
        } else if (accessorKeyword == SET_KEYWORD) {
            detector?.cjmpHasPropertyAccessor = true
            reportDuplicatedPropertyAccessor(
                SET_KEYWORD,
                checkNotNull(accessorKeywordStartOffset),
                previousAccessorRanges,
                propertyNameStartOffset,
            )
            reportPropertyAccessorModifier(
                accessorDetector,
                SET_KEYWORD,
                checkNotNull(accessorNextTokenOffset),
            )
            parsePropertySet()
            reportPropertyAccessorAnnotations(
                annotations,
                SET_KEYWORD,
                checkNotNull(firstModifierOffset),
            )
            rememberPropertyAccessorRange(
                SET_KEYWORD,
                checkNotNull(firstModifierOffset),
                previousAccessorRanges,
            )
        } else {
            errorOnCurrentToken(
                CangJieParsingBundle.message(
                    "parsing.error.expected.get.or.set.in.prop",
                    builder.tokenText?.toString() ?: CangJieParsingBundle.message("parsing.token.end.of.file"),
                ),
                hints = listOf(
                    ParsingDiagnosticHint(
                        propertyNameStartOffset,
                        (propertyNameStartOffset + PROP_KEYWORD.value.length).coerceAtMost(builder.originalText.length),
                    ),
                ),
            )
            accessor.drop()
            return false
        }

        accessor.done(PROPERTY_ACCESSOR)
        return true
    }

    context(parseContext: ParsingContext)
    private fun reportDuplicatedPropertyAccessor(
        accessorKeyword: IElementType,
        accessorKeywordStartOffset: Int,
        previousAccessorRanges: Map<IElementType, ParsingDiagnosticHint>,
        propertyNameStartOffset: Int,
    ) {
        val previousAccessorRange = previousAccessorRanges[accessorKeyword] ?: return
        val accessorName = checkNotNull(accessorKeyword as? CjKeywordToken).value
        errorOnRange(
            accessorKeywordStartOffset,
            accessorKeywordStartOffset + accessorName.length,
            CangJieParsingBundle.message("parsing.error.duplicated.get.or.set", accessorName),
            hints = listOf(
                previousAccessorRange.copy(label = CangJieParsingBundle.message("parsing.error.hint.previous.one").toString()),
                ParsingDiagnosticHint(
                    propertyNameStartOffset,
                    (propertyNameStartOffset + PROP_KEYWORD.value.length).coerceAtMost(builder.originalText.length),
                ),
            ),
        )
    }

    private fun rememberPropertyAccessorRange(
        accessorKeyword: IElementType,
        accessorRangeStartOffset: Int,
        previousAccessorRanges: MutableMap<IElementType, ParsingDiagnosticHint>,
    ) {
        val lastConsumedEndOffset = lastSignificantTokenEndOffset()
        val accessorRangeEndOffset = if (lastConsumedEndOffset >= accessorRangeStartOffset) {
            lastConsumedEndOffset
        } else {
            builder.currentOffset
        }
        previousAccessorRanges[accessorKeyword] = ParsingDiagnosticHint(
            accessorRangeStartOffset,
            accessorRangeEndOffset,
        )
    }

    context(parseContext: ParsingContext)
    private fun reportPropertyAccessorAnnotations(
        annotations: List<CjmpParsedAnnotationFacts>,
        accessorKeyword: IElementType,
        accessorHintOffset: Int,
    ) {
        val accessorName = checkNotNull(accessorKeyword as? CjKeywordToken).value
        for (annotation in annotations) {
            if (accessorKeyword == SET_KEYWORD && annotation.kind == BuiltInAnnotationKind.DEPRECATED) continue
            reportUnexpectedAnnotation(annotation, accessorName, accessorHintOffset, accessorName.length)
        }
    }

    /** 对位 DiagUnexpectedAnnoOn；注解自身与被注解的语法关键字分别保留精确范围。 */
    context(parseContext: ParsingContext)
    private fun reportUnexpectedAnnotation(annotation: CjmpParsedAnnotationFacts, target: String, targetOffset: Int, targetWidth: Int) {
        errorOnSourceRange(
            annotation.sourceFact.origin, annotation.sourceFact.sourceRange,
            CangJieParsingBundle.message("parsing.error.unexpected.annotation.on", "@${annotation.sourceName}", target),
            hints = listOf(ParsingDiagnosticHint(
                targetOffset, (targetOffset + targetWidth).coerceAtMost(builder.originalText.length),
                CangJieParsingBundle.message("parsing.error.hint.on", target).toString(),
            )),
        )
    }

    context(parseContext: ParsingContext)
    private fun reportPropertyAccessorModifier(
        detector: ModifierDetector,
        accessorKeyword: IElementType,
        accessorHintOffset: Int,
    ) {
        val accessorDescription = when (accessorKeyword) {
            GET_KEYWORD -> CangJieParsingBundle.message("parsing.accessor.getter.function").toString()
            SET_KEYWORD -> CangJieParsingBundle.message("parsing.accessor.setter.function").toString()
            else -> error("Unexpected property accessor keyword: $accessorKeyword")
        }
        val modifier = PARAMETER_MODIFIERS_IN_TOKEN_ORDER.firstNotNullOfOrNull { token ->
            detector.parsedModifierOccurrences.firstOrNull { it.token == token }
        } ?: return
        val accessorName = checkNotNull(accessorKeyword as? CjKeywordToken).value
        errorOnRange(
            modifier.startOffset,
            modifier.endOffset,
            CangJieParsingBundle.message(
                "parsing.error.expected.no.modifier.before",
                accessorDescription,
                modifier.token.value,
            ),
            hints = listOf(
                ParsingDiagnosticHint(
                    accessorHintOffset,
                    (accessorHintOffset + accessorName.length).coerceAtMost(builder.originalText.length),
                    CangJieParsingBundle.message("parsing.error.hint.before", accessorDescription).toString(),
                ),
            ),
        )
    }


    /**
     * 解析枚举类型
     *
     * Grammar:
     * ```
     * enumDeclaration
     *   : modifier* "enum" simpleName "{" "|" enumEntry ("|" enumEntry)* memberDeclaration* "}"
     *   ;
     * ```
     *
     * @return 解析结果的节点类型
     */
    context(parseContext: ParsingContext)
    private fun parseEnum(): IElementType {
        assert(_at(ENUM_KEYWORD))
        advance()

        parseIdentifierByTitle("enum", IDENTIFIER_RBRACKET_LBRACKET_SET, false)

        parseEnumBody()

        return ENUM
    }


    context(parseContext: ParsingContext)
    private fun parseEnumBody(cjmpFacts: CjmpNominalParseFacts? = null) {
        val body = mark()
        if (at(LBRACE)) {
            advance() // LBRACE

            // 官方 ParseEnumBody 的首个竖线可省略；构造项自身按 ENUM_CONSTRUCTOR 文法解析。
            if (at(OR)) advance()

            if (isEnumConstructorStart()) {
                with(parseContext.copy(statementScope = CjScopeKind.ENUM_CONSTRUCTOR)) { parseEnumList() }
            } else {
                error(CangJieParsingBundle.message("parsing.error.expecting.element", "enum constructor"))
            }

            parseMembers(ENUM_KEYWORD_Id, null, cjmpFacts)

            expect(RBRACE, "Expecting '}'")
        } else {
            error(CangJieParsingBundle.message("parsing.error.expecting.symbol", "{"))
        }
        body.done(ENUM_BODY)
    }


    context(parseContext: ParsingContext)
    private fun parseEnumList() {
        while (true) {
            // 检查是否为非穷举枚举的省略号
            if (at(ELLIPSIS)) {
                advance() // ELLIPSIS
                // 省略号必须是最后一个构造器
                break
            }

            if (!parseEnumEntry()) break
            when {
                at(RBRACE) -> break
                at(OR) -> advance()
                else -> break
            }
        }
    }

    /**
     * 判断当前位置能否开始枚举构造项。
     *
     * annotation 与 modifier 都属于随后 [ENUM_CONSTRUCTOR] 的声明前缀；首项和 `|` 后续项
     * 必须使用同一判定，不能把前缀留在 [ENUM_BODY] 上再由 raw builder 猜测 owner。
     */
    context(parseContext: ParsingContext)
    private fun isEnumConstructorStart(): Boolean =
        at(IDENTIFIER) || at(ELLIPSIS) || _atSet(AT, ATEXCL) || _atSet(MODIFIER_KEYWORDS)

    context(parseContext: ParsingContext)
    fun parseTypeCodeFragment() {
        val marker = mark()
        parseTypeRef()

        checkForUnexpectedSymbols()

        marker.done(TYPE_CODE_FRAGMENT)
    }


    /**
     * 检查意外符号
     *
     * 在代码片段解析中检查是否有意外的符号。
     */
    context(parseContext: ParsingContext)
    private fun checkForUnexpectedSymbols() {
        while (!eof()) {
            errorAndAdvance(CangJieParsingBundle.message("parsing.error.unexpected", "symbol"))
        }
    }


    /**
     * 解析枚举项
     *
     * Grammar:
     * ```
     * enumEntry
     *   : annotations modifiers simpleName ("(" typeList ")" )?
     *   ;
     * ```
     *
     * @return 是否成功解析
     */
    context(parseContext: ParsingContext)
    private fun parseEnumEntry(): Boolean {
        val entry = mark()

        val detector = ModifierDetector()
        parseAnnotations(detector)
        parseModifierList(detector, TokenSet.EMPTY)

        // 普通 @ 仍保留宏包装，但输入声明必须沿用枚举构造项作用域。
        // payload 的圆括号是类型列表，不能被通用宏输入误解析为类的主构造函数。
        if (!parseContext.disableMacroParsing && at(AT)) {
            val macroStart = builder.currentOffset
            val macro = with(parseContext.copy(backToken = true)) {
                expressionParsing.parseMacroInvocation(CjScopeKind.ENUM_CONSTRUCTOR, DeclarationParsingMode.ENUM_CONSTRUCTOR)
            }
            if (macro.expression.root == CangJieExpressionParsing.ParsedExpression.Missing) {
                entry.drop()
                return false
            }
            if (!macro.isEnumConstructor) errorOnRange(
                macroStart, lastSignificantTokenEndOffset(),
                CangJieParsingBundle.message("parsing.error.unknown.enum.constructor"),
            )
            closeDeclarationWithCommentBinders(entry, MACRO_EXPRESSION, true)
            return true
        }

        val success = parseEnumConstructorRest(detector)
        closeDeclarationWithCommentBinders(entry, if (success) ENUM_CONSTRUCTOR else INVALID_DECLARATION, true)
        return success
    }

    /** 解析枚举构造项的名称和 payload；直接条目与宏输入共用同一语法入口。 */
    context(parseContext: ParsingContext)
    fun parseEnumConstructorRest(detector: ModifierDetector? = null): Boolean {
        val declarationAnchor = builder.currentOffset
        if (!expect(IDENTIFIER, "Expecting enum constructor name")) {
            return false
        }
        // 枚举项以实际名字为来源锚点；缺名只发布语法错误/InvalidDecl，不授予闭括号或 EOF 身份。
        checkCjmpDeclarationFileRole(detector, ENUM_CONSTRUCTOR, declarationAnchor)

        if (at(LPAR)) {
            advance() // LPAR
            parseTypeList()
            expect(RPAR, "Expecting ')'")
        }

        return true
    }

    /**
     * 解析类型列表
     *
     * Grammar:
     * ```
     * typeList
     *   : typeReference ("," typeReference)*
     *   ;
     * ```
     */
    context(parseContext: ParsingContext)
    fun parseTypeList() {
        val list = mark()

        while (true) {
            parseTypeRef()
            if (!at(COMMA)) break
            advance() // COMMA
        }

        list.done(TYPE_LIST)
    }


    /**
     * 解析类型参数列表
     *
     * Grammar:
     * ```
     * typeParameterList
     *   : "<" typeParameter ("," typeParameter)* ">"
     *   ;
     * ```
     *
     * @param recoverySet 恢复伤口的token集合
     * @return 是否成功解析
     */
    context(parseContext: ParsingContext)
    private fun parseTypeParameterList(recoverySet: TokenSet): Boolean {
        var result = false
        if (at(LT)) {
            val list = mark()

            builder.disableNewlines()
            advance() // LT

            while (true) {
                if (at(COMMA)) errorAndAdvance(
                    CangJieParsingBundle.message(
                        "parsing.error.expecting.element", "type parameter declaration"
                    )
                )
                parseTypeParameter()
//          parseTypeRef(true)

                if (!at(COMMA)) break
                advance() // COMMA
                if (at(GT)) {
                    break
                }
            }

            expect(GT, "Missing '>'", recoverySet)
            builder.restoreNewlinesState()
            result = true

            list.done(TYPE_PARAMETER_LIST)
        }
        return result
    }


    /**
     * 解析类型参数
     *
     * Grammar:
     * ```
     * typeParameter
     *   : modifier* simpleName (":" userType)?
     *   ;
     * ```
     */
    context(parseContext: ParsingContext)
    private fun parseTypeParameter() {
        if (atSet(TYPE_PARAMETER_GT_RECOVERY_SET)) {
            error(CangJieParsingBundle.message("parsing.error.type.parameter.declaration.expected"))
            return
        }

        val mark = mark()

//  parseModifierList(GT_COMMA_COLON_SET)

        expect(IDENTIFIER, "Type parameter name expected", TokenSet.EMPTY)

//  if (at(COLON)) {
//      advance() // COLON
//      parseTypeRef()
//  }

        mark.done(TYPE_PARAMETER)
    }


    /**
     * 解析继承说明符
     *
     * Grammar:
     * ```
     * delegationSpecifier
     *   : typeReference
     *   ;
     * ```
     */
    context(parseContext: ParsingContext)
    private fun parseDelegationSpecifier() {
        val delegator = mark()
        val reference = mark()
        parseTypeRef()

        reference.drop()
        delegator.done(SUPER_TYPE_ENTRY)
    }


    /**
     * 解析继承说明符列表
     *
     * Grammar:
     * ```
     * delegationSpecifierList
     *   : delegationSpecifier ("&" delegationSpecifier)*
     *   ;
     * ```
     */
    context(parseContext: ParsingContext)
    private fun parseDelegationSpecifierList() {
        val list = mark()

        while (true) {
            if (at(AND)) {
                errorAndAdvance(CangJieParsingBundle.message("parsing.error.expecting.delegation.specifier"))
                continue
            }
            parseDelegationSpecifier()
            if (!at(AND)) break
            advance() // COMMA
        }

        list.done(SUPER_TYPE_LIST)
    }

    /**
     * (modifier)*
     */
    context(parseContext: ParsingContext)
    fun parseModifierList(noModifiersBefore: TokenSet): Boolean {
        return parseModifierList(null, noModifiersBefore)
    }

    /**
     * 解析类声明
     *
     * Grammar:
     * ```
     * classDeclaration
     *   : modifier* ("class" | "interface" | "struct" | "enum" | "extend")
     *     (typeParameters)? simpleName? ("<:" delegationSpecifierList)?
     *     typeConstraints? classBody?
     *   ;
     * ```
     *
     * @param detector 修饰符检测器
     * @return 解析结果的节点类型
     */
    context(parseContext: ParsingContext)
    fun parseClass(
        detector: ModifierDetector,
        nameParsingMode: NameParsingMode = NameParsingMode.REQUIRED,
        enclosingDeclarationKeyword: CjKeywordToken? = null,
    ): IElementType {
        val tokenId = getTokenId()
        val token = builder.tokenType
        val nominalKeywordOffset = builder.currentOffset
        val declarationType = when (tokenId) {
            CLASS_KEYWORD_Id -> CLASS
            INTERFACE_KEYWORD_Id -> INTERFACE
            STRUCT_KEYWORD_Id -> STRUCT
            ENUM_KEYWORD_Id -> ENUM
            EXTEND_KEYWORD_Id -> EXTEND
            else -> kotlin.error("Unexpected nominal declaration keyword: $tokenId")
        }

        // assert _atSet(CLASS_INTERFACE_STRUCT_ENUM_EXTEND_SET)
        assert(_atSet(CLASS_INTERFACE_STRUCT_ENUM_EXTEND_SET))
        // 保留关键字边界，延后检查时也不能把签名或整个声明包进 ERROR。
        val scopeError = mark()
        advance()
        val keywordEnd = mark()
        fun completeScopeCheck(reportScopeError: Boolean) {
            if (enclosingDeclarationKeyword != null && reportScopeError && parseContext.shouldReportError) {
                scopeError.errorBefore(CangJieParsingBundle.message(
                    "parsing.error.invalid.declaration.in.class",
                    (token as CjKeywordToken).value,
                    CangJieParsingBundle.message("parsing.scope.${enclosingDeclarationKeyword.value}"),
                ), keywordEnd)
            } else {
                scopeError.drop()
            }
            keywordEnd.drop()
        }

        // 官方 class/interface 在签名后检查；enum/struct/extend 在名称或目标类型前检查。
        val checkScopeAfterSignature = token == CLASS_KEYWORD || token == INTERFACE_KEYWORD
        if (!checkScopeAfterSignature) completeScopeCheck(reportScopeError = true)
        if (!checkScopeAfterSignature) checkCjmpDeclarationFileRole(detector, declarationType, nominalKeywordOffset)
        var typeParametersDeclared = false
        var hasIdentifier = false
        var declarationNameRange: Pair<Int, Int>? = null
        val declarationName = if (token != EXTEND_KEYWORD && at(IDENTIFIER)) builder.tokenText else null
        detector.cjmpDeclarationName = declarationName

        if (token == EXTEND_KEYWORD) {
            if (at(LT)) {
                parseCjmpTypeParameterList(TYPE_PARAMETER_GT_RECOVERY_SET, detector)
                typeParametersDeclared = true
            }
            parseTypeRef()
        } else {

            // enum/struct 已由 scope 检查置 owner HAS_BROKEN，官方不再报告其缺名错误。
            val reportIdentifierError = enclosingDeclarationKeyword == null || checkScopeAfterSignature
            hasIdentifier = if (reportIdentifierError) {
                val nameStart = builder.currentOffset
                val nameWidth = builder.tokenText?.length
                parseIdentifier(recoverySet = CLASS_NAME_RECOVERY_SET)
                    .also { parsed -> if (parsed && nameWidth != null) declarationNameRange = nameStart to (nameStart + nameWidth) }
            } else if (at(IDENTIFIER)) {
                // HAS_BROKEN 只抑制缺名诊断；失败时保留 token，交给后续文法恢复。
                val nameStart = builder.currentOffset
                val nameWidth = builder.tokenText?.length
                advance()
                if (nameWidth != null) declarationNameRange = nameStart to (nameStart + nameWidth)
                true
            } else {
                false
            }


            typeParametersDeclared = parseCjmpTypeParameterList(TYPE_PARAMETER_GT_RECOVERY_SET, detector)
        }

        detector.cjmpRulesShortCircuited = typeParametersDeclared && detector.hasFrozenAnnotation() &&
            (detector.isCommonDetected || detector.isSpecificDetected)


        if (at(LTCOLON)) {
            advance() // COLON
            parseDelegationSpecifierList()
        }

        val whereMarker = OptionalMarker(false)
        parseTypeConstraintsGuarded(typeParametersDeclared)
        whereMarker.error(CangJieParsingBundle.message("parsing.error.where.clause.not.allowed"))

        // 缺名称破坏 nominal 本身；泛型、继承和约束的错误不传播为 owner HAS_BROKEN。
        if (checkScopeAfterSignature) completeScopeCheck(hasIdentifier)
        if (checkScopeAfterSignature) checkCjmpDeclarationFileRole(detector, declarationType, nominalKeywordOffset)

        if (at(LBRACE)) {
            val memberScope = CjMemberParsingContext(
                kind = when (tokenId) {
                    CLASS_KEYWORD_Id -> CjMemberParsingContext.Kind.CLASS
                    STRUCT_KEYWORD_Id -> CjMemberParsingContext.Kind.STRUCT
                    INTERFACE_KEYWORD_Id -> CjMemberParsingContext.Kind.INTERFACE
                    ENUM_KEYWORD_Id -> CjMemberParsingContext.Kind.ENUM
                    EXTEND_KEYWORD_Id -> CjMemberParsingContext.Kind.EXTEND
                    else -> kotlin.error("Unexpected nominal declaration keyword: $tokenId")
                },
                declarationName = declarationName,
                declarationOrigin = builder.macroParsingJournal.sourceOrigin(
                    nominalKeywordOffset, nominalKeywordOffset + (token as CjKeywordToken).value.length,
                ),
            )
            val primaryName = if (tokenId == CLASS_KEYWORD_Id || tokenId == STRUCT_KEYWORD_Id) {
                declarationName?.takeIf { it.isNotEmpty() }?.let {
                    val isRaw = it.startsWith('`') && it.endsWith('`')
                    CjPrimaryParsingName(if (isRaw) it.substring(1, it.length - 1) else it, isRaw)
                }
            } else parseContext.primaryDeclaration
            val cjmpFacts = CjmpNominalParseFacts(
                kind = memberScope.kind,
                declarationName = declarationName,
                isCommon = detector.isCommonDetected,
                isSpecific = detector.isSpecificDetected,
                modifierRulesEnabled = cjmpModifierRulesEnabled() && parseContext.shouldReportError &&
                    !detector.cjmpRulesShortCircuited,
            )
            val diagnosticOwner = CjParsingScopeOwner(CjParsingScopeOwner.Kind.valueOf(memberScope.kind.name),
                checkNotNull(memberScope.declarationOrigin))
            with(parseContext.copy(memberScope = memberScope, statementScope = memberScope.declarationScope, primaryDeclaration = primaryName,
                diagnosticScopeOwner = diagnosticOwner)) {
                when (tokenId) {
                    ENUM_KEYWORD_Id -> parseEnumBody(cjmpFacts)
                    else -> parseClassBody(tokenId, detector, cjmpFacts)
                }
            }
            finishCjmpNominalRules(cjmpFacts, declarationNameRange)
            builder.macroParsingJournal.registerNominal(
                nominalKeywordOffset,
                CjmpNominalSyntaxFacts(cjmpFacts.directMembers.map { member ->
                    CjmpDirectMemberSyntaxFacts(
                        member.declarationType, member.sourceCommon, member.sourceSpecific, member.sourcePlatform, member.hasFunctionBody,
                        member.hasPropertyAccessor, member.hasInitializer,
                    )
                }),
            )
        } else {
            builder.macroParsingJournal.registerNominal(nominalKeywordOffset, CjmpNominalSyntaxFacts(emptyList()))
            error(CangJieParsingBundle.message("parsing.error.expecting.brace.or.inherit")) // 应该为 '{' 或者继承
        }

        reportUnsupportedGenericDeclaration(detector)

        return declarationType
    }


    /**
     * typeConstraints
     *   : ("where" typeConstraint{","})?
     *   ;
     */
    context(parseContext: ParsingContext)
    private fun parseTypeConstraintsGuarded(
        typeParameterListOccurred: Boolean
    ) {
        if (!at(WHERE_KEYWORD)) return
        // 官方无泛型声明只报告 WHERE，不制造约束节点或吞掉后续声明体。
        if (!typeParameterListOccurred) {
            errorAndAdvance(CangJieParsingBundle.message("parsing.error.unexpected.where"))
            return
        }
        parseTypeConstraints()
    }


    context(parseContext: ParsingContext)
    private fun parseTypeConstraints(): Boolean {
        if (at(WHERE_KEYWORD)) {
            parseTypeConstraintList()
            return true
        }
        return false
    }


    /**
     * 解析类型约束列表
     *
     * Grammar:
     * ```
     * typeConstraintList
     *   : "where" typeConstraint ("," typeConstraint)*
     *   ;
     * ```
     */
    context(parseContext: ParsingContext)
    private fun parseTypeConstraintList() {
        assert(_at(WHERE_KEYWORD))

        advance() // WHERE_KEYWORD

        val list = mark()

        while (true) {
            if (at(COMMA)) errorAndAdvance(CangJieParsingBundle.message("parsing.error.type.constraint.expected"))
            parseTypeConstraint()
            if (!at(COMMA)) break
            advance() // COMMA
        }

        list.done(TYPE_CONSTRAINT_LIST)
    }


    /**
     * 解析类型约束
     *
     * Grammar:
     * ```
     * typeConstraint
     *   : simpleName "<:" typeReference ("&" typeReference)*
     *   ;
     * ```
     */
    context(parseContext: ParsingContext)
    private fun parseTypeConstraint() {
        val constraint = mark()

        val reference = mark()
        if (expect(IDENTIFIER, "Expecting type parameter name", LTCOLON_COMMA_LBRACE_RBRACE_TYPE_REF_FIRST_SET)) {
            reference.done(REFERENCE_EXPRESSION)
        } else {
            reference.drop()
        }

        expect(LTCOLON, "Expecting '<:' before the upper bound", LBRACE_RBRACE_TYPE_REF_FIRST_SET)

        do {
            if (at(AND)) advance()
            parseTypeRef()
        } while (at(AND))

        constraint.done(TYPE_CONSTRAINT)
    }


    /**
     * 解析成员声明
     *
     * @param tokenId 令牌 ID
     * @param classdetector 类修饰符检测器
     * @param rollbackMacro 是否回滚宏
     */
    context(parseContext: ParsingContext)
    private fun parseMemberDeclaration(
        tokenId: Int?,
        classdetector: ModifierDetector,
        rollbackMacro: Boolean = false,
        cjmpFacts: CjmpNominalParseFacts? = null,
    ) {
        if (at(SEMICOLON)) {
            advance() // SEMICOLON
            return
        }
        val decl = mark()


        val detector = ModifierDetector()
        parseAnnotations(detector)
        val memberStartOffset = builder.currentOffset
        parseModifierList(
            detector,
            TokenSet.EMPTY,
            isParseMacro = rollbackMacro,
            parseConstBeforePrimaryConstructor = true,
        )

        val declarationStart = builder.currentOffset
        val parsedDeclType = parseMemberDeclarationRest(tokenId, classdetector, detector)
        val memberEndOffset = lastSignificantTokenEndOffset()
        val declType = if (parsedDeclType == VARIABLE) {
            // ParseClassBody 等名义 body 只接收普通变量；ParseDecl/宏片段本身仍保留模式身份。
            errorOnRange(
                declarationStart, memberEndOffset,
                CangJieParsingBundle.message("parsing.error.invalid.declaration.scope"),
            )
            INVALID_DECLARATION
        } else parsedDeclType


        when {
            declType == null -> {
                // InvalidDecl 的原生恢复已消费其自身；不能在随后合法成员处再制造错误。
                if (builder.currentOffset == declarationStart) {
                    errorWithRecovery(
                        CangJieParsingBundle.message("parsing.error.expecting.member.declaration"), TokenSet.EMPTY
                    )
                }
                decl.drop()
            }

            else -> {
                if (declType != INVALID_DECLARATION) {
                    val memberFacts = CjmpDirectMemberFacts(
                        startOffset = memberStartOffset,
                        endOffset = memberEndOffset,
                        declarationType = declType,
                        declarationName = detector.cjmpDeclarationName,
                        isCommon = detector.isCommonDetected,
                        isSpecific = detector.isSpecificDetected,
                        sourceCommon = detector.parsedModifierOccurrences.any { it.token == COMMON_KEYWORD },
                        sourceSpecific = detector.parsedModifierOccurrences.any { it.token == SPECIFIC_KEYWORD },
                        sourcePlatform = detector.parsedModifierOccurrences.any { it.token == PLATFORM_KEYWORD },
                        hasFunctionBody = detector.cjmpHasFunctionBody,
                        hasPropertyAccessor = detector.cjmpHasPropertyAccessor,
                        hasInitializer = detector.cjmpHasInitializer,
                    )
                    if (cjmpModifierRulesEnabled() && detector.cjmpFileRoleAccepted &&
                        declType in setOf(PRIMARY_CONSTRUCTOR, SECONDARY_CONSTRUCTOR) && detector.isStaticDetected &&
                        (detector.isCommonDetected || detector.isSpecificDetected)
                    ) {
                        val role = when {
                            detector.isCommonDetected -> COMMON_KEYWORD.value
                            detector.isPlatformDetected -> PLATFORM_KEYWORD.value
                            else -> SPECIFIC_KEYWORD.value
                        }
                        errorOnRange(
                            memberStartOffset,
                            memberEndOffset,
                            CangJieParsingBundle.message("parsing.error.cjmp.static.init", role),
                        )
                    }

                    // 官方先完成成员自身的static-init检查，再由父容器检查直接成员；相同规范范围保留真实先报。
                    cjmpFacts?.let { facts ->
                        facts.directMembers += memberFacts
                        if (declType == PRIMARY_CONSTRUCTOR || declType == SECONDARY_CONSTRUCTOR) {
                            facts.hasDirectConstructor = true
                        }
                        if (facts.modifierRulesEnabled &&
                            checkCjmpMemberMismatch(memberFacts, facts.kind, facts.isCommon, facts.isSpecific)
                        ) {
                            facts.hasModifierMismatch = true
                        }
                    }

                    if (cjmpAbstractRuleEnabled() && tokenId in setOf(CLASS_KEYWORD_Id, INTERFACE_KEYWORD_Id) &&
                        detector.isAbstractDetected && !detector.isCommonDetected &&
                        !(classdetector.isAbstractDetected &&
                            (classdetector.isCommonDetected || classdetector.isSpecificDetected)) &&
                        !classdetector.hasJavaMirrorAnnotation()
                    ) {
                        val declarationKind = when (declType) {
                            FUNC -> CangJieParsingBundle.message("parsing.scope.func")
                            PROPERTY -> CangJieParsingBundle.message("parsing.scope.property")
                            else -> null
                        }
                        if (declarationKind != null) {
                            if (declType == PROPERTY) {
                                val diagnosticOffset = lastSignificantTokenEndOffset()
                                errorOnRange(
                                    diagnosticOffset,
                                    diagnosticOffset,
                                    CangJieParsingBundle.message(
                                        "parsing.error.cjmp.explicit.abstract.only", declarationKind,
                                    ),
                                )
                            } else {
                                errorOnRange(
                                    memberStartOffset,
                                    memberEndOffset,
                                    CangJieParsingBundle.message(
                                        "parsing.error.cjmp.explicit.abstract.only", declarationKind,
                                    ),
                                )
                            }
                        }
                    }
                    // 原生无体 abstract 方法在非 abstract CJMP class 中还会单独报告 modifier 的作用域错误。
                    // 此项与前面的显式 abstract 规则独立，不能用整个成员错误代替 token 报告。
                    if (cjmpAbstractRuleEnabled() && declType == FUNC && detector.isAbstractDetected &&
                        !detector.cjmpHasFunctionBody && tokenId in setOf(CLASS_KEYWORD_Id, INTERFACE_KEYWORD_Id) &&
                        !(tokenId == CLASS_KEYWORD_Id && classdetector.isAbstractDetected &&
                            (classdetector.isCommonDetected || classdetector.isSpecificDetected)) &&
                        !classdetector.hasJavaMirrorAnnotation()
                    ) {
                        val abstract = checkNotNull(detector.sourceModifierOccurrences.firstOrNull { it.token == ABSTRACT_KEYWORD })
                        reportIllegalModifierInScope(abstract, detector, FUNC, memberStartOffset)
                    }
                }
                closeDeclarationWithCommentBinders(decl, declType, true)
            }
        }
    }


    /**
     * 解析成员声明的剩余部分
     *
     * @param tokenId 令牌 ID
     * @param classdetector 类修饰符检测器
     * @param detector 修饰符检测器
     * @return 解析结果的节点类型
     */
    context(parseContext: ParsingContext)
    private fun parseMemberDeclarationRest(
        tokenId: Int?, classdetector: ModifierDetector, detector: ModifierDetector
    ): IElementType? {
        var declType = parseClassCommonDeclaration(tokenId, classdetector, detector)

        if (declType != null) return declType

        if (tokenId != null && at(TILDE)) {
            val tilde = mark()
            advance() // TILDE ~
            if (builder.newlineBeforeCurrentToken() || !at(INIT_KEYWORD)) {
                tilde.error(CangJieParsingBundle.message("parsing.error.expecting.member.declaration"))
                return INVALID_DECLARATION
            }
            val scopeError = if (tokenId != CLASS_KEYWORD_Id) tilde else null
            if (scopeError == null) {
                tilde.drop()
            }
            val declarationAnchor = builder.currentOffset
            advance() // INIT_KEYWORD
            scopeError?.error(CangJieParsingBundle.message("parsing.error.invalid.declaration.scope"))
            detector.cjmpDeclarationName = TILDE.value + INIT_KEYWORD.value
            parseInitFuncRest(declarationType = FINALIZER, detector = detector, declarationAnchor = declarationAnchor)
            return FINALIZER
        }

        if (tokenId != null && (tokenId != INTERFACE_KEYWORD_Id || at(INIT_KEYWORD))) {

            // 注意：修饰符已在 parseMemberDeclaration 中解析，这里不需要再调用 parseModifierList
            val primaryGrammar = primaryParameterGrammar()

            when {
                at(INIT_KEYWORD) -> {
                    val constructorStart = builder.currentOffset
                    parseInitFunc(detector)
                    declType = SECONDARY_CONSTRUCTOR
                    if (tokenId == INTERFACE_KEYWORD_Id) {
                        errorOnRange(
                            constructorStart,
                            constructorStart + INIT_KEYWORD.value.length,
                            CangJieParsingBundle.message("parsing.error.invalid.declaration.scope"),
                        )
                    }
                }

                at(LBRACE) -> {
                    error(CangJieParsingBundle.message("parsing.error.expecting.member.declaration"))
                    parseBlock()
                    declType = FUNC
                }

                seeingPrimaryConstructor() -> {
                    // 原生只在 class/struct 的直接成员接收层淘汰错名 primary；宏输入不经过此层。
                    val nameMatches = parsePrimaryInitFunc(checkNotNull(primaryGrammar), detector)
                    declType = if (nameMatches) PRIMARY_CONSTRUCTOR else INVALID_DECLARATION
                }

            }
        }

        if (declType == null && at(IDENTIFIER)) parseInvalidMemberDeclaration()
        return declType
    }

    /**
     * 声明宏的无括号 input 在名义 body 中复用普通成员解析器识别 finalizer。
     * 普通成员入口与宏 input 入口必须共享 `~init` 的作用域诊断和函数体文法，
     * 否则 `@Annotation ~init()` 会被拆成无 input 的宏与独立 finalizer。
     */
    context(parseContext: ParsingContext)
    internal fun parseMacroFinalizer(
        scope: CjScopeKind,
        detector: ModifierDetector,
    ): IElementType? {
        val enclosingDeclarationToken = when (scope) {
            CjScopeKind.CLASS_BODY -> CLASS_KEYWORD_Id
            CjScopeKind.STRUCT_BODY -> STRUCT_KEYWORD_Id
            CjScopeKind.INTERFACE_BODY -> INTERFACE_KEYWORD_Id
            CjScopeKind.ENUM_BODY -> ENUM_KEYWORD_Id
            CjScopeKind.EXTEND_BODY -> EXTEND_KEYWORD_Id
            else -> return null
        }
        if (!at(TILDE)) return null
        return parseMemberDeclarationRest(enclosingDeclarationToken, ModifierDetector(), detector)
    }

    /** class-like 的 IS_BROKEN InvalidDecl 恢复；外层接收者决定丢弃或保留宏 input 槽位。 */
    context(parseContext: ParsingContext)
    internal fun parseInvalidMemberDeclaration() {
        checkNotNull(parseContext.memberScope)
        val invalidName = mark()
        val name = checkNotNull(builder.tokenText)
        advance()
        if (parseContext.shouldReportError) {
            invalidName.error(CangJieParsingBundle.message("parsing.error.expecting.declaration", name))
        } else {
            invalidName.drop()
        }
        // 对位 CLASS_BODY/STRUCT_BODY 的 ConsumeUntilAny；换行与分号不单独结束恢复。
        while (!eof() && !at(RBRACE) && !seeingDeclarationForRecovery()) consumeRecoveryToken()
    }

    /** 原生 SeeingDecl 的声明起点，不能替换成 expression FIRST 或包含所有 AT 的集合。 */
    private fun seeingDeclarationForRecovery(): Boolean {
        if ((declarationModifierAtLookahead(0) != null || at(INOUT_KEYWORD)) &&
            !(at(UNSAFE_KEYWORD) && lookahead(1) == LBRACE)) return true
        if (tt() in setOf(FUNC_KEYWORD, MACRO_KEYWORD, LET_KEYWORD, VAR_KEYWORD, ENUM_KEYWORD, TYPE_KEYWORD,
                STRUCT_KEYWORD, CLASS_KEYWORD, INTERFACE_KEYWORD, MAIN_KEYWORD, EXTEND_KEYWORD)) return true
        return isBuiltInAnnotation() && rawTokenText(builder, 1).toString() != "IfAvailable" && lookahead(2) != LBRACE
    }

    /**
     * 解析主构造函数
     *
     * Grammar:
     * ```
     * primaryConstructor
     *   : simpleName typeParameters? "(" parameterList? ")" typeConstraints? constructorBody?
     *   ;
     * ```
     */
    context(parseContext: ParsingContext)
    fun parsePrimaryInitFunc(grammar: CjParameterGrammar, detector: ModifierDetector? = null): Boolean {
        require(grammar.isPrimaryConstructor)
        assert(_at(IDENTIFIER))
        val declarationAnchor = builder.currentOffset
        val actualName = checkNotNull(builder.tokenText)
        detector?.cjmpDeclarationName = actualName
        val ownerName = CjPsiUtil.unquoteIdentifier(checkNotNull(parseContext.memberScope?.declarationName))
        val nameMatches = CjPsiUtil.unquoteIdentifier(actualName) == ownerName
        val nameMarker = mark()
        advance() // IDENTIFIER
        if (!nameMatches && parseContext.shouldReportError) {
            nameMarker.error(CangJieParsingBundle.message("parsing.error.primary.constructor.name", actualName, ownerName))
        } else {
            nameMarker.drop()
        }
        val cjmpParameterFacts = detector?.let {
            // static CJMP 构造器在 CheckCJMPModifiersOf 中先失败，官方不会再进入参数检查。
            val supportedSpecificRole = it.isSpecificDetected &&
                (it.isPlatformDetected || cjmpModifierSyntaxAvailable())
            CjmpParameterRuleFacts(
                declarationKind = CangJieParsingBundle.message("parsing.scope.primary.constructor"),
                isCommon = it.isCommonDetected,
                isSpecific = supportedSpecificRole,
                enabled = cjmpParameterRoleMatchingEnabled() && parseContext.shouldReportError &&
                    !(it.isStaticDetected && (it.isCommonDetected || it.isSpecificDetected)),
                rejectSpecificDefaultValues = supportedSpecificRole && cjmpModifierRulesEnabled() &&
                    !(it.isStaticDetected && (it.isCommonDetected || it.isSpecificDetected)) &&
                    !cjmpParameterDefaultValueRulesAvailable(),
                usesPlatformRole = it.isPlatformDetected,
            )
        }
        parseInitFuncRest(
            isCommon = detector?.isCommonDetected == true,
            declarationType = PRIMARY_CONSTRUCTOR,
            primaryParameterGrammar = grammar,
            declarationName = actualName,
            cjmpParameterFacts = cjmpParameterFacts,
            detector = detector,
            declarationAnchor = declarationAnchor,
        )
        return nameMatches
    }

    /**
     * 对位原生 SeeingPrimaryConstructor/SeeingPrimaryIdentifer。
     * 候选比较完整源码拼写（含反引号）的 UTF-8 字节；最终身份在签名解析后比较语义名字。
     */
    context(parseContext: ParsingContext)
    internal fun seeingPrimaryConstructor(): Boolean {
        if (primaryParameterGrammar() == null || !at(IDENTIFIER)) return false
        val ownerName = parseContext.memberScope?.declarationName ?: return false
        if (lookahead(1) !in setOf(LT, LPAR, LBRACKET, LBRACE)) return false
        return primaryNameDistance(checkNotNull(builder.tokenText), ownerName) <= 1
    }

    /** 原生 ParserUtils.cpp 的 std::string 动态规划；空 target 分支同原生返回零。 */
    private fun primaryNameDistance(source: String, target: String): Int {
        val sourceBytes = source.toByteArray(Charsets.UTF_8)
        val targetBytes = target.toByteArray(Charsets.UTF_8)
        if (targetBytes.isEmpty()) return 0
        val distances = IntArray(targetBytes.size + 1) { it }
        for (sourceIndex in sourceBytes.indices) {
            var upperLeft = distances[0]
            distances[0] = sourceIndex + 1
            for (targetIndex in targetBytes.indices) {
                val upper = distances[targetIndex + 1]
                distances[targetIndex + 1] = minOf(
                    upperLeft + if (sourceBytes[sourceIndex] == targetBytes[targetIndex]) 0 else 1,
                    minOf(upper, distances[targetIndex]) + 1,
                )
                upperLeft = upper
            }
        }
        return distances.last()
    }

    /**
     * 解析构造函数
     *
     * Grammar:
     * ```
     * constructor
     *   : "init" "(" parameterList? ")" constructorBody?
     *   ;
     * ```
     */
    context(parseContext: ParsingContext)
    fun parseInitFunc(detector: ModifierDetector? = null) {
        assert(_at(INIT_KEYWORD))
        val declarationAnchor = builder.currentOffset
        detector?.cjmpDeclarationName = INIT_KEYWORD.value
        advance() // INIT_KEYWORD

        parseInitFuncRest(
            isCommon = detector?.isCommonDetected == true,
            cjmpParameterFacts = cjmpCallableParameterFacts(
                detector,
                SECONDARY_CONSTRUCTOR,
            ),
            detector = detector,
            declarationAnchor = declarationAnchor,
        )
    }

    context(parseContext: ParsingContext)
    private fun parseInitFuncRest(
        isCommon: Boolean = false,
        declarationType: IElementType = SECONDARY_CONSTRUCTOR,
        primaryParameterGrammar: CjParameterGrammar? = null,
        cjmpParameterFacts: CjmpParameterRuleFacts? = null,
        detector: ModifierDetector? = null,
        declarationAnchor: Int,
        declarationName: String? = null,
    ) {
        require(declarationType == PRIMARY_CONSTRUCTOR || declarationType == SECONDARY_CONSTRUCTOR ||
            declarationType == FINALIZER)
        val declarationKind = when (declarationType) {
            PRIMARY_CONSTRUCTOR -> CjmpModifierDeclarationKind.PRIMARY_CONSTRUCTOR
            SECONDARY_CONSTRUCTOR -> CjmpModifierDeclarationKind.SECONDARY_CONSTRUCTOR
            FINALIZER -> CjmpModifierDeclarationKind.FINALIZER
            else -> kotlin.error("Unexpected constructor declaration type: $declarationType")
        }
        val declarationNameEnd = lastSignificantTokenEndOffset().coerceAtLeast(declarationAnchor)
        detector?.predefinedCommonAttribute = declarationType == SECONDARY_CONSTRUCTOR && isCommon

        builder.disableJoiningComplexTokens()

        // 三种声明共享官方 FuncBody 签名顺序，自身泛型不能借用所属类型的参数。
        val typeParameterListOccurred = parseCjmpTypeParameterList(
            CONSTRUCTOR_HEADER_RECOVERY_SET,
            detector,
            cjmpParameterFacts,
        )

        val parameters = parseFunctionParameterLists(
            if (declarationType == PRIMARY_CONSTRUCTOR) checkNotNull(primaryParameterGrammar)
            else CjParameterGrammar.REGULAR_TYPED,
            cjmpParameterFacts,
        )
        val parameterLists = parameters.lists

        var returnTypeStart: PsiBuilder.Marker? = null
        var returnTypeEnd: PsiBuilder.Marker? = null
        if (at(COLON)) {
            advance()
            // 与共享类型语法的实际起始分支一致；缺类型只保留原始类型错误。
            val hasTypeSyntax = atSet(BASICTYPES) || at(VARRAY_KEYWORD) || at(THIS_KEYWORD_UPPER) ||
                at(IDENTIFIER) || at(LPAR) || at(QUEST)
            if (hasTypeSyntax) returnTypeStart = mark()
            parseTypeRef(TokenSet.create(WHERE_KEYWORD))
            if (hasTypeSyntax) {
                returnTypeEnd = mark()
            } else {
                skipUntil(TokenSet.create(LBRACE, WHERE_KEYWORD))
            }
        }
        parseTypeConstraintsGuarded(typeParameterListOccurred)
        if (at(LBRACE)) {
            detector?.cjmpHasFunctionBody = true
        }
        val hasBody = at(LBRACE)
        // primary 的缺体规则依据检查后的有效属性；secondary 在 body 前已预置 COMMON。
        if (hasBody) parseInitFunctionBlock()
        // primary 的独立重载先检查参数，再检查修饰符；普通函数的 HAS_BROKEN 缺体豁免不适用。
        if (declarationType == PRIMARY_CONSTRUCTOR) {
            checkVariadicParameters(declarationKind, parameterLists, effectiveForeign = false,
                declarationAnchor, declarationNameEnd)
        }
        checkCjmpDeclarationFileRole(detector, declarationType, declarationAnchor)
        if (declarationType == PRIMARY_CONSTRUCTOR && !hasBody &&
            detector?.isCommonDetected != true) {
            reportMissingConstructorBody(declarationKind, checkNotNull(declarationName), lastSignificantTokenEndOffset())
        }

        // secondary 使用检查后的实际 FOREIGN；finalizer 不调用官方变长参数检查。
        if (declarationType == SECONDARY_CONSTRUCTOR) {
            checkVariadicParameters(declarationKind, parameterLists, detector?.isForeignDetected == true,
                declarationAnchor, declarationNameEnd)
        }

        if (detector?.cjmpFileRoleAccepted != false) {
            reportUnsupportedSpecificParameterDefaults(cjmpParameterFacts, parameterLists.size)
        }

        // 官方在正文解析后检查非法返回类型；范围仅包实际类型，不包冒号、约束或正文。
        if (returnTypeStart != null && returnTypeEnd != null) {
            if (parseContext.shouldReportError) {
                val messageKey = when (declarationType) {
                    PRIMARY_CONSTRUCTOR -> "parsing.error.primary.constructor.return.type"
                    SECONDARY_CONSTRUCTOR -> "parsing.error.constructor.return.type"
                    FINALIZER -> "parsing.error.finalizer.return.type"
                    else -> kotlin.error("Unexpected constructor declaration type: $declarationType")
                }
                returnTypeStart.errorBefore(CangJieParsingBundle.message(messageKey), returnTypeEnd)
            } else {
                returnTypeStart.drop()
            }
            returnTypeEnd.drop()
        }

        if (!hasBody && declarationType != PRIMARY_CONSTRUCTOR &&
            (declarationType == FINALIZER || detector?.isCommonDetected != true)) {
            reportMissingConstructorBody(declarationKind, null, lastSignificantTokenEndOffset())
        }

        if (declarationType == PRIMARY_CONSTRUCTOR && parameterLists.size == 1 &&
            cjmpParameterFacts?.enabled == true && detector?.cjmpFileRoleAccepted != false
        ) {
            for (parameter in cjmpParameterFacts.parameters) {
                reportCjmpRoleMismatch(
                    memberDescription = "${CangJieParsingBundle.message("parsing.scope.parameter")} ${CjPsiUtil.unquoteIdentifier(parameter.name)}",
                    memberIsCommon = parameter.isCommon,
                    memberIsSpecific = parameter.isSpecific,
                    outerKindName = cjmpParameterFacts.declarationKind,
                    outerIsCommon = cjmpParameterFacts.isCommon,
                    outerIsSpecific = cjmpParameterFacts.isSpecific,
                    startOffset = parameter.startOffset,
                    endOffset = parameter.endOffset,
                )
            }
        }

        reportUnsupportedGenericDeclaration(detector)
    }

    /** 构造器/finalizer 的原生缺体规则与普通函数的 HAS_BROKEN 抑制分开。 */
    context(parseContext: ParsingContext)
    private fun reportMissingConstructorBody(kind: CjmpModifierDeclarationKind, name: String?, position: Int) {
        val label = when (kind) {
            CjmpModifierDeclarationKind.PRIMARY_CONSTRUCTOR -> "parsing.body.owner.primary.constructor"
            CjmpModifierDeclarationKind.SECONDARY_CONSTRUCTOR -> "parsing.body.owner.constructor"
            CjmpModifierDeclarationKind.FINALIZER -> "parsing.body.owner.finalizer"
            else -> kotlin.error("A constructor body report requires its actual declaration kind: $kind")
        }
        val owner = CangJieParsingBundle.message(label)
        val identifier = if (name == null) "" else " '${CjPsiUtil.unquoteIdentifier(name)}'"
        reportMissingBody {
            errorOnPosition(position, CangJieParsingBundle.message("parsing.error.body.missing", owner, identifier),
                listOf(ParsingDiagnosticHint(position, position, CangJieParsingBundle.message("parsing.hint.body.missing", owner),
                    rangeKind = ParsingDiagnostic.RangeKind.POINT)))
        }
    }


    /**
     * 解析成员列表
     *
     * Grammar:
     * ```
     * members
     *   : memberDeclaration*
     *   ;
     * ```
     *
     * @param tokenId 令牌 ID
     * @param detector 修饰符检测器
     */
    context(parseContext: ParsingContext)
    private fun parseMembers(
        tokenId: Int?,
        detector: ModifierDetector?,
        cjmpFacts: CjmpNominalParseFacts? = null,
    ) {
        while (!eof() && !at(RBRACE)) {
            parseMemberDeclaration(tokenId, detector ?: ModifierDetector(), cjmpFacts = cjmpFacts)
        }
    }


    /**
     * 解析类体
     *
     * Grammar:
     * ```
     * classBody
     *   : "{" memberDeclaration* "}"
     *   ;
     * ```
     *
     * @param tokenId 令牌 ID
     * @param detector 修饰符检测器
     */
    context(parseContext: ParsingContext)
    private fun parseClassBody(
        tokenId: Int,
        detector: ModifierDetector,
        cjmpFacts: CjmpNominalParseFacts,
    ) {
        val body = mark()

        builder.enableNewlines()

        if (expect(LBRACE, "Expecting a class body")) {
            parseMembers(tokenId, detector, cjmpFacts)
            expect(RBRACE, "Missing '}'")
        }

        builder.restoreNewlinesState()

        body.done(
            when (tokenId) {
                INTERFACE_KEYWORD_Id -> INTERFACE_BODY

                ENUM_KEYWORD_Id -> ENUM_BODY
                else -> CLASS_BODY
            }
        )
    }


    /**
     * 解析主函数
     *
     * Grammar:
     * ```
     * mainFunction
     *   : "main" "(" parameterList? ")" (":" typeReference)? functionBody
     *   ;
     * ```
     *
     * @return 解析结果的节点类型
     */
    context(parseContext: ParsingContext)
    private fun parseMainFunc(detector: ModifierDetector): IElementType {
        assert(_at(MAIN_KEYWORD))
        val declarationAnchor = builder.currentOffset
        advance()

        if (at(RBRACE)) {
            // 缺的是参数列表，不是"函数体"。
            error(CangJieParsingBundle.message("parsing.error.expecting.symbol", "("))
            checkCjmpDeclarationFileRole(detector, MAIN_FUNC, declarationAnchor)
            return MAIN_FUNC
        }

        builder.disableJoiningComplexTokens()
        // 类型参数
        if (at(LPAR)) {
            parseValueParameterList(
                CjParameterGrammar.REGULAR_TYPED,
                VALUE_PARAMETERS_FOLLOW_SET,
            )
        } else {
            error(CangJieParsingBundle.message("parsing.error.expecting.symbol", "("))
        }

        // 返回值类型
        if (at(COLON)) {
            advance() // COLON
            parseTypeRef()
        }

        // 函数体
        // 与 parseFunctionBody 完全等价：后者内部已处理"缺体诊断 + 声明模式抑制"，
        // 这里不再重复一份分支（缺体上报只有 reportMissingBody 一个入口）。
        parseFunctionBody(diagnosticOwner = CjParsingScopeOwner(CjParsingScopeOwner.Kind.MAIN,
            builder.macroParsingJournal.sourceOrigin(declarationAnchor, declarationAnchor + MAIN_KEYWORD.value.length)))
        checkCjmpDeclarationFileRole(detector, MAIN_FUNC, declarationAnchor)
        return MAIN_FUNC
    }

    /**
     * 解析函数声明
     *
     * Grammar:
     * ```
     * functionDeclaration
     *   : modifier* "func" (simpleName | operatorName) typeParameters?
     *     "(" parameterList? ")" (":" typeReference)? typeConstraints? functionBody?
     *   ;
     * ```
     *
     * @param isInterfaceMethod 是否为接口方法
     * @param classdetector 类修饰符检测器
     * @param detector 修饰符检测器
     * @param topTokenId 顶层token ID
     * @return 解析结果的节点类型
     */
    context(parseContext: ParsingContext)
    fun parseFunction(
        isInterfaceMethod: Boolean = false,
        classdetector: ModifierDetector? = null,
        detector: ModifierDetector? = null,
        topTokenId: Int? = 0
    ): IElementType {
        assert(_at(FUNC_KEYWORD))
        val declarationAnchor = builder.currentOffset
        // 保留关键字前的零宽位置，签名解析结束后仍可报告错误而不包裹或吞掉 func token。
        val functionStart = mark()
        val functionStartEnd = mark()
        advance()
        checkCjmpDeclarationFileRole(detector, FUNC, declarationAnchor)
        val functionNameStart = builder.currentOffset

        var type: IElementType = FUNC


        builder.disableJoiningComplexTokens()

        if (detector?.isOperatorDetected == true) {
            // 运算符重载
            val operatorToken = getOperationTokenType()

            if (OPERATIONS_CAN_BE_OVERLOADED.contains(operatorToken)) {
                detector.cjmpDeclarationName = builder.tokenText
                val operator = mark()
                advanceOperationToken(operatorToken)
                operator.done(OPERATION_NAME)
            } else {
                val nameStart = builder.currentOffset
                val name = checkNotNull(builder.tokenText)
                detector.cjmpDeclarationName = name
                errorOnRange(nameStart, nameStart + name.length,
                    CangJieParsingBundle.message("parsing.error.invalid.overloaded.operator", name))
                advance()
            }
        } else {
            // 函数名
//            builder.tokenType
            if (at(IDENTIFIER)) {
                detector?.cjmpDeclarationName = builder.tokenText
                parseIdentifier()
            } else {
                reportExpectedFunctionName(declarationAnchor)
                // v1.1.3 ExpectIdentifierWithPos 失败不消费 lookahead；参数、正文或后续声明由各自文法继续处理。
            }
        }
        val functionNameEnd = lastSignificantTokenEndOffset().coerceAtLeast(functionNameStart)

        val cjmpParameterFacts = cjmpCallableParameterFacts(detector, FUNC)

        var typeParameterListOccurred = false
        if (at(LT)) {
            parseCjmpTypeParameterList(LBRACKET_LBRACE_RBRACE_LPAR_SET, detector, cjmpParameterFacts)
            typeParameterListOccurred = true
        }

        // 参数列表
        val parameters = parseFunctionParameterLists(CjParameterGrammar.REGULAR_TYPED, cjmpParameterFacts)
        val parameterLists = parameters.lists

        reportUnsupportedSpecificParameterDefaults(cjmpParameterFacts, parameterLists.size)

        // 返回值类型
        val hasReturnType = at(COLON)
        if (hasReturnType) {
            advance() // COLON
            thisTypeAllowed = topTokenId == CLASS_KEYWORD_Id && at(THIS_KEYWORD_UPPER)
            try {
                parseTypeRef()
            } finally {
                thisTypeAllowed = false
            }
        }

        parseTypeConstraintsGuarded(typeParameterListOccurred)

        // 官方 ParseFuncDecl 在解析阶段要求无体 common/specific 函数显式声明返回类型。
        // 此规则不依赖语义解析、编译模式或后续隐式返回类型推断。
        val cjmpRole = when {
            detector?.isCommonDetected == true -> "common"
            detector?.isSpecificDetected == true -> "specific"
            else -> null
        }
        if (!hasReturnType && !at(LBRACE) && cjmpRole != null && parseContext.shouldReportError) {
            functionStart.errorBefore(
                CangJieParsingBundle.message("parsing.error.cjmp.function.return.type.required", cjmpRole),
                functionStartEnd,
            )
            // 零宽节点的两条边都放在前导 trivia 之后，锚在 func 而非其前的空白或注释。
            functionStart.setCustomEdgeTokenBinders(
                WhitespacesBinders.GREEDY_RIGHT_BINDER,
                WhitespacesBinders.GREEDY_RIGHT_BINDER,
            )
        } else {
            functionStart.drop()
        }
        functionStartEnd.drop()
        if (at(LBRACE)) {
            val bodyStart = builder.currentOffset
            detector?.cjmpHasFunctionBody = true
            parseFunctionBody(diagnosticOwner = CjParsingScopeOwner(CjParsingScopeOwner.Kind.FUNCTION,
                builder.macroParsingJournal.sourceOrigin(declarationAnchor, declarationAnchor + FUNC_KEYWORD.value.length)))
            if (detector?.isForeignDetected == true) {
                errorOnRange(bodyStart, lastSignificantTokenEndOffset(),
                    CangJieParsingBundle.message("parsing.error.foreign.function.no.body"))
            }
        } else if (
            !(isInterfaceMethod || detector?.isAbstractDetected == true || detector?.isCommonDetected == true) &&
            (detector?.isForeignDetected != true) && !parameters.parameterListMissing
        ) {
            reportMissingBody {
                val name = detector?.cjmpDeclarationName?.let { " '${CjPsiUtil.unquoteIdentifier(it)}'" }.orEmpty()
                errorOnPosition(lastSignificantTokenEndOffset(), CangJieParsingBundle.message("parsing.error.body.missing", "function", name))
            }
        }

        // 官方不支持 currying；错误属于第二个实际列表，所有列表仍交给共享签名 owner。
        if (parameterLists.size > 1) {
            val second = parameters.openingOffsets[1]
            errorOnRange(second, second + 1,
                CangJieParsingBundle.message("parsing.error.function.currying", "("))
        }

        checkVariadicParameters(CjmpModifierDeclarationKind.FUNCTION, parameterLists,
            detector?.isForeignDetected == true, declarationAnchor, functionNameEnd)
        reportUnsupportedGenericDeclaration(detector)

        return type
    }

    /** 声明名称失败的共享报告入口，包含右花括号上的提前停止分支。 */
    context(parseContext: ParsingContext)
    private fun reportExpectedFunctionName(keywordOffset: Int) {
        val start = if (eof()) lastSignificantTokenEndOffset() else builder.currentOffset
        val end = if (eof()) start else start + checkNotNull(builder.tokenText).length
        errorOnCurrentToken(CangJieParsingBundle.message(
            "parsing.error.expected.declaration.name", "func", "func", builder.tokenText ?: "<EOF>",
        ), listOf(
            ParsingDiagnosticHint(start, end, CangJieParsingBundle.message("parsing.hint.expected.declaration.name", "func")),
            ParsingDiagnosticHint(keywordOffset, keywordOffset + FUNC_KEYWORD.value.length,
                CangJieParsingBundle.message("parsing.hint.after.keyword", FUNC_KEYWORD.value)),
        ))
    }

    /**
     * 解析外部函数声明
     *
     * Grammar:
     * ```
     * foreignFunction
     *   : modifier* "func" simpleName typeParameters? "(" parameterList? ")" (":" typeReference)? typeConstraints?
     *   ;
     * ```
     *
     * @param detector 修饰符检测器
     * @param topTokenId 顶层token ID
     * @return 解析结果的节点类型
     */
    context(parseContext: ParsingContext)
    fun parseForeignFunction(
        detector: ModifierDetector, topTokenId: Int = 0
    ): IElementType {
        return parseFunction(
            isInterfaceMethod = false,
            classdetector = null,
            detector = detector,
            topTokenId = topTokenId
        )
    }


    /**
     * 解析标识符
     *
     */
    context(parseContext: ParsingContext)
    private fun parseIdentifier(
        nameParsingMode: NameParsingMode = NameParsingMode.REQUIRED, recoverySet: TokenSet = TokenSet.EMPTY
    ): Boolean {

        if (atSet(KEYWORDS)) {
            error(CangJieParsingBundle.message("parsing.error.cannot.be.used", "Keywords")) // 关键字不能使用
            return false
        }

        if (nameParsingMode == NameParsingMode.REQUIRED) {
            return expect(
                IDENTIFIER, CangJieParsingBundle.message("parsing.error.expecting.cangjie.identifier"), recoverySet
            ) // 应该为标识符
        } else {
            if (at(IDENTIFIER)) {
                if (nameParsingMode == NameParsingMode.PROHIBITED) {
// TODO 禁止出现标识符，目前应该用不到

                } else {
                    assert(nameParsingMode == NameParsingMode.ALLOWED)
                    advance()
                    return true
                }
            }
        }
        return false
    }


    /**
     * 按标题解析标识符
     *
     * @param title 标题名称，用于错误消息
     * @param recoverySet 恢复伤口的令牌集合
     * @param isUnderline 是否允许下划线
     */
    context(parseContext: ParsingContext)
    private fun parseIdentifierByTitle(
        title: String, recoverySet: TokenSet = TokenSet.EMPTY, isUnderline: Boolean = false
    ): Boolean {
        if (isUnderline && expect(CangJieExpressionParsing.IDENTIFIER_RECOVERY_SET)) {
            return true
        }

        if (expect(IDENTIFIER)) {
            return true
        }

        errorWithRecovery("Expecting $title name", recoverySet)
        return false
    }

    /**
     * 解析同步表达式
     *
     * Grammar:
     * ```
     * synchronizedExpression
     *   : "synchronized" "(" expression ")" block
     *   ;
     * ```
     */
    context(parseContext: ParsingContext)
    fun parseSynchronizedExpression() {
        assert(_at(SYNCHRONIZED_KEYWORD))
        val synchronizedMarker = mark()
        advance()

        if (at(LPAR)) {
            advance()

            expressionParsing.parseExpression()

            expect(RPAR, "Expecting ')'")
        } else {
            error(CangJieParsingBundle.message("parsing.error.expecting.symbol", "("))
        }

        if (at(LBRACE)) {
            parseBlock(scope = CjScopeKind.UNKNOWN_SCOPE)
        } else {
            error(CangJieParsingBundle.message("parsing.error.expecting.symbol", "{")) // 应该为'{'
        }

        synchronizedMarker.done(SYNCHRONIZED_EXPRESSION)
    }


    /**
     * 解析外部声明块
     *
     * Grammar:
     * ```
     * foreignDeclaration
     *   : "foreign" "{" topLevelDeclaration* "}"
     *   ;
     * ```
     *
     * @return 解析结果的节点类型
     */
    context(parseContext: ParsingContext)
    private fun parseForeign(detector: ModifierDetector): IElementType {
        assert(_at(FOREIGN_KEYWORD))
        val foreignStartOffset = builder.currentOffset
        advance()
        val foreignEndOffset = lastSignificantTokenEndOffset()
        detector(FOREIGN_KEYWORD)
        recordParsedModifier(detector, FOREIGN_KEYWORD, foreignStartOffset, foreignEndOffset)

        if (at(LBRACE)) {
            parseForeignBody(detector)
        } else {
            error(CangJieParsingBundle.message("parsing.error.expecting.symbol", "{"))
        }
        // foreign 是声明集合；集合内的真实声明各自在其文法时点执行文件角色检查。
        checkCjmpDeclarationFileRole(detector, FOREIGN, foreignStartOffset)

        return FOREIGN
    }


    /**
     * 解析外部声明块体
     *
     * Grammar:
     * ```
     * foreignBody
     *   : "{" topLevelDeclaration* "}"
     *   ;
     * ```
     */
    context(parseContext: ParsingContext)
    private fun parseForeignBody(prefix: ModifierDetector) {
        assert(_at(LBRACE))
        val mark = mark()
        advance() // LBRACE

        while (!at(RBRACE) && !eof()) {
            if (at(SEMICOLON)) {
                advance()
                continue
            }
            // v1.1.3 ParseForeignDecls 每条都调用 TOPLEVEL ParseDecl；宏、变量和名义声明使用各自 owner。
            val declaration = mark()
            val detector = prefix.copyDeclarationPrefix()
            parseAnnotations(detector)
            parseModifierList(detector, TokenSet.EMPTY)
            val start = builder.currentOffset
            val declarationType = with(parseContext.copy(statementScope = CjScopeKind.TOPLEVEL, memberScope = null)) {
                parseCommonDeclaration(detector, NameParsingMode.REQUIRED, DeclarationParsingMode.TOPLEVEL)
            }
            if (declarationType != null) {
                closeDeclarationWithCommentBinders(declaration, declarationType, true)
            } else {
                declaration.drop()
                // 无合法 owner 的输入走声明恢复；不能只向 func 搜索而丢掉随后合法的宏或其它声明。
                errorWithRecovery(CangJieParsingBundle.message("parsing.error.expecting.element", "a top level declaration"),
                    TokenSet.orSet(TOP_LEVEL_DECLARATION_FIRST, TokenSet.create(AT, RBRACE)))
                if (builder.currentOffset == start && !at(RBRACE) && !eof()) advance()
            }
        }

        expect(RBRACE, "Missing '}'")
        mark.done(FOREIGN_BODY)
    }

    /**
     * 解析宏声明
     *
     * Grammar:
     * ```
     * macroDeclaration
     *   : "macro" simpleName typeParameters? "(" parameterList? ")" (":" typeReference)?
     *     typeConstraints? functionBody?
     *   ;
     * ```
     *
     * @return 解析结果的节点类型
     */

    context(parseContext: ParsingContext)
    fun parseMacro(
        enclosingDeclarationKeyword: CjKeywordToken? = null,
        detector: ModifierDetector? = null,
    ): IElementType {
        assert(_at(MACRO_KEYWORD))
        val declarationAnchor = builder.currentOffset
        val scopeError = mark()
        advance()
        if (enclosingDeclarationKeyword != null && parseContext.shouldReportError) {
            scopeError.error(CangJieParsingBundle.message(
                "parsing.error.invalid.declaration.in.class",
                MACRO_KEYWORD.value,
                CangJieParsingBundle.message("parsing.scope.${enclosingDeclarationKeyword.value}"),
            ))
        } else {
            scopeError.drop()
        }
        checkCjmpDeclarationFileRole(detector, MACRO, declarationAnchor)

        if (at(RBRACE)) {
            // 连宏名都还没有，缺的是标识符，不是"函数体"。
            error(CangJieParsingBundle.message("parsing.error.expecting.identifier"))
            return MACRO
        }

        builder.disableJoiningComplexTokens()

        // 函数名
        parseIdentifier()
        // 类型参数
        var typeParameterListOccurred = false
        if (at(LT)) {
            parseCjmpTypeParameterList(LBRACKET_LBRACE_RBRACE_LPAR_SET, detector)
            typeParameterListOccurred = true
        }


        if (at(LPAR)) {
            parseValueParameterList(CjParameterGrammar.REGULAR_TYPED, VALUE_PARAMETERS_FOLLOW_SET)
        } else {
            error(CangJieParsingBundle.message("parsing.error.expecting.symbol", "(")) // 应该为'('
        }

        // 返回值类型
        if (at(COLON)) {
            advance() // COLON
            parseTypeRef()
        }

        parseTypeConstraintsGuarded(typeParameterListOccurred)


        // 声明文件允许宏只有签名，普通源码仍要求宏体。
        if (at(LBRACE)) {
            parseFunctionBody(CjScopeKind.MACRO_BODY, CjParsingScopeOwner(CjParsingScopeOwner.Kind.MACRO,
                builder.macroParsingJournal.sourceOrigin(declarationAnchor, declarationAnchor + MACRO_KEYWORD.value.length)))
        } else {
            reportMissingBody {
                error(CangJieParsingBundle.message("parsing.error.expecting.symbol", "{"))
            }
        }
        reportUnsupportedGenericDeclaration(detector)
        return MACRO
    }

    /**
     * init 函数体恢复
     */
    context(parseContext: ParsingContext)
    fun parseInitFunctionBody(isCommon: Boolean = false) {
        if (at(LBRACE)) {
            parseInitFunctionBlock()
        } else if (!isCommon) {
            // 官方 CheckCtorBody / CheckPrimaryCtorBody 允许 common 构造器仅声明签名。
            reportMissingBody()
        }
    }

    /**
     * 函数体解析，只支持块
     */
    context(parseContext: ParsingContext)
    fun parseFunctionBody(scope: CjScopeKind = CjScopeKind.FUNC_BODY, diagnosticOwner: CjParsingScopeOwner? = null) {
        if (at(LBRACE)) {
            parseBlock(scope = scope, diagnosticOwner = diagnosticOwner)
        } else {
            reportMissingBody()
        }
    }

    /**
     * 上报"声明体缺失"类诊断。
     *
     * 【唯一】的缺体抑制点：声明文件（`.cj.d`）中函数 / 构造 / 访问器 / 属性的体允许不存在，
     * 对齐官方 `Parser::parseDeclFile`（P1 / P4 / P6）。
     *
     * 只用于"名字、参数列表、类型都已解析完，仅缺 body"的情形。
     * 残缺输入（如 `class C { func }`，连函数名都没有）在各自的上报点走
     * "缺标识符 / 缺参数列表"诊断，**不经过这里** ——
     * 这正是"缺体"与"残缺输入"在结构上分开的关键，也是本设计能不用人工分组
     * 就保证不会误豁免残缺输入的原因。
     *
     * @param report 具体诊断的上报动作。消息按 owner 不同而不同（函数 / 属性 / 访问器各有措辞），
     *   但"是否抑制"只在 [reportMissingBody] 内部判定一次 —— 新增体 owner 时不需要再判断模式。
     */
    context(parseContext: ParsingContext)
    private fun reportMissingBody(report: () -> Unit) {
        if (sourceKind.isDeclaration) return
        report()
    }

    /**
     * [reportMissingBody] 的默认文案重载：函数 / 构造 / 访问器体缺失。
     */
    context(parseContext: ParsingContext)
    private fun reportMissingBody() = reportMissingBody {
        error(CangJieParsingBundle.message("parsing.error.function.body.expected"))
    }

    /**
     * 尝试解析函数参数，类型是否必须由 typeRequired 决定
     */
    context(parseContext: ParsingContext)
    fun tryParseValueParameter(typeRequired: Boolean): Boolean {
        return parseValueParameter(CjParameterGrammar.REGULAR_TYPED, rollbackOnFailure = true, typeRequired = typeRequired)
    }

    /**
     * 解析主构造函数的值参数列表
     *
     * 专门用于解析主构造函数的参数列表，支持注解、修饰符和属性声明（let/var）。
     * 这是一个便捷方法，内部调用 `parseValueParameterList` 并传入 `isPrimaryInitFunc=true`。
     *
     * Grammar:
     * ```
     * primaryConstructorParameterList
     *   : "(" (primaryConstructorParameter ("," primaryConstructorParameter)*)? ")"
     *   ;
     *
     * primaryConstructorParameter
     *   : annotations? modifiers? ("let" | "var") IDENTIFIER ":" type ("=" expression)?
     *   ;
     * ```
     *
     * 特点：
     * - 参数可以有注解（如 `@Serializable`, `@Deprecated`）
     * - 参数可以有访问修饰符（如 `public`, `private`, `protected`）
     * - 参数可以声明为属性（使用 `let` 或 `var`）
     * - 参数不能使用宏调用
     * - 所有参数必须有名称和类型
     *
     * 示例：
     * ```cangjie
     * class Person(
     *     // 公开的不可变属性
     *     public let name: String,
     *
     *     // 私有的可变属性，带默认值
     *     private var age: Int = 0,
     *
     *     // 带注解的属性
     *     @Deprecated("Use fullAddress instead")
     *     let address: String
     * )
     * ```
     *
     * @see parseValueParameterList 底层参数列表解析方法
     * @see parseValueParameter 单个参数解析方法
     */
    context(parseContext: ParsingContext)
    fun parsePrimaryInitFuncValueParameterList(grammar: CjParameterGrammar) {
        require(grammar.isPrimaryConstructor)
        parseValueParameterList(grammar, VALUE_PARAMETERS_FOLLOW_SET)
    }

    /** primary constructor 的 CJMP 参数事实只传入真实成员参数列表。 */
    /** 从真实声明作用域选择 primary 参数文法；枚举、接口与 extend 没有这种构造器。 */
    context(parseContext: ParsingContext)
    internal fun primaryParameterGrammar(): CjParameterGrammar? = when (parseContext.memberScope?.kind) {
        CjMemberParsingContext.Kind.CLASS -> CjParameterGrammar.CLASS_PRIMARY
        CjMemberParsingContext.Kind.STRUCT -> CjParameterGrammar.STRUCT_PRIMARY
        else -> null
    }

    /**
     * 普通源码的声明参数列表；四种角色共享前缀与分隔符，按角色选择参数 rest。
     * getter/setter 的 name-only 角色不消费类型、命名标记或默认值。
     * 函数类型的推测解析使用独立入口，不通过声明参数角色放宽类型要求。
     */
    context(parseContext: ParsingContext)
    fun parseValueParameterList(
        grammar: CjParameterGrammar = CjParameterGrammar.REGULAR_TYPED,
        recoverySet: TokenSet = TokenSet.EMPTY,
    ) {
        parseValueParameterListWithCjmpFacts(grammar, recoverySet, null)
    }

    /** 含 CJMP owner 的内部入口不改变普通函数类型、访问器和函数参数的公开语法 API。 */
    context(parseContext: ParsingContext)
    private fun parseValueParameterListWithCjmpFacts(
        grammar: CjParameterGrammar,
        recoverySet: TokenSet,
        cjmpFacts: CjmpParameterRuleFacts?,
    ): ParameterSequenceState {
        val sequence = ParameterSequenceState()
        parseParameterListCore(
            parseParameter = { parseValueParameter(grammar, cjmpParameterFacts = cjmpFacts, sequence = sequence); null },
            checkMixedParameters = false,
            recoverySet = recoverySet
        )
        return sequence
    }

    private class FunctionParameterLists(
        val lists: List<ParameterSequenceState>,
        val openingOffsets: List<Int>,
    ) {
        val parameterListMissing: Boolean get() = openingOffsets.isEmpty()
    }

    /**
     * 对位 ParseFuncParameters：收集全部直接列表；缺左括号时保留一个空的破损列表，
     * 不消费外层结束符。声明结构仍是函数/构造器，缺列表也不冒充普通缺体。
     */
    context(parseContext: ParsingContext)
    private fun parseFunctionParameterLists(
        grammar: CjParameterGrammar,
        cjmpFacts: CjmpParameterRuleFacts?,
    ): FunctionParameterLists {
        if (!at(LPAR)) {
            val invalid = mark()
            errorOnCurrentToken(CangJieParsingBundle.message(
                "parsing.error.function.parameter.list", builder.tokenText ?: "<EOF>",
            ))
            invalid.done(VALUE_PARAMETER_LIST)
            return FunctionParameterLists(listOf(ParameterSequenceState()), emptyList())
        }
        val lists = mutableListOf<ParameterSequenceState>()
        val openings = mutableListOf<Int>()
        do {
            openings += builder.currentOffset
            lists += parseValueParameterListWithCjmpFacts(grammar, VALUE_PARAMETERS_FOLLOW_SET, cjmpFacts)
        } while (at(LPAR))
        return FunctionParameterLists(lists.toList(), openings.toList())
    }

    /**
     * 解析函数类型的参数列表
     *
     * 用于解析函数类型声明中的参数列表，参数可以省略名称（仅类型）。
     * 不允许混合使用命名参数和匿名参数。
     *
     * Grammar:
     * ```
     * functionTypeParameterList
     *   : "(" (functionTypeParameter ("," functionTypeParameter)*)? ")"
     *   ;
     *
     * functionTypeParameter
     *   : IDENTIFIER ":" type    // 命名参数
     *   | type                   // 匿名参数
     *   ;
     * ```
     *
     * @param typeRequired 参数是否必须有类型注解
     *
     * 示例：
     * ```cangjie
     * // 只有类型（匿名参数）
     * let callback: (Int, String) -> Unit
     *
     * // 带名称（命名参数）
     * let handler: (count: Int, message: String) -> Bool
     * ```
     */
    context(parseContext: ParsingContext)
    fun parseFunctionTypeParameterList(
        typeRequired: Boolean = true
    ) {
//   不允许处理注解和宏
        with(parseContext.copy(disableMacroParsing = true, allowParseAnnotationsInValueParameter = false)) {
            parseParameterListCore(
                parseParameter = { parseFunctionTypeParameter(typeRequired) },
                checkMixedParameters = true,
                recoverySet = TokenSet.EMPTY
            )
        }
    }

    /**
     * 参数列表解析的核心逻辑
     *
     * 提取公共的参数列表解析逻辑，避免代码重复。
     *
     * @param parseParameter 参数解析函数，返回 null（不检查混合）或 Boolean（是否为命名参数）
     * @param checkMixedParameters 是否检查混合命名/匿名参数
     * @param recoverySet 错误恢复token集合
     */
    context(parseContext: ParsingContext)
    private fun parseParameterListCore(
        parseParameter: () -> Boolean?,
        checkMixedParameters: Boolean,
        recoverySet: TokenSet
    ) {
        assert(at(LPAR))
        val parameters = mark()

        builder.disableNewlines()
        advance() // consume '('

        // 只在需要时追踪命名参数状态
        var hasNamedParameters: Boolean? = null
        var hasMixedParameters = false

        while (!at(RPAR) && !atSet(recoverySet) && !eof()) {
            // 处理连续逗号错误
            if (at(COMMA)) {
                errorAndAdvance(
                    CangJieParsingBundle.message("parsing.error.expecting", "parameter declaration")
                )
                continue
            }

            // 解析参数
            val isNamed = parseParameter()

            // 检测混合参数（仅当需要时）
            if (checkMixedParameters && isNamed != null && !hasMixedParameters) {
                when (hasNamedParameters) {
                    null -> hasNamedParameters = isNamed
                    isNamed -> { /* 一致，继续 */
                    }

                    else -> hasMixedParameters = true  // 检测到混合
                }
            }

            // 处理分隔符
            if (!parseParameterSeparator()) {
                // 参数节点已经闭合；错误后缀属于列表恢复，不能变成下一个参数或吞掉 accessor 正文。
                if (!at(RPAR)) {
                    consumeUntilAny(TokenSet.orSet(recoverySet, TokenSet.create(RPAR)), targetTokenConsumed = false)
                }
                break
            }
        }

        expect(RPAR, "Expecting ')'", recoverySet)
        builder.restoreNewlinesState()

        // 完成或报错
        if (hasMixedParameters) {
            parameters.error(CangJieParsingBundle.message("parsing.error.mixed.named.parameters"))
        } else {
            parameters.done(VALUE_PARAMETER_LIST)
        }
    }

    /**
     * 解析函数类型参数
     *
     * @param typeRequired 是否要求类型注解
     * @return true 如果是命名参数，false 如果是匿名参数（仅类型）
     */
    context(parseContext: ParsingContext)
    private fun parseFunctionTypeParameter(typeRequired: Boolean): Boolean {
        return if (!tryParseValueParameter(typeRequired)) {
            // 匿名参数：只有类型
            val valueParameter = mark()
            parseTypeRef()
            closeDeclarationWithCommentBinders(valueParameter, VALUE_PARAMETER, false)
            false
        } else {
            // 命名参数
            true
        }
    }

    /**
     * 解析参数分隔符（逗号）
     *
     * @return true 如果应该继续解析下一个参数，false 如果应该结束
     */
    context(parseContext: ParsingContext)
    private fun parseParameterSeparator(): Boolean {
        if (at(COMMA)) {
            advance() // consume ','

            if (at(RPAR)) {
                // 官方 ParseZeroOrMoreSepTrailing 允许末尾逗号，不为结束符生成额外参数。
                return false
            }
            return true
        } else if (!at(RPAR)) {
            errorAndAdvance(
                CangJieParsingBundle.message(
                    "parsing.error.expecting.found", "',' or ')'", "${builder.tokenText}"
                )
            )
            return false
        }
        return false
    }


    /**
     * 解析单个值参数
     *
     * 所有声明参数依次消费注解、修饰符、可选 let/var、名字、类型和默认值。
     * 只有真实主构造器中的 let/var 参数具有成员身份；其他上下文保留语法并报告前缀错误。
     * 名字/冒号破损与类型、默认值错误分开记录，供官方参数修饰符检查使用。
     *
     * @param rollbackOnFailure 为函数类型尝试具名参数时，按剩余参数解析结果整体回滚。
     * @param grammar 由声明作用域确定的参数文法身份。
     * @param typeRequired 仅函数类型的推测解析可调整；声明参数调用使用其完整角色要求。
     * @return 未回滚时为 true，包括已保留错误语法的参数。
     */
    context(parseContext: ParsingContext)
    private fun parseValueParameter(
        grammar: CjParameterGrammar,
        rollbackOnFailure: Boolean = false,
        typeRequired: Boolean = true,
        cjmpParameterFacts: CjmpParameterRuleFacts? = null,
        sequence: ParameterSequenceState? = null,
    ): Boolean {
        val isPrimaryInitFunc = grammar.isPrimaryConstructor
        val parameterStartOffset = builder.currentOffset
        val parameter = mark()
        // 参数前的 `@` / `@!` 属于参数 annotation surface。生产声明解析中普通
        // `@Name` 仍可能是 macro expression，因此这里只在参数专用上下文
        // 临时关闭 macro 解析，让 parseAnnotations 接管完整 annotation。
        // 原先先无条件进入 parseMacroExpression，导致 `@A x: T` 留下未闭合
        // VALUE_PARAMETER marker，并在参数列表闭合时触发 PSI marker 崩溃。
        if (parseContext.allowParseAnnotationsInValueParameter && _atSet(AT, ATEXCL)) {
            with(parseContext.copy(disableMacroParsing = true)) {
                parseAnnotations()
            }
        } else if (!parseContext.disableMacroParsing && at(AT)) {
            expressionParsing.parseMacroExpression(CjScopeKind.UNKNOWN_SCOPE, DeclarationParsingMode.ALL)
            closeDeclarationWithCommentBinders(parameter, VALUE_PARAMETER, false)
            return true
        }
        // 与 Kotlin parseValueParameter 相同，所有参数都消费完整前缀；成员身份由真实 primary 作用域决定。
        // 官方 CheckModifierInParamList 依赖名字/冒号是否破损，因此保留 token 范围直到参数解析完成。
        val modifierList = mark()
        val modifiers = mutableListOf<ParameterModifier>()
        doParseModifierListBody(null, PARAMETER_MODIFIER_KEYWORDS, TokenSet.EMPTY, parsedModifier = { token, marker ->
            val keyword = token as CjKeywordToken
            val start = marker.startOffset - builder.macroParsingJournal.markerOffsetOrigin
            captureModifierSource(keyword, start, start + keyword.value.length)
            val modifier = ParameterModifier(keyword, marker.precede(), mark(), start, parseContext.shouldReportError,
                builder.macroParsingJournal::nextDiagnosticOrder)
            if (modifiers.any { it.token == token }) {
                modifier.report(CangJieParsingBundle.message("parsing.error.parameter.duplicate.modifier", modifier.token.value))
            }
            modifiers += modifier
        })
        val modifierListEnd = mark()
        var grammarStartOffset = builder.currentOffset
        val hasLetOrVar = at(LET_KEYWORD) || at(VAR_KEYWORD)
        val memberKeywordOffset = if (isPrimaryInitFunc && hasLetOrVar) builder.currentOffset else null
        if (hasLetOrVar) {
            val keyword = mark()
            val keywordText = builder.tokenText!!
            advance()
            if (!isPrimaryInitFunc && parseContext.shouldReportError) {
                keyword.error(CangJieParsingBundle.message("parsing.error.parameter.expected.name.or.rparen", keywordText))
            } else {
                keyword.drop()
            }
            if (!isPrimaryInitFunc) grammarStartOffset = builder.currentOffset
        } else if (modifiers.lastOrNull()?.token == CONST_KEYWORD) {
            modifiers.last().report(CangJieParsingBundle.message(
                "parsing.error.parameter.const.declaration",
                if (isPrimaryInitFunc) "const member variable declaration" else "const variable declaration",
                if (isPrimaryInitFunc) "primary constructor paramlist" else "paramlist",
            ))
        }

        val isVariableLengthParameter = at(ELLIPSIS)
        val parameterResult = if (isVariableLengthParameter) {
            // 官方在 name-only/typed 分派前消费省略号；变长参数是否允许由所属声明检查。
            advance()
            val begin = if (eof()) checkNotNull(builder.lastConsumedTokenEndOffset) else builder.currentOffset
            val end = if (eof()) begin else begin + checkNotNull(builder.tokenText).length
            ParameterParseResult(noErrors = true, brokenDeclaration = false, nativeBeginOffset = begin, nativeEndOffset = end)
        } else if (grammar == CjParameterGrammar.ACCESSOR_NAME_ONLY) {
            // 官方 PROP_MEMBER_GETTER_BODY/SETTER_BODY 仅消费名字；冒号、!、默认值留给列表边界诊断。
            val hasName = expect(
                CangJieExpressionParsing.IDENTIFIER_RECOVERY_SET, "Missing parameter name", PARAMETER_NAME_RECOVERY_SET,
            )
            ParameterParseResult(noErrors = hasName, brokenDeclaration = !hasName, nativeBeginOffset = builder.currentOffset)
        } else {
            parseFunctionParameterRest(
                typeRequired = typeRequired,
                cjmpParameterFacts = cjmpParameterFacts,
                parameterStartOffset = parameterStartOffset,
            )
        }
        if (!parameterResult.noErrors && rollbackOnFailure) {
            parameter.rollbackTo()
            return false
        }

        val isMemberParameter = isPrimaryInitFunc && hasLetOrVar
        if (!isMemberParameter) grammarStartOffset = parameterResult.nativeBeginOffset
        val grammarEndOffset = parameterResult.nativeEndOffset ?: if (parameterResult.brokenDeclaration) {
            builder.currentOffset
        } else lastSignificantTokenEndOffset()
        if (sequence != null) {
            sequence.parameters += isVariableLengthParameter
            sequence.parameterRanges += ParameterSourceRange(grammarStartOffset, grammarEndOffset)
            if (sequence.firstNamed != null && !parameterResult.isNamed) {
                errorOnRange(grammarStartOffset, grammarEndOffset,
                    CangJieParsingBundle.message("parsing.error.parameter.unnamed.after.named"))
            }
            if (sequence.firstMember != null && !isMemberParameter) {
                errorOnRange(grammarStartOffset, grammarEndOffset,
                    CangJieParsingBundle.message("parsing.error.parameter.regular.after.member"))
            }
            val completedRange = ParameterSourceRange(parameterStartOffset, grammarEndOffset)
            if (parameterResult.isNamed && sequence.firstNamed == null) sequence.firstNamed = completedRange
            if (isMemberParameter && sequence.firstMember == null) sequence.firstMember = completedRange
        }
        val parameterFileRoleAccepted = if (isMemberParameter) {
            // 官方参数检查在修饰符列表为空时提前返回，不执行文件角色检查。
            val detector = ModifierDetector()
            modifiers.forEach {
                detector(it.token)
                detector.recordParsedModifier(CjmpModifierOccurrence(it.token, it.startOffset,
                    it.startOffset + it.token.value.length,
                    checkNotNull(builder.macroParsingJournal.modifierSourceAt(it.startOffset)),
                ))
            }
            val anchor = checkNotNull(memberKeywordOffset)
            val accepted = modifiers.isEmpty() || checkCjmpFileRole(detector, anchor = anchor)
            builder.macroParsingJournal.registerDeclarationRoleCheck(
                anchor,
                if (modifiers.isEmpty()) CjmpDeclarationRoleCheck.NOT_CHECKED else cjmpDeclarationRoleCheck(accepted, hasModifiers = true),
            )
            builder.macroParsingJournal.registerDeclarationModifiers(anchor,
                detector.freezeDeclarationModifiers(builder.macroParsingJournal.rootIdentity, cjmpContext,
                    builder.macroParsingJournal.sourceTokenAt(anchor), CjmpModifierDeclarationKind.VARIABLE,
                    parseContext.statementScope))
            accepted
        } else true
        checkParameterModifiers(
            modifiers,
            isMemberParameter,
            parameterResult.brokenDeclaration,
            parameterFileRoleAccepted,
            memberKeywordOffset,
        )
        // 有效类型取源码最后 token；缺失类型的 native InvalidType 以 lookahead 起点结束，会覆盖中间 trivia。
        val parameterEndOffset = if (parameterResult.brokenDeclaration) {
            builder.currentOffset
        } else {
            lastSignificantTokenEndOffset()
        }
        val parameterRole = modifiers.map { it.token }.toSet()
        if (isMemberParameter && parameterFileRoleAccepted &&
            parameterResult.declarationName != null &&
            (COMMON_KEYWORD in parameterRole || SPECIFIC_KEYWORD in parameterRole || PLATFORM_KEYWORD in parameterRole)
        ) {
            cjmpParameterFacts?.parameters?.add(
                CjmpParameterFacts(
                    startOffset = parameterStartOffset,
                    endOffset = parameterEndOffset,
                    name = parameterResult.declarationName,
                    isCommon = COMMON_KEYWORD in parameterRole,
                    isSpecific = SPECIFIC_KEYWORD in parameterRole || PLATFORM_KEYWORD in parameterRole,
                ),
            )
        }
        // 先闭合内部 token 范围，再闭合其 MODIFIER_LIST owner，禁止 marker 交叉。
        for (modifier in modifiers.asReversed()) {
            modifier.finish()
        }
        // 对位 Kotlin 参数 modifier list：没有实际修饰符时不造占位节点。
        // 仓颉 annotation 有独立容器；源码与 CJO 参数 stub 必须遵守相同形状。
        if (modifiers.isEmpty()) modifierList.drop() else modifierList.doneBefore(MODIFIER_LIST, modifierListEnd)
        modifierListEnd.drop()

        closeDeclarationWithCommentBinders(parameter, VALUE_PARAMETER, false)

        return true
    }

    /** 参数修饰符的真实 token 范围；错误只包裹该 token，不吞并名字、类型或下一个参数。 */
    private class ParameterModifier(
        val token: CjKeywordToken,
        private val start: PsiBuilder.Marker,
        private val end: PsiBuilder.Marker,
        val startOffset: Int,
        private val reportErrors: Boolean,
        private val nextDiagnosticOrder: () -> CjParsingDiagnosticOrder,
    ) {
        private val reports = mutableListOf<CjSyntaxErrorElementType>()

        /** 实际规则入队即冻结顺序；后续参数/default body 解析和倒序建树不能改变首报。 */
        fun report(message: String, hints: List<ParsingDiagnosticHint> = emptyList()) {
            if (reportErrors) reports += CjSyntaxErrorElementType(message, nextDiagnosticOrder(), hints.map { hint ->
                val basis = if (hint.source == null) startOffset else 0
                CjRelativeErrorHint(hint.startOffset - basis, hint.endOffset - basis, hint.label, hint.source, hint.rangeKind)
            })
        }

        fun finish() {
            if (reports.isEmpty()) {
                start.drop()
            } else {
                // 仅倒序物化原有嵌套，producer 顺序已经保存在入队报告中。
                var error = start
                for ((index, report) in reports.asReversed().withIndex()) {
                    if (index > 0) error = error.precede()
                    error.doneBefore(report, end)
                }
            }
            end.drop()
        }
    }

    /** 对位官方 CheckModifierInParamList；类型和默认值错误不等于 FuncParam 自身破损。 */
    private fun checkParameterModifiers(
        modifiers: List<ParameterModifier>,
        isMember: Boolean,
        brokenDeclaration: Boolean,
        fileRoleAccepted: Boolean,
        memberKeywordOffset: Int?,
    ) {
        if (isMember) {
            modifiers.firstOrNull { it.token == CONST_KEYWORD }?.report(
                CangJieParsingBundle.message("parsing.error.parameter.const.on.let.var"),
            )
            if (!fileRoleAccepted) return
            val keywordOffset = checkNotNull(memberKeywordOffset)
            // 主构造成员的第二个提示指向实际 let/var；负 delta 保留此前 modifier 的位置。
            // 官方两种 primary scope 复用 AGGREGATE_BODY_INSTANCE_INIT_MODIFIERS。
            // 非法修饰符不加入已遍历的合法集合；重复 token 只使用第一次出现的位置。
            val previous = mutableMapOf<CjKeywordToken, ParameterModifier>()
            for (modifier in modifiers.distinctBy { it.token }) {
                if (modifier.token == CONST_KEYWORD) continue
                if (modifier.token !in PARAMETER_ACCESS_MODIFIERS) {
                    modifier.report(CangJieParsingBundle.message("parsing.error.parameter.illegal.member.modifier", modifier.token.value))
                    continue
                }
                for (conflicting in PARAMETER_ACCESS_MODIFIERS) {
                    val earlier = previous[conflicting]
                    if (earlier != null) {
                        modifier.report(CangJieParsingBundle.message(
                            "parsing.error.parameter.conflicting.member.modifiers", modifier.token.value, conflicting.value,
                        ), listOf(
                            ParsingDiagnosticHint(earlier.startOffset, earlier.startOffset + earlier.token.value.length,
                                CangJieParsingBundle.message("parsing.error.hint.conflicted.modifier")),
                            ParsingDiagnosticHint(keywordOffset, keywordOffset + VAR_KEYWORD.value.length,
                                CangJieParsingBundle.message("parsing.error.hint.on", "variable declaration")),
                        ))
                    }
                }
                previous[modifier.token] = modifier
            }
        } else if (!brokenDeclaration) {
            val first = PARAMETER_MODIFIERS_IN_TOKEN_ORDER.firstNotNullOfOrNull { token ->
                modifiers.firstOrNull { it.token == token }
            }
            first?.report(CangJieParsingBundle.message("parsing.error.parameter.no.modifier", first.token.value))
        }
    }

    /** 参数序列保存第一次出现的 named/member 参数，source 列表和展开切片使用同一规则。 */
    private class ParameterSequenceState {
        var firstNamed: ParameterSourceRange? = null
        var firstMember: ParameterSourceRange? = null
        val parameters: MutableList<Boolean> = mutableListOf()
        val parameterRanges: MutableList<ParameterSourceRange> = mutableListOf()
    }

    /** 使用真实声明种类、全部参数列表及本次返回属性，与两条 raw 和结构对应共享投影。 */
    context(parseContext: ParsingContext)
    private fun checkVariadicParameters(
        kind: CjmpModifierDeclarationKind,
        lists: List<ParameterSequenceState>,
        effectiveForeign: Boolean,
        keyword: Int,
        nameEnd: Int,
    ) {
        if (!parseContext.shouldReportError) return
        val result = CjVariadicParameterRules.select(kind, lists.size, effectiveForeign,
            lists.flatMap { it.parameters })
        val ranges = lists.flatMap { it.parameterRanges }
        for (error in result.errors) {
            val range = if (error.kind == CjVariadicParameterRules.ErrorKind.NON_FOREIGN) {
                ParameterSourceRange(keyword, nameEnd)
            } else ranges[error.parameterIndex]
            val message = when (error.kind) {
                CjVariadicParameterRules.ErrorKind.NON_FOREIGN -> "parsing.error.variadic.non.foreign"
                CjVariadicParameterRules.ErrorKind.FIRST_PARAMETER -> "parsing.error.variadic.first.parameter"
                CjVariadicParameterRules.ErrorKind.NOT_LAST_PARAMETER -> "parsing.error.variadic.not.last.parameter"
            }
            errorOnRange(range.startOffset, range.endOffset, CangJieParsingBundle.message(message))
        }
    }

    private data class ParameterSourceRange(val startOffset: Int, val endOffset: Int)

    /** speculative 函数类型解析是否回退，与官方 FuncParam 的 HAS_BROKEN 分开记录。 */
    private data class ParameterParseResult(
        val noErrors: Boolean,
        val brokenDeclaration: Boolean,
        val nativeBeginOffset: Int,
        val nativeEndOffset: Int? = null,
        val declarationName: String? = null,
        val isNamed: Boolean = false,
    )


    /**
     * 解析函数参数的剩余部分
     *
     * 处理参数名称、类型注解和默认值。
     *
     * @param typeRequired 是否必须有类型注解
     * @return 是否成功解析
     */

    context(parseContext: ParsingContext)
    private fun parseFunctionParameterRest(
        typeRequired: Boolean,
        cjmpParameterFacts: CjmpParameterRuleFacts?,
        parameterStartOffset: Int,
    ): ParameterParseResult {
        var noErrors = true
        var brokenDeclaration = false
        var isDefault = false
        var parameterName: String? = null
        var nativeBeginOffset = builder.currentOffset

        // 恢复 'func foo(Array<String>) {}' 和 'func foo(: Int) {}' 这种情况
        if ((at(IDENTIFIER) && lookahead(1) == LT) || at(COLON)) {
            brokenDeclaration = true
            error(CangJieParsingBundle.message("parsing.error.missing.parameter.name")) // 缺少参数名称
            if (at(COLON)) {
                // 保留 noErrors == true 以避免函数类型解析中回滚
                advance() // :
            } else {
                noErrors = false
            }
            parseTypeRef()
        } else {
            parameterName = if (at(IDENTIFIER)) builder.tokenText else null
            if (!expect(
                CangJieExpressionParsing.IDENTIFIER_RECOVERY_SET, "Missing parameter name", PARAMETER_NAME_RECOVERY_SET
            )) {
                brokenDeclaration = true
            }
            // 原生在读取 identifier 后设置 begin，顺序诊断发生在 SetDeclBeginPos 之前。
            nativeBeginOffset = builder.currentOffset

            if (expect(EXCL)) {
                // 可以有默认值
                isDefault = true
            }

            if (at(COLON)) {
                advance() // :

                if (at(IDENTIFIER) && lookahead(1) == COLON) {
                    // 恢复 "func foo(x: y: Int)" 中错误的类型引用
                    error(CangJieParsingBundle.message("parsing.error.type.reference.expected"))
                    return ParameterParseResult(noErrors = false, brokenDeclaration = brokenDeclaration,
                        nativeBeginOffset = nativeBeginOffset, declarationName = parameterName, isNamed = isDefault)
                }

                parseTypeRef()
            } else if (typeRequired) {
                // 普通函数/构造器参数必须写类型；访问器已在共享入口按 name-only 角色结束。
                errorWithRecovery(
                    CangJieParsingBundle.message("parsing.error.parameters.must.have.type.annotation"),
                    PARAMETER_NAME_RECOVERY_SET
                )
                noErrors = false
                brokenDeclaration = true
            }
            // typeRequired=false 只供函数类型的推测解析使用。
        }

        if (at(EQ)) {
            val specificDefaultValueStart = builder.currentOffset.takeIf {
                cjmpParameterFacts?.rejectSpecificDefaultValues == true
            }
            if (isDefault) {
                advance()
            } else {
                error(CangJieParsingBundle.message("parsing.error.default.value.non.named.parameters"))
                errorAndAdvance(CangJieParsingBundle.message("parsing.error.expecting.found", "',' or ')'", "='"))
                noErrors = false
            }


            expressionParsing.parseExpression()

            if (specificDefaultValueStart != null && parseContext.shouldReportError) {
                cjmpParameterFacts?.specificParameterDefaultValues?.add(
                    CjmpParameterDefaultValue(
                        startOffset = parameterStartOffset,
                        endOffset = lastSignificantTokenEndOffset(),
                    ),
                )
            }

        }

        return ParameterParseResult(noErrors, brokenDeclaration, nativeBeginOffset,
            declarationName = parameterName, isNamed = isDefault)
    }


    /**
     * 在平台类型上恢复括号包围的单词
     *
     * 用于处理平台类型的特殊格式。
     *
     * @param offset 偏移量
     * @param word 期望的单词
     * @param consume 是否消费token
     * @return 是否成功处理
     */

    context(parseContext: ParsingContext)
    private fun recoverOnParenthesizedWordForPlatformTypes(
        offset: Int, word: String, consume: Boolean
    ): Boolean {
        // 形如 Array<(out) Foo>! 或 (Mutable)List<Bar>! 的恢复
        if (lookahead(offset) == LPAR && lookahead(offset + 1) == IDENTIFIER && lookahead(offset + 2) == RPAR && lookahead(
                offset + 3
            ) == IDENTIFIER
        ) {
            val error = mark()

            advance(offset)
            advance() // LPAR

            if (word != builder.tokenText) {
                // 不是预期的 "out" 或 "Mutable"
                error.rollbackTo()
                return false
            } else {
                advance() // IDENTIFIER ('out')
                advance() // RPAR

                if (consume) {
                    error.error(CangJieParsingBundle.message("parsing.error.unexpected", "tokens"))
                } else {
                    error.rollbackTo()
                }

                return true
            }
        }
        return false
    }


    /**
     * 解析类型参数列表
     *
     * Grammar:
     * ```
     * typeArgumentList
     *   : "<" typeArgument ("," typeArgument)* ">"
     *   ;
     * ```
     *
     * @return 是否成功解析
     */
    context(parseContext: ParsingContext)
    private fun parseTypeArgumentList(): Boolean {
        if (!at(LT)) return false

        val list = mark()
        tryParseTypeArgumentList(TokenSet.EMPTY)
        list.done(TYPE_ARGUMENT_LIST)

        return true
    }


    /**
     * 在平台类型后缀上恢复
     *
     * 处理平台类型的感叹号后缀。
     */
    context(parseContext: ParsingContext)
    private fun recoverOnPlatformTypeSuffix() {
        // 平台类型的恢复，遇到感叹号报错
        if (at(EXCL)) {
            val error = mark()
            advance() // EXCL
            error.error(CangJieParsingBundle.message("parsing.error.unexpected", "token"))
        }
    }


    /**
     * 解析函数类型
     *
     * Grammar:
     * ```
     * functionType
     *   : "(" parameterList? ")" "->" typeReference
     *   ;
     * ```
     *
     * @param functionType 函数类型标记
     */
    context(parseContext: ParsingContext)
    private fun parseFunctionType(functionType: PsiBuilder.Marker) {
        parseFunctionTypeContents(functionType).done(FUNCTION_TYPE)
    }


    /**
     * 解析函数类型内容
     *
     * @param functionType 函数类型标记
     * @return 处理后的标记
     */
    context(parseContext: ParsingContext)

    private fun parseFunctionTypeContents(functionType: PsiBuilder.Marker): PsiBuilder.Marker {
        assert(_at(LPAR)) { tt()!! }

        parseFunctionTypeParameterList(typeRequired = true)

        expect(ARROW, "Expecting '->' to specify return type of a function type", TYPE_REF_FIRST)
        parseTypeRef()

        return functionType
    }


    /**
     * 解析元组类型
     *
     * Grammar:
     * ```
     * tupleType
     *   : "(" typeReference ("," typeReference)* ")"
     *   ;
     * ```
     *
     * @return 元组中类型的数量
     */
    context(parseContext: ParsingContext)
    private fun parseTupleType(): Int {
        assert(_at(LPAR))
        var count = 0

        advance() // LPAR

        if (!at(RPAR)) {
            while (true) {
                parseTypeRef()
                count++
                if (!at(COMMA)) break
                advance() // COMMA
                // 官方 ParseTypeWithParen 只在至少两个类型后接受元组尾逗号，不能生成空类型项。
                if (count > 1 && at(RPAR)) break
            }
        } else {
            error(CangJieParsingBundle.message("parsing.error.expecting", "type"))
        }

        expectRightParenthesisInTupleType()

        return count
    }

    /**
     * 元组/括号类型缺少右括号时，官方 parser 只报告右定界符缺失并把当前类型标为损坏。
     * 本地也要消费到右括号或声明恢复点，避免后续变量声明层继续报告同一坏类型尾巴。
     */
    context(parseContext: ParsingContext)
    private fun expectRightParenthesisInTupleType() {
        if (expect(RPAR, "Expecting ')'")) return

        while (!eof() && !at(RPAR) && !at(EQ) && !at(LBRACE) && !at(RBRACE) && !at(SEMICOLON)) {
            if (builder.newlineBeforeCurrentToken()) return
            advance()
        }

        if (at(RPAR)) {
            advance()
        }
    }


    /**
     * 解析 VArray 类型
     *
     * Grammar:
     * ```
     * varrayType
     *   : "VArray" "<" typeReference "," "$" integerLiteral ">"
     *   ;
     * ```
     *
     * @return 是否成功解析
     */
    context(parseContext: ParsingContext)

    private fun parseVArrayType(): Boolean {
        if (at(VARRAY_KEYWORD)) {
            val typeRefMarker = mark()
            advance()

            if (at(LT)) {
                val typeArgumentsMismatch = mark()
                advance()
                val list = mark()
                val projection = mark()
                val typeArgumentStartsCorrectly = atVArrayTypeArgumentStart()

                parseTypeRef(TokenSet.EMPTY)

                projection.done(TYPE_PROJECTION)
                list.done(TYPE_ARGUMENT_LIST)

                if (!typeArgumentStartsCorrectly) {
                    typeArgumentsMismatch.drop()
                    skipUntil(TokenSet.create(GT, EOL_OR_SEMICOLON))
                    if (at(GT)) {
                        advance()
                    }
                    typeRefMarker.done(VARRAY_TYPE)
                    return true
                }

                if (!at(COMMA)) {
                    if (at(GT)) {
                        advance()
                    } else {
                        advanceVArrayMismatchLookahead()
                    }
                    typeArgumentsMismatch.error(PARSE_VARRAY_TYPE_ARGS_MISMATCH_MESSAGE)
                    skipUntil(TokenSet.create(EOL_OR_SEMICOLON))
                    typeRefMarker.done(VARRAY_TYPE)
                    return true
                }
                typeArgumentsMismatch.drop()
                advance()

                if (!at(DOLLAR)) {
                    skipVArrayMismatchTrivia()
                    val mismatch = mark()
                    advanceVArrayMismatchLookahead()
                    mismatch.error(PARSE_VARRAY_TYPE_ARGS_MISMATCH_MESSAGE)
                    skipUntil(TokenSet.create(EOL_OR_SEMICOLON))
                    typeRefMarker.done(VARRAY_TYPE)
                    return true
                }
                val dollar = mark()
                advance()

                val tokenStartsOnNextPhysicalLine = physicalNewlineBeforeCurrentToken()
                if (tokenStartsOnNextPhysicalLine || !at(INTEGER_LITERAL)) {
                    dollar.error(PARSE_EXPECT_INTEGER_LITERAL_VARRAY_MESSAGE)
                    if (!tokenStartsOnNextPhysicalLine) {
                        skipUntilPhysicalLineEnd()
                    }
                    typeRefMarker.done(VARRAY_TYPE)
                    return true
                }
                dollar.drop()
                advance()
                expect(GT, "Should be '>'")

            } else {
                error(PARSE_VARRAY_TYPE_PARAMETER_MESSAGE)
            }

            typeRefMarker.done(VARRAY_TYPE)
            return true
        }

        return false
    }

    private fun atVArrayTypeArgumentStart(): Boolean =
        at(VARRAY_KEYWORD) || atSet(BASICTYPES) || at(IDENTIFIER) || at(LPAR) || at(QUEST) || at(THIS_KEYWORD)

    private fun skipVArrayMismatchTrivia() {
        while (at(EOL_COMMENT) || at(BLOCK_COMMENT) || at(DOC_COMMENT) || at(SHEBANG_COMMENT)) {
            advance()
        }
    }

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

    /**
     * 解析用户类型
     *
     * Grammar:
     * ```
     * userType
     *   : simpleUserType ("." simpleUserType)*
     *   ;
     *
     * simpleUserType
     *   : simpleName typeArguments?
     *   ;
     * ```
     *
     * @return 是否包含类型参数列表
     */
    context(parseContext: ParsingContext)
    fun parseUserType(): Boolean {
        var isTypeArgumentList = false

        var userType = mark()

        var reference = mark()

        while (true) {
            recoverOnParenthesizedWordForPlatformTypes(0, "Mutable", consume = true)
            if (expect(
                    IDENTIFIER, "Expecting type name", TokenSet.orSet(
                        CangJieExpressionParsing.EXPRESSION_FIRST,
                        CangJieExpressionParsing.EXPRESSION_FOLLOW,
                        DECLARATION_FIRST
                    )
                )
            ) {
                reference.done(REFERENCE_EXPRESSION)
            } else {
                reference.drop()
                break
            }

            isTypeArgumentList = parseTypeArgumentList()

//      recoverOnPlatformTypeSuffix()  // 如果需要恢复平台类型后缀，可以取消注释

            if (!at(DOT)) {
                break
            }

            val precede = userType.precede()
            userType.done(USER_TYPE)
            userType = precede

            advance() // DOT
            reference = mark()
        }

        userType.done(USER_TYPE)
        return isTypeArgumentList
    }


    /**
     * 解析 This 类型
     *
     * Grammar:
     * ```
     * thisType
     *   : "This"
     *   ;
     * ```
     *
     * @return 是否成功解析
     */
    context(parseContext: ParsingContext)
    fun parseThisType(): Boolean {
        if (at(THIS_KEYWORD_UPPER)) {
            val typeRefMarker = mark()
            advance()
            if (!thisTypeAllowed && parseContext.shouldReportError) {
                typeRefMarker.error(CangJieParsingBundle.message("parsing.error.this.type.not.allowed"))
            } else {
                typeRefMarker.done(THIS_TYPE)
            }
            return true
        }
        return false
    }

    /**
     * 解析基本类型
     *
     * Grammar:
     * ```
     * basicType
     *   : "Bool" | "Int8" | "Int16" | "Int32" | "Int64"
     *   | "UInt8" | "UInt16" | "UInt32" | "UInt64"
     *   | "Float32" | "Float64" | "Rune" | "Unit"
     *   ;
     * ```
     *
     * @return 是否成功解析
     */
    fun parseBasicType(): Boolean {
        if (atSet(BASICTYPES)) {
            val typeRefMarker = mark()
            advance()
            typeRefMarker.done(BASIC_TYPE)
            return true
        }
        return false
    }

    /**
     * 解析类型参数列表
     */
    context(parseContext: ParsingContext)
    fun tryParseTypeArgumentList(extraRecoverySet: TokenSet): Boolean {
        builder.disableNewlines()
        advance() // LT

        do {
            if (at(COMMA)) {
                advance()
            }

            val projection = mark()

            parseTypeRef(extraRecoverySet)

            projection.done(TYPE_PROJECTION)

            if (at(GT)) break
        } while (at(COMMA))

        val atGT = at(GT)
        if (!atGT) {
            error(CangJieParsingBundle.message("parsing.error.expecting.symbol", ">"))
        } else {
            advance() // GT
        }
        builder.restoreNewlinesState()
        return atGT
    }


    /**
     * 解析可选类型
     *
     * Grammar:
     * ```
     * optionalType
     *   : "?" typeReference
     *   ;
     * ```
     */
    context(parseContext: ParsingContext)
    fun parseOptionType() {
        assert(_at(QUEST))

        val optionTypeMarker = mark()

        advance()

        parseTypeRefContents()

        optionTypeMarker.done(OPTIONAL_TYPE)
    }

    /**
     * 解析类型引用内容
     *
     * 根据当前token类型选择合适的类型解析方法。
     *
     * @param extraRecoverySet 额外的恢复伤口token集合
     * @param isConstraint 是否为约束类型
     */
    context(parseContext: ParsingContext)
    fun parseTypeRef(
        extraRecoverySet: TokenSet = TokenSet.EMPTY, isConstraint: Boolean = false
    ) {
        val typeRefMarker = mark()

        if (!isConstraint) {
            parseTypeRefContents(extraRecoverySet)
        } else {
            parseIdentifier()
        }

        typeRefMarker.done(TYPE_REFERENCE)
    }


    /**
     * 解析元组或函数类型
     *
     * 根据上下文决定是解析为元组还是函数类型。
     */
    context(parseContext: ParsingContext)
    private fun parseTupleOrFunctionType() {
        var oType = mark()

        val count = parseTupleType()

        if (at(ARROW) || at(COLON)) {
            oType.rollbackTo()
            oType = mark()
            parseFunctionType(oType)
        } else {
            if (count <= 1) {
                oType.done(PARENTHESIZED_TYPE)
            } else {
                oType.done(TUPLE_TYPE)
            }
        }
    }

    /**
     * 解析类型引用内容的具体实现
     *
     * 根据当前token类型选择合适的类型解析方法。
     */

    context(parseContext: ParsingContext)
    private fun parseTypeRefContents(extraRecoverySet: TokenSet = TokenSet.EMPTY) {
        when {
            parseVArrayType() -> return
            parseThisType() -> return
            parseBasicType() -> return
            at(IDENTIFIER) -> parseUserType()
            at(LPAR) -> parseTupleOrFunctionType()
            at(QUEST) -> parseOptionType()
            else -> errorWithRecovery(
                CangJieParsingBundle.message(
                    "parsing.error.expecting.found", "type name", "${builder.tokenText}"
                ),
                TokenSet.orSet(TYPE_REF_RECOVERY_SET, extraRecoverySet),
            )
        }
    }


}

/**
 * 声明位置出现的宏形态。
 *
 * [MACRO_CALL] 是显式宏调用语法（`@Name[...]`），[ANNOTATION] 是
 * 宏以注解形式出现；两者进入不同的展开流水线。
 */
enum class MacroType {
    MACRO_CALL, ANNOTATION,
}

/**
 * 声明解析的上下文模式。
 *
 * 三个开关分别控制：是否允许解构声明（[destructuringAllowed]）、
 * 是否允许 accessor 修饰的声明（[accessorsAllowed]）、以及 `enum`
 * 是否可按软关键字处理（[canBeEnumUsedAsSoftKeyword]）；
 * 不同声明位置按官方语法选择对应组合。
 */
enum class DeclarationParsingMode(
    val destructuringAllowed: Boolean, val accessorsAllowed: Boolean, val canBeEnumUsedAsSoftKeyword: Boolean
) {
    ALL(false, true, true), TOPLEVEL(false, true, true), MEMBER(false, true, true), MEMBER_OR_TOPLEVEL(
        false, true, true
    ),
    LOCAL(true, false, false),
    /** 枚举条目列表中的声明宏，其标识符输入是枚举构造项。 */
    ENUM_CONSTRUCTOR(false, false, false)
    // SCRIPT_TOPLEVEL(true, true, false) // 如需使用可以取消注释
}

/**
 * 控制在解析声明时对名称标识符的处理方式。
 *
 * - [REQUIRED]：必须出现标识符，缺失时会报错。
 * - [ALLOWED]：标识符可选，存在则消费，不存在也不会报错。
 * - [PROHIBITED]：禁止出现标识符，出现时会报错并前进。
 */
enum class NameParsingMode {
    REQUIRED, ALLOWED, PROHIBITED
}
