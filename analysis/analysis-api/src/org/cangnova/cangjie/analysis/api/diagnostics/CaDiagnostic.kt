package org.cangnova.cangjie.analysis.api.diagnostics

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import kotlin.reflect.KClass

enum class CaSeverity {
    ERROR,
    WARNING,
    INFO,
}

interface CaDiagnostic : CaLifetimeOwner {
    val diagnosticClass: KClass<*>
    val factoryName: String
    val severity: CaSeverity
    val defaultMessage: String
}

interface CaDiagnosticWithPsi<out PSI : PsiElement> : CaDiagnostic {
    val psi: PSI
    val textRanges: Collection<TextRange>
    override val diagnosticClass: KClass<out CaDiagnosticWithPsi<PSI>>
}

fun CaDiagnostic.getDefaultMessageWithFactoryName(): String {
    return "[$factoryName] $defaultMessage"
}
