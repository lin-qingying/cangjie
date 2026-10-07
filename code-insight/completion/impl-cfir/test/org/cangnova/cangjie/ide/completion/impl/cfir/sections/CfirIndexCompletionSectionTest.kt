package org.cangnova.cangjie.ide.completion.impl.cfir.sections

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.analysis.api.standalone.projectStructure.AnalysisApiServiceRegistrar
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.ide.completion.api.CompletionItemKind
import org.cangnova.cangjie.psi.CjFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 索引召回区段回归。
 *
 * 覆盖两件事：作用域路径看不到的**未导入**依赖声明必须被召回，且带上追加 import 的计划；
 * 与当前模块没有依赖关系的模块不得被召回。
 */
@OptIn(CaExperimentalApi::class)
class CfirIndexCompletionSectionTest : AbstractAnalysisApiExecutionTest(
    "code-insight/completion/impl-cfir/testData/sections",
) {
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    /**
     * 注册名称发现服务：测试宿主不加载 standalone 描述符，需要按生产实现补注册。
     */
    override val additionalServiceRegistrars: List<AnalysisApiServiceRegistrar<TestServices>> =
        listOf(IndexSectionTestServiceRegistrar)

    /**
     * 未导入的依赖声明由索引召回，并要求新增 import。
     *
     * 这是索引段存在的理由：导入作用域里根本没有这个名字，
     * 没有索引段时用户必须自己写出包路径才能补全。
     */
    @Test
    fun indexSectionOffersUnimportedCandidate(mainFile: CjFile) {
        val snapshots = section.collectCandidates(mainFile.requestAtReference(TARGET))
        val candidate = snapshots.single { it.lookupString == "indexedLoad" }

        assertEquals(CompletionItemKind.TOP_LEVEL, candidate.kind, "索引召回的是顶层声明")
        assertTrue(
            candidate.importPlan.requiresImportWrite,
            "未导入候选必须带追加 import 的计划，实际 ${candidate.importPlan}",
        )
    }

    /**
     * 没有依赖关系的模块不得被召回。
     *
     * 范围限定不能只看 `analysisScope`：无关模块的文件也在同一个工程里，
     * 漏掉依赖边界会让用户收到一堆当前编译根本不认识的候选。
     */
    @Test
    fun indexSectionExcludesUnrelatedModule(mainFile: CjFile) {
        val snapshots = section.collectCandidates(mainFile.requestAtReference(UNRELATED_TARGET))

        assertTrue(
            snapshots.none { it.lookupString == UNRELATED_TARGET },
            "无依赖关系的模块不得召回，实际 ${snapshots.map { it.lookupString }}",
        )
    }

    /**
     * 索引按名称召回一次，恢复必须展开全部重载。
     *
     * 索引只记名字，同名重载在名称层就是一条；若把恢复结果也按名称去重，
     * 用户就永远选不到其中一个重载。这是 A8「不得因显示名称相同而合并」的直接锁定。
     */
    @Test
    fun indexSectionKeepsOverloadsFromIndex(mainFile: CjFile) {
        val snapshots = section.collectCandidates(mainFile.requestAtReference(OVERLOAD_TARGET))
            .filter { it.lookupString == OVERLOAD_TARGET }

        assertEquals(2, snapshots.size, "两个重载都必须作为候选出现，实际 ${snapshots.map { it.tailText }}")
        assertEquals(
            2,
            snapshots.map { it.identity.deduplicationKey }.distinct().size,
            "重载的去重键必须互不相同，实际 ${snapshots.map { it.identity.deduplicationKey }}",
        )
        assertTrue(
            snapshots.any { it.tailText.startsWith("(): ") },
            "无参重载必须可见，实际 ${snapshots.map { it.tailText }}",
        )
        assertTrue(
            snapshots.any { it.tailText.contains("value: Int64") },
            "带参重载必须可见且签名体现形参，实际 ${snapshots.map { it.tailText }}",
        )
    }

    /**
     * 被更内层作用域遮蔽的索引候选不得存活。
     *
     * 本文件自己声明了同名函数时，用户写出的那个名字解析到本文件的声明；
     * 此时再把跨包同名声明列出来，选中就会插入一个指向别处的名字。
     */
    @Test
    fun indexSectionSuppressesShadowedCandidate(mainFile: CjFile) {
        val snapshots = section.collectCandidates(mainFile.requestAtReference(SHADOWED_TARGET))

        assertTrue(
            snapshots.none { it.lookupString == SHADOWED_TARGET },
            "被本文件声明遮蔽的索引候选不得存活，实际 ${snapshots.map { it.lookupString }}",
        )
    }

    /**
     * 使用的区段实例。
     */
    private val section = CfirIndexCompletionSection()

    companion object {
        /**
         * 夹具中待补全的标识符。
         */
        private const val TARGET = "indexedLoad"

        /**
         * 同名重载夹具中的标识符。
         */
        private const val OVERLOAD_TARGET = "indexedOverload"

        /**
         * 无依赖模块夹具中的标识符。
         */
        private const val UNRELATED_TARGET = "unrelatedCandidate"

        /**
         * 遮蔽夹具中的标识符。
         */
        private const val SHADOWED_TARGET = "shadowedByFile"
    }
}
