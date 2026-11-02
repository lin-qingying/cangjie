package org.cangnova.cangjie.macro.expansion

import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionDiagnostic
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionResult
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionService
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroDefinitionEntry
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroDemandClassification
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroExpansionRegistry
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroResolutionContext
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSymbolIndex
import org.cangnova.cangjie.cfir.resolve.providers.macro.PreMacroRawBuildResult
import org.cangnova.cangjie.cfir.resolve.providers.macro.bindMacroImports
import org.cangnova.cangjie.cfir.resolve.providers.macro.buildMacroSymbolIndex
import org.cangnova.cangjie.config.CompilerConfiguration

/**
 * 宏语义展开引擎 SPI。
 *
 * 实现由 `compiler:frontend` 提供（见 `FrontendMacroSemanticExpansionEngine`），
 * 本模块只声明"如何取值、如何展开、如何发布"中的**展开**部分，
 * 不包含任何 Analysis API 类型，避免 `compiler:frontend` 反向依赖 `analysis:*`。
 *
 * 引擎实例由宿主 environment 返回，框架不缓存、不复制引擎状态：
 * 每次构造都按同一个实例调用下列方法，实现必须自行保证无状态或内部同步。
 */
interface MacroSemanticExpansionEngine {
    /**
     * 按真实宏调用 demand 准备外部宏 artifact（编译、定位、resolver 校验）。
     *
     * 该调用可能启动外部进程（`cjc --compile-macro`、LSPMacroServer），
     * 框架保证它发生在 PSI 读访问之外的构造线程上。
     */
    fun prepareArtifacts(
        configuration: CompilerConfiguration,
        classifications: List<MacroDemandClassification>,
    ): MacroExpansionArtifactPreparation

    /** 构建 construction 期离线宏符号索引。 */
    fun buildSymbolIndex(
        pre: PreMacroRawBuildResult,
        artifacts: MacroExpansionArtifactPreparation,
    ): MacroSymbolIndex

    /** 绑定 pre-macro 输入的宏 import，得到 construction 期解析上下文。 */
    fun bindImports(
        pre: PreMacroRawBuildResult,
        index: MacroSymbolIndex,
    ): MacroResolutionContext

    /**
     * 对 pre-macro 输入执行宏展开。
     *
     * @param configuration 本次构造使用的编译器配置（含 executor / 片段 parser / SDK 环境）。
     * @param diagnostics artifact 准备阶段产生的结构化诊断，需并入最终 registry。
     * @param mode 构造模式；Analysis 路径使用 [MacroConstructionService.Mode.DEGRADED]。
     */
    fun expand(
        pre: PreMacroRawBuildResult,
        context: MacroResolutionContext,
        classification: MacroDemandClassification,
        configuration: CompilerConfiguration,
        diagnostics: List<MacroConstructionDiagnostic> = emptyList(),
        mode: MacroConstructionService.Mode = MacroConstructionService.Mode.DEGRADED,
    ): MacroConstructionResult

    /**
     * 没有可用宏执行器的实现。
     *
     * 不准备 artifact、不展开，直接返回 [MacroConstructionResult.ExecutorUnavailable]；
     * 框架据此放弃发布并保持实时视图（与"当前宿主不支持宏"的语义一致）。
     */
    object Unavailable : MacroSemanticExpansionEngine {
        override fun prepareArtifacts(
            configuration: CompilerConfiguration,
            classifications: List<MacroDemandClassification>,
        ): MacroExpansionArtifactPreparation = MacroExpansionArtifactPreparation()

        override fun buildSymbolIndex(
            pre: PreMacroRawBuildResult,
            artifacts: MacroExpansionArtifactPreparation,
        ): MacroSymbolIndex = buildMacroSymbolIndex(pre, macroArtifactDefinitions = artifacts.definitions)

        override fun bindImports(
            pre: PreMacroRawBuildResult,
            index: MacroSymbolIndex,
        ): MacroResolutionContext = bindMacroImports(pre, index)

        override fun expand(
            pre: PreMacroRawBuildResult,
            context: MacroResolutionContext,
            classification: MacroDemandClassification,
            configuration: CompilerConfiguration,
            diagnostics: List<MacroConstructionDiagnostic>,
            mode: MacroConstructionService.Mode,
        ): MacroConstructionResult = MacroConstructionResult.ExecutorUnavailable(MacroExpansionRegistry.EMPTY)
    }
}

/**
 * artifact 准备结果在引擎 SPI 中的投影。
 *
 * 只保留后续阶段真正消费的字段：宏定义供符号索引使用，诊断随构造结果一起发布。
 */
