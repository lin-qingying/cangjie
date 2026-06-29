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
    /**
     * 驱动前端管线的编译器参数。
     */
    val arguments: A,
    /**
     * 与参数绑定的编译器配置。
     */
    override val configuration: CompilerConfiguration,
) : PipelineArtifact() {
    /**
     * 使用新的编译器配置复制参数阶段产物。
     */
    @OptIn(PipelineArtifact.FrontendPipelineInternals::class)
    override fun withCompilerConfiguration(newConfiguration: CompilerConfiguration): PipelineArtifact {
        return copy(configuration = newConfiguration)
    }
}

/**
 * 前端配置产物。
 */
data class ConfigurationPipelineArtifact(
    /**
     * 已初始化的编译器配置。
     */
    override val configuration: CompilerConfiguration,
    /**
     * 前端环境根 disposable。
     */
    val rootDisposable: Disposable,
    
) : PipelineArtifact() {
    /**
     * 使用新的编译器配置复制配置阶段产物。
     */
    @OptIn(PipelineArtifact.FrontendPipelineInternals::class)
    override fun withCompilerConfiguration(newConfiguration: CompilerConfiguration): ConfigurationPipelineArtifact {
        return copy(configuration = newConfiguration)
    }
}

/**
 * 已产出前端结果的管线产物基类。
 */
abstract class FrontendPipelineArtifact : PipelineArtifact() {
    /**
     * 所有模块的前端输出。
     */
    abstract val frontendOutput: AllModulesFrontendOutput

    /**
     * 当前前端产物携带的编译器配置。
     */
    abstract override val configuration: CompilerConfiguration

    /**
     * 使用新的前端输出复制当前产物。
     */
    abstract fun withNewFrontendOutputImpl(newFrontendOutput: AllModulesFrontendOutput): FrontendPipelineArtifact
}

/**
 * 默认 CFIR 前端管线产物。
 */
data class DefaultCfirFrontendPipelineArtifact(
    /**
     * 所有模块的 CFIR 前端输出。
     */
    override val frontendOutput: AllModulesFrontendOutput,
    /**
     * 生成该产物时使用的编译器配置。
     */
    override val configuration: CompilerConfiguration,
    /**
     * 当前前端构建使用的 VFS 项目环境。
     */
    val environment: VfsBasedProjectEnvironment,
    /**
     * 当前前端构建实际处理的源文件列表。
     */
    val sourceFiles: List<CjSourceFile>,
) : FrontendPipelineArtifact() {
    /**
     * 使用新的编译器配置复制 CFIR 前端产物。
     */
    @FrontendPipelineInternals(OPT_IN_MESSAGE)
    override fun withCompilerConfiguration(newConfiguration: CompilerConfiguration): DefaultCfirFrontendPipelineArtifact {
        return copy(configuration = newConfiguration)
    }

    /**
     * 使用新的前端输出复制 CFIR 前端产物。
     */
    override fun withNewFrontendOutputImpl(newFrontendOutput: AllModulesFrontendOutput): FrontendPipelineArtifact {
        return copy(frontendOutput = newFrontendOutput)
    }
}
