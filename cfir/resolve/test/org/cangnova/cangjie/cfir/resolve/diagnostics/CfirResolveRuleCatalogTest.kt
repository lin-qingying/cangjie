package org.cangnova.cangjie.cfir.resolve.diagnostics

import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CfirResolveRuleCatalogTest {
    @Test
    fun allRuleIdsAreUnique() {
        val ids = CfirResolveRuleCatalog.all.map { it.id }
        assertEquals(ids.toSet().size, ids.size, "Rule IDs must be unique")
    }

    @Test
    fun catalogCoversAllResolvePhasesExceptRaw() {
        val coveredPhases = CfirResolveRuleCatalog.all.map { it.phase }.toSet()
        val expected = setOf(
            CfirResolvePhase.IMPORTS,
            CfirResolvePhase.SUPER_TYPES,
            CfirResolvePhase.TYPES,
            CfirResolvePhase.STATUS,
            CfirResolvePhase.EXTENSIONS,
            CfirResolvePhase.IMPLICIT_TYPES,
            CfirResolvePhase.BODY_RESOLVE,
            CfirResolvePhase.CHECKERS,
        )
        assertTrue(coveredPhases.containsAll(expected), "Rule catalog must cover all resolve phases")
    }

    @Test
    fun checkersRulesAreBoundToCheckersPhase() {
        val finalDiagnostics = CfirResolveRuleCatalog.byId("RULE_CHECKERS_FINAL_RESOLVE_DIAGNOSTICS")
        val stability = CfirResolveRuleCatalog.byId("RULE_CHECKERS_DIAGNOSTIC_STABILITY")
        assertNotNull(finalDiagnostics)
        assertNotNull(stability)
        assertEquals(CfirResolvePhase.CHECKERS, finalDiagnostics?.phase)
        assertEquals(CfirResolvePhase.CHECKERS, stability?.phase)
    }
}
