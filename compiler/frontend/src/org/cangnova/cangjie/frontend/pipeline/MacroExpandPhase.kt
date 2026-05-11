package org.cangnova.cangjie.frontend.pipeline

import org.cangnova.cangjie.CjInMemoryTextSourceFile
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.lightTree.LightTree2Cfir
import org.cangnova.cangjie.cfir.resolve.providers.CfirProviderImpl
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionDiagnostic
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionResult
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionService
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroDefinitionEntry
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroExpansionRegistry
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroResolutionContext
import org.cangnova.cangjie.cfir.resolve.providers.macro.PreMacroRawBuildResult
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.CompilerConfigurationKey
import org.cangnova.cangjie.config.messageCollector
import org.cangnova.cangjie.macro.DefaultMacroCollector
import org.cangnova.cangjie.macro.DefaultMacroExpander
import org.cangnova.cangjie.macro.DefaultMacroReplacer
import org.cangnova.cangjie.macro.MacroDiagnosticInfo
import org.cangnova.cangjie.macro.MacroDiagnosticSeverity
import org.cangnova.cangjie.macro.MacroExecutor
import org.cangnova.cangjie.macro.MacroFileRebuilder
import org.cangnova.cangjie.messages.CompilerMessageSeverity
import org.cangnova.cangjie.source.toSourceLinesMapping

fun interface MacroExecutorFactory {
    fun create(session: CfirSession): MacroExecutor
}

/**
 * 前端宏展开配置键。
 */
object FrontendMacroConfigurationKeys {
    @JvmField
    val MACRO_EXECUTOR_FACTORY = CompilerConfigurationKey.create<MacroExecutorFactory>("MACRO_EXECUTOR_FACTORY")

    @JvmField
    val MACRO_EXPAND_MAX_ITERATIONS = CompilerConfigurationKey.create<Int>("MACRO_EXPAND_MAX_ITERATIONS")
}

var CompilerConfiguration.macroExecutorFactory: MacroExecutorFactory?
    get() = get(FrontendMacroConfigurationKeys.MACRO_EXECUTOR_FACTORY)
    set(value) {
        putIfNotNull(FrontendMacroConfigurationKeys.MACRO_EXECUTOR_FACTORY, value)
    }

var CompilerConfiguration.macroExpandMaxIterations: Int
    get() = get(FrontendMacroConfigurationKeys.MACRO_EXPAND_MAX_ITERATIONS, 16)
    set(value) {
        put(FrontendMacroConfigurationKeys.MACRO_EXPAND_MAX_ITERATIONS, value)
    }

/**
 * 兼容包装：把现有 [DefaultMacroExpander]（text-patch + 全文件重建）作为
 * macro construction step 的暂态实现。
 *
 * Batch 1 阶段：保留语义不变，只做"接口对接"。
 * Batch 4 起将引入真正的 PreMacro / MacroSurface 模型，
 * Batch 8 起替换为 token+fragment 路径，并在 Batch 10 彻底删除该实现。
 *
 * STRICT vs DEGRADED 行为：
 * - 当 executor 缺失或展开整体失败时：
 *   - STRICT 返回 [MacroConstructionResult.Failed]（CLI 不应继续）；
 *   - DEGRADED 返回 [MacroConstructionResult.Degraded]（保留原 raw 文件 + 诊断）。
 * - 当展开成功时：返回 [MacroConstructionResult.Success]，
 *   `recordableFiles` 由 [DefaultMacroExpander] 产出的 file list 装配而成。
 */
