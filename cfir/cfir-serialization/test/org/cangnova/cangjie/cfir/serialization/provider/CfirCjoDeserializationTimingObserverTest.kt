package org.cangnova.cangjie.cfir.serialization.provider

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * [CfirCjoDeserializationTimingObserver] 计时包装的回调时机测试。
 *
 * 这一层此前完全没有测试，而接线是否正确直接决定 IDE 诊断能否区分"卡在读 `.cjo`"与"卡在逐声明
 * 反序列化"。这里用真实的 measure 调用验证上报时机，不只验证接口形状。
 */
class CfirCjoDeserializationTimingObserverTest {
    /**
     * 未注册观察者时直接执行动作：不取时钟、不产生记录，也不改变返回值。
     */
    @Test
    fun nullObserverRunsActionWithoutRecording() {
        val observer: CfirCjoDeserializationTimingObserver? = null

        val result = observer.measureCjoPackageLoad { "package" }
        val declarations = observer.measureCjoDeclaration(3) { "decl" }

        assertEquals("package", result)
        assertEquals("decl", declarations)
    }

    /**
     * 包加载与按声明两个阶段各自上报一次，阶段与声明数正确。
     */
    @Test
    fun eachStageIsReportedOnce() {
        val recorder = RecordingObserver()

        recorder.measureCjoPackageLoad { "loaded" }
        recorder.measureCjoDeclaration(5) { "deserialized" }

        assertEquals(
            listOf(CfirCjoDeserializationStage.PACKAGE_LOAD, CfirCjoDeserializationStage.DECLARATION),
            recorder.stages,
        )
        assertEquals(0, recorder.declarationCounts[0], "包加载阶段不报声明数")
        assertEquals(5, recorder.declarationCounts[1], "按声明阶段带上声明数")
        assertTrue(recorder.elapsedNanos.all { it >= 0L }, recorder.elapsedNanos.toString())
    }

    /**
     * 动作抛异常时仍上报已消耗时长并原样抛出：加载失败的包同样是耗时，值得看见。
     */
    @Test
    fun failureIsReportedAndRethrown() {
        val recorder = RecordingObserver()

        val failure = assertThrows(IllegalStateException::class.java) {
            recorder.measureCjoDeclaration(2) { throw IllegalStateException("boom") }
        }

        assertEquals("boom", failure.message)
        assertEquals(listOf(CfirCjoDeserializationStage.DECLARATION), recorder.stages)
        assertEquals(2, recorder.declarationCounts.single())
    }

    /**
     * 记录上报内容的观察者替身。
     */
    private class RecordingObserver : CfirCjoDeserializationTimingObserver {
        val stages = mutableListOf<CfirCjoDeserializationStage>()
        val elapsedNanos = mutableListOf<Long>()
        val declarationCounts = mutableListOf<Int>()

        override fun onCjoDeserializationFinished(
            stage: CfirCjoDeserializationStage,
            elapsedNanos: Long,
            declarationCount: Int,
        ) {
            stages += stage
            this.elapsedNanos += elapsedNanos
            declarationCounts += declarationCount
        }
    }
}