data class MacroExpansionArtifactPreparation(
    /** resolver 校验通过、可用于构造的宏定义。 */
    val definitions: List<MacroDefinitionEntry> = emptyList(),
    /** artifact 定位、即时编译与 resolver 产生的结构化诊断。 */
    val diagnostics: List<MacroConstructionDiagnostic> = emptyList(),
)

/**
 * 宿主无关的 SDK 句柄。
 *
 * Analysis 层不引用工具链对象（`CjSdk` 等）；宿主负责把稳定标识、根目录、
 * 进程环境与标准库路径投影成本类型。
 */
data class SdkHandle(
    /** SDK 稳定标识；同一标识表示同一套工具链。 */
    val id: String,
    /** SDK 根目录。 */
    val homePath: String,
    /** 启动宏编译器 / 宏服务时继承的环境变量。 */
    val environment: Map<String, String> = emptyMap(),
    /** 标准库根目录；参与 classpath 组装，缺失时由宿主自行决定。 */
    val stdlibPath: String? = null,
    /**
     * `LSPMacroServer` 可执行文件路径（含平台后缀）。
     *
     * 由宿主按自己的工具链约定解析后传入；Analysis/LSP 不推导 SDK 目录布局。
     * 为空表示当前宿主没有可用的宏执行器。
     */
    val macroServerPath: String? = null,
)

/**
 * 宿主无关的模块根目录集合。
 *
 * @property sourceRoots 参与宏展开的源码根目录（绝对路径）。
 * @property classpathRoots 依赖库 / 产物的 classpath 根（绝对路径）。
 */
data class ModuleRoots(
    val sourceRoots: List<String>,
    val classpathRoots: List<String>,
)

/**
 * 宏语义快照的输入 revision。
 *
 * 由宿主计算，框架只做**相等比较**：与上次发布/失败的 revision 相同即视为输入未变；
 * 任何不相等都使旧快照失效并重新构造。框架不假设 revision 单调，
 * 因此宿主可以用修改计数、内容指纹或二者组合实现。
 *
 * 覆盖范围必须至少包含 PSI、项目根、编译器设置、SDK 与宏 artifact 指纹；
 * 禁止用全局项目级计数代替模块级输入。
 */
data class MacroExpansionRevision(
    /** 模块稳定键；不同模块的 revision 不可比较。 */
    val moduleKey: String,
    /** 模块输入 PSI 的修改计数。 */
    val psiModificationCount: Long,
    /** 项目根（源码根 / classpath 根）修改计数。 */
    val projectRootModificationCount: Long,
    /** 编译器设置修改计数。 */
    val compilerSettingsModificationCount: Long,
    /** SDK 稳定标识；替换 SDK 必须使 revision 变化。 */
    val sdkKey: String?,
    /** 宏 artifact 内容指纹；无法计算时使用稳定占位值。 */
    val artifactFingerprint: String = NO_ARTIFACT_FINGERPRINT,
) {
    companion object {
        /** 宿主无法计算 artifact 指纹时使用的稳定占位值。 */
        const val NO_ARTIFACT_FINGERPRINT: String = "macro-artifacts:unspecified"
    }
}

/** 一次宏语义构造的最终结果，宿主只用于日志 / 遥测 / 收尾回调。 */
enum class MacroExpansionOutcome {
    /** 已发布新的宏语义快照。 */
    PUBLISHED,

    /** 模块没有宏调用 surface；保持实时视图，不发布快照。 */
    NO_MACRO_SURFACE,

    /** 构造失败（引擎异常、artifact 不可用、快照准备失败等）。 */
    FAILED,

    /** 会话失效、项目关闭或线程中断导致构造取消。 */
    CANCELLED,

    /** 构造期间输入 revision 变化，本次结果被丢弃。 */
    INPUT_CHANGED,
}

/**
 * 发布屏障：把"切换语义视图 + 缓存失效"放进宿主提供的原子边界。
 *
 * IDE 用 `invokeAndWait + WriteAction` 实现，LSP / standalone 用容器实际能力实现。
 * 这是框架唯一允许触碰写访问的地方；实现返回 `null` 表示本次发布被放弃
 * （例如屏障执行前输入已变化），框架按 [MacroExpansionOutcome.INPUT_CHANGED] 处理。
 */
interface MacroExpansionBarrier {
    /** 在宿主写屏障内执行 [block]；屏障拒绝执行时返回 `null`。 */
    fun <T> publish(block: () -> T): T?
}
