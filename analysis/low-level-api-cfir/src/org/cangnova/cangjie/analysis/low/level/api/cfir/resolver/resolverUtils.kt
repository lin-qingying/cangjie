/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.resolver

import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.resolve.ImplicitValueStorage
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.resolve.body.CfirAbstractBodyResolveTransformer
import org.cangnova.cangjie.cfir.resolve.dfa.*
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.BodyResolveContext
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.CfirAbstractBodyResolveTransformer
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.CfirBodyResolveTransformer
import org.cangnova.cangjie.cfir.session.CfirSession

internal fun createStubBodyResolveComponents(firSession: CfirSession): CfirAbstractBodyResolveTransformer.BodyResolveTransformerComponents {
    val scopeSession = ScopeSession()

    // This transformer is not intended for actual transformations and created here only to simplify access to resolve components
    val stubBodyResolveTransformer = CfirBodyResolveTransformer(
        session = firSession,
        phase = CfirResolvePhase.BODY_RESOLVE,
        implicitTypeOnly = false,
        scopeSession = scopeSession,
    )

    return StubBodyResolveTransformerComponents(
        firSession,
        scopeSession,
        stubBodyResolveTransformer,
        stubBodyResolveTransformer.context,
    )
}

internal open class StubBodyResolveTransformerComponents(
    session: CfirSession,
    scopeSession: ScopeSession,
    transformer: CfirBodyResolveTransformer,
    context: BodyResolveContext
) : CfirAbstractBodyResolveTransformer.BodyResolveTransformerComponents(
    session,
    scopeSession,
    transformer,
    context,
    expandTypeAliases = true,
) {
    override val dataFlowAnalyzer: CfirDataFlowAnalyzer
        get() = object : CfirDataFlowAnalyzer(this@StubBodyResolveTransformerComponents, context.dataFlowAnalyzerContext) {
            override val logicSystem: LogicSystem
                get() = error("Should not be called")

            override val receiverStack: ImplicitValueStorage
                get() = error("Should not be called")

            override fun implicitUpdated(info: TypeStatement) =
                error("Should not be called")

            override fun extractTypeStatementFrom(flow: Flow, variable: DataFlowVariable): TypeStatement? =
                null
        }
}
