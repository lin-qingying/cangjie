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
import org.cangnova.cangjie.analysis.api.symbols.CaClassSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassKind
import org.cangnova.cangjie.psi.CjSimpleNameExpression
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

    /**
     * IDE（STUBS）链路：库 extend 提供的 `Array.size` 与库签名 `Array<Byte>` 必须都能正确解析。
     *
     * 这两个症状在真实 IDE 里表现为 `'size' is not a member of 'Array'` 与
     * `RETURN_TYPE_MISMATCH: expected Array<UInt8>, actual Array<Any>`。
     */
    @Test
    fun stubsArrayMembers(mainFile: CjFile) {
        val diagnostics = analyzeForTest(mainFile) {
            mainFile.collectDiagnostics(CaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS)
        }

        assertTrue(
            diagnostics.isEmpty(),
            "STUBS 模式下 Array 成员与库签名不应产生诊断：" +
                diagnostics.joinToString { diagnostic -> "${diagnostic.factoryName}@${diagnostic.psi.textRange}" },
        )
    }

    /**
     * decompiled 库签名里的 typealias + `Array` 的 extend 成员（size/isEmpty）必须都正确解析。
     *
     * 覆盖 `LibraryBinaryDecompiled` 库：库 `.cjo` 由测试框架用本机 cjc 编译，并走仓库自己的
     * 反编译 stub 链。对应症状：`RETURN_TYPE_MISMATCH(Array<UInt8> vs Array<MyByte>)` 与
     * `NOT_MEMBER_OF('size')`。
     */
    @Test
    fun decompiledLibraryAliasExtendMembers(mainFile: CjFile) {
        val diagnostics = analyzeForTest(mainFile) {
            mainFile.collectDiagnostics(CaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS)
                .map { diagnostic -> "${diagnostic.factoryName}@${diagnostic.psi.textRange}" }
        }
        assertTrue(
            diagnostics.isEmpty(),
            "decompiled 库签名中的别名与 extend 成员不应产生诊断：\n" +
                diagnostics.joinToString("\n") { "  - $it" },
        )
    }

    /**
     * 实现弃用成员时，`DEPRECATION_OVERRIDE_WARNING` 只允许出现一次。
     *
     * `EXTENDED_AND_COMMON_CHECKERS` 是 common 与 extended 两个 filter 的并集；同一个 checker
     * 若被两个集合同时收录，就会在逐 filter 派发时重复报告同一条诊断（mariadb 真实项目里
     * `DEPRECATION_OVERRIDE_WARNING` 出现 5 对重复）。集合契约是互不重叠。
     */
    /**
     * 传递依赖：main 只声明依赖 libUser，别名包 libAlias 不在直接依赖里。
     *
     * 若别名无法在 use-site 解析（Kotlin KDoc 里的 `M1` 不依赖 `L2` 场景），这里会暴露
     * 「别名永远展不开」的问题；通过则说明依赖闭包仍能提供别名声明。
     */
    @Test
    fun decompiledCrossPackageAliasTransitive(mainFile: CjFile) {
        val diagnostics = analyzeForTest(mainFile) {
            mainFile.collectDiagnostics(CaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS)
                .map { diagnostic -> "${diagnostic.factoryName}@${diagnostic.psi.textRange}" }
        }
        assertTrue(
            diagnostics.isEmpty(),
            "仅传递依赖时别名也应可展开：\n" + diagnostics.joinToString("\n") { "  - $it" },
        )
    }

    /**
     * 跨包别名：库 A 的签名引用库 B 的 `type` 别名（真实 IDE 里是 std.io 引用 std.core.Byte）。
     *
     * STUBS 链路若只在本文件里找别名，这条必然报 RETURN_TYPE_MISMATCH / NOT_MEMBER_OF。
     */
    @Test
    fun decompiledCrossPackageAlias(mainFile: CjFile) {
        val diagnostics = analyzeForTest(mainFile) {
            mainFile.collectDiagnostics(CaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS)
                .map { diagnostic -> "${diagnostic.factoryName}@${diagnostic.psi.textRange}" }
        }
        assertTrue(
            diagnostics.isEmpty(),
            "跨包别名与 extend 成员不应产生诊断：\n" + diagnostics.joinToString("\n") { "  - $it" },
        )
    }

    /**
     * 负例：别名指向 enum 时，非穷尽 match 必须报 `NON_EXHAUSTIVE_MATCH`。
     *
     * 只断言“穷尽 match 无诊断”是 vacuous 的——检查器在别名未展开时会整体 Skipped。
     */
    @Test
    fun decompiledLibraryAliasNonexhaustiveMatch(mainFile: CjFile) {
        val diagnostics = analyzeForTest(mainFile) {
            mainFile.collectDiagnostics(CaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS)
                .map { diagnostic -> diagnostic.factoryName }
        }
        assertTrue(
            diagnostics.contains("NON_EXHAUSTIVE_MATCH"),
            "别名指向 enum 时非穷尽 match 必须报 NON_EXHAUSTIVE_MATCH，实际：$diagnostics",
        )
    }

    /**
     * 别名在使用点的三种形态：alias→enum 穷尽 match、alias→函数类型调用、外层 alias→Array 的 extend 成员。
     *
     * 这三条只要别名的展开语义有任何缺失就会直接报诊断（未知构造器 / 不可调用 / 找不到成员）。
     */
    @Test
    fun decompiledLibraryAliasUseSites(mainFile: CjFile) {
        val diagnostics = analyzeForTest(mainFile) {
            mainFile.collectDiagnostics(CaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS)
                .map { diagnostic -> "${diagnostic.factoryName}@[${diagnostic.psi.text}]${diagnostic.psi.textRange}" }
        }
        assertTrue(
            diagnostics.isEmpty(),
            "别名使用点不应产生诊断：\n" + diagnostics.joinToString("\n") { "  - $it" },
        )
    }

    /**
     * decompiled 库签名里的声明种类矩阵：interface / class / struct / enum / typealias
     * 以及函数类型、Option、tuple、VArray 等类型形态都必须被 stub 链路正确重建。
     */
    @Test
    fun decompiledLibraryTypeMatrix(mainFile: CjFile) {
        val (diagnostics, kinds) = analyzeForTest(mainFile) {
            val diagnostics = mainFile.collectDiagnostics(CaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS)
                .map { diagnostic -> "${diagnostic.factoryName}@${diagnostic.psi.textRange}" }
            val kinds = PsiTreeUtil.findChildrenOfType(mainFile, CjSimpleNameExpression::class.java)
                .filter { expression -> expression.text in setOf("Shape", "ShapeAlias", "Point", "Color") }
                .mapNotNull { expression ->
                    val classSymbol = expression.resolveToSymbols()
                        .filterIsInstance<CaClassSymbol>()
                        .firstOrNull()
                        ?: return@mapNotNull null
                    expression.text to classSymbol.classKind
                }
                .toMap()
            diagnostics to kinds
        }

        assertTrue(
            diagnostics.isEmpty(),
            "decompiled 库的声明种类矩阵不应产生诊断：\n" + diagnostics.joinToString("\n") { "  - $it" },
        )
        // 诊断为空时种类仍可能退化（class-like 的成员查找常常照样成功），因此直接断言符号种类。
        assertEquals(CaClassKind.INTERFACE, kinds["Shape"], "decompiled 库的 interface 必须保持 INTERFACE：$kinds")
        assertEquals(CaClassKind.STRUCT, kinds["Point"], "decompiled 库的 struct 必须保持 STRUCT：$kinds")
        assertEquals(CaClassKind.ENUM, kinds["Color"], "decompiled 库的 enum 必须保持 ENUM：$kinds")
    }

    @Test
    fun deprecatedOverrideReportedOnce(mainFile: CjFile) {
        val diagnostics = analyzeForTest(mainFile) {
            mainFile.collectDiagnostics(CaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS)
        }
        val overrideWarnings = diagnostics.filter {
            it.factoryName == "DEPRECATION_OVERRIDE_WARNING"
        }

        assertEquals(
            1,
            overrideWarnings.size,
            "弃用成员实现只应报告一次 DEPRECATION_OVERRIDE_WARNING：" +
                diagnostics.joinToString { diagnostic -> "${diagnostic.factoryName}@${diagnostic.psi.textRange}" },
        )
    }
}





