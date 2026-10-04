package org.cangnova.cangjie.ide.completion.impl.shared.lookup

import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementDecorator
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.codeInsight.lookup.LookupElementRenderer
import org.cangnova.cangjie.ide.completion.api.CompletionItemKind
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionOrigin
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionSortDataImpl
import org.cangnova.cangjie.ide.completion.api.lookup.ExpectedTypeMatch
import org.cangnova.cangjie.ide.completion.api.lookup.ImportCost
import org.cangnova.cangjie.ide.completion.api.lookup.toWeight
import org.cangnova.cangjie.ide.completion.api.lookup.withExpectedTypeMatch
import org.cangnova.cangjie.ide.completion.impl.shared.completionSnapshot
import org.cangnova.cangjie.ide.completion.impl.shared.namedArgumentSnapshot
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * lookup 元素工厂回归。
 *
 * 工厂是「分析结果 → 界面元素」的唯一转换点：这里验证的每一条
 * （字段映射、快照回挂、优先级折算、稳定排序）都是 B5/B6 的硬约束，
 * 破坏任何一条都会让弹窗展示与分析结论脱节。
 */
class LookupElementFactoryTest : CjParsingTestCase(
    dataPath = "",
    fileExt = "cj",
    fileType = CangJieFileType.INSTANCE,
    CangJieParserDefinition(),
) {
    /**
     * 初始化平台测试环境。
     */
    @BeforeEach
    fun setUpFixture() {
        setUp()
    }

    /**
     * 释放平台测试环境。
     */
    @AfterEach
    fun tearDownFixture() {
        tearDown()
    }

    /**
     * 快照字段必须原样映射到平台元素，不许在界面期重算语义。
     */
    @Test
    fun mapsSnapshotFields() {
        val snapshot = completionSnapshot(
            lookupString = "visible",
            kind = CompletionItemKind.MEMBER,
            origin = CompletionOrigin.MEMBER,
            tailText = "(x: Int64): Int64",
            typeText = "Int64",
        ).build()

        val element = LookupElementFactory.create(snapshot)
        assertEquals("visible", element.lookupString)
        // 展示字段经平台的呈现模型读取：builder 的尾文本、类型文本与图标
        // 由 expensive renderer 渲染（普通 renderElement 只写主文本），
        // 弹窗展示的就是这份呈现内容。
        val presentation = presentationOf(element)
        assertEquals("visible", presentation.itemText)
        assertEquals("(x: Int64): Int64", presentation.tailText)
        assertEquals("Int64", presentation.typeText)
        assertNotNull(presentation.icon, "候选必须有图标：没有图标会让弹窗出现空白行")
    }

    /**
     * 空 typeText 不得调用 withTypeText：平台把「没设置」与「显式空串」区别对待。
     */
    @Test
    fun emptyTypeTextStaysUnset() {
        val snapshot = completionSnapshot(
            lookupString = "alpha",
            kind = CompletionItemKind.LOCAL,
            origin = CompletionOrigin.LOCAL_SCOPE,
        ).build()
        val presentation = presentationOf(LookupElementFactory.create(snapshot))
        assertTrue(presentation.typeText.isNullOrEmpty(), "未设置 typeText 时不得凭空出现类型文本")
    }

    /**
     * 快照必须能经用户数据键完整取回：插入与语义复查都依赖它。
     */
    @Test
    fun snapshotRoundTripsThroughUserData() {
        val snapshot = completionSnapshot(
            lookupString = "alpha",
            kind = CompletionItemKind.LOCAL,
            origin = CompletionOrigin.LOCAL_SCOPE,
        ).build()
        val element = LookupElementFactory.create(snapshot)
        assertSame(snapshot, element.getUserData(COMPLETION_SNAPSHOT))
    }

    /**
     * 分析期权重必须经优先包装器进入平台：排序是分析结论的机械映射。
     *
     * 平台如何读取包装器内的优先级属于平台行为；这里验证两件属于本层的事：
     * 折算函数给出与权重一致的优先级，且工厂确实把元素包进优先包装器。
     */
    @Test
    fun priorityMatchesAnalysisWeight() {
        val snapshot = completionSnapshot(
            lookupString = "alpha",
            kind = CompletionItemKind.LOCAL,
            origin = CompletionOrigin.LOCAL_SCOPE,
        ).build()
        assertEquals(snapshot.weight.toDouble(), priorityOf(snapshot), 0.0)
        assertTrue(
            LookupElementFactory.create(snapshot) is PrioritizedLookupElement<*>,
            "工厂必须把元素包进 PrioritizedLookupElement，否则分析期权重不会影响展示次序",
        )
    }

    /**
     * 批量转换保持传入顺序，不在工厂内部重排。
     */
    @Test
    fun createAllKeepsOrder() {
        val first = completionSnapshot("first", CompletionItemKind.LOCAL, CompletionOrigin.LOCAL_SCOPE).build()
        val second = completionSnapshot("second", CompletionItemKind.TOP_LEVEL, CompletionOrigin.TOP_LEVEL).build()
        assertEquals(
            listOf("first", "second"),
            LookupElementFactory.createAll(listOf(first, second)).map { it.lookupString },
        )
    }

    /**
     * 稳定排序：权重降序，同权重保持到达次序。
     */
    @Test
    fun sortsByWeightKeepingArrivalOrder() {
        val low = completionSnapshot("low", CompletionItemKind.TOP_LEVEL, CompletionOrigin.TOP_LEVEL).build()
        val high1 = completionSnapshot("high1", CompletionItemKind.LOCAL, CompletionOrigin.LOCAL_SCOPE).build()
        val high2 = completionSnapshot("high2", CompletionItemKind.LOCAL, CompletionOrigin.LOCAL_SCOPE).build()
        assertEquals(
            listOf("high1", "high2", "low"),
            listOf(low, high1, high2).sortedByAnalysisWeight().map { it.lookupString },
        )
    }

    /**
     * 权重维度次序：来源 > 导入成本 > 期望类型。
     */
    @Test
    fun weightOrdersDimensions() {
        val local = CompletionSortDataImpl(CompletionOrigin.LOCAL_SCOPE).toWeight()
        val topLevel = CompletionSortDataImpl(CompletionOrigin.TOP_LEVEL).toWeight()
        assertTrue(local > topLevel, "局部来源必须排在顶层来源之前，实际 $local vs $topLevel")

        val direct = CompletionSortDataImpl(CompletionOrigin.TOP_LEVEL, importCost = ImportCost.DIRECT).toWeight()
        val addImport = CompletionSortDataImpl(CompletionOrigin.TOP_LEVEL, importCost = ImportCost.ADD_IMPORT).toWeight()
        assertTrue(direct > addImport, "已可达候选必须排在需导入候选之前，实际 $direct vs $addImport")

        val matched = CompletionSortDataImpl(CompletionOrigin.TOP_LEVEL)
            .withExpectedTypeMatch(ExpectedTypeMatch.MATCH).toWeight()
        val unknown = CompletionSortDataImpl(CompletionOrigin.TOP_LEVEL)
            .withExpectedTypeMatch(ExpectedTypeMatch.UNKNOWN).toWeight()
        val mismatched = CompletionSortDataImpl(CompletionOrigin.TOP_LEVEL)
            .withExpectedTypeMatch(ExpectedTypeMatch.MISMATCH).toWeight()
        assertTrue(matched > unknown, "MATCH 必须高于 UNKNOWN，实际 $matched vs $unknown")
        assertTrue(unknown > mismatched, "UNKNOWN 必须高于 MISMATCH，实际 $unknown vs $mismatched")
    }

    /**
     * 替换期望类型结论必须保留来源、导入成本与声明次序。
     */
    @Test
    fun withExpectedTypeMatchPreservesOtherDimensions() {
        val original = CompletionSortDataImpl(
            origin = CompletionOrigin.EXTEND,
            importCost = ImportCost.ADD_IMPORT,
            declarationOrder = 3,
        )
        val rewritten = original.withExpectedTypeMatch(ExpectedTypeMatch.MATCH)
        assertEquals(CompletionOrigin.EXTEND, rewritten.origin)
        assertEquals(ImportCost.ADD_IMPORT, rewritten.importCost)
        assertEquals(3, rewritten.declarationOrder)
        assertEquals(ExpectedTypeMatch.MATCH, rewritten.expectedTypeMatch)
    }

    /**
     * 辅助契约：只有确定匹配才加权；命名实参必须携带后缀文本。
     */
    @Test
    fun helperContracts() {
        assertTrue(isPromotedByExpectedType(ExpectedTypeMatch.MATCH))
        assertFalse(isPromotedByExpectedType(ExpectedTypeMatch.UNKNOWN), "UNKNOWN 不得当成匹配")
        assertFalse(isPromotedByExpectedType(ExpectedTypeMatch.MISMATCH))

        assertTrue(
            originRank(CompletionOrigin.LOCAL_SCOPE) < originRank(CompletionOrigin.TOP_LEVEL),
            "来源次序是契约：局部必须先于顶层",
        )

        assertTrue(namedArgumentSnapshot("value").build().insertion.requiresSuffixText(), "命名实参必须追加 `name: `")
        assertFalse(
            completionSnapshot("alpha", CompletionItemKind.LOCAL, CompletionOrigin.LOCAL_SCOPE)
                .build().insertion.requiresSuffixText(),
            "普通候选不得携带命名实参前缀",
        )
    }

    /**
     * 按平台渲染约定取得展示内容。
     *
     * 尾文本、类型文本与图标由 expensive renderer 渲染（普通 renderElement 只写主文本），
     * 弹窗展示的就是这份呈现内容；装饰器沿委托链向下找第一个非空渲染器。
     */
    private fun presentationOf(element: LookupElement): LookupElementPresentation {
        val presentation = LookupElementPresentation()
        val renderer = expensiveRendererOf(element)
        if (renderer != null) {
            renderer.renderElement(element, presentation)
        } else {
            element.renderElement(presentation)
        }
        return presentation
    }

    /**
     * 取得元素的 expensive renderer；装饰器没有自己的渲染器时沿委托链向下找。
     */
    private fun expensiveRendererOf(element: LookupElement): LookupElementRenderer<LookupElement>? {
        val decorator = element as? LookupElementDecorator<*> ?: return null
        @Suppress("UNCHECKED_CAST")
        val own = decorator.expensiveRenderer as? LookupElementRenderer<LookupElement>
        return own ?: decorator.delegate.let(::expensiveRendererOf)
    }
}
