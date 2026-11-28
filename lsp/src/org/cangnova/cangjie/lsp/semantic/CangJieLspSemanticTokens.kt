package org.cangnova.cangjie.lsp.semantic

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiRecursiveElementVisitor
import com.intellij.psi.TokenType
import com.intellij.psi.tree.TokenSet
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.macro.expansion.MacroInvocationSemanticKind
import org.cangnova.cangjie.analysis.macro.expansion.MacroSemanticExpansionService
import org.cangnova.cangjie.analysis.api.resolution.CaCall
import org.cangnova.cangjie.analysis.api.resolution.successfulCallOrNull
import org.cangnova.cangjie.analysis.api.symbols.CaClassifierSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaConstructorSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFunctionSymbol
import org.cangnova.cangjie.codeinsight.highlighting.CangJieSemanticHighlighting
import org.cangnova.cangjie.codeinsight.highlighting.CangJieSemanticHighlightingKind
import org.cangnova.cangjie.codeinsight.highlighting.CangJieSemanticHighlightingRules
import org.cangnova.cangjie.codeinsight.highlighting.CangJieHighlightingLexer
import org.cangnova.cangjie.codeinsight.highlighting.CangJieSemanticHighlightingRules.resolveSuccessfulSymbol
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.lexer.cdoc.lexer.CDocTokens
import org.cangnova.cangjie.lexer.cdoc.lexer.CDocTokens.CDOC_HIGHLIGHT_TOKENS
import org.cangnova.cangjie.lexer.cdoc.psi.impl.CDocLink
import org.cangnova.cangjie.psi.CjCallExpression
import org.cangnova.cangjie.psi.CjBinaryExpression
import org.cangnova.cangjie.psi.CjAnnotation
import org.cangnova.cangjie.psi.CjBasicType
import org.cangnova.cangjie.psi.CjClass
import org.cangnova.cangjie.psi.CjConstructorCalleeExpression
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjEnum
import org.cangnova.cangjie.psi.CjEnumConstructor
import org.cangnova.cangjie.psi.CjExtend
import org.cangnova.cangjie.psi.CjExpressionWithLabel
import org.cangnova.cangjie.psi.CjFieldVariable
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjInterface
import org.cangnova.cangjie.psi.CjLambdaExpression
import org.cangnova.cangjie.psi.CjMacroDeclaration
import org.cangnova.cangjie.psi.CjMacroExpression
import org.cangnova.cangjie.psi.CjNameBasicReferenceExpression
import org.cangnova.cangjie.psi.CjNameReferenceExpression
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjParameter
import org.cangnova.cangjie.psi.CjPatternVariable
import org.cangnova.cangjie.psi.CjProperty
import org.cangnova.cangjie.psi.CjQualifiedExpression
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.cangnova.cangjie.psi.CjStruct
import org.cangnova.cangjie.psi.CjSuperTypeCallEntry
import org.cangnova.cangjie.psi.CjTypeAlias
import org.cangnova.cangjie.psi.CjTypeParameter
import org.cangnova.cangjie.psi.CjTypeReference
import org.cangnova.cangjie.psi.CjUserType
import org.cangnova.cangjie.psi.CjVariable
import org.cangnova.cangjie.psi.CjValueArgument
import org.cangnova.cangjie.psi.CjValueArgumentName
import org.cangnova.cangjie.psi.CjVisitorUnit
import org.cangnova.cangjie.psi.psiUtil.getStrictParentOfType
import org.cangnova.cangjie.psi.stubs.elements.getAllPatternDeclarations

/**
 * 表示一个已经映射到 LSP legend 的仓颉语义 token。
 */
data class CangJieSemanticToken(
    /**
     * token 在 PSI 文件文本中的范围。
     */
    val range: TextRange,

    /**
     * token 的 LSP 类型。
     */
    val type: CangJieSemanticTokenType,

    /**
     * token 的 LSP 修饰符集合。
     */
    val modifiers: Set<CangJieSemanticTokenModifier> = emptySet(),
)

/**
 * 仓颉语义 token 类型到 LSP token type 名称的映射。
 */
enum class CangJieSemanticTokenType(val lspName: String) {
    /** 关键字或基础类型关键字。 */
    KEYWORD("keyword"),
    /** 普通注释、文档注释和 CDoc 标签。 */
    COMMENT("comment"),
    /** 字符串、字符和字符串模板片段。 */
    STRING("string"),
    /** 整数或浮点数字面量。 */
    NUMBER("number"),
    /** 运算符或无法识别的坏字符。 */
    OPERATOR("operator"),
    /** 类声明或类类型引用。 */
    CLASS("class"),
    /** 结构体声明。 */
    STRUCT("struct"),
    /** 接口声明。 */
    INTERFACE("interface"),
    /** 枚举声明。 */
    ENUM("enum"),
    /** 枚举构造项。 */
    ENUM_MEMBER("enumMember"),
    /** 普通类型引用或类型别名。 */
    TYPE("type"),
    /** 类型参数声明。 */
    TYPE_PARAMETER("typeParameter"),
    /** 函数声明或普通调用。 */
    FUNCTION("function"),
    /** 成员方法调用。 */
    METHOD("method"),
    /** 宏声明或宏调用。 */
    MACRO("macro"),
    /** 局部变量或参数风格变量。 */
    VARIABLE("variable"),
    /** 属性声明。 */
    PROPERTY("property"),
    /** 参数 token。 */
    PARAMETER("parameter"),
    /** 注解调用名称。 */
    ANNOTATION("decorator"),
    /** 标签 token。 */
    LABEL("label"),
    ;

