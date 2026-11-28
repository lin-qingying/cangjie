package org.cangnova.cangjie

import org.cangnova.cangjie.parsing.CjmpFileRole
import kotlin.test.*

class CjmpFileRoleReplayTest {
    private val normal = CjmpFileRoleMode.Compilation(true, true, true, false)
    private val common = CjmpRoleModifiers(true, false, false)
    private val specific = CjmpRoleModifiers(false, true, false)
    private val both = CjmpRoleModifiers(true, true, false)
    private fun event(id: String, order: Long, kind: CjmpRoleEventKind) = CjmpRoleEvent(CjmpRoleEventId(id), order, kind)
    private fun check(id: String, order: Long, modifiers: CjmpRoleModifiers) = event(id, order, CjmpRoleEventKind.Check(modifiers))
    private fun <T : Any> replay(mode: CjmpFileRoleMode, initial: CjmpFileRole?, completeness: CjmpRoleInputCompleteness, events: List<CjmpRoleEvent<T>>) =
        CjmpFileRoleReplay.replay(mode, initial, completeness, events, LanguageVersionSettingsImpl.DEFAULT, CjmpRoleSyntaxIdentity.VERIFIED_EQUIVALENT)

    @Test
    fun `replay preserves sequence rejection and first error`() {
        val result = assertIs<CjmpRoleReplayResult.Complete<String>>(replay(normal, CjmpFileRole.NONE, CjmpRoleInputCompleteness.COMPLETE, listOf(
            check("first", 1, specific), check("second", 2, common), check("third", 3, common),
        )))
        assertEquals(CjmpFileRole.SPECIFIC, result.finalRole)
        assertEquals(listOf(CjmpRoleEventOutcome.ACCEPTED, CjmpRoleEventOutcome.REJECTED, CjmpRoleEventOutcome.REJECTED), result.events.map { it.outcome })
        assertEquals(CjmpRoleFirstError(CjmpRoleEventId("second"), CjmpFileRoleError.COMMON_AND_SPECIFIC), result.firstError)
    }

    @Test
    fun `rejected check retains specific partial write for following event`() {
        val result = replay(normal, CjmpFileRole.NONE, CjmpRoleInputCompleteness.COMPLETE, listOf(check("both", 0, both), check("next", 1, specific)))
        assertEquals(CjmpFileRole.SPECIFIC, result.events[0].after)
        assertEquals(CjmpFileRole.SPECIFIC, result.events[1].before)
        assertEquals(CjmpRoleEventOutcome.ACCEPTED, result.events[1].outcome)
    }

    @Test
    fun `partial prefix and unknown inputs expose only observations`() {
        val partial = assertIs<CjmpRoleReplayResult.NonAuthoritative<String>>(replay(normal, null, CjmpRoleInputCompleteness.PARTIAL, listOf(check("partial", 1, common))))
        assertNull(partial.observedRole)
        assertNull(partial.events.single().before)
        assertNull(partial.firstKnownError)
        val prefix = assertIs<CjmpRoleReplayResult.NonAuthoritative<String>>(replay(normal, CjmpFileRole.COMMON, CjmpRoleInputCompleteness.PREFIX, listOf(check("prefix", 1, specific))))
        assertEquals(CjmpFileRole.COMMON, prefix.events.single().before)
        assertEquals(CjmpRoleInputCompleteness.PREFIX, prefix.completeness)
        assertEquals(CjmpRoleReplayContext.ENABLED_COMPILATION, prefix.context)
        val unknown = assertIs<CjmpRoleReplayResult.NonAuthoritative<String>>(replay(normal, CjmpFileRole.NONE, CjmpRoleInputCompleteness.COMPLETE, listOf(
            event("skip", 0, CjmpRoleEventKind.Skipped), event("unknown", 1, CjmpRoleEventKind.Unknown), check("later", 2, common),
        )))
        assertTrue(unknown.hasUnknownEvents)
        assertNull(unknown.observedRole)
        assertEquals(listOf(CjmpRoleEventOutcome.SKIPPED, CjmpRoleEventOutcome.UNKNOWN, CjmpRoleEventOutcome.UNKNOWN), unknown.events.map { it.outcome })
        assertNull(unknown.events[2].error)
    }

