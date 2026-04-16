package org.cangnova.cangjie.analysis.api.diagnostics

import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import kotlin.reflect.KClass

interface CaDiagnostic : CaLifetimeOwner {
    val diagnosticClass: KClass<*>

    val factoryName: String

    val severity: CaSeverity

    val defaultMessage: String
}

fun CaDiagnostic.getDefaultMessageWithFactoryName(): String {
    return "[$factoryName] $defaultMessage"
}