    companion object {
        /**
         * 按 enum 顺序暴露给 LSP semantic token legend 的类型名称。
         */
        val lspValues: List<String> = entries.map(CangJieSemanticTokenType::lspName)
    }
}

/**
 * 仓颉语义 token 修饰符到 LSP modifier 名称的映射。
 */
enum class CangJieSemanticTokenModifier(val lspName: String) {
    /** token 表示声明位置。 */
    DECLARATION("declaration"),
    /** token 表示定义位置。 */
    DEFINITION("definition"),
    /** token 表示只读实体。 */
    READONLY("readonly"),
    /** token 表示静态实体。 */
    STATIC("static"),
    /** token 来自文档注释或文档标签。 */
    DOCUMENTATION("documentation"),
    /** token 表示抽象 class。 */
    ABSTRACT("abstract"),
    /** token 表示构造器调用。 */
    CONSTRUCTOR("constructor"),
    /** token 表示扩展成员。 */
    EXTENSION("extension"),
    /** token 表示字段。 */
    FIELD("field"),
    /** token 表示函数值调用。 */
    FUNCTION_VALUE("functionValue"),
    /** token 表示仓颉基本类型引用。 */
    BASIC_TYPE("basicType"),
    /** token 表示顶层函数调用。 */
    PACKAGE("package"),
    /** token 表示可变声明或引用。 */
    MUTABLE("mutable"),
    /** token 表示自定义访问器属性。 */
    CUSTOM_PROPERTY("customProperty"),
    /** token 表示类型别名。 */
    TYPE_ALIAS("typeAlias"),
    /** token 表示注解调用中的命名属性。 */
    ANNOTATION_ATTRIBUTE("annotationAttribute"),
    /** token 是宏 Tokens 输入中的转义定界符。 */
    MACRO_ESCAPE("macroEscape"),
    /** token 是 Option 可选链中的问号。 */
    SAFE_ACCESS("safeAccess"),
    ;

    companion object {
        /**
         * 按 enum 顺序暴露给 LSP semantic token legend 的修饰符名称。
         */
        val lspValues: List<String> = entries.map(CangJieSemanticTokenModifier::lspName)
    }
}

/**
 * LSP semantic token 收集器。
 *
 * 词法与声明 token 由本协议适配层收集；引用和调用类别复用 code-insight 的 Analysis API
 * 分类规则，再投影为 LSP token type/modifier。
 */
object CangJieSemanticTokenCollector {
    /**
     * 需要按 LSP operator 处理的仓颉词法 token 集合。
     */
    private val operatorTokens: TokenSet = TokenSet.andNot(
        CjTokens.OPERATIONS,
        TokenSet.orSet(
            TokenSet.create(CjTokens.IDENTIFIER, CjTokens.AT),
            CjTokens.KEYWORDS,
        ),
    )

    /**
     * 字符串和字符字面量主体 token 集合。
     */
    private val stringTokens: TokenSet = TokenSet.create(
        CjTokens.OPEN_QUOTE,
        CjTokens.CLOSING_QUOTE,
        CjTokens.REGULAR_STRING_PART,
        CjTokens.RUNE_LITERAL,
    )

    /**
     * 字符串模板和转义片段 token 集合。
     */
    private val stringEscapeTokens: TokenSet = TokenSet.create(
        CjTokens.ESCAPE_SEQUENCE,
        CjTokens.SHORT_TEMPLATE_ENTRY_START,
        CjTokens.LONG_TEMPLATE_ENTRY_START,
        CjTokens.LONG_TEMPLATE_ENTRY_END,
    )

    /** 收集指定 PSI 文件的词法、声明和 Analysis API 语义 token。 */
    fun collect(file: CjFile, session: CaSession, range: TextRange? = null): List<CangJieSemanticToken> {
        val tokensByRange = linkedMapOf<String, CangJieSemanticToken>()
        collectLexicalTokens(file.text, range, tokensByRange)
        collectStructuralTokens(file, session, range, tokensByRange)
        return tokensByRange.values.sortedWith(tokenOrder)
    }

