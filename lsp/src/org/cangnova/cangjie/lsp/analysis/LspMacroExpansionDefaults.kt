package org.cangnova.cangjie.lsp.analysis

import org.cangnova.cangjie.analysis.api.projectStructure.CaSourceModule
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.addCangJieSourceRoot
import org.cangnova.cangjie.config.addClasspathRoot
import org.cangnova.cangjie.config.languageVersionSettings
import org.cangnova.cangjie.config.moduleName
import org.cangnova.cangjie.config.targetPlatform
import org.cangnova.cangjie.config.useLightTree
import org.cangnova.cangjie.frontend.pipeline.FrontendMacroSemanticExpansionEngine
import org.cangnova.cangjie.frontend.pipeline.MacroCompilationCacheContext
import org.cangnova.cangjie.frontend.pipeline.MacroExecutorFactory
import org.cangnova.cangjie.frontend.pipeline.installDefaultMacroFragmentParserFactory
import org.cangnova.cangjie.frontend.pipeline.macroCompilationCacheContext
import org.cangnova.cangjie.frontend.pipeline.macroCompilerEnvironment
import org.cangnova.cangjie.frontend.pipeline.macroConstructionMode
import org.cangnova.cangjie.frontend.pipeline.macroConstructionReadAccess
import org.cangnova.cangjie.frontend.pipeline.macroExecutorFactory
import org.cangnova.cangjie.frontend.pipeline.macroExpansionDemandAutoCompilationEnabled
import org.cangnova.cangjie.frontend.pipeline.macroSdkHome
import org.cangnova.cangjie.macro.expansion.MacroConstructionReadAccess
import org.cangnova.cangjie.macro.expansion.ModuleRoots
import org.cangnova.cangjie.macro.expansion.SdkHandle
import org.cangnova.cangjie.macro.process.LspMacroServerMacroExecutor

/**
 * LSP 的进程内宏展开装配。
 *
 * 依据的代码事实：
 * - 完整宏引擎只存在于 `compiler:frontend`：artifact 定位/编译/校验
 *   （`prepareMacroArtifactDefinitionsForExpansion`、`MacroArtifactLocator/Resolver`）、
 *   符号索引、import 绑定、CFIR stable splice 全在该模块；`macro:macro-process` 只提供执行器。
 * - LSP 没有工具链模型：HOME、环境变量、标准库路径、`LSPMacroServer` 路径全部来自宿主传入的
 *   [SdkHandle]，本文件不推导 SDK 目录布局。
 * - LSP 没有宏包编译编排器（`ExternalCjcMacroPackageCompilationOrchestrator` 需要 cjc 命令执行器），
 *   因此显式关闭 demand 自动编译，只消费已编译好的宏 artifact。
 *
 * 通过 [LspMacroExpansionOptions] 注入；未配置时 LSP 不注册 environment，行为与之前一致。
 */
@OptIn(CompilerConfiguration.Internals::class)
fun frontendMacroExpansionOptions(sdk: SdkHandle): LspMacroExpansionOptions =
    LspMacroExpansionOptions(
        engine = FrontendMacroSemanticExpansionEngine(),
        sdkProvider = { sdk },
        compilerConfigurationProvider = { module: CaSourceModule, roots: ModuleRoots, _: SdkHandle? ->
            buildFrontendMacroConfiguration(module, roots, sdk)
        },
        fragmentParserInstaller = { module: CaSourceModule, configuration: CompilerConfiguration ->
            // 与 IDE 相同：片段 parser 属于宿主装配，必须在构造前安装到配置上。
            configuration.installDefaultMacroFragmentParserFactory(module.project)
        },
    )

/**
 * 组装一份可供 `FrontendMacroSemanticExpansionEngine` 使用的编译配置。
 *
 * 只使用 [SdkHandle] 已携带的值；执行器路径为空时保持 `macroExecutorFactory = null`，
 * 展开会得到 `ExecutorUnavailable`，而不是猜测一个不存在的可执行文件。
 */
@OptIn(CompilerConfiguration.Internals::class)
private fun buildFrontendMacroConfiguration(
    module: CaSourceModule,
    roots: ModuleRoots,
    sdk: SdkHandle,
): CompilerConfiguration = CompilerConfiguration().apply {
    languageVersionSettings = module.languageVersionSettings
    targetPlatform = module.targetPlatform
    moduleName = module.name
    useLightTree = false
    macroConstructionMode = org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionService.Mode.DEGRADED
    macroSdkHome = sdk.homePath
    macroCompilerEnvironment = sdk.environment
    // 没有 cjc 宏包编译编排器：只使用已编译 artifact，不假装能自动编译。
    macroExpansionDemandAutoCompilationEnabled = false
    macroCompilationCacheContext = MacroCompilationCacheContext(
        targetPlatform = module.targetPlatform,
        runtimeLoaderEnvironmentFingerprint = "${sdk.id}@${sdk.homePath}",
    )
    macroConstructionReadAccess = MacroConstructionReadAccess.Direct

    roots.sourceRoots.forEach { addCangJieSourceRoot(it) }
    roots.classpathRoots.forEach { addClasspathRoot(it) }
    sdk.stdlibPath?.let { addClasspathRoot(it) }

    sdk.macroServerPath?.let { serverPath ->
        macroExecutorFactory = MacroExecutorFactory {
            LspMacroServerMacroExecutor(
                macroServerPath = serverPath,
                sdkHome = sdk.homePath,
                processEnvironment = sdk.environment,
            )
        }
    }
}
