@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.analysis.stubs

import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.PsiManager
import com.intellij.psi.stubs.PsiFileStub
import com.intellij.psi.impl.source.tree.CompositeElement
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubElementRegistryService
import com.intellij.lang.ASTNode
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.api.standalone.projectStructure.AnalysisApiServiceRegistrar
import org.cangnova.cangjie.analysis.api.util.requireIsInstance
import org.cangnova.cangjie.analysis.decompiled.psi.BuiltinsVirtualFileProvider
import org.cangnova.cangjie.analysis.decompiled.psi.file.CjDecompiledFile
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.cangnova.cangjie.analysis.test.framework.projectStructure.cjTestModuleStructure
import org.cangnova.cangjie.annotations.BuiltInAnnotationRegistry
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.CjPsiFactory
import org.cangnova.cangjie.psi.stubs.CangJieFileStubKind
import org.cangnova.cangjie.psi.stubs.impl.CangJieFileStubImpl
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText

/**
 * 诊断工具：枚举所有 builtins `.cjo` 反编译文件中 stub 树与重新解析出的 AST 之间
 * 的**全部**结构分歧（FileTrees.reconcilePsi 只报总数，且在第一处就让 AST 加载失败）。
 *
 * 原理：
 * 1. `CjoCompiledStubsTestEngine.compute` 从 `.cjo` 二进制直接构建 compiled stub 树
 *    （与 `CompiledStubBuilder` 同源，即 reconcile 断言里的 20383 一侧）；
 * 2. 用 `CjPsiFactory.forPackage` 以与 `CjDecompiledFile` 完全一致的解析上下文
 *    （sourceModuleName + SOURCE kind）把反编译文本重新解析成纯 AST
 *    （即 reconcile 断言里 "stubbed node length" 一侧）；
 * 3. 双序列（stub 先序 / AST stubbed 节点先序）带重同步窗口的对齐扫描，
 *    报告每一处 STUB_EXTRA / AST_EXTRA / TYPE_CHANGE 及其祖先链与文本位置。
 *
 * 运行方式：
 * - 全量：`:analysis:stubs:test --tests "*CjoStubAstConsistencyDiagnosticTest*"`
 * - 单文件：加 `-Dcangjie.builtins.test.file=std.core.cjo`
 *
 * 完整报告写入 `analysis/stubs/build/reports/stub-ast-consistency-report.txt`。
 */
class CjoStubAstConsistencyDiagnosticTest : AbstractAnalysisApiBasedTest() {
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    override val additionalServiceRegistrars: List<AnalysisApiServiceRegistrar<TestServices>> = listOf(
        CjoCompiledStubsTestServiceRegistrar,
    )

    @Test
    fun diagnoseAllStubAstMismatches() {
        CjoCompiledTestEnvironment.withFullStdlibFixture {
            val testDataFile = CjoCompiledTestEnvironment.locateRepositoryRoot()
                .resolve("analysis")
                .resolve("stubs")
                .resolve("testData")
                .resolve("builtins")
                .resolve("test.cj")
            runTest(testDataFile.toString()) { testServices ->
                val project = testServices.cjTestModuleStructure.project
                CjoCompiledTestEnvironment.installBuiltinsProjectStructure(project)
                val psiManager = PsiManager.getInstance(project)
                val targetFileName = System.getProperty("cangjie.builtins.test.file")

                val builtinFiles = BuiltinsVirtualFileProvider.getInstance()
                    .getBuiltinVirtualFiles()
                    .sortedBy { it.path }
                    .filter { virtualFile -> targetFileName == null || virtualFile.name == targetFileName }
                    .mapNotNull { virtualFile -> psiManager.findFile(virtualFile) }

                assertTrue(builtinFiles.isNotEmpty(), "Builtins provider must expose `.cjo` files")

                val report = StringBuilder()
                var filesWithDivergence = 0

                for (file in builtinFiles) {
                    requireIsInstance<CjDecompiledFile>(file)
                    val fileReport = diagnoseSingleFile(file)
                    report.append(fileReport)
                    if (fileReport.contains("DIVERGENCE #") || fileReport.contains("COUNT MISMATCH")) {
                        filesWithDivergence++
                    }
                }

                val reportPath = writeReport(report.toString())
                println(report.toString())
                println("Full report written to: $reportPath")

                assertTrue(
                    filesWithDivergence == 0,
                    "Found stub/AST divergence in $filesWithDivergence file(s). See $reportPath and stdout above.",
                )
            }
        }
    }