    /**
     * 通过高亮 lexer 收集关键字、注释、字符串、数字和运算符 token。
     */
    private fun collectLexicalTokens(
        text: CharSequence,
        range: TextRange?,
        destination: MutableMap<String, CangJieSemanticToken>,
    ) {
        val lexer = CangJieHighlightingLexer()
        lexer.start(text)
        var pendingQuestionMark: TextRange? = null
        while (lexer.tokenType != null) {
            val tokenType = lexer.tokenType!!
            val tokenRange = TextRange(lexer.tokenStart, lexer.tokenEnd)
            if (!CjTokens.WHITE_SPACE_OR_COMMENT_BIT_SET.contains(tokenType)) {
                pendingQuestionMark?.let { questionRange ->
                    if (questionRange.shouldInclude(range) &&
                        CangJieSemanticHighlightingRules.isSafeAccessQuestionMark(CjTokens.QUEST, tokenType)
                    ) {
                        val classification = CangJieSemanticHighlightingRules.safeAccess()
                        putToken(
                            destination,
                            CangJieSemanticToken(
                                questionRange,
                                classification.kind.toLspType(),
                                setOf(CangJieSemanticTokenModifier.SAFE_ACCESS),
                            ),
                        )
                    }
                }
                pendingQuestionMark = null
            }

            val type = semanticTypeOfLexicalToken(tokenType)
            if (type != null && tokenRange.shouldInclude(range)) {
                putToken(destination, CangJieSemanticToken(tokenRange, type, modifiersOf(tokenType)))
            }

            if (tokenType == CjTokens.QUEST) pendingQuestionMark = tokenRange
            lexer.advance()
        }
    }

    /**
     * 将词法 token 类型映射为 LSP 语义 token 类型。
     */
    private fun semanticTypeOfLexicalToken(tokenType: com.intellij.psi.tree.IElementType): CangJieSemanticTokenType? = when {
        tokenType in CjTokens.KEYWORDS || tokenType in CjTokens.BASICTYPES || tokenType in CjTokens.SOFT_KEYWORDS ->
            CangJieSemanticTokenType.KEYWORD

        tokenType == CjTokens.EOL_COMMENT ||
            tokenType == CjTokens.SHEBANG_COMMENT ||
            tokenType == CjTokens.BLOCK_COMMENT ||
            tokenType == CjTokens.DOC_COMMENT ||
            tokenType in CDOC_HIGHLIGHT_TOKENS ||
            tokenType == CDocTokens.TAG_NAME -> CangJieSemanticTokenType.COMMENT

        tokenType in stringTokens || tokenType in stringEscapeTokens -> CangJieSemanticTokenType.STRING
        tokenType == CjTokens.INTEGER_LITERAL || tokenType == CjTokens.FLOAT_LITERAL -> CangJieSemanticTokenType.NUMBER
        tokenType in operatorTokens -> CangJieSemanticTokenType.OPERATOR
        tokenType == TokenType.BAD_CHARACTER || tokenType == CjTokens.UNKNOWN_TOKEN_START -> CangJieSemanticTokenType.OPERATOR
        else -> null
    }

    /**
     * 根据词法 token 类型计算 LSP 语义修饰符。
     */
    private fun modifiersOf(tokenType: com.intellij.psi.tree.IElementType): Set<CangJieSemanticTokenModifier> = when {
        tokenType == CjTokens.DOC_COMMENT ||
            tokenType in CDOC_HIGHLIGHT_TOKENS ||
            tokenType == CDocTokens.TAG_NAME -> setOf(CangJieSemanticTokenModifier.DOCUMENTATION)

        else -> emptySet()
    }

