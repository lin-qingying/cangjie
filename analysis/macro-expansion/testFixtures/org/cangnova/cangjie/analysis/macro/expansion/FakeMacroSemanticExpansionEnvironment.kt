package org.cangnova.cangjie.analysis.macro.expansion

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.analysis.api.projectStructure.CaSourceModule
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirResolvableModuleSession
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.macro.expansion.MacroConstructionReadAccess
import org.cangnova.cangjie.macro.expansion.MacroExpansionBarrier
import org.cangnova.cangjie.macro.expansion.MacroExpansionOutcome
import org.cangnova.cangjie.macro.expansion.MacroExpansionRevision
import org.cangnova.cangjie.macro.expansion.MacroSemanticExpansionEngine
import org.cangnova.cangjie.macro.expansion.ModuleRoots
import org.cangnova.cangjie.macro.expansion.SdkHandle

/**
 * 测试宿主环境：所有取值可编程，回调只计数不产生副作用。
 *
 * 默认行为是"无 SDK、无源码根、revision 恒定、无引擎"；用例按需覆盖。
 * 注册方式：[register]，或由测试自行经 `MacroSemanticExpansionEnvironment.registerExtension` 注册。
 */
class FakeMacroSemanticExpansionEnvironment : MacroSemanticExpansionEnvironment {
    /** SDK 取值。 */
    var sdkProvider: (CaSourceModule) -> SdkHandle? = { null }

    /** 模块根目录取值。 */
    var rootsProvider: (CaSourceModule) -> ModuleRoots = { ModuleRoots(emptyList(), emptyList()) }

    /** 输入 revision 取值；默认恒定，用例可替换为可变计数器。 */
    var revisionProvider: (CaSourceModule) -> MacroExpansionRevision = { module ->
        MacroExpansionRevision(
            moduleKey = module.moduleDescription,
            psiModificationCount = 0,
            projectRootModificationCount = 0,
            compilerSettingsModificationCount = 0,
            sdkKey = null,
        )
    }

    /** 逐文件读访问；默认直接执行（headless 用例没有真实 PSI 读取）。 */
    var readAccess: MacroConstructionReadAccess = MacroConstructionReadAccess.Direct

    /** 发布屏障；默认直接执行。 */
    var barrier: MacroExpansionBarrier = ImmediateBarrier

    /** 编译器配置装配。 */
    @OptIn(CompilerConfiguration.Internals::class)
    var configurationProvider: (CaSourceModule, ModuleRoots, SdkHandle?) -> CompilerConfiguration =
        { _, _, _ -> CompilerConfiguration() }

    /** 片段 parser 装配。 */
    var fragmentParserInstaller: (CaSourceModule, CompilerConfiguration) -> Unit = { _, _ -> }

    /** 引擎取值；默认没有可用引擎。 */
    var engineProvider: () -> MacroSemanticExpansionEngine = { MacroSemanticExpansionEngine.Unavailable }

    /** `onConstructionStarted` 调用次数。 */
    var constructionStartedCount: Int = 0
        private set

    /** `onPublished` 调用记录：session -> 文件集合。 */
    val publishedFiles: MutableList<Pair<LLCfirResolvableModuleSession, Set<VirtualFile>>> = mutableListOf()

    /** `onAbandoned` 调用记录：session -> 结果。 */
    val abandonedOutcomes: MutableList<Pair<LLCfirResolvableModuleSession, MacroExpansionOutcome>> = mutableListOf()

    /** 把本环境注册到 [project] 的 `macroExpansionEnvironment` 扩展点。 */
    fun register(project: Project) {
        MacroSemanticExpansionEnvironment.registerExtension(project, this)
    }

    override fun sdk(module: CaSourceModule): SdkHandle? = sdkProvider(module)

    override fun roots(module: CaSourceModule): ModuleRoots = rootsProvider(module)

    override fun revision(module: CaSourceModule): MacroExpansionRevision = revisionProvider(module)

    override fun readAccess(): MacroConstructionReadAccess = readAccess

    override fun barrier(): MacroExpansionBarrier = barrier

    override fun compilerConfiguration(
        module: CaSourceModule,
        roots: ModuleRoots,
        sdk: SdkHandle?,
    ): CompilerConfiguration = configurationProvider(module, roots, sdk)

    override fun installFragmentParser(module: CaSourceModule, configuration: CompilerConfiguration) {
        fragmentParserInstaller(module, configuration)
    }

    override fun engine(): MacroSemanticExpansionEngine = engineProvider()

    override fun onConstructionStarted(session: LLCfirResolvableModuleSession) {
        constructionStartedCount++
    }

    override fun onPublished(session: LLCfirResolvableModuleSession, files: Set<VirtualFile>) {
        publishedFiles += session to files
    }

    override fun onAbandoned(session: LLCfirResolvableModuleSession, outcome: MacroExpansionOutcome) {
        abandonedOutcomes += session to outcome
    }

    /** 直接执行的发布屏障。 */
    private object ImmediateBarrier : MacroExpansionBarrier {
        override fun <T> publish(block: () -> T): T? = block()
    }
}
