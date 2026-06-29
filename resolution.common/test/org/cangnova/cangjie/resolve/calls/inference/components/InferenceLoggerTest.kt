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

/**
 * [InferenceLogger] 基础委托行为和 Dummy 实现的回归测试。
 */
class InferenceLoggerTest {

    /**
     * 验证单 origin 包装会执行 action、返回 action 结果，并把 origin 传递给日志器。
     */
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

    /**
     * 验证双 origin 包装会执行 action、返回 action 结果，并记录两个来源对象。
     */
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

    /**
     * 验证泛型约束、错误和变量固定日志回调会暴露完整参数。
     */
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

    /**
     * 验证 Dummy 日志器对所有回调保持 no-op 且不改变返回值。
     */
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

/**
 * 记录所有回调参数的测试日志器。
 */
private class RecordingInferenceLogger : InferenceLogger() {
    /**
     * action 内部手动记录的事件序列。
     */
    val events = mutableListOf<String>()

    /**
     * 单 origin 包装收到的 origin 列表。
     */
    val originCalls = mutableListOf<Any?>()

    /**
     * 双 origin 包装收到的 origin 组合列表。
     */
    val originPairs = mutableListOf<OriginPair>()

    /**
     * 约束日志回调收到的参数列表。
     */
    val variableConstraintCalls = mutableListOf<VariableConstraintCall>()

    /**
     * 错误日志回调收到的参数列表。
     */
    val errorCalls = mutableListOf<ErrorCall>()

    /**
     * 固定变量日志回调收到的参数列表。
     */
    val fixVariableCalls = mutableListOf<FixVariableCall>()

    /**
     * 记录 action 内部事件。
     */
    fun record(event: String) {
        events += event
    }

    /**
     * 记录单 origin 后委托给父类执行 action。
     */
    override fun <T> withOrigin(origin: Any?, action: () -> T): T {
        originCalls += origin
        return super.withOrigin(origin, action)
    }

    /**
     * 记录双 origin 后委托给父类执行 action。
     */
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

    /**
     * 记录变量约束日志回调参数。
     */
    override fun log(variable: TypeVariableMarker, constraint: Constraint, context: Any?) {
        variableConstraintCalls += VariableConstraintCall(variable, constraint, context)
    }

    /**
     * 记录错误日志回调参数。
     */
    override fun logError(error: ConstraintSystemError, context: Any?) {
        errorCalls += ErrorCall(error, context)
    }

    /**
     * 记录变量固定日志回调参数。
     */
    override fun logFixVariable(variable: TypeVariableMarker, resultType: CangJieTypeMarker, context: Any?) {
        fixVariableCalls += FixVariableCall(variable, resultType, context)
    }
}

/**
 * 双 origin 调用的参数快照。
 */
private data class OriginPair(
    /**
     * 第一个来源所有者。
     */
    val firstOwner: Any?,

    /**
     * 第一个来源对象。
     */
    val firstOrigin: Any?,

    /**
     * 第二个来源所有者。
     */
    val secondOwner: Any?,

    /**
     * 第二个来源对象。
     */
    val secondOrigin: Any?,
)

/**
 * 变量约束日志回调的参数快照。
 */
private data class VariableConstraintCall(
    /**
     * 参与日志记录的类型变量。
     */
    val variable: TypeVariableMarker,

    /**
     * 参与日志记录的约束。
     */
    val constraint: Constraint,

    /**
     * 日志上下文对象。
     */
    val context: Any?,
)

/**
 * 错误日志回调的参数快照。
 */
private data class ErrorCall(
    /**
     * 被记录的约束系统错误。
     */
    val error: ConstraintSystemError,

    /**
     * 日志上下文对象。
     */
    val context: Any?,
)

/**
 * 变量固定日志回调的参数快照。
 */
private data class FixVariableCall(
    /**
     * 被固定的类型变量。
     */
    val variable: TypeVariableMarker,

    /**
     * 类型变量固定后的结果类型。
     */
    val resultType: CangJieTypeMarker,

    /**
     * 日志上下文对象。
     */
    val context: Any?,
)

/**
 * 只提供调试名称的测试类型变量。
 */
private data class FakeTypeVariable(
    /**
     * `toString` 返回的调试名称。
     */
    private val debugName: String,
) : TypeVariableMarker {
    /**
     * 返回调试名称。
     */
    override fun toString(): String = debugName
}

/**
 * 只提供调试名称的测试类型。
 */
private data class FakeType(
    /**
     * `toString` 返回的调试名称。
     */
    private val debugName: String,
) : CangJieTypeMarker {
    /**
     * 返回调试名称。
     */
    override fun toString(): String = debugName
}

/**
 * 创建带指定调试名称的测试约束。
 */
private fun FakeConstraint(debugName: String): Constraint = Constraint(
    kind = ConstraintKind.UPPER,
    type = FakeType(debugName),
    position = IncorporationConstraintPosition(FakeInitialConstraint()),
    derivedFrom = emptySet(),
    isNoInfer = false,
)

/**
 * 创建测试用初始约束。
 */
private fun FakeInitialConstraint(): InitialConstraint =
    InitialConstraint(FakeType("A"), FakeType("B"), ConstraintKind.UPPER, SimpleConstraintSystemConstraintPosition)