    /**
     * 通过 PSI 结构收集声明、引用、调用和标签等语义 token。
     */
    private fun collectStructuralTokens(
        file: CjFile,
        session: CaSession,
        range: TextRange?,
        destination: MutableMap<String, CangJieSemanticToken>,
    ) {
        val visitor = object : CjVisitorUnit() {
            override fun visitElement(element: PsiElement) {
                val elementType = element.node?.elementType
                if (element is CjAnnotation) {
                    val classification = CangJieSemanticHighlightingRules.classifyAnnotationCall(session, element)
                    val name = element.calleeExpression?.constructorReferenceExpression
                    if (classification != null && name != null) {
                        addSemanticToken(destination, name, classification, range)
                    }
                }

                if (element is CjConstructorCalleeExpression &&
                    element.parent !is CjSuperTypeCallEntry &&
                    element.parent !is CjAnnotation
                ) {
                    val constructor = resolveSuccessfulSymbol(session, element) as? CaConstructorSymbol
                    val classification = constructor?.let { CangJieSemanticHighlightingRules.classifyConstructor(session, it) }
                    if (classification != null) {
                        addSemanticToken(destination, element.constructorReferenceExpression, classification, range)
                    }
                }

                if (element is CjValueArgument) {
                    val argumentName = element.getArgumentName() as? CjValueArgumentName
                    if (argumentName != null) {
                        val classification = if (element.getStrictParentOfType<CjAnnotation>() != null) {
                            CangJieSemanticHighlightingRules.classifyAnnotationAttributeName(session, argumentName)
                        } else {
                            CangJieSemanticHighlightingRules.namedArgument()
                        }
                        if (classification != null) {
                            addSemanticToken(destination, argumentName.referenceExpression, classification, range)
                        }
                    }
                }

                when {
                    element is CDocLink -> addStructuralToken(destination, element, CangJieSemanticTokenType.TYPE, range)
                    elementType != null && elementType in CjTokens.SOFT_KEYWORDS ->
                        addStructuralToken(destination, element, CangJieSemanticTokenType.KEYWORD, range)
                }
            }

            override fun visitExpressionWithLabel(expression: CjExpressionWithLabel) {
                addStructuralToken(destination, expression.getTargetLabel(), CangJieSemanticTokenType.LABEL, range)
                super.visitExpressionWithLabel(expression)
            }

            override fun visitSuperTypeCallEntry(call: CjSuperTypeCallEntry) {
                val typeElement = call.calleeExpression.typeReference?.typeElement
                if (typeElement is CjUserType) {
                    val constructor = resolveSuccessfulSymbol(session, call) as? CaConstructorSymbol
                    val classification = constructor?.let { CangJieSemanticHighlightingRules.classifyConstructor(session, it) }
                    if (classification != null) {
                        addSemanticToken(destination, typeElement.referenceExpression, classification, range)
                    }
                }
                super.visitSuperTypeCallEntry(call)
            }

            override fun visitTypeParameter(parameter: CjTypeParameter) {
                addDeclarationToken(destination, parameter.nameIdentifier, CangJieSemanticTokenType.TYPE_PARAMETER, range)
                super.visitTypeParameter(parameter)
            }

            override fun visitParameter(parameter: CjParameter) {
                val isPropertyParameter = parameter.letOrVarKeyword != null
                val modifiers = buildList {
                    if (isPropertyParameter && !parameter.isMutable) add(CangJieSemanticTokenModifier.READONLY)
                    if (isPropertyParameter && parameter.isMutable) add(CangJieSemanticTokenModifier.MUTABLE)
                }.toTypedArray()
                addDeclarationToken(
                    destination,
                    parameter.nameIdentifier,
                    if (isPropertyParameter) CangJieSemanticTokenType.PROPERTY else CangJieSemanticTokenType.PARAMETER,
                    range,
                    *modifiers,
                )
                super.visitParameter(parameter)
            }

            override fun visitBasicType(type: CjBasicType) {
                val classification = CangJieSemanticHighlightingRules.classifyBasicType()
                addSemanticToken(destination, type, classification, range)
                super.visitBasicType(type)
            }

            override fun visitUserType(type: CjUserType) {
                val typeReference = type.getStrictParentOfType<CjTypeReference>()
                val reference = type.referenceExpression
                if (typeReference?.typeElement === type && reference != null && !reference.isConstructorCalleeName()) {
                    val symbol = resolveSuccessfulSymbol(session, typeReference) as? CaClassifierSymbol
                    val classification = symbol?.let(CangJieSemanticHighlightingRules::classifyClassifier)
                    if (classification != null) addSemanticToken(destination, reference, classification, range)
                }
                super.visitUserType(type)
            }

            override fun visitNamedFunction(function: CjNamedFunction) {
                addDeclarationToken(destination, function.nameIdentifier, CangJieSemanticTokenType.FUNCTION, range)
                super.visitNamedFunction(function)
            }

            override fun visitEnum(cenum: CjEnum) {
                addDeclarationToken(destination, cenum.nameIdentifier, CangJieSemanticTokenType.ENUM, range)
                super.visitEnum(cenum)
            }

            override fun visitEnumConstructor(enumConstructor: CjEnumConstructor) {
                addDeclarationToken(destination, enumConstructor.nameIdentifier, CangJieSemanticTokenType.ENUM_MEMBER, range)
                super.visitEnumConstructor(enumConstructor)
            }

            override fun visitVariable(variable: CjVariable<*>) {
                addDeclarationToken(
                    destination,
                    variable.nameIdentifier,
                    CangJieSemanticTokenType.VARIABLE,
                    range,
                    *variableModifiersOf(variable),
                )
                super.visitVariable(variable)
            }

            override fun visitFieldVariable(field: CjFieldVariable) {
                addDeclarationToken(
                    destination,
                    field.nameIdentifier,
                    CangJieSemanticTokenType.VARIABLE,
                    range,
                    *variableModifiersOf(field),
                )
                super.visitFieldVariable(field)
            }

            override fun visitPatternVariable(variable: CjPatternVariable) {
                val modifiers = variableModifiersOf(variable)
                variable.pattern.getAllPatternDeclarations().forEach { declaration ->
                    val nameIdentifier = (declaration as? PsiNameIdentifierOwner)?.nameIdentifier
                    addDeclarationToken(
                        destination,
                        nameIdentifier,
                        CangJieSemanticTokenType.VARIABLE,
                        range,
                        *modifiers,
                    )
                }
                super.visitPatternVariable(variable)
            }

            override fun visitProperty(property: CjProperty) {
                addDeclarationToken(
                    destination,
                    property.nameIdentifier,
                    CangJieSemanticTokenType.PROPERTY,
                    range,
                    *propertyModifiersOf(property),
                )
                super.visitProperty(property)
            }

            override fun visitTypeAlias(typeAlias: CjTypeAlias) {
                addDeclarationToken(destination, typeAlias.nameIdentifier, CangJieSemanticTokenType.TYPE, range)
                super.visitTypeAlias(typeAlias)
            }

            override fun visitClass(cclass: CjClass) {
                addDeclarationToken(destination, cclass.nameIdentifier, CangJieSemanticTokenType.CLASS, range)
                super.visitClass(cclass)
            }

            override fun visitStruct(cstruct: CjStruct) {
                addDeclarationToken(destination, cstruct.nameIdentifier, CangJieSemanticTokenType.STRUCT, range)
                super.visitStruct(cstruct)
            }

            override fun visitInterface(cinterface: CjInterface) {
                addDeclarationToken(destination, cinterface.nameIdentifier, CangJieSemanticTokenType.INTERFACE, range)
                super.visitInterface(cinterface)
            }

            override fun visitCallExpression(expression: CjCallExpression) {
                val callee = expression.calleeExpression
                if (callee != null && callee !is CjLambdaExpression && callee !is CjCallExpression) {
                    val call = with(session) { expression.resolveToCall()?.successfulCallOrNull<CaCall>() }
                    val classification = call?.let { CangJieSemanticHighlightingRules.classifyCall(session, it) }
                    val nameElement = callee.semanticCallNameElement()
                    if (classification != null) {
                        addSemanticToken(destination, nameElement, classification, range)
                    }
                }
                super.visitCallExpression(expression)
            }

            override fun visitBinaryExpression(expression: CjBinaryExpression) {
                val operationReference = expression.operationReference
                if (operationReference.operationSignTokenType != CjTokens.EQ) {
                    val function = resolveSuccessfulSymbol(session, expression) as? CaFunctionSymbol
                    val classification = function?.let(CangJieSemanticHighlightingRules::classifyFunction)
                    if (classification != null) {
                        addSemanticToken(destination, operationReference, classification, range)
                    }
                }
                super.visitBinaryExpression(expression)
            }

            override fun visitSimpleNameExpression(expression: CjSimpleNameExpression) {
                val isCallCallee = expression.isCallCallee()
                val skip = expression.parent is CjValueArgumentName || expression.isDeclarationName() ||
                    expression.isConstructorCalleeName()
                val isReference = expression is CjNameReferenceExpression || expression is CjNameBasicReferenceExpression
                val symbol = if (skip || !isReference) null else resolveSuccessfulSymbol(session, expression)
                if (symbol != null && isCallCallee && !CangJieSemanticHighlightingRules.isValueReferenceSymbol(symbol)) return
                val classification = symbol?.let(CangJieSemanticHighlightingRules::classifyReference)
                classification ?: return
                addSemanticToken(destination, expression, classification, range)
            }

            override fun visitMacroExpression(expression: CjMacroExpression) {
                CangJieSemanticHighlightingRules.macroEscapeTokenElements(expression).forEach { escapedToken ->
                    addSemanticToken(
                        destination,
                        escapedToken,
                        CangJieSemanticHighlightingRules.macroEscape(),
                        range,
                    )
                }
                val nameElement = expression.referenceExpression?.referencedNameElement
                when (MacroSemanticExpansionService.getInstance(expression.project).invocationKind(expression)) {
                    MacroInvocationSemanticKind.MACRO -> if (nameElement != null) {
                        addSemanticToken(destination, nameElement, CangJieSemanticHighlightingRules.macroCall(), range)
                    }

                    MacroInvocationSemanticKind.BUILTIN_ANNOTATION -> {
                        val classification = CangJieSemanticHighlightingRules.annotationCall()
                        if (nameElement != null) addSemanticToken(destination, nameElement, classification, range)
                        CangJieSemanticHighlightingRules.builtinMacroAnnotationAttributeNameElements(expression)
                            .forEach { attributeName ->
                                addSemanticToken(
                                    destination,
                                    attributeName,
                                    CangJieSemanticHighlightingRules.annotationAttributeName(),
                                    range,
                                )
                            }
                    }

                    MacroInvocationSemanticKind.ANNOTATION_CANDIDATE -> {
                        val classification = CangJieSemanticHighlightingRules.classifyMacroAnnotationCall(
                            session,
                            expression,
                        )
                        if (classification != null && nameElement != null) {
                            addSemanticToken(destination, nameElement, classification, range)
                            CangJieSemanticHighlightingRules.macroAnnotationAttributeNameElements(session, expression)
                                .forEach { attributeName ->
                                    addSemanticToken(
                                        destination,
                                        attributeName,
                                        CangJieSemanticHighlightingRules.annotationAttributeName(),
                                        range,
                                    )
                                }
                        }
                    }

                    null -> Unit
                }
                super.visitMacroExpression(expression)
            }

            override fun visitMacroDeclaration(function: CjMacroDeclaration) {
                val kind = CangJieSemanticHighlightingRules.macroDeclaration().kind
                addDeclarationToken(destination, function.nameIdentifier, kind.toLspType(), range)
                super.visitMacroDeclaration(function)
            }
        }

        file.accept(object : PsiRecursiveElementVisitor() {
            override fun visitElement(element: PsiElement) {
                element.accept(visitor)
                super.visitElement(element)
            }
        })
    }