class FrontendMacroConstructionService(
    private val configuration: CompilerConfiguration,
) : MacroConstructionService {
    override fun expand(
        pre: PreMacroRawBuildResult,
        context: MacroResolutionContext,
        mode: MacroConstructionService.Mode,
    ): MacroConstructionResult {
        val session = pre.session
        val registry = MacroExpansionRegistry()
        val rawFiles = pre.files.map { it.cfirFile }

        // baseline 第 4 节："官方同包 macro def/call 禁止"。
        // 即便 expander 把展开做了出来，同包 def/call 也应当被诊断为非法。
        reportSamePackageMacroDefinitions(pre, context, registry)

        // baseline 第 12 节 Batch 5："alias conflict / macro package / ..."。
        reportAliasConflicts(context, registry)

        val output = MacroExpandPhase.expandAndCollect(session, rawFiles, configuration, registry)

        // 当 expandAndCollect 产生 ERROR 级 registry 诊断、且 mode == STRICT 时，
        // 视为 construction 失败：返回 Failed，由调用方决定是否进入 resolve。
        return if (registry.hasErrors && mode == MacroConstructionService.Mode.STRICT) {
            MacroConstructionResult.Failed(registry)
        } else if (registry.hasErrors) {
            // DEGRADED 模式：保留原 raw 文件 + 诊断 registry
            MacroConstructionService.degradedOf(pre, output, registry)
        } else {
            MacroConstructionService.successOf(pre, output, registry)
        }
    }

    /**
     * 报告"同包 macro def + call"非法形态。
     *
     * 这是 [MacroSymbolIndex][org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSymbolIndex]
     * 的产出之一：source 包内 `macro func Foo` 同时出现 `@Foo` 调用是非法的，
     * 必须以独立 macro package 提供方为目标。
     */
    private fun reportSamePackageMacroDefinitions(
        pre: PreMacroRawBuildResult,
        context: MacroResolutionContext,
        registry: MacroExpansionRegistry,
    ) {
        val sourceMacros: List<MacroDefinitionEntry> = context.symbolIndex.sources
        if (sourceMacros.isEmpty()) return

        val collector = DefaultMacroCollector()
        val callSites = collector.collect(pre.files.map { it.cfirFile })

        for (callSite in callSites) {
            val callPackageString = callSite.callInfo.packageName
            val callPackage = if (callPackageString.isBlank()) {
                org.cangnova.cangjie.name.FqName.ROOT
            } else {
                org.cangnova.cangjie.name.FqName.fromString(callPackageString)
            }
            val callName = org.cangnova.cangjie.name.Name.identifier(callSite.callInfo.idName)
            val sameDef = context.symbolIndex.samePackageMacroDef(callPackage, callName) ?: continue
            val message = buildString {
                append("Macro call `@")
                append(sameDef.name.asString())
                append("` cannot resolve to a macro definition declared in the same package `")
                append(if (callPackage.isRoot) "<root>" else callPackage.asString())
                append("`; macros must be provided by a separate macro package, artifact, or builtin.")
            }
            configuration.messageCollector.report(CompilerMessageSeverity.ERROR, message)
            registry.addDiagnostic(
                MacroConstructionDiagnostic(MacroConstructionDiagnostic.Severity.ERROR, message)
            )
        }
    }

    /**
     * 报告 alias 冲突（同一短名绑到多个 fqn）。
     */
    private fun reportAliasConflicts(
        context: MacroResolutionContext,
        registry: MacroExpansionRegistry,
    ) {
        for (conflict in context.aliasConflicts) {
            val message = buildString {
                append("Macro import alias `")
                append(conflict.alias.asString())
                append("` is bound to multiple targets: ")
                append(conflict.targets.joinToString(", ") { it.asString() })
            }
            configuration.messageCollector.report(CompilerMessageSeverity.ERROR, message)
            registry.addDiagnostic(
                MacroConstructionDiagnostic(MacroConstructionDiagnostic.Severity.ERROR, message)
            )
        }
    }
}

object MacroExpandPhase {
    /**
     * 兼容旧调用方的入口。
     *
     * 内部把展开诊断转发到 [CompilerConfiguration.messageCollector]，
     * 保留与原行为一致的"无 executor 报错但返回原文件"逻辑。
     */
    fun expand(
        session: CfirSession,
        files: List<CfirFile>,
        configuration: CompilerConfiguration,
    ): List<CfirFile> {
        val registry = MacroExpansionRegistry()
        return expandAndCollect(session, files, configuration, registry)
    }