    /**
     * 对单个反编译文件执行 stub/AST 双序列对齐诊断，返回文本报告。
     */
    private fun diagnoseSingleFile(file: CjDecompiledFile): String {
        val sb = StringBuilder()
        sb.appendLine("================================================================")
        sb.appendLine("FILE: ${file.name}")

        val binaryStub = CjoCompiledStubsTestEngine.compute(file)
        if (binaryStub.kind is CangJieFileStubKind.Invalid) {
            sb.appendLine("  SKIP: invalid file stub (incompatible .cjo)")
            return sb.toString()
        }

        val packageFqName: FqName = binaryStub.getPackageFqName()
        val decompiledText = requireNotNull(file.text) { "no decompiled text for ${file.name}" }

        // 与 CjDecompiledFile.parserLanguageModuleName() 完全一致的解析上下文。
        val factory = CjPsiFactory.forPackage(file.project, packageFqName)
        val probeFile = factory.createFile(
            "${file.name.substringBeforeLast('.')}.stub-ast-probe.cj",
            decompiledText,
        )
        val astRoot = probeFile.node
            ?: error("probe file AST is null for ${file.name}")

        val stubs = flattenStubs(binaryStub)
        val astNodes = collectStubbedAstNodes(astRoot)

        sb.appendLine("  package=$packageFqName module=${BuiltInAnnotationRegistry.sourceModuleName(packageFqName)}")
        sb.appendLine("  stubCount=${stubs.size} astStubbedNodes=${astNodes.size} delta=${stubs.size - astNodes.size}")

        val histogramDeltas = histogramDelta(stubs, astNodes)
        if (histogramDeltas.isNotEmpty()) {
            sb.appendLine("  histogram deltas (stub - ast):")
            histogramDeltas.forEach { (type, delta) ->
                sb.appendLine("    $type: $delta")
            }
        }

        if (stubs.size != astNodes.size) {
            sb.appendLine("  COUNT MISMATCH: stub=${stubs.size} ast=${astNodes.size}")
        }

        val divergences = alignAndReportDivergences(stubs, astNodes, decompiledText)
        if (divergences.isEmpty()) {
            sb.appendLine("  no aligned divergences found")
        } else {
            divergences.forEach { divergence -> sb.appendLine(divergence) }
        }

        dumpSpineReconciliation(sb, astNodes)
        return sb.toString()
    }

    /**
     * 按 `FileTrees.reconcilePsi` 的真实口径计算 spine：
     * 根 FileElement 恒计入 + 每个满足 `getStubFactory(type) != null && factory.shouldCreateStub(node)`
     * 的 CompositeElement（spine 的 visitor 只走 Composite，叶节点不进 spine）。
     * 列出被 `shouldCreateStub` 否决（stub 树有桩、spine 不计）的节点。
     */
    private fun dumpSpineReconciliation(sb: StringBuilder, astNodes: List<ASTNode>) {
        val vetoed = ArrayList<ASTNode>()
        var passing = 0
        for (node in astNodes) {
            if (node !is com.intellij.psi.impl.source.tree.CompositeElement) continue
            val factory = StubElementRegistryService.getInstance().getStubFactory(node.elementType)
            when {
                factory == null -> Unit // 非 stub 类型，spine 不计
                factory.shouldCreateStub(node) -> passing++
                else -> vetoed.add(node)
            }
        }
        val spineCount = 1 + passing // FileElement 根恒计入
        sb.appendLine("  platformSpineCount=$spineCount (root + $passing passing) vetoedByShouldCreateStub=${vetoed.size}")
        vetoed.forEachIndexed { index, node ->
            val parentType = node.treeParent?.elementType?.toString() ?: "<none>"
            sb.appendLine("    VETOED #$index type=${astTypeString(node)} parentType=$parentType offset=${node.startOffset} text='${trim(node.text)}'")
        }
    }

