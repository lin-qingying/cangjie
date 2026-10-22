@file:OptIn(
    org.cangnova.cangjie.analysis.api.CaPlatformInterface::class,
    org.cangnova.cangjie.analysis.api.CaImplementationDetail::class,
)

package org.cangnova.cangjie.analysis.realproject

import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiManager
import org.cangnova.cangjie.LanguageVersionSettings
import org.cangnova.cangjie.analysis.api.components.CaDiagnosticCheckerFilter
import org.cangnova.cangjie.analysis.api.impl.base.projectStructure.CaBuiltinsModuleImpl
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.standalone.projectStructure.CaStandaloneLibraryModule
import org.cangnova.cangjie.analysis.api.standalone.projectStructure.CaStandaloneSourceModule
import org.cangnova.cangjie.analysis.api.standalone.session.CaStandaloneAnalysisContext
import org.cangnova.cangjie.analysis.low.level.api.cfir.diagnostics.DiagnosticCollectionFailures
import org.cangnova.cangjie.analysis.api.standalone.session.CaStandaloneSessionBuilder
import org.cangnova.cangjie.analysis.decompiler.stub.file.CjoBinaryFileReader
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.cangnova.cangjie.psi.CjFile
import com.intellij.openapi.project.Project
import java.nio.file.Path

/**
 * 单个真实项目的已准备会话：检出、cjpm 结果、项目模型与 standalone 模块图。
 *
 * 由 [RealProjectTestBase] 创建并注入测试；测试只通过该会话取分析结果。
 *
 * @property config 项目配置。
 * @property checkout 检出目录。
 * @property buildResults `cjpm clean/build` 结果。
 * @property model 项目结构。
 * @property dependencyCjoCount 作为依赖挂载的 `.cjo` 数量。
 */