    /**
     * 添加带声明和定义修饰符的结构 token。
     */
    private fun addDeclarationToken(
        destination: MutableMap<String, CangJieSemanticToken>,
        element: PsiElement?,
        type: CangJieSemanticTokenType,
        range: TextRange?,
        vararg extraModifiers: CangJieSemanticTokenModifier,
    ) {
        val modifiers = linkedSetOf(
            CangJieSemanticTokenModifier.DECLARATION,
            CangJieSemanticTokenModifier.DEFINITION,
        ).apply {
            addAll(extraModifiers)
        }
        addStructuralToken(destination, element, type, range, modifiers)
    }

    /** 添加由共享 Analysis API 分类器产生的语义 token。 */
    private fun addSemanticToken(
        destination: MutableMap<String, CangJieSemanticToken>,
        element: PsiElement?,
        classification: CangJieSemanticHighlighting,
        range: TextRange?,
    ) {
        val modifiers = buildSet {
            if (classification.isMutable == false) add(CangJieSemanticTokenModifier.READONLY)
            if (classification.isStatic) add(CangJieSemanticTokenModifier.STATIC)
            if (CangJieSemanticHighlightingKind.MUTABLE_VARIABLE in classification.additionalKinds ||
                CangJieSemanticHighlightingKind.MUTABLE_PROPERTY in classification.additionalKinds
            ) {
                add(CangJieSemanticTokenModifier.MUTABLE)
            }
            when (classification.kind) {
                CangJieSemanticHighlightingKind.ABSTRACT_CLASS -> add(CangJieSemanticTokenModifier.ABSTRACT)
                CangJieSemanticHighlightingKind.BASIC_TYPE_REFERENCE -> add(CangJieSemanticTokenModifier.BASIC_TYPE)
                CangJieSemanticHighlightingKind.CLASS_CONSTRUCTOR_CALL,
                CangJieSemanticHighlightingKind.CONSTRUCTOR_CALL,
                CangJieSemanticHighlightingKind.STRUCT_CONSTRUCTOR_CALL,
                -> add(CangJieSemanticTokenModifier.CONSTRUCTOR)

                CangJieSemanticHighlightingKind.EXTENSION_FUNCTION_CALL,
                CangJieSemanticHighlightingKind.EXTENSION_PROPERTY,
                -> add(CangJieSemanticTokenModifier.EXTENSION)

                CangJieSemanticHighlightingKind.PACKAGE_VARIABLE,
                CangJieSemanticHighlightingKind.PACKAGE_FUNCTION_CALL,
                -> add(CangJieSemanticTokenModifier.PACKAGE)

                CangJieSemanticHighlightingKind.INSTANCE_VARIABLE,
                CangJieSemanticHighlightingKind.STATIC_VARIABLE,
                -> add(CangJieSemanticTokenModifier.FIELD)

                CangJieSemanticHighlightingKind.VARIABLE_AS_FUNCTION_CALL ->
                    add(CangJieSemanticTokenModifier.FUNCTION_VALUE)

                CangJieSemanticHighlightingKind.MACRO_ESCAPE ->
                    add(CangJieSemanticTokenModifier.MACRO_ESCAPE)
                CangJieSemanticHighlightingKind.SAFE_ACCESS ->
                    add(CangJieSemanticTokenModifier.SAFE_ACCESS)

                CangJieSemanticHighlightingKind.INSTANCE_PROPERTY_CUSTOM_PROPERTY_DECLARATION,
                CangJieSemanticHighlightingKind.STATIC_PROPERTY_CUSTOM_PROPERTY_DECLARATION,
                -> add(CangJieSemanticTokenModifier.CUSTOM_PROPERTY)

                CangJieSemanticHighlightingKind.TYPE_ALIAS -> add(CangJieSemanticTokenModifier.TYPE_ALIAS)
                CangJieSemanticHighlightingKind.ANNOTATION_ATTRIBUTE_NAME ->
                    add(CangJieSemanticTokenModifier.ANNOTATION_ATTRIBUTE)
                else -> Unit
            }
        }
        addStructuralToken(destination, element, classification.kind.toLspType(), range, modifiers)
    }

