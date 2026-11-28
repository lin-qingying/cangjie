/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.cfir.pipeline

import org.cangnova.cangjie.cfir.resolve.providers.cfirProvider

import org.cangnova.cangjie.cfir.analysis.checkers.MppCheckerKind

import org.cangnova.cangjie.CjPsiSourceFile
import org.cangnova.cangjie.CjSourceFile
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.reportMissingProgramEntryIfNeeded
import org.cangnova.cangjie.cfir.analysis.collectors.components.DiagnosticComponentsFactory
import org.cangnova.cangjie.cfir.builder.PsiRawCfirBuilder
import org.cangnova.cangjie.cfir.builder.toParsingContext
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.common.moduleData
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticWithParameters1
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticWithParameters2
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticWithParameters3
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticWithParameters4
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticWithSource
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticRelatedInformation
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticRelatedSourceLocation
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticContext
import org.cangnova.cangjie.cfir.diagnostics.Severity
import org.cangnova.cangjie.cfir.diagnostics.impl.BaseDiagnosticsCollector
import org.cangnova.cangjie.cfir.diagnostics.impl.PendingDiagnosticsReporterImpl
import org.cangnova.cangjie.cfir.lightTree.LightTree2Cfir
import org.cangnova.cangjie.cfir.resolve.providers.CfirProviderImpl
import org.cangnova.cangjie.cfir.resolve.providers.macro.*
import org.cangnova.cangjie.cfir.session.*
import org.cangnova.cangjie.cfir.withFileAnalysisExceptionWrapping
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjCodeFragment
import org.cangnova.cangjie.parsing.CangJieParser
import org.cangnova.cangjie.parsing.CjParserSettings
import org.cangnova.cangjie.parsing.toCangJieParsingErrorListener
import org.cangnova.cangjie.messages.CjParsingDiagnosticCollector
import org.cangnova.cangjie.messages.MessageCollector
import org.cangnova.cangjie.source.AbstractCjSourceElement
import org.cangnova.cangjie.source.CjBinarySourceElement
import org.cangnova.cangjie.source.CjLightSourceElement
import org.cangnova.cangjie.source.CjOffsetsOnlySourceElement
import org.cangnova.cangjie.source.CjPsiSourceElement
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.source.CjSourceElementKind
import org.cangnova.cangjie.source.CjSourceLocation
import org.cangnova.cangjie.source.readSourceFileWithMapping
import com.intellij.openapi.util.TextRange
import com.intellij.psi.tree.IElementType
import com.intellij.util.diff.FlyweightCapableTreeStructure
import java.util.Collections
import java.util.IdentityHashMap

/**
 * 从 PSI 构建尚未注册的 Pre-Macro Raw CFIR
 * （对齐 K2 `buildFirFromKtFiles` 在 macro construction 之前的部分）。
 *
 * 流程：
 * 1. 创建 [PsiRawCfirBuilder]
 * 2. 遍历 PSI 文件，构建 Raw CFIR（仅结构）
 * 3. **不**写入 [CfirProviderImpl]；返回 [PreMacroRawBuildResult]，
 *    由上层经 [MacroConstructionService] 与 [recordExpandedRawFilesOnce] 完成注册。
 */
fun CfirSession.buildPreMacroRawCfirFromCjFiles(cjFiles: Collection<CjFile>,
    parserSettings: CjParserSettings = CjParserSettings(languageVersionSettings.languageVersion)): PreMacroRawBuildResult {
    val firProvider = cfirProvider as CfirProviderImpl
    val files = cjFiles.map { cjFile ->
        val builder = PsiRawCfirBuilder(this, firProvider.cangjieScopeProvider,
            annotationMetadata = org.cangnova.cangjie.cfir.resolve.providers.macro.CfirAnnotationMetadataBuilder())
        val context = cjmpSettings.toParsingContext(languageVersionSettings)
        val parsed = if (cjFile is CjCodeFragment) CangJieParser.parseCompilationFragment(
            cjFile, cjFile.fragmentParserSeed.copy(cjmpContext = context, settings = parserSettings), cjFile.parserCreationContext,
        ) else CangJieParser.parseCompilationFile(cjFile, context, parserSettings)
        parsed.withTargetFile(builder::buildPreMacroCfirFile).also { pre ->
            pre.cfirFile.replaceSourceFile(parsed.sourceFile)
            pre.cfirFile.replaceSourceFileLinesMapping(parsed.sourceFileLinesMapping)
        }
    }
    return buildPreMacroRawFiles(
        session = this,
        files = files,
    )
}

