package org.cangnova.cangjie.parsing

import org.cangnova.cangjie.CjmpDeclarationRole
import java.util.Collections
import java.util.UUID

/**
 * 实际声明文法完成修饰符检查后发布的属性；空集合是有效结果，不表示缺少解析事实。
 * 来源前缀、检查三态及返回属性各自保存。早期 platform 在 parser 中归一为 SPECIFIC，
 * CONST 是声明自身的语法属性，INOUT 不属于声明 AttributePack。
 */
class CjParsedDeclarationModifiers(
    val rootIdentity: UUID,
    val parsingContext: CjmpParsingContext,
    attributes: Set<CjModifierKind>,
    val hasConstModifier: Boolean,
    /** 同一冻结词法输入中的实际声明入口，编译语境变化不改变这个源身份。 */
    val sourceToken: CjSourceTokenId,
    val declarationKind: CjmpModifierDeclarationKind,
    val scope: CjScopeKind,
) {
    val attributes: Set<CjModifierKind> = Collections.unmodifiableSet(LinkedHashSet(attributes))
    val role: CjmpDeclarationRole = CjmpDeclarationRole(
        CjModifierKind.COMMON in this.attributes, CjModifierKind.SPECIFIC in this.attributes,
    )

    init {
        require(this.attributes.none { it == CjModifierKind.CONST || it == CjModifierKind.INOUT || it == CjModifierKind.PLATFORM })
    }

    fun hasModifier(kind: CjModifierKind): Boolean = if (kind == CjModifierKind.CONST) hasConstModifier else kind in attributes

    override fun equals(other: Any?): Boolean = other is CjParsedDeclarationModifiers &&
        rootIdentity == other.rootIdentity && parsingContext == other.parsingContext &&
        attributes == other.attributes && hasConstModifier == other.hasConstModifier &&
        sourceToken == other.sourceToken && declarationKind == other.declarationKind && scope == other.scope

    override fun hashCode(): Int {
        var result = rootIdentity.hashCode()
        result = 31 * result + parsingContext.hashCode()
        result = 31 * result + attributes.hashCode()
        result = 31 * result + hasConstModifier.hashCode()
        result = 31 * result + sourceToken.hashCode()
        result = 31 * result + declarationKind.hashCode()
        return 31 * result + scope.hashCode()
    }
}
