package org.cangnova.cangjie.cfir.resolve.inference

import org.cangnova.cangjie.resolve.calls.inference.model.Constraint
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintError
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintKind
import org.cangnova.cangjie.resolve.calls.inference.model.IncorporationConstraintPosition
import org.cangnova.cangjie.resolve.calls.inference.model.InitialConstraint
import org.cangnova.cangjie.resolve.calls.inference.model.SimpleConstraintSystemConstraintPosition
import org.cangnova.cangjie.type.model.CangJieTypeMarker
import org.cangnova.cangjie.type.model.TypeVariableMarker
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * [CfirInferenceLogger] 日志块、origin 和 item 收集行为测试。
 */
class CfirInferenceLoggerTest {

    /**
     * 验证 logStage 会创建 unknown owner 的顶层 block。
     */
    @Test
    fun `log stage creates top-level block with unknown owner`() {
        val logger = CfirInferenceLogger()
        val system = FakeConstraintSystemMarker("stage-system")

        logger.logStage("Resolution Stages > Fake", system)

        assertEquals(1, logger.topLevelElements.size)
        val block = logger.topLevelElements.single()
        assertEquals("Resolution Stages > Fake", block.name)
        assertEquals(CfirInferenceLogger.BlockOwner.Unknown, block.owner)
        assertTrue(block.items.isEmpty())
    }

    /**
     * 验证 logger 能把变量、约束、错误和 fixed variable 写入当前 block。
     */
    @Test
    fun `logger collects variable constraint error and fix-variable items into current block`() {
        val logger = CfirInferenceLogger()
        val system = FakeConstraintSystemMarker("items-system")
        val variable = FakeTypeVariable("T")
        val constraint = FakeConstraint("Generated")
        val error = ConstraintError(FakeType("Lower"), FakeType("Upper"), IncorporationConstraintPosition(fakeInitialConstraint()))
        val resultType = FakeType("String")

        logger.logStage("Call Completion", system)
        logger.logNewVariable(variable, system)
        logger.log(variable, constraint, system)
        logger.logError(error, system)
        logger.logFixVariable(variable, resultType, system)

        val block = logger.topLevelElements.single()
        assertEquals(4, block.items.size)
        assertTrue(block.items[0] is CfirInferenceLogger.NewVariableElement)
        assertTrue(block.items[1] is CfirInferenceLogger.ConstraintElement)
        assertTrue(block.items[2] is CfirInferenceLogger.ErrorElement)
        assertTrue(block.items[3] is CfirInferenceLogger.FixVariableElement)

        val constraintElement = block.items[1] as CfirInferenceLogger.ConstraintElement
        assertTrue(constraintElement.formatted.contains("Generated"))
        val fixVariableElement = block.items[3] as CfirInferenceLogger.FixVariableElement
        assertEquals(resultType, fixVariableElement.resultType)
    }

    /**
     * 验证 withOrigin 会把缓存的 initial constraint 作为派生约束 origin。
     */
    @Test
    fun `withOrigin attaches cached initial constraint as origin for derived constraint`() {
        val logger = CfirInferenceLogger()
        val system = FakeConstraintSystemMarker("origin-system")
        val initialConstraint = fakeInitialConstraint()
        val variable = FakeTypeVariable("T")
        val derivedConstraint = FakeConstraint("Derived")

        logger.logStage("Call Completion", system)
        logger.logInitial(initialConstraint, system)
        logger.withOrigin(initialConstraint) {
            logger.log(variable, derivedConstraint, system)
        }

        val block = logger.topLevelElements.single()
        val initialElement = block.items[0] as CfirInferenceLogger.ConstraintElement
        val derivedElement = block.items[1] as CfirInferenceLogger.ConstraintElement
        assertEquals(listOf(initialElement), derivedElement.origins)
    }

    /**
     * 验证回到已知 constraint system 时创建 continuation block。
     */
    @Test
    fun `revisiting known system creates continuation block`() {
        val logger = CfirInferenceLogger()
        val firstSystem = FakeConstraintSystemMarker("first-system")
        val secondSystem = FakeConstraintSystemMarker("second-system")
        val variable = FakeTypeVariable("T")

        logger.logStage("Call Completion", firstSystem)
        logger.logNewVariable(variable, firstSystem)
        logger.logStage("Resolution Stages > Other", secondSystem)
        logger.logNewVariable(variable, secondSystem)

        logger.log(variable, FakeConstraint("BackToFirst"), firstSystem)

        assertEquals(3, logger.topLevelElements.size)
        val continuation = logger.topLevelElements[2]
        assertEquals("Continue Call Completion", continuation.name)
        assertEquals(CfirInferenceLogger.BlockOwner.Unknown, continuation.owner)
        assertEquals(1, continuation.items.size)
        assertTrue(continuation.items.single() is CfirInferenceLogger.ConstraintElement)
    }

    /**
     * 验证注册新阶段时会丢弃尾部空 block。
     */
    @Test
    fun `registering a new stage drops trailing empty block`() {
        val logger = CfirInferenceLogger()
        val system = FakeConstraintSystemMarker("same-system")

        logger.logStage("Unused Stage", system)
        logger.logStage("Used Stage", system)
        logger.logNewVariable(FakeTypeVariable("T"), system)

        assertEquals(1, logger.topLevelElements.size)
        val block = logger.topLevelElements.single()
        assertEquals("Used Stage", block.name)
        assertEquals(1, block.items.size)
    }
}

/**
 * 推断日志测试使用的 constraint system marker。
 */
private data class FakeConstraintSystemMarker(val debugName: String) : org.cangnova.cangjie.resolve.calls.inference.components.ConstraintSystemMarker {
    /**
     * 返回 marker 调试名称。
     */
    override fun toString(): String = debugName
}

/**
 * 推断日志测试使用的类型变量 marker。
 */
private data class FakeTypeVariable(private val debugName: String) : TypeVariableMarker {
    /**
     * 与变量名对应的 fake lookup tag。
     */
    val lookupTag = FakeLookupTag(debugName)
    /**
     * 返回变量调试名称。
     */
    override fun toString(): String = debugName
}

/**
 * 推断日志测试使用的 lookup tag。
 */
private data class FakeLookupTag(val name: String)

/**
 * 推断日志测试使用的类型 marker。
 */
private data class FakeType(private val debugName: String) : CangJieTypeMarker {
    /**
     * 返回类型调试名称。
     */
    override fun toString(): String = debugName
}

/**
 * 构造带稳定调试名称的 fake constraint。
 */
private fun FakeConstraint(debugName: String): Constraint = Constraint(
    kind = ConstraintKind.UPPER,
    type = FakeType(debugName),
    position = IncorporationConstraintPosition(fakeInitialConstraint()),
    derivedFrom = emptySet(),
    isNoInfer = false,
)

/**
 * 构造日志 origin 使用的初始约束。
 */
private fun fakeInitialConstraint(): InitialConstraint =
    InitialConstraint(FakeType("A"), FakeType("B"), ConstraintKind.UPPER, SimpleConstraintSystemConstraintPosition)
