package org.cangnova.cangjie.resolve.calls.inference.components

import org.cangnova.cangjie.resolve.calls.inference.model.Constraint
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintKind
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintError
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintSystemError
import org.cangnova.cangjie.resolve.calls.inference.model.IncorporationConstraintPosition
import org.cangnova.cangjie.resolve.calls.inference.model.InitialConstraint
import org.cangnova.cangjie.resolve.calls.inference.model.SimpleConstraintSystemConstraintPosition
import org.cangnova.cangjie.type.model.CangJieTypeMarker
import org.cangnova.cangjie.type.model.TypeVariableMarker
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class InferenceLoggerTest {

    @Test
    fun `withOrigin delegates and returns action result`() {
        val logger = RecordingInferenceLogger()
        val origin = InitialConstraint(FakeType("A"), FakeType("B"), ConstraintKind.UPPER, SimpleConstraintSystemConstraintPosition)

        val result = logger.withOrigin(origin) {
            logger.record("inside")
            "done"
        }

        assertEquals("done", result)
        assertEquals(listOf("inside"), logger.events)
        assertEquals(listOf(origin), logger.originCalls)
    }

    @Test
    fun `withOrigins delegates and returns action result`() {
        val logger = RecordingInferenceLogger()
        val firstVariable = FakeTypeVariable("T")
        val secondVariable = FakeTypeVariable("R")
        val firstConstraint = FakeConstraint("first")
        val secondConstraint = FakeConstraint("second")

        val result = logger.withOrigins(firstVariable, firstConstraint, secondVariable, secondConstraint) {
            logger.record("two-origins")
            42
        }

        assertEquals(42, result)
        assertEquals(listOf("two-origins"), logger.events)
        assertEquals(
            listOf(OriginPair(firstVariable, firstConstraint, secondVariable, secondConstraint)),
            logger.originPairs,
        )
    }

    @Test
    fun `logger exposes generic callbacks for constraint error and fix-variable events`() {
        val logger = RecordingInferenceLogger()
        val variable = FakeTypeVariable("T")
        val constraint = FakeConstraint("derived")
        val error = ConstraintError(FakeType("Lower"), FakeType("Upper"), IncorporationConstraintPosition(FakeInitialConstraint()))
        val resultType = FakeType("String")
        val context = Any()

        logger.log(variable, constraint, context)
        logger.logError(error, context)
        logger.logFixVariable(variable, resultType, context)

        assertEquals(listOf(VariableConstraintCall(variable, constraint, context)), logger.variableConstraintCalls)
        assertEquals(listOf(ErrorCall(error, context)), logger.errorCalls)
        assertEquals(listOf(FixVariableCall(variable, resultType, context)), logger.fixVariableCalls)
    }

    @Test
    fun `dummy logger accepts all callbacks as no-op`() {
        val variable = FakeTypeVariable("T")
        val constraint = FakeConstraint("derived")
        val error = ConstraintError(FakeType("Lower"), FakeType("Upper"), IncorporationConstraintPosition(FakeInitialConstraint()))
        val resultType = FakeType("String")
        val initial = InitialConstraint(FakeType("A"), FakeType("B"), ConstraintKind.UPPER, SimpleConstraintSystemConstraintPosition)
        val context = Any()

        InferenceLogger.Dummy.logInitial(initial, context)
        InferenceLogger.Dummy.logNewVariable(variable, context)
        InferenceLogger.Dummy.log(variable, constraint, context)
        InferenceLogger.Dummy.logError(error, context)
        InferenceLogger.Dummy.logReadiness(InferenceLogger.FixationLogRecord(emptyMap(), variable), context)
        InferenceLogger.Dummy.logFixVariable(variable, resultType, context)

        val originResult = InferenceLogger.Dummy.withOrigin(initial) { "origin" }
        val originsResult = InferenceLogger.Dummy.withOrigins(variable, constraint, variable, constraint) { "origins" }

        assertEquals("origin", originResult)
        assertEquals("origins", originsResult)
        assertTrue(true)
    }
}

private class RecordingInferenceLogger : InferenceLogger() {
    val events = mutableListOf<String>()
    val originCalls = mutableListOf<Any?>()
    val originPairs = mutableListOf<OriginPair>()
    val variableConstraintCalls = mutableListOf<VariableConstraintCall>()
    val errorCalls = mutableListOf<ErrorCall>()
    val fixVariableCalls = mutableListOf<FixVariableCall>()

    fun record(event: String) {
        events += event
    }

    override fun <T> withOrigin(origin: Any?, action: () -> T): T {
        originCalls += origin
        return super.withOrigin(origin, action)
    }

    override fun <T> withOrigins(
        firstOwner: Any?,
        firstOrigin: Any?,
        secondOwner: Any?,
        secondOrigin: Any?,
        action: () -> T,
    ): T {
        originPairs += OriginPair(firstOwner, firstOrigin, secondOwner, secondOrigin)
        return super.withOrigins(firstOwner, firstOrigin, secondOwner, secondOrigin, action)
    }

    override fun log(variable: TypeVariableMarker, constraint: Constraint, context: Any?) {
        variableConstraintCalls += VariableConstraintCall(variable, constraint, context)
    }

    override fun logError(error: ConstraintSystemError, context: Any?) {
        errorCalls += ErrorCall(error, context)
    }

    override fun logFixVariable(variable: TypeVariableMarker, resultType: CangJieTypeMarker, context: Any?) {
        fixVariableCalls += FixVariableCall(variable, resultType, context)
    }
}

private data class OriginPair(
    val firstOwner: Any?,
    val firstOrigin: Any?,
    val secondOwner: Any?,
    val secondOrigin: Any?,
)

private data class VariableConstraintCall(
    val variable: TypeVariableMarker,
    val constraint: Constraint,
    val context: Any?,
)

private data class ErrorCall(
    val error: ConstraintSystemError,
    val context: Any?,
)

private data class FixVariableCall(
    val variable: TypeVariableMarker,
    val resultType: CangJieTypeMarker,
    val context: Any?,
)

private data class FakeTypeVariable(private val debugName: String) : TypeVariableMarker {
    override fun toString(): String = debugName
}

private data class FakeType(private val debugName: String) : CangJieTypeMarker {
    override fun toString(): String = debugName
}

private fun FakeConstraint(debugName: String): Constraint = Constraint(
    kind = ConstraintKind.UPPER,
    type = FakeType(debugName),
    position = IncorporationConstraintPosition(FakeInitialConstraint()),
    derivedFrom = emptySet(),
    isNoInfer = false,
)

private fun FakeInitialConstraint(): InitialConstraint =
    InitialConstraint(FakeType("A"), FakeType("B"), ConstraintKind.UPPER, SimpleConstraintSystemConstraintPosition)