/**
 * 经 LightTree 构建尚未注册的 Pre-Macro Raw CFIR。
 * 行为镜像 [buildPreMacroRawCfirFromCjFiles] 的 PSI 形式。
 */
fun CfirSession.buildPreMacroRawCfirViaLightTree(
    lightTreeFiles: Collection<CjSourceFile>,
    reportFilesAndLines: ((String, Int) -> Unit)? = null,
    messageCollector: MessageCollector? = null,
    parserSettings: CjParserSettings = CjParserSettings(languageVersionSettings.languageVersion),
): PreMacroRawBuildResult {
    val firProvider = cfirProvider as CfirProviderImpl
    val builder = LightTree2Cfir(
        session = this,
        scopeProvider = firProvider.cangjieScopeProvider,
        parserSettings = parserSettings,
    )
    val rawFilesWithSurfaces = lightTreeFiles.map { sourceFile ->
        val (code, linesMapping) = sourceFile.getContentsAsStream().reader(Charsets.UTF_8).use {
            it.readSourceFileWithMapping()
        }
        val preFile = builder.buildPreMacroCfirFile(code, sourceFile, linesMapping)
        reportFilesAndLines?.invoke(sourceFile.path ?: sourceFile.name, linesMapping.linesCount)
        preFile
    }
    messageCollector?.toCangJieParsingErrorListener()?.let { listener ->
        CjParsingDiagnosticCollector.publishSyntaxDiagnostics(
            rawFilesWithSurfaces.flatMap { it.parsingDiagnosticPublication.reports }, parserSettings.diagnostics,
        ).forEach(listener::onError)
    }
    return buildPreMacroRawFiles(
        session = this,
        files = rawFilesWithSurfaces,
    )
}

/**
 * 历史入口：从 PSI 构建 Raw CFIR 并立即注册到 source provider。
 *
 * 内部走 [MacroConstructionService.Identity] 的 stub + [recordExpandedRawFilesOnce]，
 * 保留外部签名以兼容测试 fixture。
 *
 * 生产 pipeline 推荐拆分为：
 *   `buildPreMacroRawCfirFromCjFiles → MacroConstructionService.expand → recordExpandedRawFilesOnce`
 */
fun CfirSession.buildCfirFromCjFiles(cjFiles: Collection<CjFile>): List<CfirFile> {
    return buildRecordableCfirFromCjFiles(cjFiles).files
}

/**
 * 历史入口：经 LightTree 构建 Raw CFIR 并立即注册。
 * 镜像 [buildCfirFromCjFiles]。
 */
fun CfirSession.buildCfirViaLightTree(
    lightTreeFiles: Collection<CjSourceFile>,
    reportFilesAndLines: ((String, Int) -> Unit)? = null,
): List<CfirFile> {
    return buildRecordableCfirViaLightTree(lightTreeFiles, reportFilesAndLines).files
}

/**
 * 构建无宏源码的 identity recordable 包装，并通过唯一入口写入 source provider。
 *
 * 这是 Kotlin `buildFirFromKtFiles -> provider.recordFile` 在仓颉宏 construction
 * 边界下的对应形态：调用方拿到的是 [RecordableRawCfirFiles]，后续只能通过
 * [runResolution] 进入 ordinary resolve。
 */
fun CfirSession.buildRecordableCfirFromCjFiles(cjFiles: Collection<CjFile>): RecordableRawCfirFiles {
    val pre = buildPreMacroRawCfirFromCjFiles(cjFiles)
    return finalizeIdentity(pre)
}

/**
 * LightTree 版本的 identity recordable 构建入口，语义同 [buildRecordableCfirFromCjFiles]。
 */
fun CfirSession.buildRecordableCfirViaLightTree(
    lightTreeFiles: Collection<CjSourceFile>,
    reportFilesAndLines: ((String, Int) -> Unit)? = null,
): RecordableRawCfirFiles {
    val pre = buildPreMacroRawCfirViaLightTree(lightTreeFiles, reportFilesAndLines)
    return finalizeIdentity(pre)
}

/**
 * 对无宏 construction 结果执行 identity 收尾。
 *
 * 该方法使用 [MacroConstructionService.Identity] 产出可注册文件，并通过
 * [recordExpandedRawFilesOnce] 完成 source provider 注册，保证历史入口也不绕过宏构造边界。
 */
