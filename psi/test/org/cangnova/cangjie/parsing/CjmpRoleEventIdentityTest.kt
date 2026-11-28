package org.cangnova.cangjie.parsing

import com.intellij.lang.PsiBuilderFactory
import org.cangnova.cangjie.CjmpFileRoleMode
import org.cangnova.cangjie.CjmpRoleEventOutcome
import org.cangnova.cangjie.CjmpRoleInputCompleteness
import org.cangnova.cangjie.CjmpRoleSyntaxIdentity
import org.cangnova.cangjie.LanguageVersionSettingsImpl
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import java.util.UUID
import org.cangnova.cangjie.CjmpRoleEventKind
import org.cangnova.cangjie.CjmpRoleModifiers

/** 独立根的局部序号可重复，合并后的检查顺序必须与稳定身份分离。 */
class CjmpRoleEventIdentityTest : CjParsingTestCase("", "cj", CangJieFileType.INSTANCE, CangJieParserDefinition()) {
    @BeforeEach fun setUpFixture() = setUp()
    @AfterEach fun tearDownFixture() = tearDown()

    @Test
    fun independentRootEventsCanBeReplayedInTheirInsertionOrder() {
        val context = CjmpParsingContext.Compilation(true, true, true)
        fun event(role: String): CjmpParsedRoleEvent {
            val source = "$role func declaration(): Unit {}"
            val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), source)
            return CangJieLightParser.parse(builder, cjmpContext = context).roleEvents.single()
        }
        val common = event("common")
        val specific = event("specific")
        assertNotEquals(common.id.rootIdentity, specific.id.rootIdentity)
        assertEquals(0L, common.id.sequence)
        assertEquals(0L, specific.id.sequence)
        val snapshot = CjmpFileRoleSyntaxSnapshot(context, CjmpFileRole.NONE,
            CjmpRoleInputCompleteness.COMPLETE, listOf(common, specific))
        val replay = CjmpRoleSnapshotReplay.replay(snapshot, CjmpFileRoleMode.Compilation(true, true, true, false),
            LanguageVersionSettingsImpl.DEFAULT, CjmpRoleSyntaxIdentity.VERIFIED_EQUIVALENT)
        assertEquals(listOf(CjmpRoleEventOutcome.ACCEPTED, CjmpRoleEventOutcome.REJECTED),
            requireNotNull(replay.resultsFor(listOf(common, specific))).map { it.outcome })
        assertEquals(listOf(0L, 1L), replay.result.events.map { it.event.order })
        assertNull(replay.resultsFor(listOf(common.copy())))
        assertNull(replay.resultsFor(listOf(specific, common)))
    }

    @Test
    fun rootSequenceValidationIncludesNonAdjacentEvents() {
        val firstRoot = UUID.randomUUID()
        val secondRoot = UUID.randomUUID()
        val kind = CjmpRoleEventKind.Check(CjmpRoleModifiers(true, false, false))
        val first = CjmpParsedRoleEvent(CjmpParsedRoleEventId(firstRoot, 1), 0, kind)
        val middle = CjmpParsedRoleEvent(CjmpParsedRoleEventId(secondRoot, 0), 0, kind)
        val earlier = CjmpParsedRoleEvent(CjmpParsedRoleEventId(firstRoot, 0), 0, kind)
        assertFailsWith<IllegalArgumentException> {
            CjmpFileRoleSyntaxSnapshot(CjmpParsingContext.SyntaxOnly, CjmpFileRole.NONE,
                CjmpRoleInputCompleteness.COMPLETE, listOf(first, middle, earlier))
        }
        assertFailsWith<IllegalArgumentException> {
            CjmpFileRoleSyntaxSnapshot(CjmpParsingContext.SyntaxOnly, CjmpFileRole.NONE,
                CjmpRoleInputCompleteness.COMPLETE, listOf(first, first.copy()))
        }
    }
}
