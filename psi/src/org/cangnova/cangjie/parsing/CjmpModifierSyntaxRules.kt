package org.cangnova.cangjie.parsing

import org.cangnova.cangjie.lexer.CjKeywordToken

/** 由共享语法解析器提供的真实修饰符 token 与源码范围。 */
internal data class CjmpModifierOccurrence(
    val token: CjKeywordToken,
    val startOffset: Int,
    val endOffset: Int,
    val sourceFact: CjModifierSourceFact,
) {
    /** 规则使用规范角色身份；早期 platform 的版本、源拼写和范围仍由真实 occurrence 保存。 */
    val ruleKind: CjModifierKind
        get() = when (val kind = checkNotNull(token.cjModifierKindOrNull())) {
            CjModifierKind.PLATFORM -> CjModifierKind.SPECIFIC
            else -> kind
        }
}

internal sealed interface CjmpModifierSyntaxViolation {
    val modifier: CjmpModifierOccurrence

    data class IllegalTarget(override val modifier: CjmpModifierOccurrence) : CjmpModifierSyntaxViolation

    data class Conflict(
        override val modifier: CjmpModifierOccurrence,
        val earlierModifier: CjmpModifierOccurrence,
    ) : CjmpModifierSyntaxViolation

}

/**
 * 原生 CheckDeclModifiers/GetModifierAttrs 的完整规则执行器。
 * 非法 token 与冲突 token 的属性返回和 WARNING 候选是不同结果，不能共用一个过滤列表。
 */
internal object CjmpModifierSyntaxRules {
    data class Result(
        val violations: List<CjmpModifierSyntaxViolation>,
        val returnedKinds: Set<CjModifierKind>,
        val validModifiers: List<CjmpModifierOccurrence>,
    )

    sealed interface Redundancy {
        data class Explicit(val higher: CjmpModifierOccurrence) : Redundancy
        data object InterfaceMember : Redundancy
    }

    data class RedundantModifier(val modifier: CjmpModifierOccurrence, val reason: Redundancy)

    fun scopeRules(
        declaration: CjmpModifierDeclarationKind,
        scope: CjScopeKind,
    ): Map<CjModifierKind, List<CjModifierKind>>? = CjModifierSyntaxRuleTables.current[declaration]?.get(scope)

    /** 空表不执行 CJMP 文件检查；缺表、空表和 UNKNOWN_SCOPE 由声明 owner 独立处理。 */
    fun hasFileRoleCheckRules(declaration: CjmpModifierDeclarationKind, scope: CjScopeKind): Boolean =
        scopeRules(declaration, scope)?.isNotEmpty() == true

    fun check(
        declaration: CjmpModifierDeclarationKind,
        scope: CjScopeKind,
        modifiers: List<CjmpModifierOccurrence>,
        ignoreScopeCheck: Boolean,
    ): Result {
        val rules = checkNotNull(scopeRules(declaration, scope))
        require(rules.isNotEmpty() && scope != CjScopeKind.UNKNOWN_SCOPE)
        val violations = mutableListOf<CjmpModifierSyntaxViolation>()
        val returned = linkedSetOf<CjModifierKind>()
        val traversed = mutableListOf<CjmpModifierOccurrence>()
        val valid = mutableListOf<CjmpModifierOccurrence>()
        for (modifier in modifiers) {
            val sourceKind = checkNotNull(modifier.token.cjModifierKindOrNull())
            val conflicts = rules[modifier.ruleKind]
            if (conflicts == null) {
                if (!ignoreScopeCheck) {
                    violations += CjmpModifierSyntaxViolation.IllegalTarget(modifier)
                    returned += sourceKind
                }
                continue
            }
            var collided = false
            for (conflict in conflicts) {
                val earlier = traversed.firstOrNull { it.ruleKind == conflict } ?: continue
                violations += CjmpModifierSyntaxViolation.Conflict(modifier, earlier)
                collided = true
                valid.removeAll { it.ruleKind == conflict }
            }
            traversed += modifier
            if (!collided) valid += modifier
            returned += sourceKind
        }
        return Result(violations, returned, valid)
    }

    /** 每个有效 token 依原生 vector 优先序选择一条；sealed 的优先级高于 abstract。 */
    fun redundantModifiers(
        declaration: CjmpModifierDeclarationKind,
        scope: CjScopeKind,
        modifiers: List<CjmpModifierOccurrence>,
        ignoreScopeCheck: Boolean,
    ): List<RedundantModifier> {
        val result = mutableListOf<RedundantModifier>()
        for (modifier in modifiers) {
            val kind = modifier.ruleKind
            val higherKinds = when {
                declaration == CjmpModifierDeclarationKind.CLASS && scope == CjScopeKind.TOPLEVEL -> when (kind) {
                    CjModifierKind.PUBLIC -> listOf(CjModifierKind.SEALED)
                    CjModifierKind.OPEN -> listOf(CjModifierKind.SEALED, CjModifierKind.ABSTRACT)
                    else -> emptyList()
                }
                declaration == CjmpModifierDeclarationKind.INTERFACE && scope == CjScopeKind.TOPLEVEL -> when (kind) {
                    CjModifierKind.PUBLIC, CjModifierKind.OPEN -> listOf(CjModifierKind.SEALED)
                    else -> emptyList()
                }
                else -> emptyList()
            }
            val higher = higherKinds.firstNotNullOfOrNull { expected -> modifiers.firstOrNull { it.ruleKind == expected } }
            if (higher != null) {
                result += RedundantModifier(modifier, Redundancy.Explicit(higher))
            } else if (!ignoreScopeCheck && (
                    declaration == CjmpModifierDeclarationKind.INTERFACE && scope == CjScopeKind.TOPLEVEL && kind == CjModifierKind.OPEN ||
                    declaration in setOf(CjmpModifierDeclarationKind.FUNCTION, CjmpModifierDeclarationKind.PROPERTY) &&
                        scope == CjScopeKind.INTERFACE_BODY && kind in setOf(CjModifierKind.PUBLIC, CjModifierKind.OPEN)
                )
            ) {
                result += RedundantModifier(modifier, Redundancy.InterfaceMember)
            }
        }
        return result
    }
}
