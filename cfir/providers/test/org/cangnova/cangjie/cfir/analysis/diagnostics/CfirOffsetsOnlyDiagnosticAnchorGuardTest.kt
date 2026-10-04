package org.cangnova.cangjie.cfir.analysis.diagnostics

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * 偏移-only 诊断锚点架构守卫。
 *
 * IDE（analysis API low-level）收集诊断时要求每条诊断锚定在真实 PSI/LightTree 源元素上；
 * `CjOffsetsOnlySourceElement` 只有起止 offset、没有 PSI 锚点，落到
 * `analysis/low-level-api-cfir/.../LLCfirDiagnosticReporter` 会被 `error("Unknown diagnostic
 * type ...")` 拒绝，整条收集流程对该 CFIR 元素中断。
 *
 * CLI 编译器路径（`PendingDiagnosticsReporterImpl`）可以消费 offsets-only 诊断，因此这些
 * 构造点在 CLI 上不会暴露问题、只在 IDE 上崩溃。本守卫把"禁止在 CFIR 诊断生产者里新增
 * offsets-only 锚点"固化为规则，防止回归；现存存量以显式白名单记录，W 系列修复逐批移除。
 */
class CfirOffsetsOnlyDiagnosticAnchorGuardTest {
    /**
     * 现存（已豁免）的 offsets-only 锚点构造点，按 `相对路径` 归一。
     *
     * 修复任一构造点后应同步从本集合移除对应条目，使本测试保持"禁新增"。
     * 条目形如 `相对路径#构造函数所在行`；行号随文件演进而变，因此匹配时只用路径，
     * 行号仅作定位提示。
     */
    private val knownOffsetsOnlySites: Set<String> = setOf(
        // cfir/checkers —— 诊断源构造辅助
        "cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirDeclarationDiagnosticSources.kt",
        "cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirCJMappingCheckers.kt",
        "cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirAnnotationLookup.kt",
        "cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirCAnnotationChecker.kt",
        "cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirConstDeclarationChecker.kt",
        "cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirForeignFunctionReturnTypeChecker.kt",
        "cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirFunctionLambdaChecker.kt",
        "cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirFunctionSemanticsChecker.kt",
        "cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirConstructorDelegationChecker.kt",
        "cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirVariableLambdaInitializerTypeMismatchChecker.kt",
        "cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirBuiltInAnnotationSemanticsChecker.kt",
        "cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirObjCCallPropertyChecker.kt",
        "cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirStringInterpolationToStringChecker.kt",
        "cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirApiLevelRefHigherChecker.kt",
        "cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirClassifierAsExpressionChecker.kt",
        "cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/type/CfirHideResolvedTypeRefChecker.kt",
        "cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/CfirVArraySizeLiteralUtils.kt",
        // cfir/checkers —— cone 诊断映射
        "cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt",
        // cfir/resolve
        "cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/calls/stages/CfirMapArguments.kt",
        // cfir/providers
        "cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/DeclaredSupertypeClassification.kt",
    )

    /**
     * CFIR 诊断生产者与 IDE 侧消费方的生产源目录。
     *
     * `analysis` 一并纳入：IDE 路径只会消费 PSI/Light 诊断，任何 offsets-only 诊断或锚点
     * 出现在那里都意味着 IDE 链路无法把该诊断投影成 PSI 诊断。
     */
    private val scannedRoots = arrayOf(
        "cfir/checkers/src",
        "cfir/resolve/src",
        "cfir/providers/src",
        "analysis",
    )

    /**
     * 允许出现 offsets-only 诊断构造的框架层目录。
     *
     * `CjDiagnosticFactoryN.on()` 的 offsets-only 分支与宏重映射
     * （`PendingDiagnosticsReporterImpl.remapSourceIfNeeded`）都必须保留：前者是
     * `AbstractCjSourceElement` sealed 层级的必要分支，后者是仓颉宏展开语义的既有实现。
     */
    private val frameworkRoots = arrayOf("common/diagnostics")

    /**
     * 验证 CFIR 诊断生产者没有新增 offsets-only 诊断锚点。
     *
     * 现存存量按文件路径豁免；任何**不在**豁免集合中的生产文件一旦出现
     * `CjOffsetsOnlySourceElement(` 构造即视为新增违规并失败。
     */
    @Test
    fun noNewOffsetsOnlyDiagnosticAnchorInCfirProducers() {
        val newViolations = sourceFilesUnder(*scannedRoots)
            .filterNot { it.fileName.toString().startsWith("CfirOffsetsOnlyDiagnosticAnchorGuardTest") }
            .flatMap { path ->
                val text = Files.readString(path, UTF_8)
                val relative = root.relativize(path).toString().replace('\\', '/')
                if (OffsetsOnlyConstructor.containsMatchIn(text) && relative !in knownOffsetsOnlySites) {
                    listOf(relative)
                } else {
                    emptyList()
                }
            }
            .distinct()

        assertTrue(
            newViolations.isEmpty(),
            "Do not introduce new CjOffsetsOnlySourceElement anchors in CFIR diagnostic producers; " +
                "keep the PSI/LightTree anchor and override the highlighted range via " +
                "fakeElement(kind, CjSourceElementOffsetStrategy.Custom.*):\n" +
                newViolations.joinToString(separator = "\n"),
        )
    }

