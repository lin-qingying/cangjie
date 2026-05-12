package org.cangnova.cangjie.frontend.pipeline

import com.intellij.lang.LighterASTNode
import com.intellij.lang.PsiBuilderFactory
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.psi.tree.IElementType
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.cangnova.cangjie.CangJieCoreEnvironment
import org.cangnova.cangjie.CangJieCoreEnvironmentMode
import org.cangnova.cangjie.CjPsiSourceFile
import org.cangnova.cangjie.CjSourceFile
import org.cangnova.cangjie.cfir.DependencyListForCliModule
import org.cangnova.cangjie.cfir.builder.PsiRawCfirBuilder
import org.cangnova.cangjie.cfir.builder.macro.MacroPayloadTokenizer
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.entrypoint.configuration.initializeCfirFrontendConfiguration
import org.cangnova.cangjie.cfir.entrypoint.session.CfirDefaultSessionFactory
import org.cangnova.cangjie.cfir.extensions.CfirExtensionRegistrar
import org.cangnova.cangjie.cfir.lightTree.LightTree2Cfir
import org.cangnova.cangjie.cfir.lightTree.LightTreeRawCfirDeclarationBuilder
import org.cangnova.cangjie.cfir.pipeline.AllModulesFrontendOutput
import org.cangnova.cangjie.cfir.pipeline.CfirSessionConstructionUtils
import org.cangnova.cangjie.cfir.pipeline.CfirSessionProducer
import org.cangnova.cangjie.cfir.pipeline.SessionWithSources
import org.cangnova.cangjie.cfir.pipeline.buildPreMacroRawCfirFromCjFiles
import org.cangnova.cangjie.cfir.pipeline.buildPreMacroRawCfirViaLightTree
import org.cangnova.cangjie.cfir.pipeline.resolveAndCheckCfir
import org.cangnova.cangjie.cfir.pipeline.resolveAndCheckCfirAfterConstruction
import org.cangnova.cangjie.cfir.resolve.providers.CfirProviderImpl
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroCallNode
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionResult
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionService
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroFragmentParser
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceParam
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceToken
import org.cangnova.cangjie.cfir.resolve.providers.macro.PreMacroRawBuildResult
import org.cangnova.cangjie.cfir.resolve.providers.macro.TokenBackedMacroFragmentParser
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.cangjieSourceRoots
import org.cangnova.cangjie.config.classpathRoots
import org.cangnova.cangjie.config.diagnosticsCollector
import org.cangnova.cangjie.config.languageVersionSettings
import org.cangnova.cangjie.config.messageCollector
import org.cangnova.cangjie.config.moduleName
import org.cangnova.cangjie.config.useLightTree
import org.cangnova.cangjie.frontend.environment.VfsBasedProjectEnvironment
import org.cangnova.cangjie.frontend.environment.findFileByPath
import org.cangnova.cangjie.frontend.environment.forAllFiles
import org.cangnova.cangjie.frontend.sources.CollectedCjSources
import org.cangnova.cangjie.frontend.sources.GroupedCjSources
import org.cangnova.cangjie.frontend.sources.allFiles
import org.cangnova.cangjie.frontend.sources.collectCjSources
import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.messages.CompilerMessageLocationWithRange
import org.cangnova.cangjie.messages.CompilerMessageSourceLocation
import org.cangnova.cangjie.messages.CompilerMessageSeverity
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.parsing.CangJieLightParser
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNodeTypes
import org.cangnova.cangjie.psi.CjParameter
import org.cangnova.cangjie.psi.CjPsiFactory
import org.cangnova.cangjie.source.CjPsiSourceElement
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.source.psi
import org.cangnova.cangjie.source.readSourceFileWithMapping
import java.io.File

