package org.cangnova.cangjie.cli.pipeline

import org.cangnova.cangjie.CjInMemoryTextSourceFile
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.lightTree.LightTree2Cfir
import org.cangnova.cangjie.cfir.resolve.providers.CfirProviderImpl
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

object CliMacroConfigurationKeys {
    @JvmField
    val MACRO_EXECUTOR_FACTORY = CompilerConfigurationKey.create<MacroExecutorFactory>("MACRO_EXECUTOR_FACTORY")

    @JvmField
    val MACRO_EXPAND_MAX_ITERATIONS = CompilerConfigurationKey.create<Int>("MACRO_EXPAND_MAX_ITERATIONS")
}

var CompilerConfiguration.macroExecutorFactory: MacroExecutorFactory?
    get() = get(CliMacroConfigurationKeys.MACRO_EXECUTOR_FACTORY)
    set(value) {
        putIfNotNull(CliMacroConfigurationKeys.MACRO_EXECUTOR_FACTORY, value)
    }

var CompilerConfiguration.macroExpandMaxIterations: Int
    get() = get(CliMacroConfigurationKeys.MACRO_EXPAND_MAX_ITERATIONS, 16)
    set(value) {
        put(CliMacroConfigurationKeys.MACRO_EXPAND_MAX_ITERATIONS, value)
    }

object MacroExpandPhase {
    fun expand(
        session: CfirSession,
        files: List<CfirFile>,
        configuration: CompilerConfiguration,
    ): List<CfirFile> {
        val collector = DefaultMacroCollector()
        val macroSites = collector.collect(files)
        if (macroSites.isEmpty()) {
            return files
        }

        val executorFactory = configuration.macroExecutorFactory
        if (executorFactory == null) {
            configuration.messageCollector.report(
                CompilerMessageSeverity.ERROR,
                buildString {
                    append("Macro calls were found, but no macro executor is configured")
                    val names = macroSites.map { it.callInfo.idName }.filter { it.isNotBlank() }.distinct()
                    if (names.isNotEmpty()) {
                        append(": ")
                        append(names.joinToString(", "))
                    }
                },
            )
            return files
        }

        val executor = runCatching { executorFactory.create(session) }.getOrElse { throwable ->
            configuration.messageCollector.report(
                CompilerMessageSeverity.ERROR,
                "Failed to create macro executor: ${throwable.message ?: throwable::class.simpleName}",
            )
            return files
        }

        executor.use {
            val expander = DefaultMacroExpander(
                collector = collector,
                executor = it,
                replacer = DefaultMacroReplacer(CliMacroFileRebuilder(session)),
            )
            val output = expander.expandAll(files, configuration.macroExpandMaxIterations)
            output.diagnostics.forEach { diagnostic ->
                configuration.messageCollector.report(
                    severity = diagnostic.toCompilerSeverity(),
                    message = diagnostic.toDisplayMessage(),
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

    private fun MacroDiagnosticInfo.toDisplayMessage(): String {
        return if (hint.isBlank()) {
            message
        } else {
            "$message\nhint: $hint"
        }
    }
}

private class CliMacroFileRebuilder(
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
