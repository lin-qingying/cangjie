package org.cangnova.cangjie

import org.cangnova.cangjie.parsing.CjmpFileRole

/** 文件角色检查的完整上下文；语法模式与禁用编译模式具有不同的状态行为。 */
public sealed interface CjmpFileRoleMode {
    public data object SyntaxOnly : CjmpFileRoleMode
    public data class Compilation(
        val enabled: Boolean,
        val compileCommon: Boolean,
        val compileSpecific: Boolean,
        val mixedCommonSpecificDeclarations: Boolean,
    ) : CjmpFileRoleMode
}

public enum class CjmpFileRoleError {
    SPECIFIC_IN_NON_SPECIFIC,
    COMMON_IN_NON_COMMON,
    COMMON_AND_SPECIFIC,
    UNEXPECTED_CJMP_DECL,
}

/** 返回检查结束时的文件角色；调用方负责记录首个错误和清除修饰符。 */
public data class CjmpFileRoleTransitionResult(val role: CjmpFileRole, val error: CjmpFileRoleError?)

/** 不持有解析器状态的文件角色转移。 */
public object CjmpFileRoleTransition {
    public fun check(
        mode: CjmpFileRoleMode,
        previous: CjmpFileRole,
        declaration: CjmpDeclarationRole,
    ): CjmpFileRoleTransitionResult {
        if (mode is CjmpFileRoleMode.Compilation && !mode.enabled) {
            return CjmpFileRoleTransitionResult(previous, null)
        }
        val next = roleOf(
            previous.isCommon || declaration.isCommon,
            previous.isSpecific || declaration.isSpecific,
        )
        if (mode is CjmpFileRoleMode.SyntaxOnly) return CjmpFileRoleTransitionResult(next, null)
        mode as CjmpFileRoleMode.Compilation
        if (mode.mixedCommonSpecificDeclarations) {
            val error = if (!mode.compileCommon && !mode.compileSpecific && (declaration.isCommon || declaration.isSpecific)) {
                CjmpFileRoleError.UNEXPECTED_CJMP_DECL
            } else null
            return CjmpFileRoleTransitionResult(if (error == null) next else previous, error)
        }
        // 旧版先提交 specific，之后 common 检查失败不会撤销已提交的角色。
        val specificError = when {
            declaration.isSpecific && !mode.compileSpecific -> CjmpFileRoleError.SPECIFIC_IN_NON_SPECIFIC
            declaration.isSpecific && previous.isCommon -> CjmpFileRoleError.COMMON_AND_SPECIFIC
            else -> null
        }
        if (specificError != null) return CjmpFileRoleTransitionResult(previous, specificError)
        val afterSpecific = roleOf(previous.isCommon, previous.isSpecific || declaration.isSpecific)
        val commonError = when {
            declaration.isCommon && !mode.compileCommon -> CjmpFileRoleError.COMMON_IN_NON_COMMON
            declaration.isCommon && afterSpecific.isSpecific -> CjmpFileRoleError.COMMON_AND_SPECIFIC
            else -> null
        }
        return if (commonError != null) CjmpFileRoleTransitionResult(afterSpecific, commonError)
        else CjmpFileRoleTransitionResult(next, null)
    }

    private fun roleOf(isCommon: Boolean, isSpecific: Boolean): CjmpFileRole = when {
        isCommon && isSpecific -> CjmpFileRole.BOTH
        isCommon -> CjmpFileRole.COMMON
        isSpecific -> CjmpFileRole.SPECIFIC
        else -> CjmpFileRole.NONE
    }
}
