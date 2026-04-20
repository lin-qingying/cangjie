package org.cangnova.cangjie.analysis.api.diagnostics

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import kotlin.reflect.KClass

interface CaDiagnosticWithPsi<out PSI : PsiElement> : CaDiagnostic {
    val psi: PSI

    val textRanges: Collection<TextRange>

    override val diagnosticClass: KClass<out CaDiagnosticWithPsi<PSI>>
}
