package org.cangnova.cangjie.cli.pipeline

import com.intellij.openapi.Disposable
import org.cangnova.cangjie.CjSourceFile
import org.cangnova.cangjie.cli.common.arguments.CommonCompilerArguments
import org.cangnova.cangjie.cli.compiler.VfsBasedProjectEnvironment
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.cfir.pipeline.AllModulesFrontendOutput

/**
 * 鍛戒护琛屽弬鏁颁骇鐗? */
data class ArgumentsPipelineArtifact<out A : CommonCompilerArguments>(
    val arguments: A,
    override val configuration: CompilerConfiguration,
) : PipelineArtifact() {
    @OptIn(PipelineArtifact.CliPipelineInternals::class)
    override fun withCompilerConfiguration(newConfiguration: CompilerConfiguration): PipelineArtifact {
        return copy(configuration = newConfiguration)
    }
}

/**
 * 缂栬瘧閰嶇疆浜х墿
 */
data class ConfigurationPipelineArtifact(
    override val configuration: CompilerConfiguration,
    val rootDisposable: Disposable,

) : PipelineArtifact() {
    @OptIn(PipelineArtifact.CliPipelineInternals::class)
    override fun withCompilerConfiguration(newConfiguration: CompilerConfiguration): ConfigurationPipelineArtifact {
        return copy(configuration = newConfiguration)
    }
}
abstract class FrontendPipelineArtifact : PipelineArtifact() {
    abstract val frontendOutput: AllModulesFrontendOutput
    abstract override val configuration: CompilerConfiguration
    abstract fun withNewFrontendOutputImpl(newFrontendOutput: AllModulesFrontendOutput): FrontendPipelineArtifact
}

data class DefaultCfirFrontendPipelineArtifact(
    override val frontendOutput: AllModulesFrontendOutput,
    override val configuration: CompilerConfiguration,
    val environment: VfsBasedProjectEnvironment,
    val sourceFiles: List<CjSourceFile>,
) : FrontendPipelineArtifact() {
    @CliPipelineInternals(OPT_IN_MESSAGE)
    override fun withCompilerConfiguration(newConfiguration: CompilerConfiguration): DefaultCfirFrontendPipelineArtifact {
        return copy(configuration = newConfiguration)
    }

    override fun withNewFrontendOutputImpl(newFrontendOutput: AllModulesFrontendOutput): FrontendPipelineArtifact {
        return copy(frontendOutput = newFrontendOutput)
    }
}