    @Test
    fun `unknown prefix retains only conclusions shared by every possible role`() {
        val commonMode = CjmpFileRoleMode.Compilation(true, true, false, false)
        val result = assertIs<CjmpRoleReplayResult.NonAuthoritative<String>>(replay(
            commonMode, CjmpFileRole.NONE, CjmpRoleInputCompleteness.COMPLETE,
            listOf(event("lazy", 0, CjmpRoleEventKind.Unknown), check("wrong", 1, specific), check("common", 2, common)),
        ))
        assertNull(result.events[1].before)
        assertNull(result.events[1].after)
        assertEquals(CjmpRoleEventOutcome.REJECTED, result.events[1].outcome)
        assertEquals(CjmpFileRoleError.SPECIFIC_IN_NON_SPECIFIC, result.events[1].error)
        assertEquals(CjmpRoleEventOutcome.UNKNOWN, result.events[2].outcome)
        assertTrue(result.hasUnknownEvents)
        assertNull(result.observedRole)
    }

    @Test
    fun `unknown state can converge without claiming a complete input`() {
        val mixed = CjmpFileRoleMode.Compilation(true, true, true, true)
        val result = assertIs<CjmpRoleReplayResult.NonAuthoritative<String>>(replay(
            mixed, null, CjmpRoleInputCompleteness.PARTIAL, listOf(check("both", 0, both)),
        ))
        assertNull(result.events.single().before)
        assertEquals(CjmpRoleEventOutcome.ACCEPTED, result.events.single().outcome)
        assertEquals(CjmpFileRole.BOTH, result.events.single().after)
        assertEquals(CjmpFileRole.BOTH, result.observedRole)
        assertEquals(CjmpRoleInputCompleteness.PARTIAL, result.completeness)
    }

    @Test
    fun `partial replay agrees with all concrete role transitions`() {
        for (enabled in listOf(false, true)) for (compileCommon in listOf(false, true))
            for (compileSpecific in listOf(false, true)) for (mixed in listOf(false, true))
                for (isCommon in listOf(false, true)) for (isSpecific in listOf(false, true)) {
                    val mode = CjmpFileRoleMode.Compilation(enabled, compileCommon, compileSpecific, mixed)
                    val declaration = CjmpDeclarationRole(isCommon, isSpecific)
                    val concrete = CjmpFileRole.entries.map { CjmpFileRoleTransition.check(mode, it, declaration) }
                    val actual = replay(mode, null, CjmpRoleInputCompleteness.PARTIAL,
                        listOf(check("member", 0, CjmpRoleModifiers(isCommon, isSpecific, false)))).events.single()
                    val expected = when {
                        !enabled -> CjmpRoleEventOutcome.NOT_CHECKED
                        concrete.all { it.error == null } -> CjmpRoleEventOutcome.ACCEPTED
                        concrete.all { it.error != null } -> CjmpRoleEventOutcome.REJECTED
                        else -> CjmpRoleEventOutcome.UNKNOWN
                    }
                    assertEquals(expected, actual.outcome, "$mode $declaration")
                    assertEquals(concrete.map { it.role }.distinct().singleOrNull(), actual.after, "$mode $declaration")
                    if (expected == CjmpRoleEventOutcome.REJECTED) {
                        assertEquals(concrete.map { it.error }.distinct().singleOrNull(), actual.error, "$mode $declaration")
                    }
                }
    }

    @Test
    fun `mixed mode and explicit initial state are respected`() {
        val mixed = CjmpFileRoleMode.Compilation(true, true, false, true)
        val result = assertIs<CjmpRoleReplayResult.Complete<String>>(replay(mixed, CjmpFileRole.SPECIFIC, CjmpRoleInputCompleteness.COMPLETE, listOf(check("common", 7, common))))
        assertEquals(CjmpFileRole.SPECIFIC, result.events.single().before)
        assertEquals(CjmpFileRole.BOTH, result.finalRole)
        assertNull(result.firstError)
    }

    @Test
    fun `syntax only and disabled compilation do not claim acceptance`() {
        val syntax = assertIs<CjmpRoleReplayResult.NonAuthoritative<String>>(replay(CjmpFileRoleMode.SyntaxOnly, CjmpFileRole.NONE, CjmpRoleInputCompleteness.COMPLETE, listOf(check("x", 0, both))))
        assertEquals(CjmpFileRole.BOTH, syntax.observedRole)
        assertEquals(CjmpRoleEventOutcome.NOT_CHECKED, syntax.events.single().outcome)
        assertEquals(CjmpRoleReplayContext.SYNTAX_ONLY, syntax.context)
        val disabled = assertIs<CjmpRoleReplayResult.NonAuthoritative<String>>(replay(normal.copy(enabled = false), CjmpFileRole.COMMON, CjmpRoleInputCompleteness.COMPLETE, listOf(check("x", 0, specific))))
        assertEquals(CjmpFileRole.COMMON, disabled.observedRole)
        assertEquals(CjmpRoleEventOutcome.NOT_CHECKED, disabled.events.single().outcome)
        assertEquals(CjmpRoleReplayContext.DISABLED_COMPILATION, disabled.context)
    }