    /**
     * 添加一个由 PSI 元素范围确定的结构 token。
     */
    private fun addStructuralToken(
        destination: MutableMap<String, CangJieSemanticToken>,
        element: PsiElement?,
        type: CangJieSemanticTokenType,
        range: TextRange?,
        modifiers: Set<CangJieSemanticTokenModifier> = emptySet(),
    ) {
        if (element == null) return
        val textRange = element.textRange ?: return
        if (textRange.isEmpty || !textRange.shouldInclude(range)) return
        putToken(destination, CangJieSemanticToken(textRange, type, modifiers))
    }

    private fun CangJieSemanticHighlightingKind.toLspType(): CangJieSemanticTokenType = when (this) {
        CangJieSemanticHighlightingKind.CLASS,
        CangJieSemanticHighlightingKind.ABSTRACT_CLASS,
        CangJieSemanticHighlightingKind.CLASS_CONSTRUCTOR_CALL,
        CangJieSemanticHighlightingKind.CONSTRUCTOR_CALL,
        -> CangJieSemanticTokenType.CLASS

        CangJieSemanticHighlightingKind.STRUCT,
        CangJieSemanticHighlightingKind.STRUCT_CONSTRUCTOR_CALL,
        -> CangJieSemanticTokenType.STRUCT

        CangJieSemanticHighlightingKind.INTERFACE -> CangJieSemanticTokenType.INTERFACE
        CangJieSemanticHighlightingKind.ENUM -> CangJieSemanticTokenType.ENUM
        CangJieSemanticHighlightingKind.ENUM_CONSTRUCTOR -> CangJieSemanticTokenType.ENUM_MEMBER
        CangJieSemanticHighlightingKind.ANNOTATION -> CangJieSemanticTokenType.ANNOTATION
        CangJieSemanticHighlightingKind.ANNOTATION_ATTRIBUTE_NAME -> CangJieSemanticTokenType.PARAMETER
        CangJieSemanticHighlightingKind.MACRO_CALL -> CangJieSemanticTokenType.MACRO
        CangJieSemanticHighlightingKind.MACRO_DECLARATION -> CangJieSemanticTokenType.MACRO
        CangJieSemanticHighlightingKind.MACRO_ESCAPE -> CangJieSemanticTokenType.MACRO
        CangJieSemanticHighlightingKind.SAFE_ACCESS -> CangJieSemanticTokenType.OPERATOR
        CangJieSemanticHighlightingKind.TYPE_REFERENCE,
        CangJieSemanticHighlightingKind.BASIC_TYPE_REFERENCE,
        CangJieSemanticHighlightingKind.TYPE_ALIAS,
        -> CangJieSemanticTokenType.TYPE

        CangJieSemanticHighlightingKind.TYPE_PARAMETER -> CangJieSemanticTokenType.TYPE_PARAMETER
        CangJieSemanticHighlightingKind.LOCAL_VARIABLE,
        CangJieSemanticHighlightingKind.MUTABLE_VARIABLE,
        CangJieSemanticHighlightingKind.PACKAGE_VARIABLE,
        CangJieSemanticHighlightingKind.STATIC_VARIABLE,
        CangJieSemanticHighlightingKind.INSTANCE_VARIABLE,
        -> CangJieSemanticTokenType.VARIABLE

        CangJieSemanticHighlightingKind.PARAMETER,
        CangJieSemanticHighlightingKind.NAMED_ARGUMENT,
        -> CangJieSemanticTokenType.PARAMETER

        CangJieSemanticHighlightingKind.STATIC_PROPERTY,
        CangJieSemanticHighlightingKind.INSTANCE_PROPERTY,
        CangJieSemanticHighlightingKind.INSTANCE_PROPERTY_CUSTOM_PROPERTY_DECLARATION,
        CangJieSemanticHighlightingKind.STATIC_PROPERTY_CUSTOM_PROPERTY_DECLARATION,
        CangJieSemanticHighlightingKind.EXTENSION_PROPERTY,
        CangJieSemanticHighlightingKind.MUTABLE_PROPERTY,
        -> CangJieSemanticTokenType.PROPERTY

        CangJieSemanticHighlightingKind.FUNCTION_CALL,
        CangJieSemanticHighlightingKind.PACKAGE_FUNCTION_CALL,
        CangJieSemanticHighlightingKind.VARIABLE_AS_FUNCTION_CALL,
        -> CangJieSemanticTokenType.FUNCTION

        CangJieSemanticHighlightingKind.INSTANCE_FUNCTION_CALL,
        CangJieSemanticHighlightingKind.STATIC_FUNCTION_CALL,
        CangJieSemanticHighlightingKind.EXTENSION_FUNCTION_CALL,
        -> CangJieSemanticTokenType.METHOD
    }

