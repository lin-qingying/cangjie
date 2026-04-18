/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.diagnostics

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.AbstractCjSourceElement
import org.cangnova.cangjie.CjFakePsiSourceElement
import org.cangnova.cangjie.CjFakeSourceElementKind
import org.cangnova.cangjie.SuspiciousFakeSourceCheck
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.addValueFor
import org.cangnova.cangjie.diagnostics.*

internal class LLCfirDiagnosticReporter : PendingDiagnosticReporter() {
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

        val psiDiagnostic = when (diagnostic) {
            is CjPsiDiagnostic -> diagnostic
            is CjLightDiagnostic -> diagnostic.toPsiDiagnostic()
            else -> error("Unknown diagnostic type ${diagnostic::class.simpleName}")
        }
        pendingDiagnostics.addValueFor(psiDiagnostic.psiElement, psiDiagnostic)
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


private fun CjLightDiagnostic.toPsiDiagnostic(): CjPsiDiagnostic {
    val psiSourceElement = element.unwrapToCjPsiSourceElement()
        ?: error("Diagnostic should be created from PSI in IDE")
    @Suppress("UNCHECKED_CAST")
    return when (this) {
        is CjLightSimpleDiagnostic -> CjPsiSimpleDiagnostic(
            psiSourceElement,
            severity,
            factory,
            positioningStrategy,
            context,
        )

        is CjLightDiagnosticWithParameters1<*> -> CjPsiDiagnosticWithParameters1(
            psiSourceElement,
            a,
            severity,
            factory as CjDiagnosticFactory1<Any?>,
            positioningStrategy,
            context,
        )

        is CjLightDiagnosticWithParameters2<*, *> -> CjPsiDiagnosticWithParameters2(
            psiSourceElement,
            a, b,
            severity,
            factory as CjDiagnosticFactory2<Any?, Any?>,
            positioningStrategy,
            context,
        )

        is CjLightDiagnosticWithParameters3<*, *, *> -> CjPsiDiagnosticWithParameters3(
            psiSourceElement,
            a, b, c,
            severity,
            factory as CjDiagnosticFactory3<Any?, Any?, Any?>,
            positioningStrategy,
            context,
        )

        is CjLightDiagnosticWithParameters4<*, *, *, *> -> CjPsiDiagnosticWithParameters4(
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