    @Test
    fun `platform spelling is resolved in consuming language version`() {
        val events = listOf(check("platform", 0, CjmpRoleModifiers(false, false, true)))
        val alpha = assertIs<CjmpRoleReplayResult.Complete<String>>(CjmpFileRoleReplay.replay(normal, CjmpFileRole.NONE, CjmpRoleInputCompleteness.COMPLETE, events, LanguageVersionSettingsImpl(LanguageVersion.CANGJIE_1_1_0_ALPHA_65), CjmpRoleSyntaxIdentity.VERIFIED_EQUIVALENT))
        val beta = assertIs<CjmpRoleReplayResult.NonAuthoritative<String>>(CjmpFileRoleReplay.replay(normal, CjmpFileRole.NONE, CjmpRoleInputCompleteness.COMPLETE, events, LanguageVersionSettingsImpl(LanguageVersion.CANGJIE_1_1_0_BETA_20), CjmpRoleSyntaxIdentity.UNKNOWN))
        assertEquals(CjmpFileRole.SPECIFIC, alpha.finalRole)
        assertNull(beta.observedRole)
        assertEquals(CjmpRoleEventOutcome.UNKNOWN, beta.events.single().outcome)
        val syntax = assertIs<CjmpRoleReplayResult.NonAuthoritative<String>>(CjmpFileRoleReplay.replay(CjmpFileRoleMode.SyntaxOnly, CjmpFileRole.NONE, CjmpRoleInputCompleteness.COMPLETE, events, LanguageVersionSettingsImpl(LanguageVersion.CANGJIE_1_1_0_ALPHA_65), CjmpRoleSyntaxIdentity.VERIFIED_EQUIVALENT))
        assertNull(syntax.observedRole)
        assertEquals(CjmpRoleEventOutcome.UNKNOWN, syntax.events.single().outcome)
    }

    @Test
    fun `identities retain their owner type`() {
        data class Owner(val number: Int)
        val event = CjmpRoleEvent(CjmpRoleEventId(Owner(3)), 0, CjmpRoleEventKind.Check(common))
        assertEquals(Owner(3), replay(normal, CjmpFileRole.NONE, CjmpRoleInputCompleteness.COMPLETE, listOf(event)).events.single().event.id.value)
    }

    @Test
    fun `duplicate identities and unordered events are rejected`() {
        assertFailsWith<IllegalArgumentException> { replay(normal, CjmpFileRole.NONE, CjmpRoleInputCompleteness.COMPLETE, listOf(check("x", 0, common), check("x", 1, specific))) }
        assertFailsWith<IllegalArgumentException> { replay(normal, CjmpFileRole.NONE, CjmpRoleInputCompleteness.COMPLETE, listOf(check("x", 1, common), check("y", 1, specific))) }
    }

    @Test
    fun `extend only checks a single distinct role token in cjmp compilation`() {
        val commonMode = CjmpFileRoleMode.Compilation(true, true, false, false)
        val events = listOf(
            event("multiple", 0, CjmpRoleEventKind.Extend(specific, 2)),
            event("wrong", 1, CjmpRoleEventKind.Extend(specific, 1)),
            check("later", 2, common),
        )
        val result = assertIs<CjmpRoleReplayResult.Complete<String>>(
            replay(commonMode, CjmpFileRole.NONE, CjmpRoleInputCompleteness.COMPLETE, events),
        )
        assertEquals(listOf(CjmpRoleEventOutcome.SKIPPED, CjmpRoleEventOutcome.REJECTED, CjmpRoleEventOutcome.ACCEPTED),
            result.events.map { it.outcome })
        assertEquals(CjmpFileRole.COMMON, result.finalRole)
        assertEquals(CjmpRoleFirstError(CjmpRoleEventId("wrong"), CjmpFileRoleError.SPECIFIC_IN_NON_SPECIFIC), result.firstError)

        val noMode = commonMode.copy(compileCommon = false)
        val inactive = replay(noMode, CjmpFileRole.NONE, CjmpRoleInputCompleteness.COMPLETE,
            listOf(event("extend", 0, CjmpRoleEventKind.Extend(common, 1))))
        assertEquals(CjmpRoleEventOutcome.SKIPPED, inactive.events.single().outcome)
        assertEquals(CjmpFileRole.NONE, inactive.events.single().after)
    }
}