private fun CfirSession.finalizeIdentity(pre: PreMacroRawBuildResult): RecordableRawCfirFiles {
    val provider = cfirProvider as CfirProviderImpl
    val result = MacroConstructionService.Identity.expandWithDefaultContext(
        pre = pre,
        mode = MacroConstructionService.Mode.STRICT,
    )
    val success = result as? MacroConstructionResult.Success
        ?: error("Identity macro construction must return Success, got ${result::class.simpleName}")
    recordExpandedRawFilesOnce(provider, success.recordableFiles, success.registry)
    return success.recordableFiles
}

/**
 * 将通用源文件列表转换为 PSI 文件列表。
 *
 * 该工具只服务 PSI raw CFIR 构建入口，调用方必须保证集合中的源文件实际为 [CjPsiSourceFile]。
 */
fun List<CjSourceFile>.asCjFilesList(): List<CjFile> {
    return map { (it as CjPsiSourceFile).psiFile as CjFile }
}

/**
 * 运行完整 resolve 流水线（对齐 K2 的 runResolution）。
 *
 * 推荐入口接受 [RecordableRawCfirFiles]：它由 [MacroConstructionService] 产出，
 * 是 source CFIR 文件进入 ordinary resolve 的唯一规范输入（baseline 第 2 节硬性边界 #1）。
 */
fun CfirSession.runResolution(files: RecordableRawCfirFiles): Pair<ScopeSession, List<CfirFile>> {
    return runResolutionFiles(files.files)
}

/**
 * 内部实现：公开 API 不再接收裸 `List<CfirFile>`，避免绕过 macro construction 边界。
 */
private fun CfirSession.runResolutionFiles(cfirFiles: List<CfirFile>): Pair<ScopeSession, List<CfirFile>> {
    val resolveProcessor = CfirTotalResolveProcessor(this)
    val resolvedFiles = resolveProcessor.process(cfirFiles)
    return resolveProcessor.scopeSession to resolvedFiles
}

/**
 * 运行语义检查器（对齐 K2 的 runCheckers）。
 *
 * 通过 [CheckersComponent] 执行所有注册的 checker，
 * 诊断结果通过 [diagnosticReporter] 收集。
 *
 * @param scopeSession 作用域缓存会话
 * @param firFiles 待检查的 CFIR 文件
 * @param diagnosticsCollector 诊断收集器，checker 产生的诊断会写入该对象。
 */
fun CfirSession.runCheckers(
    scopeSession: ScopeSession,
    firFiles: Collection<CfirFile>,
    diagnosticsCollector: BaseDiagnosticsCollector,
    mppCheckerKind: MppCheckerKind,
    /** 已由更大的编译单元发布 parser 报告时关闭本组重放；语义 checker 始终独立运行。 */
    includeParsingDiagnostics: Boolean = mppCheckerKind == MppCheckerKind.Common,
): Map<CfirFile, List<CjDiagnostic>> {
    if (includeParsingDiagnostics) diagnosticsCollector.reportParsingDiagnostics(firFiles)
    val collector = DiagnosticComponentsFactory.create(this, scopeSession, mppCheckerKind)
    val registry = macroExpansionRegistry
    val diagnosticsReporter = PendingDiagnosticsReporterImpl(
        delegate = diagnosticsCollector,
        sourceMapper = { source -> registry?.originSourceForGeneratedSource(source) },
    )
    diagnosticsReporter.beginDiagnosticCollection()
    for (file in firFiles) {
        withFileAnalysisExceptionWrapping(file) {
            collector.collectDiagnostics(file, diagnosticsReporter)
        }
    }
    if (mppCheckerKind == MppCheckerKind.Common) {
        collector.collectDiagnosticsInSettings(diagnosticsReporter)
        reportMissingProgramEntryIfNeeded(firFiles, this, diagnosticsCollector)
    }
    return firFiles.associateWith {
        val path = it.sourceFile?.path ?: return@associateWith emptyList()
        diagnosticsCollector.diagnosticsByFilePath[path] ?: emptyList()
    }
}

/**
 * 完整的 resolve + check 流程（对齐 K2 的 resolveAndCheckFir）。
 *
 * 流程：
 * 1. 运行 resolve 流水线
 * 2. 运行 checker（当前已集成在 resolve 中）
 * 3. 返回前端输出
 */
fun resolveAndCheckCfir(
    session: CfirSession,
    cfirFiles: RecordableRawCfirFiles,
    diagnosticsCollector: BaseDiagnosticsCollector,
): SingleModuleFrontendOutput {
    val (scopeSession, fir) = session.runResolution(cfirFiles)
    session.runCheckers(scopeSession, fir, diagnosticsCollector, MppCheckerKind.Common)
    return SingleModuleFrontendOutput(session, scopeSession, fir)
}