    /** stub 先序展平（含根）。 */
    private fun flattenStubs(root: StubElement<*>): List<StubElement<*>> {
        val out = ArrayList<StubElement<*>>(40_000)
        fun walk(stub: StubElement<*>) {
            out.add(stub)
            stub.childrenStubs.forEach { walk(it) }
        }
        walk(root)
        return out
    }

    /**
     * AST 中所有 stubbed 节点的先序序列（含文件根）。
     *
     * 与平台 `FileElement.getStubbedSpine()` 同口径：文件根无条件计入，
     * 其余节点按 stub 注册表判定（`getStubFactory(type) != null && shouldCreateStub(node)`），
     * 这样两侧序列（stub 展平含根 / AST stubbed 节点含根）可以逐个对账。
     *
     * 注意：文件 element type（`IStubFileElementType`）不是 `IStubElementType` 的子类型，
     * 用 `elementType is IStubElementType` 判断会把文件根整体漏掉，造成恒定差一。
     */
    private fun collectStubbedAstNodes(root: ASTNode): List<ASTNode> {
        val out = ArrayList<ASTNode>(40_000)
        val registry = StubElementRegistryService.getInstance()
        fun shouldCreateStub(node: ASTNode): Boolean {
            if (node === root) return true
            if (node !is CompositeElement) return false
            val factory = registry.getStubFactory(node.elementType) ?: return false
            return factory.shouldCreateStub(node)
        }
        fun walk(node: ASTNode) {
            if (shouldCreateStub(node)) {
                out.add(node)
            }
            node.getChildren(null).forEach { walk(it) }
        }
        walk(root)
        return out
    }

    private fun stubTypeString(stub: StubElement<*>): String =
        ((stub as? PsiFileStub<*>)?.type ?: stub.stubType).toString()

    private fun astTypeString(node: ASTNode): String = node.elementType.toString()

    /** 按元素类型统计两序列计数差。 */
    private fun histogramDelta(stubs: List<StubElement<*>>, astNodes: List<ASTNode>): Map<String, Int> {
        val counts = HashMap<String, Int>()
        stubs.forEach { stub -> counts.merge(stubTypeString(stub), 1, Int::plus) }
        astNodes.forEach { node -> counts.merge(astTypeString(node), -1, Int::plus) }
        return counts.filterValues { it != 0 }.toSortedMap()
    }

