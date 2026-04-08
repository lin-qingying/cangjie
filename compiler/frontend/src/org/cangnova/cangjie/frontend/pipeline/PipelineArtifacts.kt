package org.cangnova.cangjie.frontend.pipeline

import com.intellij.openapi.Disposable
import org.cangnova.cangjie.CjSourceFile
import org.cangnova.cangjie.cfir.pipeline.AllModulesFrontendOutput
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.frontend.arguments.CommonCompilerArguments
import org.cangnova.cangjie.frontend.environment.VfsBasedProjectEnvironment

/**
 * 前端参数产物。
 */
data class ArgumentsPipelineArtifact<out A : CommonCompilerArguments>(
    val arguments: A,
    override val configuration: CompilerConfiguration,
) : PipelineArtifact() {
    @OptIn(PipelineArtifact.FrontendPipelineInternals::class)
    override fun withCompilerConfiguration(newConfiguration: CompilerConfiguration): PipelineArtifact {
        return copy(configuration = newConfiguration)
    }
}

/**
 * 前端配置产物。
 */
data class ConfigurationPipelineArtifact(
    override val configuration: CompilerConfiguration,
    val rootDisposable: Disposable,
    
) : PipelineArtifact() {
    @OptIn(PipelineArtifact.FrontendPipelineInternals::class)
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
    @FrontendPipelineInternals(OPT_IN_MESSAGE)
    override fun withCompilerConfiguration(newConfiguration: CompilerConfiguration): DefaultCfirFrontendPipelineArtifact {
        return copy(configuration = newConfiguration)
    }

    override fun withNewFrontendOutputImpl(newFrontendOutput: AllModulesFrontendOutput): FrontendPipelineArtifact {
        return copy(frontendOutput = newFrontendOutput)
    }
}
