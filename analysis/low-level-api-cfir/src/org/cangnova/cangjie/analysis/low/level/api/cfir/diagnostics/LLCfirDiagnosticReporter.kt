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

internal class LLCfirDiagnosticReporter(
    private val sourceMapper: (AbstractCjSourceElement) -> AbstractCjSourceElement? = { null },
) : PendingDiagnosticReporter() {
    private val pendingDiagnostics = mutableMapOf<PsiElement, MutableList<CjPsiDiagnostic>>()
    private val _committedDiagnostics = mutableMapOf<PsiElement, MutableList<CjPsiDiagnostic>>()

    val committedDiagnostics get() = _committedDiagnostics.ifEmpty { emptyMap() }
    override val hasErrors: Boolean
        get() = committedDiagnostics.any { (_, diagnostics) -> diagnostics.any { it.severity.isError } }

    override val hasWarningsForWError: Boolean
        get() = committedDiagnostics.any { (_, diagnostics) -> diagnostics.any { it.severity.isErrorWhenWError } }

    override fun report(diagnostic: CjDiagnostic?, context: DiagnosticContext) {
        if (diagnostic == null) return
        if (context.isDiagnosticSuppressed(diagnostic)) return

        // Implicit imports for scripts are currently implemented via CFIR-tree mutation (they do not exist in default importing scopes).
        // So as a temporary solution we filter out related diagnostics here.
        if (diagnostic.isAboutImplicitImport()) return

        val psiDiagnostic = diagnostic.toPsiDiagnostic()
        pendingDiagnostics.addValueFor(psiDiagnostic.psiElement, psiDiagnostic)
    }

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

@OptIn(SuspiciousFakeSourceCheck::class)
private fun CjDiagnostic.isAboutImplicitImport(): Boolean {
    if (this !is CjPsiDiagnostic) return false
    return (element is CjFakePsiSourceElement && (element as CjFakePsiSourceElement).kind == CjFakeSourceElementKind.ImplicitImport)
}


private fun CjLightDiagnostic.toPsiDiagnosticFromLight(): CjPsiDiagnostic {
    val psiSourceElement = element.unwrapToCjPsiSourceElement()
        ?: error("Diagnostic should be created from PSI in IDE")
    return (this as CjDiagnostic).toPsiDiagnosticAt(psiSourceElement)
}

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
