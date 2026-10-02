package org.cangnova.cangjie.cfir.lightTree

import org.cangnova.cangjie.CjInMemoryTextSourceFile
import org.cangnova.cangjie.cfir.builder.BodyBuildingMode
import org.cangnova.cangjie.cfir.builder.CfirRawBuildSource
import org.cangnova.cangjie.cfir.builder.CfirRawBuildStage
import org.cangnova.cangjie.cfir.builder.CfirRawBuildTimingObserver
import org.cangnova.cangjie.cfir.builder.registerRawBuildTimingObserver
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider
import org.cangnova.cangjie.source.toSourceLinesMapping
import org.cangnova.cangjie.test.JUnit3RunnerWithInners
import org.junit.Assert.assertTrue
import org.junit.runner.RunWith

/**
 * raw 构建耗时观察者的 seam 测试。
 *
 * IDE 首开文件走 PSI 还是 LightTree 取决于 LightTree 可用性，阶段维度则决定"解析慢"与
 * "转换慢"能否区分。三条事实必须成立：
 *
 * 1. LightTree 路径依次上报 `parse` 与 `convert` 两个阶段；
 * 2. PSI 路径只上报 `convert`——PSI 解析由 IntelliJ 平台完成，本项目取不到其耗时；
 * 3. 未注册观察者时构建照常完成（零开销路径）。
 */
@RunWith(JUnit3RunnerWithInners::class)
class LightTreeRawBuildTimingObserverTest : AbstractLightTree2CfirConverterTestCase() {
    /**
     * 经 LightTree 解析入口构建时，解析段与转换段各自上报一次。
     */
    fun testLightTreePathReportsParseThenConvert() {
        val session = createTestSession()
        val observer = RecordingRawBuildObserver()
        session.registerRawBuildTimingObserver(observer)

        // 走「源码文本 → LightTree → CFIR」入口，解析发生在本项目内，必须可计时。
        LightTree2Cfir(session, session.cangjieScopeProvider)
            .buildCfirFileWithSurfaces(SOURCE, sourceFile("parseAndConvert"), SOURCE.toSourceLinesMapping())

        assertEquals(
            listOf(
                SegmentEvent(CfirRawBuildSource.LIGHT_TREE, CfirRawBuildStage.PARSE, null),
                SegmentEvent(CfirRawBuildSource.LIGHT_TREE, CfirRawBuildStage.CONVERT, BodyBuildingMode.NORMAL),
            ),
            observer.events,
        )
        assertTrue("每个阶段都必须上报耗时", observer.elapsedNanos.size == 2)
        assertTrue(observer.elapsedNanos.all { it >= 0L })
    }

    /**
     * PSI 路径只上报转换段：解析由平台负责，本项目没有可计时的边界。
     */
    fun testPsiPathReportsConvertOnly() {
        val session = createTestSession()
        val observer = RecordingRawBuildObserver()
        session.registerRawBuildTimingObserver(observer)

        createCjFile("psi", SOURCE).toCfirFile(session)

        assertEquals(
            listOf(SegmentEvent(CfirRawBuildSource.PSI, CfirRawBuildStage.CONVERT, BodyBuildingMode.NORMAL)),
            observer.events,
        )
    }

    /**
     * body 延迟构建模式在转换段如实上报策略。
     */
    fun testLazyBodiesModeIsReported() {
        val session = createTestSession()
        val observer = RecordingRawBuildObserver()
        session.registerRawBuildTimingObserver(observer)

        buildCfirFileFromLightTree(SOURCE, session, fileName = "lazy.cj", bodyBuildingMode = BodyBuildingMode.LAZY_BODIES)

        assertEquals(
            listOf(SegmentEvent(CfirRawBuildSource.LIGHT_TREE, CfirRawBuildStage.CONVERT, BodyBuildingMode.LAZY_BODIES)),
            observer.events,
        )
    }

    /**
     * 未注册观察者时构建照常完成（零开销路径）。
     */
    fun testBuildSucceedsWithoutObserver() {
        val file = buildCfirFileFromLightTree(SOURCE, createTestSession(), fileName = "noObserver.cj")
        assertTrue(file.name.endsWith("noObserver.cj"))
    }

    /**
     * 一次阶段执行的观察事件。
     */
    private data class SegmentEvent(
        val source: CfirRawBuildSource,
        val stage: CfirRawBuildStage,
        val bodyBuildingMode: BodyBuildingMode?,
    )

    /**
     * 记录回调的观察者替身。
     */
    private class RecordingRawBuildObserver : CfirRawBuildTimingObserver {
        val events: MutableList<SegmentEvent> = mutableListOf()
        val elapsedNanos: MutableList<Long> = mutableListOf()

        override fun onRawBuildFinished(
            source: CfirRawBuildSource,
            stage: CfirRawBuildStage,
            bodyBuildingMode: BodyBuildingMode?,
            elapsedNanos: Long,
        ) {
            events.add(SegmentEvent(source, stage, bodyBuildingMode))
            this.elapsedNanos.add(elapsedNanos)
        }
    }

    private companion object {
        private const val SOURCE = "func add(value: Int64): Int64 { return value + 1 }"

        private fun sourceFile(name: String) = CjInMemoryTextSourceFile(name, null, SOURCE)
    }
}
