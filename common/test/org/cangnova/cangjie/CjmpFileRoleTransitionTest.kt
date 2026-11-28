package org.cangnova.cangjie

import org.cangnova.cangjie.parsing.CjmpFileRole
import kotlin.test.Test
import kotlin.test.assertEquals

class CjmpFileRoleTransitionTest {
    private val empty = CjmpFileRole.NONE
    private val common = CjmpDeclarationRole(true, false)
    private val specific = CjmpDeclarationRole(false, true)
    private val both = CjmpDeclarationRole(true, true)

    @Test
    fun legacyOrderAndRejectedTransitions() {
        val mode = CjmpFileRoleMode.Compilation(true, true, true, false)
        for ((first, second) in listOf(common to specific, specific to common)) {
            val accepted = CjmpFileRoleTransition.check(mode, empty, first)
            val rejected = CjmpFileRoleTransition.check(mode, accepted.role, second)
            assertEquals(CjmpFileRoleError.COMMON_AND_SPECIFIC, rejected.error)
            assertEquals(accepted.role, rejected.role)
        }
        assertEquals(CjmpFileRoleTransitionResult(CjmpFileRole.SPECIFIC, CjmpFileRoleError.COMMON_AND_SPECIFIC),
            CjmpFileRoleTransition.check(mode, empty, both))
        assertEquals(CjmpFileRoleTransitionResult(CjmpFileRole.SPECIFIC, CjmpFileRoleError.COMMON_IN_NON_COMMON),
            CjmpFileRoleTransition.check(mode.copy(compileCommon = false), empty, both))
        assertEquals(CjmpFileRoleError.SPECIFIC_IN_NON_SPECIFIC,
            CjmpFileRoleTransition.check(mode.copy(compileSpecific = false, compileCommon = false), empty, both).error)
        assertEquals(CjmpFileRoleError.COMMON_IN_NON_COMMON,
            CjmpFileRoleTransition.check(mode.copy(compileCommon = false), empty, common).error)
        assertEquals(CjmpFileRoleTransitionResult(empty, CjmpFileRoleError.SPECIFIC_IN_NON_SPECIFIC),
            CjmpFileRoleTransition.check(mode.copy(compileCommon = false, compileSpecific = false), empty, both))
        assertEquals(CjmpFileRoleTransitionResult(CjmpFileRole.COMMON, CjmpFileRoleError.SPECIFIC_IN_NON_SPECIFIC),
            CjmpFileRoleTransition.check(mode.copy(compileSpecific = false), CjmpFileRole.COMMON, both))
        assertEquals(CjmpFileRoleTransitionResult(empty, null), CjmpFileRoleTransition.check(
            mode.copy(compileCommon = false, compileSpecific = false), empty, CjmpDeclarationRole(false, false)))
    }

    @Test
    fun mixedVersionAndModes() {
        val mixed = CjmpFileRoleMode.Compilation(true, true, false, true)
        assertEquals(CjmpFileRoleTransitionResult(CjmpFileRole.BOTH, null),
            CjmpFileRoleTransition.check(mixed, empty, both))
        for (first in listOf(common, specific)) {
            val second = if (first == common) specific else common
            val firstResult = CjmpFileRoleTransition.check(mixed, empty, first)
            assertEquals(CjmpFileRole.BOTH, CjmpFileRoleTransition.check(mixed, firstResult.role, second).role)
        }
        for (declaration in listOf(common, specific, both)) {
            assertEquals(CjmpFileRoleTransitionResult(empty, CjmpFileRoleError.UNEXPECTED_CJMP_DECL),
                CjmpFileRoleTransition.check(mixed.copy(compileCommon = false), empty, declaration))
            assertEquals(CjmpFileRoleTransitionResult(empty, null),
                CjmpFileRoleTransition.check(mixed.copy(enabled = false), empty, declaration))
        }
        assertEquals(CjmpFileRole.BOTH, CjmpFileRoleTransition.check(CjmpFileRoleMode.SyntaxOnly, empty, both).role)
        assertEquals(CjmpFileRoleTransitionResult(empty, null), CjmpFileRoleTransition.check(
            mixed.copy(compileCommon = false), empty, CjmpDeclarationRole(false, false)))
    }
}
