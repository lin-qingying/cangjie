package org.cangnova.cangjie.phaser

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PhaseBuildersTest {
    @Test
    fun `then invokes named phases through the phaser protocol and flattens their names`() {
        val context = TestContext()
        val first = TestPhase<String, Int>(
            name = "first",
            transform = { phaseContext, input ->
                phaseContext.events += "first-body:${phaseContext.inVerbosePhase}"
                input.length
            },
            disabledOutput = { -1 },
            preconditions = setOf({ input -> assertEquals("hello", input) }),
            postconditions = setOf({ output -> assertEquals(5, output) }),
            preactions = setOf({ _, _, phaseContext ->
                phaseContext.events += "first-before:${phaseContext.inVerbosePhase}"
            }),
            postactions = setOf({ _, _, phaseContext ->
                phaseContext.events += "first-after:${phaseContext.inVerbosePhase}"
            }),
        )
        val second = TestPhase<Int, String>(
            name = "second",
            transform = { phaseContext, input ->
                phaseContext.events += "second-body:${phaseContext.inVerbosePhase}"
                "x".repeat(input)
            },
            disabledOutput = { "disabled" },
            prerequisite = setOf(first),
            preconditions = setOf({ input -> assertTrue(input > 0) }),
            postconditions = setOf({ output -> assertEquals(5, output.length) }),
            preactions = setOf({ _, _, phaseContext ->
                phaseContext.events += "second-before:${phaseContext.inVerbosePhase}"
            }),
            postactions = setOf({ _, _, phaseContext ->
                phaseContext.events += "second-after:${phaseContext.inVerbosePhase}"
            }),
        )
        val third = TestPhase<String, String>(
            name = "third",
            transform = { _, input -> input },
            disabledOutput = { it },
            prerequisite = setOf(second),
        )
        val phase = first then second then third
        val phaseConfig = PhaseConfig(
            verbose = PhaseSet.Enum(setOf("first", "second", "third")),
            checkConditions = true,
        )
        val phaserState = PhaserState()

        val output = phase.invoke(phaseConfig, phaserState, context, "hello")

        assertEquals("xxxxx", output)
        assertEquals(
            listOf(
                "first-before:true",
                "first-body:true",
                "first-after:true",
                "second-before:true",
                "second-body:true",
                "second-after:true",
            ),
            context.events,
        )
        assertEquals(3, phaserState.phaseCount)
        assertTrue(phaserState.alreadyDone.containsAll(setOf(first, second, third)))
        assertFalse(context.inVerbosePhase)
        assertEquals(
            listOf("first", "second", "third"),
            phase.getNamedSubphases(startDepth = 4).map { (_, namedPhase) -> namedPhase.name },
        )
        assertEquals(
            listOf(4, 4, 4),
            phase.getNamedSubphases(startDepth = 4).map { (depth, _) -> depth },
        )
    }

    @Test
    fun `then passes disabled phase configuration to each named phase`() {
        val context = TestContext()
        val first = TestPhase<Int, Int>(
            name = "enabled",
            transform = { _, input -> input + 1 },
            disabledOutput = { it },
        )
        val second = TestPhase<Int, Int>(
            name = "disabled",
            transform = { _, _ -> error("disabled phase body must not run") },
            disabledOutput = { it },
            prerequisite = setOf(first),
        )
        val phaserState = PhaserState()

        val output = (first then second).invoke(
            PhaseConfig(disabled = PhaseSet.Enum(setOf("disabled"))),
            phaserState,
            context,
            1,
        )

        assertEquals(2, output)
        assertEquals(1, phaserState.phaseCount)
        assertEquals(setOf(first), phaserState.alreadyDone)
    }

    private class TestContext : LoggingContext {
        override var inVerbosePhase: Boolean = false
        val events: MutableList<String> = mutableListOf()
    }

    private class TestPhase<Input, Output>(
        name: String,
        private val transform: (TestContext, Input) -> Output,
        private val disabledOutput: (Input) -> Output,
        prerequisite: Set<AnyNamedPhase> = emptySet(),
        preconditions: Set<Checker<Input>> = emptySet(),
        postconditions: Set<Checker<Output>> = emptySet(),
        preactions: Set<Action<Input, TestContext>> = emptySet(),
        postactions: Set<Action<Pair<Input, Output>, TestContext>> = emptySet(),
    ) : NamedCompilerPhase<TestContext, Input, Output>(
        name = name,
        prerequisite = prerequisite,
        preconditions = preconditions,
        postconditions = postconditions,
        preactions = preactions,
        postactions = postactions,
    ) {
        override fun phaseBody(context: TestContext, input: Input): Output = transform(context, input)

        override fun outputIfNotEnabled(
            phaseConfig: PhaseConfig,
            phaserState: PhaserState,
            context: TestContext,
            input: Input,
        ): Output = disabledOutput(input)
    }
}
