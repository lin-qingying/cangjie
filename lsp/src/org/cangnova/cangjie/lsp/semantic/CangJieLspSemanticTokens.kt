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

data class CangJieSemanticToken(
    val range: TextRange,
    val type: CangJieSemanticTokenType,
    val modifiers: Set<CangJieSemanticTokenModifier> = emptySet(),
)

enum class CangJieSemanticTokenType(val lspName: String) {
    KEYWORD("keyword"),
    COMMENT("comment"),
    STRING("string"),
    NUMBER("number"),
    OPERATOR("operator"),
    CLASS("class"),
    STRUCT("struct"),
    INTERFACE("interface"),
    ENUM("enum"),
    ENUM_MEMBER("enumMember"),
    TYPE("type"),
    TYPE_PARAMETER("typeParameter"),
    FUNCTION("function"),
    METHOD("method"),
    MACRO("macro"),
    VARIABLE("variable"),
    PROPERTY("property"),
    PARAMETER("parameter"),
    LABEL("label"),
    ;

    companion object {
        val lspValues: List<String> = entries.map(CangJieSemanticTokenType::lspName)
    }
}

enum class CangJieSemanticTokenModifier(val lspName: String) {
    DECLARATION("declaration"),
    DEFINITION("definition"),
    READONLY("readonly"),
    STATIC("static"),
    DOCUMENTATION("documentation"),
    ;

    companion object {
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
    private val operatorTokens: TokenSet = TokenSet.andNot(
        CjTokens.OPERATIONS,
        TokenSet.orSet(
            TokenSet.create(CjTokens.IDENTIFIER, CjTokens.AT),
            CjTokens.KEYWORDS,
        ),
    )

    private val stringTokens: TokenSet = TokenSet.create(
        CjTokens.OPEN_QUOTE,
        CjTokens.CLOSING_QUOTE,
        CjTokens.REGULAR_STRING_PART,
        CjTokens.RUNE_LITERAL,
    )

    private val stringEscapeTokens: TokenSet = TokenSet.create(
        CjTokens.ESCAPE_SEQUENCE,
        CjTokens.SHORT_TEMPLATE_ENTRY_START,
        CjTokens.LONG_TEMPLATE_ENTRY_START,
        CjTokens.LONG_TEMPLATE_ENTRY_END,
    )

    fun collect(file: CjFile, range: TextRange? = null): List<CangJieSemanticToken> {
        val tokensByRange = linkedMapOf<String, CangJieSemanticToken>()
        collectLexicalTokens(file.text, range, tokensByRange)
        collectStructuralTokens(file, range, tokensByRange)
        return tokensByRange.values.sortedWith(tokenOrder)
    }

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

    private fun modifiersOf(tokenType: com.intellij.psi.tree.IElementType): Set<CangJieSemanticTokenModifier> = when {
        tokenType == CjTokens.DOC_COMMENT ||
            tokenType in CDOC_HIGHLIGHT_TOKENS ||
            tokenType == CDocTokens.TAG_NAME -> setOf(CangJieSemanticTokenModifier.DOCUMENTATION)

        else -> emptySet()
    }

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

    private fun putToken(
        destination: MutableMap<String, CangJieSemanticToken>,
        token: CangJieSemanticToken,
    ) {
        destination[token.range.key()] = token
    }

    private fun propertyModifiersOf(property: CjProperty): Array<CangJieSemanticTokenModifier> = buildList {
        if (!property.isVar) add(CangJieSemanticTokenModifier.READONLY)
        if (property.isStatic) add(CangJieSemanticTokenModifier.STATIC)
    }.toTypedArray()

    private fun variableModifiersOf(variable: CjVariable<*>): Array<CangJieSemanticTokenModifier> = buildList {
        if (!variable.isVar) add(CangJieSemanticTokenModifier.READONLY)
        if (variable.isStatic) add(CangJieSemanticTokenModifier.STATIC)
    }.toTypedArray()

    private fun TextRange.shouldInclude(filter: TextRange?): Boolean =
        filter == null || intersectsStrict(filter)

    private fun TextRange.intersectsStrict(other: TextRange): Boolean =
        startOffset < other.endOffset && other.startOffset < endOffset

    private fun TextRange.key(): String = "$startOffset:$endOffset"

    private val tokenOrder = compareBy<CangJieSemanticToken>(
        { it.range.startOffset },
        { it.range.endOffset },
        { it.type.ordinal },
    )
}
