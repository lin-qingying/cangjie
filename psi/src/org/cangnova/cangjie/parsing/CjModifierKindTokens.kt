package org.cangnova.cangjie.parsing

import com.intellij.psi.tree.IElementType
import org.cangnova.cangjie.lexer.CjKeywordToken
import org.cangnova.cangjie.lexer.CjTokens

/** 源码 token 与跨解析入口的修饰符身份一一对应，不按文本或归一化角色推断。 */
fun CjModifierKind.token(): CjKeywordToken = when (this) {
    CjModifierKind.ABSTRACT -> CjTokens.ABSTRACT_KEYWORD
    CjModifierKind.MUT -> CjTokens.MUT_KEYWORD
    CjModifierKind.PUBLIC -> CjTokens.PUBLIC_KEYWORD
    CjModifierKind.PRIVATE -> CjTokens.PRIVATE_KEYWORD
    CjModifierKind.INTERNAL -> CjTokens.INTERNAL_KEYWORD
    CjModifierKind.PROTECTED -> CjTokens.PROTECTED_KEYWORD
    CjModifierKind.OPERATOR -> CjTokens.OPERATOR_KEYWORD
    CjModifierKind.FOREIGN -> CjTokens.FOREIGN_KEYWORD
    CjModifierKind.CONST -> CjTokens.CONST_KEYWORD
    CjModifierKind.UNSAFE -> CjTokens.UNSAFE_KEYWORD
    CjModifierKind.SEALED -> CjTokens.SEALED_KEYWORD
    CjModifierKind.REDEF -> CjTokens.REDEF_KEYWORD
    CjModifierKind.OVERRIDE -> CjTokens.OVERRIDE_KEYWORD
    CjModifierKind.OPEN -> CjTokens.OPEN_KEYWORD
    CjModifierKind.STATIC -> CjTokens.STATIC_KEYWORD
    CjModifierKind.INOUT -> CjTokens.INOUT_KEYWORD
    CjModifierKind.COMMON -> CjTokens.COMMON_KEYWORD
    CjModifierKind.SPECIFIC -> CjTokens.SPECIFIC_KEYWORD
    CjModifierKind.PLATFORM -> CjTokens.PLATFORM_KEYWORD
}

private val modifierKindsByToken: Map<IElementType, CjModifierKind> = CjModifierKind.entries.associateBy { it.token() }

/** 只有实际 modifier token 才有身份，annotation 与错误节点不能混入集合。 */
fun IElementType.cjModifierKindOrNull(): CjModifierKind? = modifierKindsByToken[this]
