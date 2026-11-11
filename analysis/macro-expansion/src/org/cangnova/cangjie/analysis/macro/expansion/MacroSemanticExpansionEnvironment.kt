package org.cangnova.cangjie.analysis.macro.expansion

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.analysis.api.projectStructure.CaSourceModule
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirResolvableModuleSession
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.extensions.ProjectExtensionDescriptor
import org.cangnova.cangjie.macro.expansion.MacroConstructionReadAccess
import org.cangnova.cangjie.macro.expansion.MacroExpansionBarrier
import org.cangnova.cangjie.macro.expansion.MacroExpansionOutcome
import org.cangnova.cangjie.macro.expansion.MacroExpansionRevision
import org.cangnova.cangjie.macro.expansion.MacroSemanticExpansionEngine
import org.cangnova.cangjie.macro.expansion.ModuleRoots
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.macro.expansion.SdkHandle

/**
 * 宿主环境 SPI：只提供"值的来源"与"屏障/回调"，不承载语义决策。
 *
 * 扩展点 `org.cangnova.cangjie.macroExpansionEnvironment` 由本模块声明；
 * IDE、LSP、standalone / 测试各自注册一份实现。没有注册环境时，
 * [MacroSemanticSessionConfigurator] 不触发任何构造，行为与未接入宏展开一致。
 *
 * 硬性约束：
 * - 本接口引用 [LLCfirResolvableModuleSession]，因此放在 Analysis 层，
 *   不能下沉到 `macro:macro-common`；
 * - 环境实现不得决定"何时构造、何时发布"，这些决策属于
 *   [MacroSemanticExpansionService] 与 [MacroSemanticExpansionConstruction]。
 */
interface MacroSemanticExpansionEnvironment {
    /** 当前模块使用的 SDK；返回 `null` 表示没有可用工具链。 */
    fun sdk(module: CaSourceModule): SdkHandle?

    /** 当前模块的源码根与 classpath 根。 */
    fun roots(module: CaSourceModule): ModuleRoots

    /**
     * 当前模块的输入 revision。
     *
     * 必须可比较（`equals`），且至少覆盖 PSI、roots、编译器设置、SDK 与宏 artifact 指纹；
     * 宿主不得自建第二套 tracker，也不得用全局项目级计数代替模块级输入。
     */
    fun revision(module: CaSourceModule): MacroExpansionRevision

    /** 逐文件读访问（PSI / 解析器读取），实现可以是 IntelliJ ReadAction。 */
    fun readAccess(): MacroConstructionReadAccess

    /**
     * 发布屏障：唯一允许触碰写访问的地方。
     *
     * 实现必须保证"发布快照 + 失效 raw CFIR / file structure"对其他读者原子可见。
     */
    fun barrier(): MacroExpansionBarrier

    /**
     * 含宏 SDK、执行器、宏包编译编排的编译器配置。
     *
     * 实现负责把 [sdk] 投影为宏编译器环境、`LSPMacroServer` 路径与标准库 classpath，
     * 并把 [MacroConstructionReadAccess] 写入配置（构造期 PSI 短读访问）。
     */
    fun compilerConfiguration(
        module: CaSourceModule,
        roots: ModuleRoots,
        sdk: SdkHandle?,
    ): CompilerConfiguration

    /** 宿主相关的片段 parser 装配。 */
    fun installFragmentParser(module: CaSourceModule, configuration: CompilerConfiguration)

    /** 宏展开引擎；所有权唯一，由 environment 返回。 */
    fun engine(): MacroSemanticExpansionEngine

    /**
     * 宿主已知可用的外部宏包。
     *
     * import 候选只有落在这些包、同项目 macro package 或官方 stdlib 宏包时，
     * 才会触发 artifact preparation。宿主可从依赖模型 / SDK 提供；默认空集。
     */
    fun externalMacroPackages(module: CaSourceModule): Set<FqName> = emptySet()

    /** 一次构造开始（同一 session 每次构造恰好一次）。 */
    fun onConstructionStarted(session: LLCfirResolvableModuleSession)

    /**
     * 快照已发布（或复用了其他会话已发布的快照）。
     *
     * [files] 是需要重新分析源码文件；复用路径传入空集合，宿主仍应结束本次构造。
     */
    fun onPublished(session: LLCfirResolvableModuleSession, files: Set<VirtualFile>)

    /** 已发布快照因输入 revision 变化而失效。 */
    fun onSnapshotInvalidated(module: CaSourceModule) {}

    /** 本次构造未发布快照；[outcome] 说明原因。 */
    fun onAbandoned(session: LLCfirResolvableModuleSession, outcome: MacroExpansionOutcome)

    companion object : ProjectExtensionDescriptor<MacroSemanticExpansionEnvironment>(
        "org.cangnova.cangjie.macroExpansionEnvironment",
        MacroSemanticExpansionEnvironment::class.java,
    ) {
        /** 读取工程内已注册的环境；没有注册时返回 `null`。 */
        fun getInstance(project: Project): MacroSemanticExpansionEnvironment? =
            getInstances(project).firstOrNull()
    }
}