/** 一个 Platform 检查上下文及其可达源码输出。 */
private data class PlatformCheckerSourceGraph(
    val leaf: SingleModuleFrontendOutput,
    val outputs: List<SingleModuleFrontendOutput>,
)

/** 用稳定的文件路径把 LightTree 身份映射回源码文件。 */
private fun Collection<PlatformCheckerSourceGraph>.lightTreeSourceFilePaths(): IdentityHashMap<FlyweightCapableTreeStructure<*>, String> {
    val result = IdentityHashMap<FlyweightCapableTreeStructure<*>, String>()
    for (graph in this) {
        for (output in graph.outputs) {
            for (file in output.fir) {
                val sourceFilePath = file.sourceFile?.path ?: continue
                val source = file.source as? CjLightSourceElement ?: continue
                val previousPath = result.put(source.treeStructure, sourceFilePath)
                check(previousPath == null || previousPath == sourceFilePath) {
                    "A LightTree source structure is associated with more than one source file"
                }
            }
        }
    }
    return result
}

/**
 * 按源码模块数据的依赖关系构造 Platform 检查图。
 *
 * CLI 的 HMPP 分组可以产生多个互不依赖的源码图；脚本 session 则通过 refinement 依赖连接到
 * 普通源码 session。二进制依赖没有对应的前端输出，因此不参与源码输出闭包。
 */
private fun List<SingleModuleFrontendOutput>.platformCheckerSourceGraphs(): List<PlatformCheckerSourceGraph> {
    if (isEmpty()) return emptyList()

    val outputByModuleData = IdentityHashMap<CfirModuleData, SingleModuleFrontendOutput>(size)
    for (output in this) {
        check(outputByModuleData.put(output.session.moduleData, output) == null) {
            "A source module has more than one frontend output"
        }
    }

    val dependenciesByModuleData = IdentityHashMap<CfirModuleData, List<CfirModuleData>>(size)
    val modulesUsedAsDependencies = identitySet<CfirModuleData>()
    for (output in this) {
        val moduleData = output.session.moduleData
        val sourceDependencies = mutableListOf<CfirModuleData>()
        val seenDependencies = identitySet<CfirModuleData>()
        for (dependency in moduleData.dependencies + moduleData.refinementDependencies) {
            val dependencyModuleData = outputByModuleData[dependency]?.session?.moduleData ?: continue
            if (seenDependencies.add(dependencyModuleData)) sourceDependencies += dependencyModuleData
        }
        dependenciesByModuleData[moduleData] = sourceDependencies
        modulesUsedAsDependencies.addAll(sourceDependencies)
    }

    val visiting = identitySet<CfirModuleData>()
    val validated = identitySet<CfirModuleData>()
    fun validateAcyclic(moduleData: CfirModuleData) {
        if (moduleData in validated) return
        check(visiting.add(moduleData)) { "Source module frontend outputs contain a dependency cycle" }
        dependenciesByModuleData[moduleData].orEmpty().forEach(::validateAcyclic)
        visiting.remove(moduleData)
        validated.add(moduleData)
    }
    for (output in this) validateAcyclic(output.session.moduleData)

    val leaves = filter { it.session.moduleData !in modulesUsedAsDependencies }
    check(leaves.isNotEmpty()) { "Source module frontend outputs have no terminal module" }

    return leaves.map { leaf ->
        val visited = identitySet<CfirModuleData>()
        val graphOutputs = mutableListOf<SingleModuleFrontendOutput>()
        fun collectDependenciesFirst(moduleData: CfirModuleData) {
            if (!visited.add(moduleData)) return
            dependenciesByModuleData[moduleData].orEmpty().forEach(::collectDependenciesFirst)
            graphOutputs += checkNotNull(outputByModuleData[moduleData])
        }
        collectDependenciesFirst(leaf.session.moduleData)
        PlatformCheckerSourceGraph(leaf, graphOutputs)
    }
}

private fun <T : Any> identitySet(): MutableSet<T> = Collections.newSetFromMap(IdentityHashMap())

/**
 * Platform 图会重复检查同时属于多个 HMPP module group 的 common source。
 * 只合并 source、工厂、严重级别、完整参数和相关位置完全相同的重复诊断。
 */
