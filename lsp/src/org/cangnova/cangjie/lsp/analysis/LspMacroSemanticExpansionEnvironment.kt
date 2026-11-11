package org.cangnova.cangjie.lsp.analysis

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibraryModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaSourceModule
import org.cangnova.cangjie.analysis.api.platform.modification.CaModificationTracker
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirResolvableModuleSession
import org.cangnova.cangjie.analysis.macro.expansion.MacroSemanticExpansionEnvironment
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.macro.expansion.MacroConstructionReadAccess
import org.cangnova.cangjie.macro.expansion.MacroExpansionBarrier
import org.cangnova.cangjie.macro.expansion.MacroExpansionOutcome
import org.cangnova.cangjie.macro.expansion.MacroExpansionRevision
import org.cangnova.cangjie.macro.expansion.MacroSemanticExpansionEngine
import org.cangnova.cangjie.macro.expansion.ModuleRoots
import org.cangnova.cangjie.macro.expansion.SdkHandle
import org.cangnova.cangjie.name.FqName
import java.util.Collections
import java.util.IdentityHashMap

/**
 * LSP 侧宏语义展开的产品接入点。
 *
 * LSP 核心模块**不依赖** `compiler:frontend`：引擎、SDK 与 `CompilerConfiguration` 装配
 * 由产品壳在启动时通过本类型注入（见计划开放问题 1）。未提供本类型时 LSP 不注册 environment，
 * 行为与未接入宏展开一致。
 */
data class LspMacroExpansionOptions(
    /** 宏展开引擎；产品壳可选择进程内 `FrontendMacroSemanticExpansionEngine` 或其它实现。 */
    val engine: MacroSemanticExpansionEngine,
    /** 构造一份可展开的编译器配置（包含执行器 / 片段 parser / 宏 SDK 环境）。 */
    val compilerConfigurationProvider: (CaSourceModule, ModuleRoots, SdkHandle?) -> CompilerConfiguration,
    /** SDK 取值；默认没有工具链。 */
    val sdkProvider: (CaSourceModule) -> SdkHandle? = { null },
    /** 宿主相关片段 parser 装配。 */
    val fragmentParserInstaller: (CaSourceModule, CompilerConfiguration) -> Unit = { _, _ -> },
    /** 宿主已知可用的外部宏包；用于 import 候选的 demand 允许集合。 */
    val externalMacroPackagesProvider: (CaSourceModule) -> Set<FqName> = { emptySet() },
    /** 快照发布后的刷新钩子（diagnostics / semantic tokens）。 */
    val publishListener: (LLCfirResolvableModuleSession, Set<VirtualFile>) -> Unit = { _, _ -> },
    /** 已发布快照因输入变化而失效后的刷新钩子。 */
    val invalidationListener: (CaSourceModule) -> Unit = {},
    /** 构造开始 / 放弃钩子，供遥测或宿主状态展示。 */
    val constructionStartedListener: (LLCfirResolvableModuleSession) -> Unit = {},
    val abandonedListener: (LLCfirResolvableModuleSession, MacroExpansionOutcome) -> Unit = { _, _ -> },
)

/**
 * LSP 宿主的宏语义展开环境。
 *
 * 只负责 LSP 特有的取值与屏障：
 * - roots 来自 [CaSourceModule] 的 PSI 根与依赖库二进制根；
 * - revision 来自 LSP 的 [CaModificationTracker]（见 [revision] 的事实说明）；
 * - 读访问直接执行：LSP 现有分析路径（`CangjieAnalysisFacade` 及其实现）没有任何 `ReadAction`
 *   包装，全部在请求线程直接读写 PSI；因此这里沿用 [MacroConstructionReadAccess.Direct]，
 *   不额外引入 LSP 运行时并不存在的读锁语义；
 * - 发布屏障使用与本容器实测可用的 `invokeAndWait + WriteAction`（见
 *   `MacroExpansionHostContainerProbeTest`）。
 *
 * 引擎、SDK 与编译器配置装配全部来自 [options]，因此本类不引入 `compiler:frontend` 依赖。
 */
