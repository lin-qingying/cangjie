@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.analysis.api.standalone.base.index

import com.intellij.openapi.progress.EmptyProgressIndicatorBase
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.analysis.api.analyze
import org.cangnova.cangjie.analysis.api.platform.index.CaSymbolIndexEntryKind
import org.cangnova.cangjie.analysis.api.platform.index.CaSymbolIndexNameFilter
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.psi.CjFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 真实模块图上的名称发现。
 *
 * 断言到**名称条目**为止：符号恢复不在索引职责内，由 Analysis 层从包作用域查询完成，
 * 因此这里不用假符号替代，也不重复验证恢复路径。
 */
class CaStandaloneSymbolIndexQueryTest : AbstractAnalysisApiExecutionTest(
    "analysis/analysis-api-standalone/testData/symbolIndex",
) {
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    @Test
    fun sourceIndexPreservesEntriesAndScope(mainFile: CjFile) {
        val service = CaStandaloneSymbolIndexQueryService(mainFile.project)
        // 范围由调用方绑定：索引只按绑定范围过滤，模块依赖可见性来自 Analysis 的 analysisScope。
        // 传 allScope 会把无关模块一并召回，那不是索引的职责。
        val analysisScope = analyze(mainFile) { analysisScope }
        val query = service.createSymbolIndexQuery(analysisScope)

        // 索引按「包 + 名称 + 种类」成条：同包重载在名称层就是同一个名字，
        // 它们由恢复阶段按包级查询展开，**不是**在索引里靠签名区分。
        val overloads = query.findDeclarations(CaSymbolIndexNameFilter("indexedLoad"))
        assertEquals(2, overloads.size, "两个包各自成条")
        assertEquals(2, overloads.map { it.packageFqName.asString() }.toSet().size, "跨包同名必须保留包身份")

        val widgets = query.findDeclarations(CaSymbolIndexNameFilter("IndexedWidget"))
        assertEquals(2, widgets.size, "同名的两个类必须各自成条")
        assertEquals(setOf(CaSymbolIndexEntryKind.CLASS_LIKE), widgets.map { it.kind }.toSet())

        // 枚举构造器必须带所属类型，否则恢复时无法定位到枚举；
        // 同名不同 payload 个数在名称层同样是一个名字，由恢复阶段各自展开。
        val dots = query.findDeclarations(CaSymbolIndexNameFilter("IndexedDot"))
        assertEquals(1, dots.size, "同一个枚举的同名构造器在名称层成一条")
        assertEquals(setOf(CaSymbolIndexEntryKind.ENUM_CONSTRUCTOR), dots.map { it.kind }.toSet())
        val owner = dots.single().ownerClassId
        assertEquals("IndexedShape", owner?.shortClassName?.asString(), "条目必须带上所属枚举")

        // 类型别名与类走同一条分类通道：名称层无法区分二者，恢复阶段才分化。
        val aliases = query.findDeclarations(CaSymbolIndexNameFilter("IndexedAlias"))
        assertEquals(1, aliases.size, "类型别名必须能被按名发现")
        assertEquals(setOf(CaSymbolIndexEntryKind.CLASS_LIKE), aliases.map { it.kind }.toSet())

        val names = query.findDeclarations(CaSymbolIndexNameFilter("indexed")).map { it.name.asString() }
        assertFalse("indexedOutside" in names, "不能召回没有依赖关系的模块")
        assertTrue("indexedLoad" in names)

        val narrow = service.createSymbolIndexQuery(GlobalSearchScope.fileScope(mainFile))
        assertTrue(narrow.findDeclarations(CaSymbolIndexNameFilter("indexedLoad")).isEmpty())

        val collections = service.fileCollectionCount.get()
        val summaries = service.summaryBuildCount.get()
        repeat(5) { query.findDeclarations(CaSymbolIndexNameFilter("indexed")) }
        assertEquals(collections, service.fileCollectionCount.get(), "连续查询不重新遍历文件")
        assertEquals(summaries, service.summaryBuildCount.get(), "连续查询不重新读取 stub 摘要")
        assertEquals(1, collections)
        assertTrue(service.queryNanos.get() > 0)
    }

    /**
     * 平台失效后必须重建索引，不得继续用旧快照回答。
     *
     * 缓存键是平台修改计数；失效会推进它，因此下一次查询必须重新收集文件。
     * 若缓存键取错（例如只按会话或永不失效），用户会在模块图变更后一直看到旧候选，
     * 而且是**静默**的——没有任何异常，只是候选永远不更新。
     */
    @Test
    fun indexRebuildsAfterInvalidation(indexRebuildsAfterInvalidation: CjFile) {
        var version = 1L
        val service = CaStandaloneSymbolIndexQueryService(indexRebuildsAfterInvalidation.project)
        service.versionProvider = { version }
        val query = service.createSymbolIndexQuery(analyze(indexRebuildsAfterInvalidation) { analysisScope })
        val filter = CaSymbolIndexNameFilter("rebuildCandidate")

        assertTrue(query.findDeclarations(filter).isNotEmpty(), "失效前应能发现依赖模块的声明")
        val collectionsBefore = service.fileCollectionCount.get()
        query.findDeclarations(filter)
        assertEquals(collectionsBefore, service.fileCollectionCount.get(), "同一版本内不得重建")

        // 版本推进等价于平台失效：下一次查询必须重新收集文件。
        version++
        assertTrue(query.findDeclarations(filter).isNotEmpty(), "重建后仍应能发现同一声明")
        assertTrue(
            service.fileCollectionCount.get() > collectionsBefore,
            "版本推进后必须重新收集文件，实际计数未推进",
        )
    }

    /**
     * 索引查询必须响应取消，不得把取消吞成空候选。
     *
     * 把取消吞掉的后果是用户在长查询期间按下取消，界面收到的是「没有候选」
     * 而不是「这次请求被取消」，两者在补全上的处理完全不同。
     */
    @Test
    fun indexQueryHonoursCancellation(indexQueryHonoursCancellation: CjFile) {
        val service = CaStandaloneSymbolIndexQueryService(indexQueryHonoursCancellation.project)
        val query = service.createSymbolIndexQuery(analyze(indexQueryHonoursCancellation) { analysisScope })
        val filter = CaSymbolIndexNameFilter("cancelCandidate")
        // 先建立索引：本用例要验证的是**查询**响应取消，不是构建期。
        assertTrue(query.findDeclarations(filter).isNotEmpty())

        val canceled = object : EmptyProgressIndicatorBase() {
            override fun isCanceled(): Boolean = true
            override fun cancel() = Unit
            override fun start() = Unit
            override fun stop() = Unit
            override fun isRunning(): Boolean = true
        }
        assertThrows(ProcessCanceledException::class.java) {
            ProgressManager.getInstance().runProcess<Any?>({ query.findDeclarations(filter) }, canceled)
        }
    }
}