private class PlatformCheckerDiagnosticsCollector(
    private val delegate: BaseDiagnosticsCollector,
    private val lightTreeSourceFilePaths: IdentityHashMap<FlyweightCapableTreeStructure<*>, String>,
) : BaseDiagnosticsCollector() {
    private val reported = mutableSetOf<PlatformSourceDiagnosticIdentity>()

    override val diagnostics: List<CjDiagnostic>
        get() = delegate.diagnostics

    override val diagnosticsByFilePath: Map<String?, List<CjDiagnostic>>
        get() = delegate.diagnosticsByFilePath

    override val hasErrors: Boolean
        get() = delegate.hasErrors

    override val hasWarningsForWError: Boolean
        get() = delegate.hasWarningsForWError

    override fun report(diagnostic: CjDiagnostic?, context: DiagnosticContext) {
        if (diagnostic is CjDiagnosticWithSource && !context.isDiagnosticSuppressed(diagnostic)) {
            val identity = PlatformSourceDiagnosticIdentity(
                filePath = context.containingFilePath,
                factoryName = diagnostic.factoryName,
                severity = diagnostic.severity,
                sourceElementClass = diagnostic.element.javaClass,
                sourceElementRange = TextRange(diagnostic.element.startOffset, diagnostic.element.endOffset),
                sourceElementType = (diagnostic.element as? CjSourceElement)?.elementType,
                sourceElementKind = (diagnostic.element as? CjSourceElement)?.kind,
                ranges = diagnostic.textRanges,
                parameters = diagnostic.parameters(),
                message = diagnostic.renderMessage(),
                relatedInformation = diagnostic.relatedInformation.map { it.stableIdentity(lightTreeSourceFilePaths) },
            )
            if (!reported.add(identity)) return
        }
        delegate.report(diagnostic, context)
    }
}

private data class PlatformSourceDiagnosticIdentity(
    val filePath: String?,
    val factoryName: String,
    val severity: Severity,
    val sourceElementClass: Class<out AbstractCjSourceElement>,
    val sourceElementRange: TextRange,
    val sourceElementType: IElementType?,
    val sourceElementKind: CjSourceElementKind?,
    val ranges: List<TextRange>,
    val parameters: List<Any?>,
    val message: String,
    val relatedInformation: List<PlatformRelatedInformationIdentity>,
)

private data class PlatformRelatedInformationIdentity(
    val message: String,
    val filePath: String?,
    val elementClass: Class<out AbstractCjSourceElement>?,
    val elementRange: TextRange?,
    val elementType: IElementType?,
    val elementKind: CjSourceElementKind?,
    val binarySourceIdentity: CjBinarySourceElement?,
    val binarySourceLocation: CjSourceLocation?,
    val sourceLocation: CjDiagnosticRelatedSourceLocation?,
)

private fun CjDiagnosticRelatedInformation.stableIdentity(
    lightTreeSourceFilePaths: IdentityHashMap<FlyweightCapableTreeStructure<*>, String>,
): PlatformRelatedInformationIdentity {
    val source = element as? CjSourceElement
    return PlatformRelatedInformationIdentity(
        message = message,
        filePath = sourceLocation?.filePath ?: element?.sourceFilePath(lightTreeSourceFilePaths),
        elementClass = element?.javaClass,
        elementRange = element?.let { TextRange(it.startOffset, it.endOffset) },
        elementType = source?.elementType,
        elementKind = source?.kind,
        binarySourceIdentity = element as? CjBinarySourceElement,
        binarySourceLocation = (element as? CjBinarySourceElement)?.sourceLocation,
        sourceLocation = sourceLocation,
    )
}

private fun AbstractCjSourceElement.sourceFilePath(
    lightTreeSourceFilePaths: IdentityHashMap<FlyweightCapableTreeStructure<*>, String>,
): String? = when (this) {
    is CjBinarySourceElement -> sourceLocation?.filePath ?: binaryFilePath
    is CjPsiSourceElement -> psi.containingFile?.virtualFile?.path
    is CjLightSourceElement -> lightTreeSourceFilePaths[treeStructure]
    is CjOffsetsOnlySourceElement -> sourceAnchor?.sourceFilePath(lightTreeSourceFilePaths)
}

private fun CjDiagnosticWithSource.parameters(): List<Any?> = when (this) {
    is CjDiagnosticWithParameters1<*> -> listOf(a)
    is CjDiagnosticWithParameters2<*, *> -> listOf(a, b)
    is CjDiagnosticWithParameters3<*, *, *> -> listOf(a, b, c)
    is CjDiagnosticWithParameters4<*, *, *, *> -> listOf(a, b, c, d)
    else -> emptyList()
}