    private fun CjExpression.semanticCallNameElement(): PsiElement? = when (this) {
        is CjConstructorCalleeExpression -> constructorReferenceExpression
        is CjQualifiedExpression -> selectorExpression?.semanticCallNameElement()
        is CjCallExpression -> calleeExpression?.semanticCallNameElement()
        else -> this
    }

    private fun CjSimpleNameExpression.isDeclarationName(): Boolean =
        (parent as? org.cangnova.cangjie.psi.CjNamedDeclaration)?.nameIdentifier == this

    private fun CjSimpleNameExpression.isCallCallee(): Boolean {
        var ancestor: PsiElement? = parent
        while (ancestor != null) {
            if (ancestor is CjCallExpression &&
                (ancestor.valueArgumentList != null || ancestor.lambdaArguments.isNotEmpty()) &&
                isWithin(ancestor.calleeExpression)
            ) {
                return true
            }
            ancestor = ancestor.parent
        }
        return false
    }

    private fun CjSimpleNameExpression.isConstructorCalleeName(): Boolean {
        var ancestor: PsiElement? = parent
        while (ancestor != null) {
            if (ancestor is CjConstructorCalleeExpression) {
                return ancestor.constructorReferenceExpression == this
            }
            ancestor = ancestor.parent
        }
        return false
    }