class RealProjectSession internal constructor(
    val config: RealProjectConfig,
    val checkout: Path,
    val buildResults: List<CjpmRunner.Result>,
    val model: CjpmProjectModel,
    val dependencyCjoCount: Int,
    private val context: CaStandaloneAnalysisContext,
    private val files: List<CjFile>,
) {
    /** 最后一次构建结果。 */
    val buildResult: CjpmRunner.Result get() = buildResults.last()

    /** 报告目录（每个项目一份）。 */
    val reportDir: Path
        get() = config.workspaceRoot
            .resolve("reports")
            .resolve(checkout.fileName.toString())

    /** JSON 报告路径。 */
    val reportFile: Path get() = reportDir.resolve("cfir.json")

    /** 断言 cjpm 构建成功；失败时附带输出尾部。 */
    fun requireBuildSucceeded() {
        check(buildResult.exitCode == 0) {
            "cjpm build 失败:\n${buildResult.output.takeLast(8_000)}"
        }
    }

    /** 逐文件收集 CFIR 诊断；单文件异常被捕获在结果里。 */
    fun analyzeAll(): List<FileAnalysis> {
        val analyses = files.map(::analyzeFile)
        lastAnalyses = analyses
        return analyses
    }

    /** 最近一次 [analyzeAll] 的结果，供汇总页使用。 */
    var lastAnalyses: List<FileAnalysis> = emptyList()
        private set

    /** 写出当前项目的 JSON 与 HTML 报告。 */
    fun writeReport(analyses: List<FileAnalysis>) {
        RealProjectReport.write(
            config = config,
            checkout = checkout,
            model = model,
            buildResults = buildResults,
            dependencyCjoCount = dependencyCjoCount,
            analyses = analyses,
            reportFile = reportFile,
        )
        RealProjectHtmlReport.writeProject(
            file = reportFile.parent.resolve("report.html"),
            config = config,
            checkout = checkout,
            buildResults = buildResults,
            dependencyCjoCount = dependencyCjoCount,
            analyses = analyses,
        )
    }

    /** 构建/准备失败时也要留下一份可视化报告。 */
    fun writeFailureReport(failure: Throwable) {
        RealProjectHtmlReport.writeFailure(
            file = reportFile.parent.resolve("report.html"),
            config = config,
            checkout = checkout,
            buildResults = buildResults,
            failure = failure,
        )
    }

    private fun analyzeFile(file: CjFile): FileAnalysis {
        val startedAt = System.currentTimeMillis()
        DiagnosticCollectionFailures.clear()
        return try {
            val diagnostics = context.analyze(file) {
                file.collectDiagnostics(CaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS)
            }
            val collectionFailures = DiagnosticCollectionFailures.drain().map { failure -> failure.stackTraceToString() }
            val text = file.text
            FileAnalysis(
                path = file.virtualFile?.path ?: file.name,
                diagnostics = diagnostics.map { diagnostic ->
                    val range = diagnostic.textRanges.firstOrNull()
                    val position = positionOf(text, range?.startOffset ?: -1)
                    DiagnosticRecord(
                        name = diagnostic.factoryName,
                        severity = diagnostic.severity.name,
                        message = diagnostic.defaultMessage,
                        startOffset = range?.startOffset ?: -1,
                        endOffset = range?.endOffset ?: -1,
                        line = position.line,
                        column = position.column,
                        psiText = diagnostic.psi.text.take(120),
                        snippet = position.snippet,
                    )
                },
                error = null,
                durationMillis = System.currentTimeMillis() - startedAt,
                collectionFailures = collectionFailures,
            )
        } catch (cancelled: ProcessCanceledException) {
            throw cancelled
        } catch (failure: Throwable) {
            val collectionFailures = DiagnosticCollectionFailures.drain().map { swallowed -> swallowed.stackTraceToString() }
            FileAnalysis(
                path = file.virtualFile?.path ?: file.name,
                diagnostics = emptyList(),
                error = failure.stackTraceToString(),
                durationMillis = System.currentTimeMillis() - startedAt,
                collectionFailures = collectionFailures,
            )
        }
    }

    /** 由偏移计算 1-based 行/列与该行文本。 */
    private fun positionOf(text: String, offset: Int): SourcePosition {
        if (offset < 0 || offset > text.length) return SourcePosition(-1, -1, "")
        val lineStart = text.lastIndexOf('\n', (offset - 1).coerceAtLeast(0)).let { if (it < 0) 0 else it + 1 }
        val line = text.substring(0, lineStart).count { it == '\n' } + 1
        val lineEnd = text.indexOf('\n', offset).let { if (it < 0) text.length else it }
        return SourcePosition(
            line = line,
            column = offset - lineStart + 1,
            snippet = text.substring(lineStart, lineEnd).trim().take(200),
        )
    }

    private data class SourcePosition(val line: Int, val column: Int, val snippet: String)

    companion object {
        /** 执行检出、cjpm 构建并组装 standalone 模块图。 */
        fun prepare(
            config: RealProjectConfig,
            project: Project,
            languageVersionSettings: LanguageVersionSettings,
        ): RealProjectSession {
            val checkout = GitFixture.prepare(config)
            val buildResults = CjpmRunner(config, checkout).build()
            val model = CjpmProjectModel.read(checkout)
            check(model.cjFiles.isNotEmpty()) { "未找到 .cj 源文件: ${model.sourceRoots}" }

            val psiManager = PsiManager.getInstance(project)
            val targetPlatform = CangJiePlatforms.defaultCangJiePlatform
            val builtinsModule = CaBuiltinsModuleImpl(targetPlatform, project)
            val sourceModule = CaStandaloneSourceModule(
                name = model.name,
                languageVersionSettings = languageVersionSettings,
                project = project,
                psiRoots = model.sourceRoots.map { path -> psiItem(psiManager, path) },
                targetPlatform = targetPlatform,
            ).apply {
                directRegularDependencies += builtinsModule
            }

            val dependencyCjoItems = collectDependencyCjoItems(psiManager, model)
            val libraryModules = if (dependencyCjoItems.isEmpty()) {
                emptyList()
            } else {
                listOf(
                    CaStandaloneLibraryModule(
                        libraryName = "cjpm-dependencies",
                        project = project,
                        binaryRoots = dependencyCjoItems,
                        targetPlatform = targetPlatform,
                    ),
                )
            }
            sourceModule.directRegularDependencies += libraryModules

            val context = CaStandaloneSessionBuilder(project)
                .build(listOf<CaModule>(sourceModule, builtinsModule) + libraryModules)
            val files = model.cjFiles.mapNotNull { path -> psiItem(psiManager, path) as? CjFile }
            check(files.isNotEmpty()) { "未能把 .cj 文件恢复为 PSI: ${model.sourceRoots}" }

            return RealProjectSession(
                config = config,
                checkout = checkout,
                buildResults = buildResults,
                model = model,
                dependencyCjoCount = dependencyCjoItems.size,
                context = context,
                files = files,
            )
        }

        /** 把 cjpm 产出的 `.cjo` 恢复为 library binary roots，并排除项目自身的包。 */
        private fun collectDependencyCjoItems(
            psiManager: PsiManager,
            model: CjpmProjectModel,
        ): List<PsiFileSystemItem> {
            if (model.cjoFiles.isEmpty()) return emptyList()
            return model.cjoFiles.mapNotNull { path ->
                val virtualFile = virtualFile(path) ?: return@mapNotNull null
                val packageFqName = runCatching { CjoBinaryFileReader.readPackageFqName(virtualFile) }
                    .getOrNull()
                    ?: return@mapNotNull null
                if (packageFqName.asString() in model.sourcePackages) return@mapNotNull null
                runCatching { psiManager.findFile(virtualFile) }.getOrNull()
            }
        }

        /** headless 环境只能用 StandardFileSystems 恢复 VirtualFile。 */
        private fun virtualFile(path: Path): VirtualFile? {
            val normalizedPath = path.toAbsolutePath().normalize().toString().replace('\\', '/')
            val fileSystem = StandardFileSystems.local()
            return fileSystem.findFileByPath(normalizedPath)
                ?: fileSystem.refreshAndFindFileByPath(normalizedPath)
        }

        private fun psiItem(psiManager: PsiManager, path: Path): PsiFileSystemItem {
            val virtualFile = virtualFile(path) ?: error("Cannot restore VirtualFile for `$path`.")
            return if (virtualFile.isDirectory) {
                psiManager.findDirectory(virtualFile) ?: error("Cannot restore PSI directory for `$path`.")
            } else {
                psiManager.findFile(virtualFile) ?: error("Cannot restore PSI file for `$path`.")
            }
        }
    }
}