/** 对每个源码依赖图的终端 session 执行 Platform 检查。 */
fun List<SingleModuleFrontendOutput>.runPlatformCheckers(diagnosticsCollector: BaseDiagnosticsCollector) {
    val graphs = platformCheckerSourceGraphs()
    val platformDiagnosticsCollector = PlatformCheckerDiagnosticsCollector(
        diagnosticsCollector,
        graphs.lightTreeSourceFilePaths(),
    )
    for (graph in graphs) {
        graph.leaf.session.runCheckers(
            graph.leaf.scopeSession,
            graph.outputs.flatMap { it.fir },
            platformDiagnosticsCollector,
            MppCheckerKind.Platform,
        )
    }
}

/**
 * 完整的 construction + resolve 流程。
 *
 * 这是 baseline 第 1 节定义的"主流程"代码级表达：
 * ```
 * pre → MacroConstructionService.expand → recordExpandedRawFilesOnce → resolve
 * ```
 *
 * 当 construction 返回 [MacroConstructionResult.Failed] /
 * [MacroConstructionResult.ExecutorUnavailable] / [MacroConstructionResult.Blocked] 时，
 * 文件不会被 record，ordinary resolve 不会运行；诊断由调用方从 registry 自行处理。
 *
 * check 由调用方在确定 Common/Platform 角色后统一调度。当前 batch 仅暴露 Success / Degraded 两种"可注册"分支；
 * 其他分支返回 `null`，由上层判断 baseline 行为。
 */
fun buildAndResolveCfirAfterConstruction(
    session: CfirSession,
    pre: PreMacroRawBuildResult,
    classification: MacroDemandClassification,
    constructionService: MacroConstructionService,
    constructionMode: MacroConstructionService.Mode,
    macroArtifactDefinitions: List<MacroDefinitionEntry> = emptyList(),
    preConstructionDiagnostics: List<org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionDiagnostic> = emptyList(),
): Pair<MacroConstructionResult, SingleModuleFrontendOutput?> {
    // baseline 第 1 节"主流程"：先 symbol index，再 bindMacroImports，再 expand。
    val symbolIndex = buildMacroSymbolIndex(
        pre = pre,
        macroArtifactDefinitions = macroArtifactDefinitions,
    )
    val context = bindMacroImports(pre, symbolIndex)
    if (preConstructionDiagnostics.any {
            it.severity == org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionDiagnostic.Severity.ERROR
        } && constructionMode == MacroConstructionService.Mode.STRICT
    ) {
        classification.freezeFailurePath(MacroFailurePolicy.STRICT)
        val registry = org.cangnova.cangjie.cfir.resolve.providers.macro.MacroExpansionRegistry().apply {
            addAll(preConstructionDiagnostics)
            pre.files.forEach { preFile -> registerFileSurfaces(preFile.cfirFile, preFile.surfaces) }
            pre.files.forEach { preFile ->
                if (preFile.isMacroPackage) registerDeclaredMacroPackage(preFile.cfirFile.packageDirective.packageFqName)
            }
        }
        session.register(MacroExpansionRegistry::class, registry)
        return MacroConstructionService.failedOf(pre, registry) to null
    }
    // 展开耗时由 MacroConstructionService.expand 模板方法统一统计。
    val result = constructionService.expand(
        pre = pre,
        context = context,
        classification = classification,
        mode = constructionMode,
        preConstructionDiagnostics = preConstructionDiagnostics,
    )
    val recordable: RecordableRawCfirFiles = when (result) {
        is MacroConstructionResult.Success -> result.recordableFiles
        is MacroConstructionResult.Degraded -> result.recordableFiles
        is MacroConstructionResult.Failed,
        is MacroConstructionResult.ExecutorUnavailable,
        is MacroConstructionResult.Blocked -> return result to null
    }
    // macro package 身份是 ordinary resolve/check 面的公共事实：import 宏包 = 宏注册，永不报 unused。
    result.registry.let { registry ->
        pre.files.forEach { preFile ->
            if (preFile.isMacroPackage) registry.registerDeclaredMacroPackage(preFile.cfirFile.packageDirective.packageFqName)
        }
    }
    val provider = session.cfirProvider as CfirProviderImpl
    recordExpandedRawFilesOnce(provider, recordable, result.registry)
    val (scopeSession, fir) = session.runResolution(recordable)
    val output = SingleModuleFrontendOutput(session, scopeSession, fir)
    return result to output
}