    private fun PsiElement.isWithin(root: PsiElement?): Boolean {
        var current: PsiElement? = this
        while (current != null) {
            if (current === root) return true
            current = current.parent
        }
        return false
    }

    /**
     * 按文本范围键写入 token，后写入的结构 token 可以覆盖同范围词法 token。
     */
    private fun putToken(
        destination: MutableMap<String, CangJieSemanticToken>,
        token: CangJieSemanticToken,
    ) {
        val key = token.range.key()
        val existing = destination[key]
        val isFunctionValueCall = CangJieSemanticTokenModifier.FUNCTION_VALUE in token.modifiers ||
            CangJieSemanticTokenModifier.FUNCTION_VALUE in existing?.modifiers.orEmpty()
        val valueReferenceToken = when {
            existing != null && existing.type.isValueReferenceToken() -> existing
            token.type.isValueReferenceToken() -> token
            else -> null
        }
        destination[key] = if (isFunctionValueCall && valueReferenceToken != null) {
            valueReferenceToken.copy(
                modifiers = valueReferenceToken.modifiers + CangJieSemanticTokenModifier.FUNCTION_VALUE,
            )
        } else {
            token
        }
    }

    private fun CangJieSemanticTokenType.isValueReferenceToken(): Boolean =
        this == CangJieSemanticTokenType.VARIABLE ||
            this == CangJieSemanticTokenType.PROPERTY ||
            this == CangJieSemanticTokenType.PARAMETER

    /**
     * 计算属性声明的语义 token 修饰符。
     */
    private fun propertyModifiersOf(property: CjProperty): Array<CangJieSemanticTokenModifier> = buildList {
        if (!property.isVar) add(CangJieSemanticTokenModifier.READONLY)
        if (property.isVar) add(CangJieSemanticTokenModifier.MUTABLE)
        if (property.isStatic) add(CangJieSemanticTokenModifier.STATIC)
        if (property.parent is CjExtend) add(CangJieSemanticTokenModifier.EXTENSION)
        if (property.getter?.bodyExpression != null || property.setter?.bodyExpression != null) {
            add(CangJieSemanticTokenModifier.CUSTOM_PROPERTY)
        }
    }.toTypedArray()

    /**
     * 计算变量声明的语义 token 修饰符。
     */
    private fun variableModifiersOf(variable: CjVariable<*>): Array<CangJieSemanticTokenModifier> = buildList {
        if (!variable.isVar) add(CangJieSemanticTokenModifier.READONLY)
        if (variable.isVar) add(CangJieSemanticTokenModifier.MUTABLE)
        if (variable.isStatic) add(CangJieSemanticTokenModifier.STATIC)
        if (variable is CjFieldVariable) add(CangJieSemanticTokenModifier.FIELD)
    }.toTypedArray()

    /**
     * 判断文本范围是否应被当前请求范围包含。
     */
    private fun TextRange.shouldInclude(filter: TextRange?): Boolean =
        filter == null || intersectsStrict(filter)

    /**
     * 判断两个范围是否存在严格交集。
     */
    private fun TextRange.intersectsStrict(other: TextRange): Boolean =
        startOffset < other.endOffset && other.startOffset < endOffset

    /**
     * 构造范围去重键。
     */
    private fun TextRange.key(): String = "$startOffset:$endOffset"

    /**
     * LSP semantic token 的稳定排序规则。
     */
    private val tokenOrder = compareBy<CangJieSemanticToken>(
        { it.range.startOffset },
        { it.range.endOffset },
        { it.type.ordinal },
    )
}
