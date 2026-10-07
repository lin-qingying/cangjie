@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.analysis.api.standalone.base.index

import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.analysis.api.analyze
import org.cangnova.cangjie.analysis.api.platform.index.CaSymbolIndexEntryKind
import org.cangnova.cangjie.analysis.api.platform.index.CaSymbolIndexNameFilter
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.psi.CjFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
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
}
