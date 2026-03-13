package org.cangjie.cfir.diagnostics

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.cangjie.cfir.diagnostics.Severity

interface DiagnosticMarker {
    val psiElement: PsiElement
    val factoryName: String
    val severity: Severity
    val textRanges: List<TextRange>
}

interface DiagnosticWithParameters1Marker<A> : DiagnosticMarker {
    val a: A
}

interface DiagnosticWithParameters2Marker<A, B> : DiagnosticMarker {
    val a: A
    val b: B
}

interface DiagnosticWithParameters3Marker<A, B, C> : DiagnosticMarker {
    val a: A
    val b: B
    val c: C
}

interface DiagnosticWithParameters4Marker<A, B, C, D> : DiagnosticMarker {
    val a: A
    val b: B
    val c: C
    val d: D
}


