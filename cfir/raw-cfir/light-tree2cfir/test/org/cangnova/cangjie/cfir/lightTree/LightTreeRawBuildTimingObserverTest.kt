package org.cangnova.cangjie.cfir.lightTree

import org.cangnova.cangjie.cfir.builder.BodyBuildingMode
import org.cangnova.cangjie.cfir.builder.CfirRawBuildSource
import org.cangnova.cangjie.cfir.builder.CfirRawBuildTimingObserver
import org.cangnova.cangjie.cfir.builder.registerRawBuildTimingObserver
import org.cangnova.cangjie.test.JUnit3RunnerWithInners
import org.junit.Assert.assertTrue
import org.junit.runner.RunWith

/**
 * raw 构建耗时观察者的 seam 测试。
 *
 * IDE 首开文件走 PSI 还是 LightTree 取决于 LightTree 可用性，两条路径必须各自上报一次，
 * 否则首开卡顿无法区分是解析慢还是 raw 构建慢。
 */
@RunWith(JUnit3RunnerWithInners::class)
class LightTreeRawBuildTimingObserverTest : AbstractLightTree2CfirConverterTestCase() {
    /**
     * LightTree 路径与 PSI 路径分别上报自己的来源与 body 策略。
     */
    fun testBothPathsReportTheirSource() {
        val session = createTestSession()
        val observer = RecordingRawBuildObserver()
        session.registerRawBuildTimingObserver(observer)

        buildCfirFileFromLightTree(SOURCE, session, fileName = "lightTree.cj")
        assertEquals(
            listOf(CfirRawBuildSource.LIGHT_TREE to BodyBuildingMode.NORMAL),
            observer.events,
        )

        createCjFile("psi", SOURCE).toCfirFile(session)
        assertEquals(
            listOf(
                CfirRawBuildSource.LIGHT_TREE to BodyBuildingMode.NORMAL,
                CfirRawBuildSource.PSI to BodyBuildingMode.NORMAL,
            ),
            observer.events,
        )
        assertEquals(observer.events.size, observer.elapsedNanos.size)
        assertTrue(observer.elapsedNanos.all { it >= 0L })
    }

    /**
     * 未注册观察者时构建照常完成（零开销路径）。
     */
    fun testBuildSucceedsWithoutObserver() {
        val file = buildCfirFileFromLightTree(SOURCE, createTestSession(), fileName = "noObserver.cj")
        assertTrue(file.name.endsWith("noObserver.cj"))
    }

    /**
     * 记录回调的观察者替身：来源与 body 策略用于断言，耗时只验证非负。
     */
    private class RecordingRawBuildObserver : CfirRawBuildTimingObserver {
        val events: MutableList<Pair<CfirRawBuildSource, BodyBuildingMode>> = mutableListOf()
        val elapsedNanos: MutableList<Long> = mutableListOf()

        override fun onRawBuildFinished(source: CfirRawBuildSource, bodyBuildingMode: BodyBuildingMode, elapsedNanos: Long) {
            events.add(source to bodyBuildingMode)
            this.elapsedNanos.add(elapsedNanos)
        }
    }

    private companion object {
        private const val SOURCE = "func add(value: Int64): Int64 { return value + 1 }"
    }
}
