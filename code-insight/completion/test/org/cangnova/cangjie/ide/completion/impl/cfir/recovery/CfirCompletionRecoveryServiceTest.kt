package org.cangnova.cangjie.ide.completion.impl.cfir.recovery

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.api.standalone.projectStructure.AnalysisApiServiceRegistrar
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.ide.completion.api.CompletionSnapshot
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionIdentityData
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionIdentityKind
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionImportPlan
import org.cangnova.cangjie.ide.completion.impl.cfir.sections.CfirIndexCompletionSection
import org.cangnova.cangjie.ide.completion.impl.cfir.sections.CfirTopLevelCompletionSection
import org.cangnova.cangjie.ide.completion.impl.cfir.sections.IndexSectionTestServiceRegistrar
import org.cangnova.cangjie.ide.completion.impl.cfir.sections.collectCandidates
import org.cangnova.cangjie.ide.completion.impl.cfir.sections.requestAtReference
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 候选语义复查回归。
 *
 * 复查的存在理由是**补全弹窗的生命周期长于分析会话**，因此这里固定三条：
 * 1. 分析结束后仍能凭身份恢复回同一个重载（同名不同参数不得互相认领）；
 * 2. 身份失效时返回 `null`，而不是抛异常或静默给出过期目标；
 * 3. 导入需求在复查时按文件**当前文本**重算，不复用分析期结论。
 *
 * 夹具受分析测试框架限制为「每个用例一个 .cj」，因此**跨包未导入**的恢复路径
 * 无法在本套件内构造；它由 `getPackageScope` 回退承担，此处只固定单文件可达的部分。
 */