object CfirFrontendPipelinePhase : PipelinePhase<ConfigurationPipelineArtifact, DefaultCfirFrontendPipelineArtifact>(
    name = "CfirFrontendPipelinePhase",
    postActions = setOf(CheckCompilationErrors.CheckDiagnosticCollector),
) {
    override fun executePhase(input: ConfigurationPipelineArtifact): DefaultCfirFrontendPipelineArtifact? {
        val (configuration, rootDisposable) = input
        configuration.initializeCfirFrontendConfiguration()

        val (environment, sourcesProvider) = createEnvironmentAndSources(configuration, rootDisposable) ?: return null
        val sources = sourcesProvider()

        if (sources.allSources.isEmpty()) {
            configuration.messageCollector.report(
                CompilerMessageSeverity.ERROR,
                "No source files",
            )
            return null
        }

        val rootModuleName = Name.identifier(configuration.moduleName ?: "main")
        val factory = CfirDefaultSessionFactory()
        val sessionFactoryContext = CfirDefaultSessionFactory.Context()
        val extensionRegistrars = emptyList<CfirExtensionRegistrar>()
        configuration.installDefaultMacroFragmentParserFactory(environment)

        // Baseline 第 1 节"主流程"：
        //   pre → MacroConstructionService.expand → recordExpandedRawFilesOnce → resolve & check
        //
        // 当前 batch 仍以 STRICT 模式驱动 CLI：构造失败立刻终止该 module 的 resolve。
        val constructionService = FrontendMacroConstructionService(configuration)

        val sessionsWithSources = buildSessions(
            configuration = configuration,
            rootModuleName = rootModuleName,
            groupedSources = sources.groupedSources,
            classpathRoots = sources.classpathRoots,
            factory = factory,
            sessionFactoryContext = sessionFactoryContext,
            extensionRegistrars = extensionRegistrars,
        )

        val outputs = sessionsWithSources.mapNotNull { (session, sessionSources) ->
            val pre = buildPreMacroFromSources(
                session = session,
                sources = sessionSources,
                environment = environment,
                useLightTree = configuration.useLightTree,
            )
            val (result, output) = resolveAndCheckCfirAfterConstruction(
                session = session,
                pre = pre,
                constructionService = constructionService,
                constructionMode = MacroConstructionService.Mode.STRICT,
                diagnosticsCollector = configuration.diagnosticsCollector,
            )
            if (output == null) {
                reportConstructionFailure(configuration, result)
            }
            output
        }

        return DefaultCfirFrontendPipelineArtifact(
            frontendOutput = AllModulesFrontendOutput(outputs),
            configuration = configuration,
            environment = environment,
            sourceFiles = sources.allSources,
        )
    }

    private data class EnvironmentAndSources(
        val environment: VfsBasedProjectEnvironment,
        val sources: () -> CollectedCjSources,
    )

    private fun createEnvironmentAndSources(
        configuration: CompilerConfiguration,
        rootDisposable: Disposable,
    ): EnvironmentAndSources? {
        return when (configuration.useLightTree) {
            true -> {
                val coreEnvironment = CangJieCoreEnvironment.create(rootDisposable, CangJieCoreEnvironmentMode.Production)
                val projectEnvironment = coreEnvironment.toVfsBasedProjectEnvironment()
                val sources = { collectCjSources(configuration, projectEnvironment) }
                EnvironmentAndSources(projectEnvironment, sources)
            }

            false -> {
                val coreEnvironment = CangJieCoreEnvironment.create(rootDisposable, CangJieCoreEnvironmentMode.Production)
                val projectEnvironment = coreEnvironment.toVfsBasedProjectEnvironment()
                val sources = { collectPsiSources(configuration, projectEnvironment) }
                EnvironmentAndSources(projectEnvironment, sources)
            }
        }.takeUnless { CheckCompilationErrors.CheckDiagnosticCollector.checkHasErrors(configuration) }
    }

    private fun collectPsiSources(
        configuration: CompilerConfiguration,
        environment: VfsBasedProjectEnvironment,
    ): CollectedCjSources {
        val platformSources = linkedSetOf<CjSourceFile>()
        val commonSources = linkedSetOf<CjSourceFile>()
        val sourcesByModuleName = linkedMapOf<String, MutableSet<CjSourceFile>>()

        configuration.cangjieSourceRoots.forAllFiles(configuration, environment.project) { virtualFile, isCommon, moduleName ->
            val psiFile = com.intellij.psi.PsiManager.getInstance(environment.project).findFile(virtualFile) as? CjFile ?: return@forAllFiles
            val sourceFile = CjPsiSourceFile(psiFile)
            if (moduleName == null) {
                if (isCommon) commonSources.add(sourceFile) else platformSources.add(sourceFile)
            } else {
                commonSources.add(sourceFile)
                sourcesByModuleName.getOrPut(moduleName) { linkedSetOf() }.add(sourceFile)
            }
        }

        return CollectedCjSources(
            groupedSources = GroupedCjSources(
                platformSources = platformSources,
                commonSources = commonSources,
                sourcesByModuleName = sourcesByModuleName,
            ),
            classpathRoots = configuration.classpathRoots.map { File(it.path) },
        )
    }

    private fun CangJieCoreEnvironment.toVfsBasedProjectEnvironment(): VfsBasedProjectEnvironment {
        return VfsBasedProjectEnvironment(
            project = project,
            knownFileSystems = listOf(StandardFileSystems.local(), StandardFileSystems.jar()),
        )
    }

    private fun buildSessions(
        configuration: CompilerConfiguration,
        rootModuleName: Name,
        groupedSources: GroupedCjSources,
        classpathRoots: List<File>,
        factory: CfirDefaultSessionFactory,
        sessionFactoryContext: CfirDefaultSessionFactory.Context,
        extensionRegistrars: List<CfirExtensionRegistrar>,
    ): List<SessionWithSources<CjSourceFile>> {
        val classpathPaths = classpathRoots.map { it.absolutePath }
        val moduleGroups = buildModuleGroups(groupedSources, rootModuleName)

        return moduleGroups.flatMap { (moduleName, moduleSources) ->
            val dependencyList = DependencyListForCliModule.build(moduleName) {
                if (classpathPaths.isNotEmpty()) {
                    dependencies(classpathPaths)
                }
            }

            CfirSessionConstructionUtils.prepareSessions(
                files = moduleSources.toList(),
                configuration = configuration,
                rootModuleName = moduleName,
                dependencyList = dependencyList,
                createSharedLibrarySession = {
                    factory.createSharedLibrarySession(
                        mainModuleName = moduleName,
                        extensionRegistrars = extensionRegistrars,
                        languageVersionSettings = configuration.languageVersionSettings,
                        context = sessionFactoryContext,
                    )
                },
                createLibrarySession = { sharedLibrarySession ->
                    factory.createLibrarySession(
                        sharedLibrarySession = sharedLibrarySession,
                        moduleDataProvider = dependencyList.moduleDataProvider,
                        extensionRegistrars = extensionRegistrars,
                        languageVersionSettings = configuration.languageVersionSettings,
                        context = sessionFactoryContext,
                    )
                },
                createSourceSession = CfirSessionProducer { _, moduleData, _, sessionConfigurator ->
                    factory.createSourceSession(
                        moduleData = moduleData,
                        extensionRegistrars = extensionRegistrars,
                        configuration = configuration,
                        context = sessionFactoryContext,
                        init = sessionConfigurator,
                    )
                },
            )
        }
    }

    private fun buildModuleGroups(
        groupedSources: GroupedCjSources,
        rootModuleName: Name,
    ): Map<Name, Set<CjSourceFile>> {
        if (groupedSources.sourcesByModuleName.isEmpty()) {
            return mapOf(rootModuleName to groupedSources.allFiles.toSet())
        }

        val groupedByName = groupedSources.sourcesByModuleName.mapKeys { (name, _) -> Name.identifier(name) }
        val assigned = groupedSources.sourcesByModuleName.values.flatten().toSet()
        val unassignedPlatformSources = groupedSources.platformSources.filterNot { it in assigned }.toSet()

        val rootGroup = unassignedPlatformSources + groupedSources.commonSources
        val result = linkedMapOf<Name, Set<CjSourceFile>>(rootModuleName to rootGroup)
        groupedByName.forEach { (name, sources) ->
            result[name] = sources + groupedSources.commonSources
        }
        return result
    }

    private fun buildPreMacroFromSources(
        session: CfirSession,
        sources: List<CjSourceFile>,
        environment: VfsBasedProjectEnvironment,
        useLightTree: Boolean,
    ): PreMacroRawBuildResult {
        return if (useLightTree) {
            session.buildPreMacroRawCfirViaLightTree(sources)
        } else {
            session.buildPreMacroRawCfirFromCjFiles(sources.toCjFiles(environment))
        }
    }

    /**
     * 安装生产级 token-backed fragment parser。
     *
     * 用户/测试显式配置优先；默认配置只负责把 token-stage re-eval 后的
     * fragment 交回 PSI 或 LightTree raw builder 重新构造 typed CFIR payload。
     */
    private fun CompilerConfiguration.installDefaultMacroFragmentParserFactory(
        environment: VfsBasedProjectEnvironment,
    ) {
        if (macroFragmentParserFactory != null) return

        val project = environment.project
        val useLightTreeParser = useLightTree
        macroFragmentParserFactory = MacroFragmentParserFactory { session ->
            TokenBackedMacroFragmentParser(
                reparse = { text, mode, owner ->
                    if (useLightTreeParser) {
                        reparseLightTreeMacroFragment(session, text, mode, owner)
                    } else {
                        reparsePsiMacroFragment(project, session, text, mode, owner)
                    }
                },
                reTokenize = { tokens -> tokens.reTokenizeMacroSurfaceTokens() },
            )
        }
    }

    private fun reparsePsiMacroFragment(
        project: Project,
        session: CfirSession,
        text: String,
        mode: MacroFragmentParser.Mode,
        owner: MacroCallNode,
    ): Any? {
        val surface = owner.surface
        val packageFqName = surface.scopeContext.packageFqName
        val sourcePsi = (surface.sourceRange?.source as? CjPsiSourceElement)?.psi
        val psiFactory = sourcePsi?.let { CjPsiFactory.contextual(it) } ?: CjPsiFactory(project)
        val builder = PsiRawCfirBuilder(session)

        return when {
            surface is MacroSurfaceParam -> {
                val parameter = psiFactory.createSingleParameter(text) ?: return null
                val original = surface.replaceHandle.carrier as? CfirValueParameter ?: return null
                builder.buildValueParameterInPackage(
                    parameter = parameter,
                    containingSymbol = original.containingDeclarationSymbol,
                    packageFqName = packageFqName,
                )
            }
            mode == MacroFragmentParser.Mode.EXPRESSION -> {
                val expression = psiFactory.createExpressionIfPossible(text) ?: return null
                builder.buildExpressionInPackage(expression, packageFqName)
            }
            else -> {
                val declaration = runCatching {
                    psiFactory.createDeclaration<CjDeclaration>(text)
                }.getOrNull() ?: return null
                builder.buildDeclarationInPackage(declaration, packageFqName)
            }
        }
    }

    private fun reparseLightTreeMacroFragment(
        session: CfirSession,
        text: String,
        mode: MacroFragmentParser.Mode,
        owner: MacroCallNode,
    ): Any? {
        val surface = owner.surface
        val packageFqName = surface.scopeContext.packageFqName
        return when {
            surface is MacroSurfaceParam -> {
                val original = surface.replaceHandle.carrier as? CfirValueParameter ?: return null
                val parsed = parseLightTreeFragment(session, "func __macro_fragment__($text) {}")
                val parameter = parsed.tree.findFirst(CjNodeTypes.VALUE_PARAMETER) ?: return null
                parsed.builder.buildValueParameterInPackage(
                    parameter = parameter,
                    containingSymbol = original.containingDeclarationSymbol,
                    packageFqName = packageFqName,
                )
            }
            mode == MacroFragmentParser.Mode.EXPRESSION -> {
                val parsed = parseLightTreeFragment(session, "let __macro_fragment_value__ =\n$text")
                val variable = parsed.tree.findFirst(CjNodeTypes.VARIABLE) ?: return null
                val declaration = parsed.builder.buildDeclarationInPackage(variable, packageFqName)
                (declaration as? CfirPatternVariable)?.initializer
            }
            else -> {
                val parsed = parseLightTreeFragment(session, text)
                val declaration = parsed.tree.findFirstDeclaration() ?: return null
                parsed.builder.buildDeclarationInPackage(declaration, packageFqName)
            }
        }
    }

    private data class ParsedLightTreeFragment(
        val tree: FlyweightCapableTreeStructure<LighterASTNode>,
        val builder: LightTreeRawCfirDeclarationBuilder,
    )

    private fun parseLightTreeFragment(
        session: CfirSession,
        text: String,
    ): ParsedLightTreeFragment {
        val parserDefinition = CangJieParserDefinition()
        val psiBuilder = PsiBuilderFactory.getInstance().createBuilder(
            parserDefinition,
            CangJieLexer(),
            text,
        )
        val lightTree = CangJieLightParser.parse(psiBuilder)
        return ParsedLightTreeFragment(
            tree = lightTree,
            builder = LightTreeRawCfirDeclarationBuilder(
                session = session,
                baseScopeProvider = session.cangjieScopeProvider,
                tree = lightTree,
                source = text,
            ),
        )
    }

    private fun CjPsiFactory.createSingleParameter(text: String): CjParameter? {
        return runCatching {
            createParameterList("($text)").parameters.singleOrNull()
        }.getOrNull()
    }

    private fun FlyweightCapableTreeStructure<LighterASTNode>.findFirstDeclaration(): LighterASTNode? {
        return findFirst(*fragmentDeclarationTypes)
    }

    private fun FlyweightCapableTreeStructure<LighterASTNode>.findFirst(
        vararg tokenTypes: IElementType,
    ): LighterASTNode? {
        val accepted = tokenTypes.toSet()
        fun visit(node: LighterASTNode): LighterASTNode? {
            if (node.tokenType in accepted) return node
            val childrenRef = Ref<Array<LighterASTNode>>()
            val count = getChildren(node, childrenRef)
            val children = childrenRef.get() ?: LighterASTNode.EMPTY_ARRAY
            try {
                for (index in 0 until count) {
                    visit(children[index])?.let { return it }
                }
            } finally {
                disposeChildren(children, count)
            }
            return null
        }
        return visit(root)
    }

    private val fragmentDeclarationTypes: Array<IElementType> = arrayOf(
        CjNodeTypes.CLASS,
        CjNodeTypes.INTERFACE,
        CjNodeTypes.STRUCT,
        CjNodeTypes.ENUM,
        CjNodeTypes.EXTEND,
        CjNodeTypes.FUNC,
        CjNodeTypes.MAIN_FUNC,
        CjNodeTypes.MACRO,
        CjNodeTypes.FINALIZER,
        CjNodeTypes.PRIMARY_CONSTRUCTOR,
        CjNodeTypes.SECONDARY_CONSTRUCTOR,
        CjNodeTypes.VARIABLE,
        CjNodeTypes.FIELD,
        CjNodeTypes.PROPERTY,
        CjNodeTypes.TYPEALIAS,
        CjNodeTypes.FOREIGN,
    )

    private fun reportConstructionFailure(
        configuration: CompilerConfiguration,
        result: MacroConstructionResult,
    ) {
        val label = when (result) {
            is MacroConstructionResult.Failed -> "Macro construction failed"
            is MacroConstructionResult.ExecutorUnavailable -> "Macro executor unavailable"
            is MacroConstructionResult.Blocked -> "Macro construction blocked"
            is MacroConstructionResult.Success,
            is MacroConstructionResult.Degraded -> return
        }
        val diagnostics = result.registry.diagnostics
            .filter { it.severity == org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionDiagnostic.Severity.ERROR }
        if (diagnostics.isEmpty()) {
            configuration.messageCollector.report(CompilerMessageSeverity.ERROR, "$label: no further details")
            return
        }
        for (diagnostic in diagnostics) {
            val surface = diagnostic.originSurfaceId?.let(result.registry.originSurfaceById::get)
            configuration.messageCollector.report(
                CompilerMessageSeverity.ERROR,
                "$label: ${diagnostic.message}",
                (surface?.sourceRange?.source ?: diagnostic.originSource).toCompilerMessageLocation(),
            )
        }
    }

    private fun CjSourceElement?.toCompilerMessageLocation(): CompilerMessageSourceLocation? {
        val psi = this?.psi ?: return null
        val containingFile = psi.containingFile ?: return null
        val text = containingFile.text ?: return null
        val start = startOffset.coerceIn(0, text.length)
        val end = endOffset.coerceIn(start, text.length)
        val lineStartOffset = text.lastIndexOf('\n', (start - 1).coerceAtLeast(0)).let { if (it < 0) 0 else it + 1 }
        val lineEndOffset = text.indexOf('\n', start).let { if (it < 0) text.length else it }
        val line = text.take(start).count { it == '\n' } + 1
        val column = start - lineStartOffset + 1
        val lineEnd = text.take(end).count { it == '\n' } + 1
        val lineEndStartOffset = text.lastIndexOf('\n', (end - 1).coerceAtLeast(0)).let { if (it < 0) 0 else it + 1 }
        val columnEnd = end - lineEndStartOffset + 1
        return CompilerMessageLocationWithRange.create(
            path = containingFile.virtualFile?.path ?: containingFile.name,
            lineStart = line,
            columnStart = column,
            lineEnd = lineEnd,
            columnEnd = columnEnd,
            lineContent = text.substring(lineStartOffset, lineEndOffset),
        )
    }

    private fun List<CjSourceFile>.toCjFiles(environment: VfsBasedProjectEnvironment): List<CjFile> {
        return mapNotNull { sourceFile ->
            when (sourceFile) {
                is CjPsiSourceFile -> sourceFile.psiFile as? CjFile
                else -> sourceFile.path
                    ?.let { path -> environment.findFileByPath(path) }
                    ?.let { virtualFile -> com.intellij.psi.PsiManager.getInstance(environment.project).findFile(virtualFile) as? CjFile }
            }
        }
    }
}

private fun List<MacroSurfaceToken>.reTokenizeMacroSurfaceTokens(): List<MacroSurfaceToken> {
    val payloadTokens = MacroPayloadTokenizer.tokenize(joinToString(separator = "") { it.text }, baseOffset = 0)
    return payloadTokens.map { token ->
        MacroSurfaceToken(
            text = token.text,
            startOffset = token.startOffset,
            endOffset = token.endOffset,
            kindName = token.kindName,
        )
    }
}