    /**
     * 带重同步窗口的双序列对齐：报告所有 STUB_EXTRA / AST_EXTRA / TYPE_CHANGE。
     */
    private fun alignAndReportDivergences(
        stubs: List<StubElement<*>>,
        astNodes: List<ASTNode>,
        fileText: String,
    ): List<String> {
        val report = ArrayList<String>()
        val window = RESYNC_WINDOW
        var i = 0
        var j = 0
        var divergenceIndex = 0

        fun stubPath(stub: StubElement<*>?): String {
            if (stub == null) return "<none>"
            val parts = ArrayList<String>()
            var cur: StubElement<*>? = stub
            while (cur != null) {
                parts.add("${stubTypeString(cur)}${stubNameSuffix(cur)}")
                cur = cur.parentStub
            }
            return parts.reversed().joinToString(" / ")
        }

        fun astPath(node: ASTNode?): String {
            if (node == null) return "<none>"
            val parts = ArrayList<String>()
            var cur: ASTNode? = node
            while (cur != null) {
                parts.add(astTypeString(cur))
                cur = cur.treeParent
            }
            return parts.reversed().joinToString(" / ")
        }

        while (i < stubs.size && j < astNodes.size) {
            val sType = stubTypeString(stubs[i])
            val aType = astTypeString(astNodes[j])
            if (sType == aType) {
                i++
                j++
                continue
            }

            divergenceIndex++
            if (divergenceIndex > MAX_DIVERGENCES) {
                report.add("  ...more than $MAX_DIVERGENCES divergences, truncating.")
                break
            }

            // 重同步：stub 侧多出的节点（ast[j] 能在 stub 前方小窗口内找到）。
            var stubExtra = -1
            for (k in 1..window) {
                if (i + k >= stubs.size) break
                if (stubTypeString(stubs[i + k]) == aType) {
                    stubExtra = k
                    break
                }
            }
            // 重同步：AST 侧多出的节点（stubs[i] 能在 ast 前方小窗口内找到）。
            var astExtra = -1
            if (stubExtra < 0) {
                for (k in 1..window) {
                    if (j + k >= astNodes.size) break
                    if (astTypeString(astNodes[j + k]) == sType) {
                        astExtra = k
                        break
                    }
                }
            }

            val kind = when {
                stubExtra > 0 -> "STUB_EXTRA ($stubExtra stub(s) without AST counterpart)"
                astExtra > 0 -> "AST_EXTRA ($astExtra ast node(s) without stub)"
                else -> "TYPE_CHANGE"
            }

            report.add("  DIVERGENCE #$divergenceIndex [$kind] at stub[$i] vs ast[$j]")
            report.add("    STUB: type=$sType stub=${trim(stubs[i].toString())}")
            report.add("          path=${stubPath(stubs[i])}")
            if (stubExtra > 0) {
                report.add("          unmatched stubs:")
                for (k in 0 until stubExtra) {
                    report.add(
                        "            stub[$i + $k] type=${stubTypeString(stubs[i + k])} stub=${trim(stubs[i + k].toString())} " +
                            "path=${stubPath(stubs[i + k].parentStub)}",
                    )
                }
            }
            report.add("    AST : type=$aType text='${trim(astNodes[j].text)}' offset=${astNodes[j].startOffset}")
            report.add("          path=${astPath(astNodes[j])}")
            if (astExtra > 0) {
                report.add("          unmatched ast nodes:")
                for (k in 0 until astExtra) {
                    report.add(
                        "            ast[$j + $k] type=${astTypeString(astNodes[j + k])} " +
                            "text='${trim(astNodes[j + k].text)}' offset=${astNodes[j + k].startOffset}",
                    )
                }
            }
            report.add("    file context at ast offset: '${trim(fileText.substring(maxOf(0, astNodes[j].startOffset - 60), minOf(fileText.length, astNodes[j].startOffset + 120)))}'")

            when {
                stubExtra > 0 -> i += stubExtra
                astExtra > 0 -> j += astExtra
                else -> {
                    i++
                    j++
                }
            }
        }

        if (i < stubs.size) {
            report.add("  TAIL: ${stubs.size - i} trailing stub(s) without AST counterpart:")
            for (k in i until minOf(stubs.size, i + 20)) {
                report.add("    stub[$k] type=${stubTypeString(stubs[k])} stub=${trim(stubs[k].toString())} path=${stubPath(stubs[k].parentStub)}")
            }
        }
        if (j < astNodes.size) {
            report.add("  TAIL: ${astNodes.size - j} trailing AST stubbed node(s) without stub:")
            for (k in j until minOf(astNodes.size, j + 20)) {
                report.add("    ast[$k] type=${astTypeString(astNodes[k])} text='${trim(astNodes[k].text)}' offset=${astNodes[k].startOffset}")
            }
        }
        return report
    }

    private fun stubNameSuffix(stub: StubElement<*>): String {
        val rendered = trim(stub.toString())
        return if (rendered.isEmpty()) "" else "($rendered)"
    }

    private fun trim(text: String, max: Int = 160): String {
        val oneLine = text.replace("\\s+".toRegex(), " ").trim()
        return if (oneLine.length <= max) oneLine else oneLine.take(max) + "...<${oneLine.length - max} more>"
    }

    private fun writeReport(content: String): Path {
        val reportPath = CjoCompiledTestEnvironment.locateRepositoryRoot()
            .resolve("analysis")
            .resolve("stubs")
            .resolve("build")
            .resolve("reports")
            .resolve("stub-ast-consistency-report.txt")
        Files.createDirectories(reportPath.parent)
        Files.writeString(reportPath, content)
        return reportPath
    }

    private companion object {
        /** 重同步搜索窗口。 */
        const val RESYNC_WINDOW = 400

        /** 单文件最大记录分歧数。 */
        const val MAX_DIVERGENCES = 500
    }
}