    /**
     * 共用展开实现：同时填充 [MacroExpansionRegistry]，
     * 供 [FrontendMacroConstructionService] 做 construction 结果决策。
     */
    internal fun expandAndCollect(
        session: CfirSession,
        files: List<CfirFile>,
        configuration: CompilerConfiguration,
        registry: MacroExpansionRegistry,
    ): List<CfirFile> {
        val collector = DefaultMacroCollector()
        val macroSites = collector.collect(files)
        if (macroSites.isEmpty()) {
            return files
        }

        val executorFactory = configuration.macroExecutorFactory
        if (executorFactory == null) {
            val message = buildString {
                append("Macro calls were found, but no macro executor is configured")
                val names = macroSites.map { it.callInfo.idName }.filter { it.isNotBlank() }.distinct()
                if (names.isNotEmpty()) {
                    append(": ")
                    append(names.joinToString(", "))
                }
            }
            configuration.messageCollector.report(CompilerMessageSeverity.ERROR, message)
            registry.addDiagnostic(
                MacroConstructionDiagnostic(MacroConstructionDiagnostic.Severity.ERROR, message)
            )
            return files
        }

        val executor = runCatching { executorFactory.create(session) }.getOrElse { throwable ->
            val message = "Failed to create macro executor: ${throwable.message ?: throwable::class.simpleName}"
            configuration.messageCollector.report(CompilerMessageSeverity.ERROR, message)
            registry.addDiagnostic(
                MacroConstructionDiagnostic(MacroConstructionDiagnostic.Severity.ERROR, message)
            )
            return files
        }

        executor.use {
            val expander = DefaultMacroExpander(
                collector = collector,
                executor = it,
                replacer = DefaultMacroReplacer(FrontendMacroFileRebuilder(session)),
            )
            val output = expander.expandAll(files, configuration.macroExpandMaxIterations)
            output.diagnostics.forEach { diagnostic ->
                val severity = diagnostic.toCompilerSeverity()
                val display = diagnostic.toDisplayMessage()
                configuration.messageCollector.report(severity, display)
                registry.addDiagnostic(
                    MacroConstructionDiagnostic(diagnostic.toRegistrySeverity(), display)
                )
            }
            return output.files
        }
    }

    private fun MacroDiagnosticInfo.toCompilerSeverity(): CompilerMessageSeverity {
        return when (severity) {
            MacroDiagnosticSeverity.INFO -> CompilerMessageSeverity.INFO
            MacroDiagnosticSeverity.WARNING -> CompilerMessageSeverity.WARNING
            else -> CompilerMessageSeverity.ERROR
        }
    }

    private fun MacroDiagnosticInfo.toRegistrySeverity(): MacroConstructionDiagnostic.Severity {
        return when (severity) {
            MacroDiagnosticSeverity.INFO -> MacroConstructionDiagnostic.Severity.INFO
            MacroDiagnosticSeverity.WARNING -> MacroConstructionDiagnostic.Severity.WARNING
            else -> MacroConstructionDiagnostic.Severity.ERROR
        }
    }

    private fun MacroDiagnosticInfo.toDisplayMessage(): String {
        return if (hint.isBlank()) {
            message
        } else {
            "$message\nhint: $hint"
        }
    }
}

private class FrontendMacroFileRebuilder(
    session: CfirSession,
) : MacroFileRebuilder {
    private val lightTreeBuilder = LightTree2Cfir(
        session = session,
        scopeProvider = (session.cfirProvider as CfirProviderImpl).cangjieScopeProvider,
    )

    override fun rebuild(originalFile: CfirFile, updatedText: String): CfirFile {
        val originalSourceFile = originalFile.sourceFile
        val rebuiltSourceFile = CjInMemoryTextSourceFile(
            name = originalSourceFile?.name ?: originalFile.name,
            path = originalSourceFile?.path,
            text = updatedText,
        )
        return lightTreeBuilder.buildCfirFile(
            code = updatedText,
            sourceFile = rebuiltSourceFile,
            linesMapping = updatedText.toSourceLinesMapping(),
        )
    }
}
