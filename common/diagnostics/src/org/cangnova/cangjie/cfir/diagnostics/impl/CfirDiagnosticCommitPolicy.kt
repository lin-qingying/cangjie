package org.cangnova.cangjie.cfir.diagnostics.impl

import org.cangnova.cangjie.cfir.diagnostics.CjDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticCommitPolicy

/**
 * 对齐仓颉官方诊断引擎的提交期裁决。
 */
object CfirDiagnosticCommitPolicy : DiagnosticCommitPolicy {
    /**
     * 官方 `CompilerDiagnosticHandler::HandleDiagnose` 用 `HasPrevDiag` 按 (主提示起点, 消息) 去重；
     * CFIR 以诊断名、渲染消息与首范围作为等价键，比官方键更严格，不会合并不同诊断。
     */
    override fun isDuplicateOfCommitted(diagnostic: CjDiagnostic, committed: Sequence<CjDiagnostic>): Boolean =
        committed.any { it.hasSameDiagnosticIdentity(diagnostic) }

    override fun isSupersededByMoreSpecific(
        diagnostic: CjDiagnostic,
        pending: Sequence<CjDiagnostic>,
        committed: Sequence<CjDiagnostic>,
    ): Boolean {
        val candidates = pending + committed
        return diagnostic.isGenericTypeMismatchSupersededByReturnMismatch(candidates) ||
            diagnostic.isAccessibilityErrorSupersededByReturnTypeError(candidates)
    }
}

/**
 * 判断两个诊断是否在名称、消息和首范围上等价。
 */
private fun CjDiagnostic.hasSameDiagnosticIdentity(other: CjDiagnostic): Boolean =
    factoryName == other.factoryName &&
        renderMessage() == other.renderMessage() &&
        firstRange.startOffset == other.firstRange.startOffset &&
        firstRange.endOffset == other.firstRange.endOffset

/** 判断诊断是否是可见性（accessibility）错误；用于同范围诊断的取代规则。 */
internal fun CjDiagnostic.isAccessibilityError(): Boolean = factoryName == "CFIR_ACCESSIBILITY_ERROR"

/**
 * 官方诊断引擎对同范围、同严重级别错误只保留一个；继承检查中返回类型错误比随后产生的
 * 可见性错误更具体，因此相同首范围上的可见性错误由返回类型诊断取代。
 */
private fun CjDiagnostic.isAccessibilityErrorSupersededByReturnTypeError(candidates: Sequence<CjDiagnostic>): Boolean {
    if (!isAccessibilityError()) return false
    return candidates.any { candidate ->
        candidate.factoryName in RETURN_TYPE_OVERRIDE_ERROR_NAMES &&
            candidate.severity == severity &&
            candidate.firstRange.startOffset == firstRange.startOffset &&
            candidate.firstRange.endOffset == firstRange.endOffset
    }
}

/** 能在同范围上取代可见性错误的返回类型诊断名称集合。 */
private val RETURN_TYPE_OVERRIDE_ERROR_NAMES: Set<String> = setOf(
    "CFIR_RETURN_TYPE_INCOMPATIBLE",
    "CFIR_RETURN_TYPE_INVARIANCE",
)

/**
 * RETURN_TYPE_MISMATCH 是 return 根表达式上的专用分类；同一起点且覆盖通用
 * TYPE_MISMATCH 范围时，专用诊断拥有该位置，避免重复输出通用类型不匹配。
 */
private fun CjDiagnostic.isGenericTypeMismatchSupersededByReturnMismatch(candidates: Sequence<CjDiagnostic>): Boolean {
    if (factoryName != "CFIR_TYPE_MISMATCH") return false
    return candidates.any { candidate ->
        candidate.factoryName == "CFIR_RETURN_TYPE_MISMATCH" &&
            candidate.firstRange.startOffset == firstRange.startOffset &&
            candidate.firstRange.endOffset >= firstRange.endOffset
    }
}