class LspMacroSemanticExpansionEnvironment(
    private val project: Project,
    options: LspMacroExpansionOptions,
) : MacroSemanticExpansionEnvironment {
    @Volatile
    private var options: LspMacroExpansionOptions = options

    /** 保留产品壳回调，并叠加语言服务器所需的诊断刷新回调。 */
    @Synchronized
    internal fun addLifecycleListeners(
        publishListener: (LLCfirResolvableModuleSession, Set<VirtualFile>) -> Unit,
        invalidationListener: (CaSourceModule) -> Unit,
        abandonedListener: (LLCfirResolvableModuleSession, MacroExpansionOutcome) -> Unit,
    ) {
        val existing = options
        options = existing.copy(
            publishListener = { session, files ->
                try {
                    existing.publishListener(session, files)
                } finally {
                    publishListener(session, files)
                }
            },
            invalidationListener = { module ->
                try {
                    existing.invalidationListener(module)
                } finally {
                    invalidationListener(module)
                }
            },
            abandonedListener = { session, outcome ->
                try {
                    existing.abandonedListener(session, outcome)
                } finally {
                    abandonedListener(session, outcome)
                }
            },
        )
    }

    override fun sdk(module: CaSourceModule): SdkHandle? = options.sdkProvider(module)

    override fun externalMacroPackages(module: CaSourceModule): Set<FqName> =
        options.externalMacroPackagesProvider(module)

    override fun roots(module: CaSourceModule): ModuleRoots = collectRoots(module)

    /**
     * 输入 revision。
     *
     * 事实依据（`AnalysisApiLspProjectStructureState`）：
     * - 模块计数与全局计数只在 `invalidate(modules)` 中递增；
     * - `configure(workspaceState)` 在文档打开/修改/保存/关闭与工作区变化时重建模块，
     *   并对 `previousModules + currentModules` 调用 `invalidate`，因此其中任一变化都会同时
     *   递增全局计数与相应模块计数，且模块对象会被重建；
     * - LSP 没有独立的"编译器设置计数"；语言/模块设置变化会走同一条 `configure` 路径。
     *
     * 因此这里使用模块计数作为 `psiModificationCount`、全局计数作为 `projectRootModificationCount`，
     * `compilerSettingsModificationCount` 固定为 0 是安全的（设置变化已由前两者覆盖）。
     * 这不是"模块级隔离"：当前 LSP 的任何文档变化都会让所有模块失效，正确性优先，
     * 后续若要细化，需要给 LSP 增加按文件/按模块的独立计数。
     */
    override fun revision(module: CaSourceModule): MacroExpansionRevision {
        val tracker = CaModificationTracker.getInstance(project)
        return MacroExpansionRevision(
            moduleKey = module.moduleDescription,
            psiModificationCount = tracker?.getModuleModificationCount(module) ?: 0L,
            projectRootModificationCount = tracker?.modificationCount ?: 0L,
            compilerSettingsModificationCount = 0L,
            sdkKey = options.sdkProvider(module)?.id,
        )
    }

    override fun readAccess(): MacroConstructionReadAccess = MacroConstructionReadAccess.Direct

    override fun barrier(): MacroExpansionBarrier = LspMacroExpansionBarrier

    override fun compilerConfiguration(
        module: CaSourceModule,
        roots: ModuleRoots,
        sdk: SdkHandle?,
    ): CompilerConfiguration = options.compilerConfigurationProvider(module, roots, sdk)

    override fun installFragmentParser(module: CaSourceModule, configuration: CompilerConfiguration) {
        options.fragmentParserInstaller(module, configuration)
    }

    override fun engine(): MacroSemanticExpansionEngine = options.engine

    override fun onConstructionStarted(session: LLCfirResolvableModuleSession) {
        options.constructionStartedListener(session)
    }

    override fun onPublished(session: LLCfirResolvableModuleSession, files: Set<VirtualFile>) {
        options.publishListener(session, files)
    }

    override fun onSnapshotInvalidated(module: CaSourceModule) {
        options.invalidationListener(module)
    }

    override fun onAbandoned(session: LLCfirResolvableModuleSession, outcome: MacroExpansionOutcome) {
        options.abandonedListener(session, outcome)
    }

    /** 收集模块及其直接依赖的源码根 / classpath 根。 */
    private fun collectRoots(sourceModule: CaSourceModule): ModuleRoots {
        val visited = Collections.newSetFromMap(IdentityHashMap<CaModule, Boolean>())
        val pending = ArrayDeque<CaModule>().apply { add(sourceModule) }
        val sourceRoots = linkedSetOf<String>()
        val classpathRoots = linkedSetOf<String>()

        while (pending.isNotEmpty()) {
            val module = pending.removeFirst()
            if (!visited.add(module)) continue

            when (module) {
                is CaSourceModule -> {
                    /*
                     * LSP 的 psiRoots 是源码**文件**集合（见 CaLspSourceModule），不是目录；
                     * 宏构造的 sourceRoots 必须是目录，因此优先用模块显式保存的源码根，
                     * 只有非 LSP 模块实现才回退到"psiRoots 中的目录"。
                     */
                    val explicitRoots = (module as? CaLspSourceModule)?.sourceRoots
                    if (explicitRoots != null) {
                        explicitRoots.mapTo(sourceRoots) { it.toString() }
                    } else {
                        module.psiRoots
                            .filter { it.isDirectory }
                            .mapNotNull { it.virtualFile }
                            .mapTo(sourceRoots) { it.path }
                    }
                }

                is CaLibraryModule -> module.binaryRoots.mapNotNull { it.virtualFile }
                    .filter { it.isValid }
                    .mapTo(classpathRoots) { virtualFile ->
                        if (virtualFile.isDirectory) virtualFile.path else virtualFile.parent?.path ?: virtualFile.path
                    }
            }

            pending.addAll(module.allDirectDependencies)
        }

        return ModuleRoots(sourceRoots.toList(), classpathRoots.toList())
    }

    /**
     * LSP 发布屏障：与本容器实测可用的 `invokeAndWait + WriteAction` 形态一致。
     *
     * 返回 `null` 表示屏障被放弃（例如容器不可用），框架按输入已变处理。
     */
    private object LspMacroExpansionBarrier : MacroExpansionBarrier {
        override fun <T> publish(block: () -> T): T? {
            var result: T? = null
            ApplicationManager.getApplication().invokeAndWait(
                Runnable { result = WriteAction.compute<T?, RuntimeException> { block() } },
            )
            return result
        }
    }
}
