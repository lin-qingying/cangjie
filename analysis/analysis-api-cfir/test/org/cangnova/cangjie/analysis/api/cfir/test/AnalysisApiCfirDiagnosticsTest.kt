package org.cangnova.cangjie.analysis.api.cfir.test

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.components.CaDiagnosticCheckerFilter
import org.cangnova.cangjie.analysis.api.diagnostics.CaDiagnosticWithPsi
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNamedFunction
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * CFIR Analysis API diagnostics facade 的回归测试。
 *
 * 这里覆盖文件级与声明级 diagnostics 查询，确保 checker filter、extend 声明和接口类型参数
 * 在 Analysis API 懒解析链路中保持一致的诊断边界。
 */
class AnalysisApiCfirDiagnosticsTest : AbstractAnalysisApiExecutionTest(
    "analysis/analysis-api-cfir/testData/diagnostics",
) {
    /**
     * 使用 standalone CFIR 配置执行 diagnostics facade 测试。
     */
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    /**
     * 验证 common、extended、experimental 等 checker filter 对同一文件的诊断集合划分。
     *
     * 契约：三个集合互不重叠，`EXTENDED_AND_COMMON_CHECKERS` 恰好是 common 与 extended 的并集。
     * fixture 里的未使用局部变量按官方语义（`chir_dce_unused_variable` warning，可用 `-Woff unused` 关闭，
     * 属 CHIR DCE 阶段）落在扩展检查器集合，因此只能出现在 extended 一侧。
     */
    @Test
    fun collectDiagnostics(mainFile: CjFile) {
        val commonDiagnostics = analyzeForTest(mainFile) {
            mainFile.collectDiagnostics(CaDiagnosticCheckerFilter.ONLY_COMMON_CHECKERS)
        }
        val allDiagnostics = analyzeForTest(mainFile) {
            mainFile.collectDiagnostics(CaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS)
        }
        val extraDiagnostics = analyzeForTest(mainFile) {
            mainFile.collectDiagnostics(CaDiagnosticCheckerFilter.ONLY_EXTENDED_CHECKERS)
        }
        val experimentalDiagnostics = analyzeForTest(mainFile) {
            mainFile.collectDiagnostics(CaDiagnosticCheckerFilter.ONLY_EXPERIMENTAL_CHECKERS)
        }

        val commonNames = commonDiagnostics.map { it.factoryName }
        val extraNames = extraDiagnostics.map { it.factoryName }
        val allNames = allDiagnostics.map { it.factoryName }
        val unusedVariable = "UNUSED_VARIABLE"

        assertTrue(commonNames.contains("UNRESOLVED_IMPORT"))
        assertTrue(unusedVariable !in commonNames, "未使用局部变量属于扩展检查器，不应出现在 common 集合：$commonNames")
        assertTrue(unusedVariable in extraNames, "fixture 的未使用局部变量应落在扩展检查器集合：$extraNames")
        assertEquals(
            (commonNames + extraNames).sorted(),
            allNames.sorted(),
            "EXTENDED_AND_COMMON_CHECKERS 应恰好是 common 与 extended 的并集。",
        )
        assertTrue(experimentalDiagnostics.isEmpty())
    }

    /**
     * 验证合法 extend 文件在扩展与公共 checker 合并模式下不会产生额外诊断。
     */
    @Test
    fun extendDiagnostics(mainFile: CjFile) {
        val allDiagnostics = analyzeForTest(mainFile) {
            mainFile.collectDiagnostics(CaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS)
        }

        assertTrue(
            allDiagnostics.isEmpty(),
            "合法 extend 文件收集 diagnostics 不应抛异常，也不应产生额外诊断: " +
                allDiagnostics.joinToString { diagnostic -> "${diagnostic.factoryName}@${diagnostic.psi.textRange}" },
        )
    }

    /**
     * 验证命名函数声明级 diagnostics 查询不会破坏后续文件级 diagnostics 收集。
     */
    @Test
    fun namedFunctionDiagnostics(mainFile: CjFile) {
        val function = PsiTreeUtil.findChildrenOfType(mainFile, CjNamedFunction::class.java).single()
        analyzeForTest(mainFile) {
            function.diagnostics(CaDiagnosticCheckerFilter.ONLY_COMMON_CHECKERS)
            mainFile.collectDiagnostics(CaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS)
        }
    }

    /**
     * 验证同名 classifier 在**类型使用位置**的歧义诊断能穿过 analysis API 收集链路。
     *
     * 回归背景：该诊断原先为了把高亮收窄到末段名称，把锚点降级为仅含 offsets 的源元素。
     * analysis API 的 low-level reporter 只能消费带 PSI 锚点的诊断，命中兜底分支时会抛出
     * `error("Unknown diagnostic type CjOffsetsOnlyDiagnosticWithParameters1")`；该异常被
     * `suppressAndLogExceptions` 吞掉后会连带中止该 CFIR 元素上剩余 checker，使这条诊断
     * （及其后的诊断）整体丢失——表现为一类无法复现的"IDE 少报诊断"。
     *
     * 同时校验官方语义：官方 `sema_ambiguous_use` 由 `DiagAmbiguousUse` 以**整节点**锚定
     * （`Sema/Diags.cpp` 的重载不接收 Range，`DiagnosticEngine` 恒取 `node.begin..node.end`），
     * 因此高亮必须覆盖整条类型引用（含类型实参），不能只标末段名称。
     */
    @Test
    fun ambiguousClassifierTypeUse(mainFile: CjFile) {
        val diagnostics = analyzeForTest(mainFile) {
            mainFile.collectDiagnostics(CaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS)
        }

        val ambiguousUses = diagnostics.filter { it.factoryName == "AMBIGUOUS_USE" }
        assertTrue(
            ambiguousUses.isNotEmpty(),
            "同名 classifier 的类型使用位置歧义必须能在 analysis API 链路中被收集；" +
                "若为空，说明 low-level reporter 因诊断类型不受支持而中断了收集：" +
                diagnostics.joinToString { diagnostic -> "${diagnostic.factoryName}@${diagnostic.psi.textRange}" },
        )

        // 每条诊断都必须有可导航的 PSI 锚点：IDE 需要它绘制波浪线并提供快速修复入口。
        for (diagnostic in ambiguousUses) {
            assertNotNull(
                diagnostic.psi,
                "AMBIGUOUS_USE 必须锚定真实 PSI，而不是仅含 offsets 的源元素。",
            )
        }

        // 高亮必须覆盖整条类型引用（含类型实参），而非仅末段名称。
        val wholeTypeUses = ambiguousUses.filter { diagnostic ->
            diagnostic.textRanges.any { range ->
                mainFile.text.substring(range.startOffset, range.endOffset) in setOf("B<Y, X>", "C<Y, X>")
            }
        }
        assertTrue(
            wholeTypeUses.isNotEmpty(),
            "官方 sema_ambiguous_use 以整节点锚定，类型使用位置的高亮应覆盖整条类型引用" +
                "（含类型实参），实际范围为：" +
                ambiguousUses.joinToString { diagnostic ->
                    "${diagnostic.textRanges.map { range -> mainFile.text.substring(range.startOffset, range.endOffset) }}"
                },
        )
    }

    /**
     * 验证接口成员签名可以解析外层类型参数，不会在懒解析 diagnostics 中误报未解析引用。
     */
    @Test
    fun interfaceTypeParameterDiagnostics(mainFile: CjFile) {
        val diagnostics = analyzeForTest(mainFile) {
            mainFile.collectDiagnostics(CaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS)
        }

        assertFalse(
            diagnostics.any { diagnostic ->
                diagnostic.factoryName == "UNRESOLVED_REFERENCE" && diagnostic.textRanges.any { range ->
                    mainFile.text.substring(range.startOffset, range.endOffset) == "T"
                }
            },
            "interface 成员签名中的外层类型参数 `T` 不应在 Analysis API 懒解析链路上退化成 UNRESOLVED_REFERENCE: " +
                diagnostics.joinToString { diagnostic -> "${diagnostic.factoryName}@${diagnostic.psi.text}" },
        )
    }

    /**
     * 基本类型与 `CPointer` 的 extend 成员调用不得产生假的未解析诊断。
     *
     * 定位说明（实测结论，勿按直觉假设）：`collectDiagnostics` 走的是**编译器 checker 链**，
     * 不是 IDE 高亮那条链路。本用例在基线 `eb2c704ec` 上同样通过，说明现场的“整片变红”
     * 并非由这条链产生——红色的真正来源是 Analysis 侧成员作用域为空（`typeScope` 取不到
     * 基本类型接收者的成员）以及库 extend 身份还原抛错。因此本用例的价值是**必要条件**：
     * 修复不得让编译器诊断多出假的未解析引用。红色本身的护栏见
     * `StandaloneDecompiledExtendMemberTest`（库 extend 符号查询，基线直接抛错）与
     * `ExtendReceiverShapesExecutionTest`（接收者形状矩阵）。
     *
     * 用例自带控制组：对 `Int64` 调用不存在的方法 `bogus` 必须报出来。没有这个控制组，
     * “没有未解析诊断”的断言可能只是因为诊断收集链路自己空转（历史上 reporter 抛异常
     * 被 `suppressAndLogExceptions` 吞掉，就会连带中止该元素上剩余 checker）而假通过。
     */
    @Test
    fun primitiveExtendMemberDiagnostics(mainFile: CjFile) {
        val diagnostics = analyzeForTest(mainFile) {
            mainFile.collectDiagnostics(CaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS)
        }

        fun textOf(diagnostic: CaDiagnosticWithPsi<*>): List<String> =
            diagnostic.textRanges.map { range -> mainFile.text.substring(range.startOffset, range.endOffset) }

        fun at(name: String) = diagnostics.filter { diagnostic -> name in textOf(diagnostic) }

        val rendered = diagnostics.joinToString { diagnostic -> "${diagnostic.factoryName}@${textOf(diagnostic)}" }

        // 控制组：链路必须真的在报诊断。
        assertTrue(
            at("bogus").isNotEmpty(),
            "控制组缺失：对 Int64 调用不存在的方法必须产生诊断，否则说明收集链路空转，主断言没有证明力。实际：$rendered",
        )

        // 主断言：extend 成员调用不得退化成未解析引用（红色波浪线的直接来源）。
        for (extendMember in listOf("doubled", "pointeeTag")) {
            assertTrue(
                at(extendMember).none { diagnostic -> diagnostic.factoryName == "UNRESOLVED_REFERENCE" },
                "extend 成员 `$extendMember` 不得在 Analysis API 诊断链路上退化成 UNRESOLVED_REFERENCE。实际：$rendered",
            )
        }

        // 基本类型 / 内建类型本身也不能变红：除控制组的 `bogus` 外不应有任何未解析引用。
        val strayUnresolved = diagnostics.filter { diagnostic ->
            diagnostic.factoryName == "UNRESOLVED_REFERENCE" && "bogus" !in textOf(diagnostic)
        }
        assertTrue(
            strayUnresolved.isEmpty(),
            "除控制组 `bogus` 外不得出现未解析引用；基本类型与 CPointer 自身必须保持已解析。实际：" +
                strayUnresolved.joinToString { diagnostic -> "${diagnostic.factoryName}@${textOf(diagnostic)}" },
        )
    }

    /** Analysis API exposes the peer range and witness attached to an undecidable extend-order diagnostic. */
    @Test
    fun extendCheckSequencePeerNote(mainFile: CjFile) {
        val diagnostics = analyzeForTest(mainFile) {
            mainFile.collectDiagnostics(CaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS)
        }
        val sequenceDiagnostics = diagnostics.filter {
            it.factoryName == "EXTEND_CHECK_SEQUENCE_CANNOT_DECIDE"
        }

        assertEquals(2, sequenceDiagnostics.size)
        for (diagnostic in sequenceDiagnostics) {
            val peerNote = diagnostic.relatedInformation.single()
            val peerPsi = requireNotNull(peerNote.psi)
            assertNotNull(peerNote.textRange)
            assertEquals(mainFile, peerPsi.containingFile)
            assertNotEquals(diagnostic.psi, peerPsi, "the note navigates to the peer declaration range")
            assertTrue(peerNote.message.contains("I1"))
            assertTrue(peerNote.message.contains("I3"))
        }
        assertTrue(
            sequenceDiagnostics.any {
                val note = it.relatedInformation.single().message
                "I2" in note && "I1" in note && "I4" in note && "I3" in note
            },
            "the peer note preserves both opposing subtype relations",
        )
    }
}