    /**
     * 验证 offsets-only **诊断**的构造只存在于框架层。
     *
     * `CjOffsetsOnly*Diagnostic` 由 `CjDiagnosticFactoryN.on()` 的 offsets-only 分支与宏重映射
     * 产生。CLI 侧能消费它们，但 IDE 的 low-level reporter 无法投影成 PSI 诊断；因此除框架层
     * 外，任何模块直接构造 offsets-only 诊断都是绕过工厂分派的越权用法。
     */
    @Test
    fun offsetsOnlyDiagnosticIsOnlyConstructedInFramework() {
        val constructor = Regex("""\bCjOffsetsOnly(?:Simple)?Diagnostic\w*\s*\(""")
        val violations = sourceFilesUnder(*(scannedRoots + frameworkRoots))
            .filterNot { it.fileName.toString().startsWith("CfirOffsetsOnlyDiagnosticAnchorGuardTest") }
            .map { root.relativize(it).toString().replace('\\', '/') }
            .filter { relative -> constructor.containsMatchIn(Files.readString(root.resolve(relative), UTF_8)) }
            .filterNot { relative -> frameworkRoots.any { relative.startsWith("$it/") } }
            .distinct()

        assertTrue(
            violations.isEmpty(),
            "CjOffsetsOnly*Diagnostic must only be constructed by the framework " +
                "(CjDiagnosticFactoryN.on() offsets-only branch and the macro remap in " +
                "PendingDiagnosticsReporterImpl); found in:\n" +
                violations.joinToString(separator = "\n"),
        )
    }

    /**
     * 验证白名单没有变成永久豁免：已豁免文件若不再包含 offsets-only 构造，应被提醒移除。
     *
     * 这条规则防止"修复后忘记从白名单删除"，导致白名单无意义膨胀。它不直接失败，
     * 而是把"已无匹配构造"的白名单文件报告出来供人工确认。
     */
    @Test
    fun knownOffsetsOnlySitesRemainReferenced() {
        val emptyFiles = sourceFilesUnder(*scannedRoots)
            .map { root.relativize(it).toString().replace('\\', '/') }
            .filter { it in knownOffsetsOnlySites }
            .filterNot { relative ->
                val path = root.resolve(relative)
                Files.exists(path) && OffsetsOnlyConstructor.containsMatchIn(Files.readString(path, UTF_8))
            }

        assertTrue(
            emptyFiles.isEmpty(),
            "These files no longer construct CjOffsetsOnlySourceElement; remove them from " +
                "knownOffsetsOnlySites so the exemption set cannot grow stale:\n" +
                emptyFiles.joinToString(separator = "\n"),
        )
    }

    /**
     * 收集指定顶层目录下的生产 Kotlin 源文件。
     *
     * 排除测试源集与生成/构建输出，避免把夹具和生成代码误判为生产违规。
     */
    private fun sourceFilesUnder(vararg topLevelDirs: String): List<Path> =
        topLevelDirs.flatMap { dir ->
            val start = root.resolve(dir)
            if (!Files.exists(start)) return@flatMap emptyList()
            val stream = Files.walk(start)
            try {
                stream
                    .filter { Files.isRegularFile(it) }
                    .filter { it.toString().endsWith(".kt") }
                    .filter { !isGeneratedOrBuildOutput(it) }
                    .filter { !isTestSource(it) }
                    .toList()
            } finally {
                stream.close()
            }
        }

    /**
     * 判断路径是否位于测试源集。
     */
    private fun isTestSource(path: Path): Boolean =
        root.relativize(path).any { part ->
            part.toString() in setOf("test", "tests", "testFixtures", "testFixturesResources", "testResources")
        }

    /**
     * 判断路径是否位于生成目录或构建输出目录。
     */
    private fun isGeneratedOrBuildOutput(path: Path): Boolean =
        root.relativize(path).any { part ->
            part.toString() in setOf("bin", "build", ".gradle", "out")
        }

    /**
     * 匹配 offsets-only 源元素构造调用。
     */
    private companion object {
        /**
         * 当前 checkout 根目录。
         */
        val root: Path = findRepoRoot()

        /**
         * 匹配 `CjOffsetsOnlySourceElement(` 构造调用（允许跨空白）。
         */
        val OffsetsOnlyConstructor: Regex = Regex("""\bCjOffsetsOnlySourceElement\s*\(""")

        /**
         * 从当前工作目录向上查找仓库根。
         */
        private fun findRepoRoot(): Path {
            var current = Paths.get("").toAbsolutePath()
            while (current.parent != null) {
                if (Files.exists(current.resolve("settings.gradle.kts"))) {
                    return current
                }
                current = current.parent
            }
            error("Cannot locate repository root from ${Paths.get("").toAbsolutePath()}")
        }
    }
}
