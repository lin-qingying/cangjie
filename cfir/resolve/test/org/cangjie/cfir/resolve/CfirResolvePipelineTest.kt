package org.cangjie.cfir.resolve

import org.cangjie.cfir.declarations.CfirInvalidDeclaration
import org.cangjie.cfir.declarations.CfirResolvePhase
import org.cangjie.cfir.resolve.framework.createCfirResolveTestSessionContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CfirResolvePipelineTest {
    @Test
    fun processToPhaseUpdatesResolvePhase() {
        val context = createCfirResolveTestSessionContext("<test-module>")
        val declaration = CfirInvalidDeclaration(
            moduleData = context.moduleData,
            reason = "<synthetic> resolve phase progression",
        )

        CfirTotalResolveProcessor(context.session, context.phaseRegistry).processToPhase(declaration, CfirResolvePhase.STATUS)

        assertEquals(CfirResolvePhase.STATUS, declaration.resolvePhase)
    }

    @Test
    fun processToCheckersUpdatesResolvePhase() {
        val context = createCfirResolveTestSessionContext("<test-module>")
        val declaration = CfirInvalidDeclaration(
            moduleData = context.moduleData,
            reason = "<synthetic> resolve checkers progression",
        )

        CfirTotalResolveProcessor(context.session, context.phaseRegistry).processToPhase(declaration, CfirResolvePhase.CHECKERS)

        assertEquals(CfirResolvePhase.CHECKERS, declaration.resolvePhase)
    }
}
