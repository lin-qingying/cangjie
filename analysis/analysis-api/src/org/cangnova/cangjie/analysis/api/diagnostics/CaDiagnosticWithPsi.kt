package org.cangnova.cangjie.analysis.api.diagnostics

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import kotlin.reflect.KClass

/**
 * 携带源码位置的诊断。
 *
 * - 在 [CaDiagnostic] 基础上额外提供 PSI 与高亮区间,供 IDE 直接绘制下划线/红色波浪线;
 * - 类型参数 [PSI] 用于约束诊断关联的具体 PSI 类型(如 `CjReferenceExpression`),
 *   方便调用方在不强转的情况下访问位置感知字段。
 *
 * 对齐 Kotlin Analysis API 的 `KaDiagnosticWithPsi`。
 */
interface CaDiagnosticWithPsi<out PSI : PsiElement> : CaDiagnostic {
    /** 诊断绑定的源码 PSI,例如出错的引用、类型参数列表等。 */
    val psi: PSI

    /**
     * 诊断要高亮的文本区间集合。
     *
     * 多数诊断只占用一个区间;少数(如 mismatch 类)可同时高亮多个位置(实参 + 期望签名等)。
     */
    val textRanges: Collection<TextRange>

    override val diagnosticClass: KClass<out CaDiagnosticWithPsi<PSI>>
}
