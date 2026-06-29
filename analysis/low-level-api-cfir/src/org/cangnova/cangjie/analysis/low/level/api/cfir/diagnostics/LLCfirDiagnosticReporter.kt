/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.diagnostics

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.source.CjFakeSourceElementKind
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.addValueFor
import org.cangnova.cangjie.cfir.diagnostics.*
import org.cangnova.cangjie.cfir.diagnostics.PendingDiagnosticReporter
import org.cangnova.cangjie.source.AbstractCjSourceElement
import org.cangnova.cangjie.source.CjFakePsiSourceElement
import org.cangnova.cangjie.source.CjPsiSourceElement
import org.cangnova.cangjie.source.SuspiciousFakeSourceCheck

/**
 * low-level CFIR diagnostics 收集使用的 pending reporter。
 */
internal class LLCfirDiagnosticReporter(
    /**
     * 将宏展开等生成源码位置映射回原始源码位置的函数。
     */
    private val sourceMapper: (AbstractCjSourceElement) -> AbstractCjSourceElement? = { null },
) : PendingDiagnosticReporter() {
    /**
     * 尚未提交到最终结果的 diagnostics，按 PSI 元素归组。
     */
    private val pendingDiagnostics = mutableMapOf<PsiElement, MutableList<CjPsiDiagnostic>>()

    /**
     * 已经提交给调用方的 diagnostics，按 PSI 元素归组。
     */
    private val _committedDiagnostics = mutableMapOf<PsiElement, MutableList<CjPsiDiagnostic>>()

    /**
     * 当前已提交 diagnostics 的只读 map。
     */
    val committedDiagnostics get() = _committedDiagnostics.ifEmpty { emptyMap() }

    /**
     * 已提交 diagnostics 中是否存在 error。
     */
    override val hasErrors: Boolean
        get() = committedDiagnostics.any { (_, diagnostics) -> diagnostics.any { it.severity.isError } }

    /**
     * 已提交 diagnostics 中是否存在 `-Werror` 下会提升为 error 的 warning。
     */
    override val hasWarningsForWError: Boolean
        get() = committedDiagnostics.any { (_, diagnostics) -> diagnostics.any { it.severity.isErrorWhenWError } }

    /**
     * 接收 checker 报告的 diagnostic，过滤 suppressed 和隐式 import 相关诊断后进入 pending 集合。
     */
    override fun report(diagnostic: CjDiagnostic?, context: DiagnosticContext) {
        if (diagnostic == null) return
        if (context.isDiagnosticSuppressed(diagnostic)) return

        // Implicit imports for scripts are currently implemented via CFIR-tree mutation (they do not exist in default importing scopes).
        // So as a temporary solution we filter out related diagnostics here.
        if (diagnostic.isAboutImplicitImport()) return

        val psiDiagnostic = diagnostic.toPsiDiagnostic()
        pendingDiagnostics.addValueFor(psiDiagnostic.psiElement, psiDiagnostic)
    }

    /**
     * 将任意 CFIR diagnostic 转换为 PSI diagnostic，并应用 source mapper。
     */
    private fun CjDiagnostic.toPsiDiagnostic(): CjPsiDiagnostic {
        val currentElement = when (this) {
            is CjPsiDiagnostic -> element
            is CjDiagnosticWithSource -> element
            else -> error("Unknown diagnostic type ${this::class.simpleName}")
        }
        val mappedElement = sourceMapper(currentElement) as? CjPsiSourceElement
        if (mappedElement != null && mappedElement != currentElement) {
            return toPsiDiagnosticAt(mappedElement)
        }
        return when (this) {
            is CjPsiDiagnostic -> this
            is CjLightDiagnostic -> this.toPsiDiagnosticFromLight()
            else -> error("Unknown diagnostic type ${this::class.simpleName}")
        }
    }

    /**
     * 提交指定 source element 上的 pending diagnostics，或在需要时提交全部 pending diagnostics。
     */
    override fun checkAndCommitReportsOn(element: AbstractCjSourceElement, context: DiagnosticContext, commitEverything: Boolean) {
        for ((diagnosticElement, pendingList) in pendingDiagnostics) {
            val committedList = _committedDiagnostics.getOrPut(diagnosticElement) { mutableListOf() }
            val iterator = pendingList.iterator()
            while (iterator.hasNext()) {
                val diagnostic = iterator.next()
                when {
                    context.isDiagnosticSuppressed(diagnostic as CjDiagnostic) -> {
                        if (diagnostic.element == element ||
                            diagnostic.element.startOffset >= element.startOffset && diagnostic.element.endOffset <= element.endOffset
                        ) {
                            iterator.remove()
                        }
                    }
                    diagnostic.element == element || commitEverything -> {
                        iterator.remove()
                        committedList += diagnostic
                    }
                }
            }
        }
    }
}

/**
 * 判断 diagnostic 是否来自脚本隐式 import 的 fake source。
 */
@OptIn(SuspiciousFakeSourceCheck::class)
private fun CjDiagnostic.isAboutImplicitImport(): Boolean {
    if (this !is CjPsiDiagnostic) return false
    return (element is CjFakePsiSourceElement && (element as CjFakePsiSourceElement).kind == CjFakeSourceElementKind.ImplicitImport)
}


/**
 * 将 light diagnostic 解包为 IDE 路径需要的 PSI diagnostic。
 */
private fun CjLightDiagnostic.toPsiDiagnosticFromLight(): CjPsiDiagnostic {
    val psiSourceElement = element.unwrapToCjPsiSourceElement()
        ?: error("Diagnostic should be created from PSI in IDE")
    return (this as CjDiagnostic).toPsiDiagnosticAt(psiSourceElement)
}

/**
 * 在指定 PSI source element 上重新创建保持原 factory 和参数的 PSI diagnostic。
 */
@Suppress("UNCHECKED_CAST")
private fun CjDiagnostic.toPsiDiagnosticAt(psiSourceElement: CjPsiSourceElement): CjPsiDiagnostic {
    @Suppress("UNCHECKED_CAST")
    return when (this) {
        is CjSimpleDiagnostic -> CjPsiSimpleDiagnostic(
            psiSourceElement,
            severity,
            factory,
            positioningStrategy,
            context,
        )

        is CjDiagnosticWithParameters1<*> -> CjPsiDiagnosticWithParameters1(
            psiSourceElement,
            a,
            severity,
            factory as CjDiagnosticFactory1<Any?>,
            positioningStrategy,
            context,
        )

        is CjDiagnosticWithParameters2<*, *> -> CjPsiDiagnosticWithParameters2(
            psiSourceElement,
            a, b,
            severity,
            factory as CjDiagnosticFactory2<Any?, Any?>,
            positioningStrategy,
            context,
        )

        is CjDiagnosticWithParameters3<*, *, *> -> CjPsiDiagnosticWithParameters3(
            psiSourceElement,
            a, b, c,
            severity,
            factory as CjDiagnosticFactory3<Any?, Any?, Any?>,
            positioningStrategy,
            context,
        )

        is CjDiagnosticWithParameters4<*, *, *, *> -> CjPsiDiagnosticWithParameters4(
            psiSourceElement,
            a, b, c, d,
            severity,
            factory as CjDiagnosticFactory4<Any?, Any?, Any?, Any?>,
            positioningStrategy,
            context,
        )
        else -> error("Unknown diagnostic type ${this::class.simpleName}")
    }
}