@OptIn(CaExperimentalApi::class)
class CfirCompletionRecoveryServiceTest : AbstractAnalysisApiExecutionTest(
    "code-insight/completion/impl-cfir/testData/recovery",
) {
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    /**
     * 注册名称索引服务：索引区段用它产出「导入作用域里看不到」的候选。
     */
    override val additionalServiceRegistrars: List<AnalysisApiServiceRegistrar<TestServices>> =
        listOf(IndexSectionTestServiceRegistrar)

    /**
     * 分析结束后凭身份恢复回同一个重载。
     *
     * 夹具里 `overload` 有两个重载；只按短名匹配会恢复到任意一个，
     * 因此断言恢复结果与快照的限定名、身份键完全一致。
     */
    @Test
    fun recoversTheSameOverloadAfterAnalysis(mainFile: CjFile) {
        val snapshot = overloads(mainFile, OVERLOAD).first()
        val recovered = requireNotNull(
            recoveryService.recover(mainFile, snapshot.identity),
        ) { "分析结束后必须能凭身份恢复候选" }
        assertEquals(
            snapshot.identity.deduplicationKey,
            recovered.identity.deduplicationKey,
            "恢复到的必须是用户实际选中的那个重载",
        )
        assertEquals(snapshot.identity.qualifiedName, recovered.qualifiedName, "恢复后的限定名不得漂移")
    }

    /**
     * 另一个重载恢复回它自己，不被同名者顶替。
     */
    @Test
    fun recoversTheOtherOverloadDistinctly(mainFile: CjFile) {
        val all = overloads(mainFile, OVERLOAD)
        assertTrue(all.size >= 2, "夹具必须同时存在两个不同参数的重载，实际 ${all.map { it.tailText }}")
        val last = all.last()
        val recovered = requireNotNull(
            recoveryService.recover(mainFile, last.identity),
        ) { "分析结束后必须能凭身份恢复候选" }
        assertEquals(
            last.identity.deduplicationKey,
            recovered.identity.deduplicationKey,
            "两个重载必须恢复回各自的身份",
        )
        assertTrue(
            all.map { it.identity.deduplicationKey }.toSet().size == all.size,
            "夹具的两个重载必须具有不同身份键，否则本用例无法区分",
        )
    }

    /**
     * 未被任何声明使用的身份判为失效：返回 `null` 而不是给出过期目标。
     */
    @Test
    fun reportsDanglingIdentity(mainFile: CjFile) {
        val dangling = CompletionIdentityData.of(
            kind = CompletionIdentityKind.SYMBOL,
            handle = "no.such.package.gone(): Int64",
            name = "gone",
            qualifiedName = "no.such.package.gone",
        )
        assertNull(recoveryService.recover(mainFile, dangling), "不存在的身份必须报告失效")
    }

    /**
     * 关键字没有可失效的声明身份：必须始终可恢复。
     *
     * 若关键字被判为失效，弹窗里每一条关键字候选都会被静默丢弃——
     * 这比不给关键字更糟，因为用户会以为「这个位置没有关键字可选」。
     */
    @Test
    fun keywordIdentityNeverDangles(mainFile: CjFile) {
        val keyword = CompletionIdentityData.of(
            kind = CompletionIdentityKind.KEYWORD,
            handle = "func",
            name = "func",
        )
        val recovered = requireNotNull(recoveryService.recover(mainFile, keyword)) { "关键字不得被判为失效" }
        assertTrue(recovered.visible, "关键字必须始终可见")
    }

    /**
     * 本包内可直接引用的声明，复查后不得要求追加 import。
     *
     * 复用分析期结论会在目标已导入时写出重复 import 行；这条断言固定「复查时重算」。
     */
    @Test
    fun reachableTargetNeedsNoImport(mainFile: CjFile) {
        val snapshot = overloads(mainFile, REACHABLE).first()
        val recovered = requireNotNull(
            recoveryService.recover(mainFile, snapshot.identity),
        ) { "同包声明必须可恢复" }
        assertEquals(
            CompletionImportPlan.NoImport,
            recovered.importPlan,
            "同包已可直接引用，不得要求追加 import，实际 ${recovered.importPlan}",
        )
    }

    /**
     * 声明级导入已存在时，复查不得再要求写 import。
     *
     * 仓颉里同一个目标有三种等价的导入写法（包级 / 声明级 / 星号），
     * 只比对整行文本必然漏判，而漏判的后果是往已经能用的文件里重复写一行 import。
     */
    @Test
    fun recoveryAcceptsDeclarationLevelImport(mainFile: CjFile) {
        assertAlreadyReachable(mainFile)
    }

    /**
     * 星号导入已存在时，复查同样不得再要求写 import。
     */
    @Test
    fun recoveryAcceptsStarImport(mainFile: CjFile) {
        assertAlreadyReachable(mainFile)
    }

    /**
     * 未导入的候选由索引区段产出（作用域路径看不到它），再交给复查。
     */
    private fun assertAlreadyReachable(mainFile: CjFile) {
        val snapshot = CfirIndexCompletionSection()
            .collectCandidates(mainFile.requestAtReference(IMPORTED_TARGET))
            .single { it.lookupString == IMPORTED_TARGET }

        val recovered = requireNotNull(recoveryService.recover(mainFile, snapshot.identity)) {
            "已导入的声明必须能被复查定位"
        }
        assertEquals(
            CompletionImportPlan.NoImport,
            recovered.importPlan,
            "文件里已有等价 import 时复查不得再要求写一行",
        )
    }

    /**
     * 取得夹具里 `overload` 的全部重载快照。
     *
     * 快照经区段产出：签名必须在分析内渲染，这同时验证了
     * 「区段产出的快照在分析结束后仍然完整可用」——这正是 10.2 的前提。
     */
    private fun overloads(mainFile: CjFile, name: String): List<CompletionSnapshot> =
        CfirTopLevelCompletionSection().collectCandidates(mainFile.requestAtReference(TARGET))
            .filter { it.lookupString == name }

    /**
     * 使用的复查服务。
     */
    private val recoveryService = CfirCompletionRecoveryService()

    companion object {
        /** 夹具中待补全的标识符。 */
        private const val TARGET = "completionTarget"

        /** 已导入夹具中的目标声明名。 */
        private const val IMPORTED_TARGET = "recoverTarget"

        /** 夹具中的重载函数名。 */
        private const val OVERLOAD = "overload"

        /** 夹具中可直接引用的函数名。 */
        private const val REACHABLE = "reachable"
    }
}