package org.cangnova.cangjie.lsp.semantic

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementVisitor
import com.intellij.psi.TokenType
import com.intellij.psi.tree.TokenSet
import org.cangnova.cangjie.codeinsight.highlighting.CangJieHighlightingLexer
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.lexer.cdoc.lexer.CDocTokens
import org.cangnova.cangjie.lexer.cdoc.lexer.CDocTokens.CDOC_HIGHLIGHT_TOKENS
import org.cangnova.cangjie.lexer.cdoc.psi.impl.CDocLink
import org.cangnova.cangjie.psi.CjCallExpression
import org.cangnova.cangjie.psi.CjClass
import org.cangnova.cangjie.psi.CjEnum
import org.cangnova.cangjie.psi.CjEnumConstructor
import org.cangnova.cangjie.psi.CjExpressionWithLabel
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjInterface
import org.cangnova.cangjie.psi.CjMacroDeclaration
import org.cangnova.cangjie.psi.CjMacroExpression
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjProperty
import org.cangnova.cangjie.psi.CjQualifiedExpression
import org.cangnova.cangjie.psi.CjStruct
import org.cangnova.cangjie.psi.CjSuperTypeCallEntry
import org.cangnova.cangjie.psi.CjTypeAlias
import org.cangnova.cangjie.psi.CjTypeParameter
import org.cangnova.cangjie.psi.CjTypeReference
import org.cangnova.cangjie.psi.CjUserType
import org.cangnova.cangjie.psi.CjVariable
import org.cangnova.cangjie.psi.CjVisitorUnit

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
 * 这是 LSP 协议适配层的模型：它复用 code-insight 的高亮 lexer 和仓颉 PSI，
 * 但不把 LSP token type/modifier 反向暴露给 code-insight。
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

    /**
     * 收集指定 PSI 文件的语义 token。
     *
     * 结果同时包含词法 token 和结构 token，并按协议编码所需顺序稳定排序。
     */
    fun collect(file: CjFile, range: TextRange? = null): List<CangJieSemanticToken> {
        val tokensByRange = linkedMapOf<String, CangJieSemanticToken>()
        collectLexicalTokens(file.text, range, tokensByRange)
        collectStructuralTokens(file, range, tokensByRange)
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
        while (lexer.tokenType != null) {
            val tokenType = lexer.tokenType!!
            val tokenRange = TextRange(lexer.tokenStart, lexer.tokenEnd)
            val type = semanticTypeOfLexicalToken(tokenType)
            if (type != null && tokenRange.shouldInclude(range)) {
                putToken(destination, CangJieSemanticToken(tokenRange, type, modifiersOf(tokenType)))
            }
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
        tokenType == TokenType.BAD_CHARACTER -> CangJieSemanticTokenType.OPERATOR
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
        range: TextRange?,
        destination: MutableMap<String, CangJieSemanticToken>,
    ) {
        val visitor = object : CjVisitorUnit() {
            override fun visitElement(element: PsiElement) {
                val elementType = element.node?.elementType
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
                    addStructuralToken(destination, typeElement.referenceExpression, CangJieSemanticTokenType.CLASS, range)
                }
                super.visitSuperTypeCallEntry(call)
            }

            override fun visitTypeParameter(parameter: CjTypeParameter) {
                addDeclarationToken(destination, parameter.nameIdentifier, CangJieSemanticTokenType.TYPE_PARAMETER, range)
                super.visitTypeParameter(parameter)
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

            override fun visitTypeReference(typeReference: CjTypeReference) {
                val typeElement = typeReference.typeElement
                if (typeElement is CjUserType) {
                    addStructuralToken(destination, typeElement.referenceExpression, CangJieSemanticTokenType.TYPE, range)
                }
                super.visitTypeReference(typeReference)
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
                expression.referenceExpression?.referencedNameElement?.let { nameElement ->
                    addStructuralToken(
                        destination,
                        nameElement,
                        if (expression.parent is CjQualifiedExpression) CangJieSemanticTokenType.METHOD else CangJieSemanticTokenType.FUNCTION,
                        range,
                    )
                }
                super.visitCallExpression(expression)
            }

            override fun visitMacroExpression(expression: CjMacroExpression) {
                expression.referenceExpression?.referencedNameElement?.let { nameElement ->
                    addStructuralToken(destination, nameElement, CangJieSemanticTokenType.MACRO, range)
                }
                super.visitMacroExpression(expression)
            }

            override fun visitMacroDeclaration(function: CjMacroDeclaration) {
                addDeclarationToken(destination, function.nameIdentifier, CangJieSemanticTokenType.MACRO, range)
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

    /**
     * 按文本范围键写入 token，后写入的结构 token 可以覆盖同范围词法 token。
     */
    private fun putToken(
        destination: MutableMap<String, CangJieSemanticToken>,
        token: CangJieSemanticToken,
    ) {
        destination[token.range.key()] = token
    }

    /**
     * 计算属性声明的语义 token 修饰符。
     */
    private fun propertyModifiersOf(property: CjProperty): Array<CangJieSemanticTokenModifier> = buildList {
        if (!property.isVar) add(CangJieSemanticTokenModifier.READONLY)
        if (property.isStatic) add(CangJieSemanticTokenModifier.STATIC)
    }.toTypedArray()

    /**
     * 计算变量声明的语义 token 修饰符。
     */
    private fun variableModifiersOf(variable: CjVariable<*>): Array<CangJieSemanticTokenModifier> = buildList {
        if (!variable.isVar) add(CangJieSemanticTokenModifier.READONLY)
        if (variable.isStatic) add(CangJieSemanticTokenModifier.STATIC)
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
