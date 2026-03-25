package org.cangnova.cangjie.cfir.types

import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.cfir.session.languageVersionSettings

class TypeComponents(val session: CfirSession) : CfirSessionComponent {
    val typeContext: ConeInferenceContext = object : ConeInferenceContext {
        override val session: CfirSession
            get() = this@TypeComponents.session
    }

    val typeApproximator: ConeTypeApproximator = ConeTypeApproximator(typeContext, session.languageVersionSettings)
}

private val CfirSession.typeComponents: TypeComponents by CfirSession.sessionComponentAccessor()

val CfirSession.typeContext: ConeInferenceContext
    get() = typeComponents.typeContext

val CfirSession.typeApproximator: ConeTypeApproximator
    get() = typeComponents.typeApproximator
