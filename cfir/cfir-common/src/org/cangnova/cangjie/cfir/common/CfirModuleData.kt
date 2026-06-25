package org.cangnova.cangjie.cfir.common

import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.platform.TargetPlatform
import org.cangnova.cangjie.platform.isCommon
import org.cangnova.cangjie.utils.shouldNotBeCalled

/**
 * CFIR 模块数据抽象。
 * 仓颉模块系统使用四级 AccessLevel（PRIVATE / INTERNAL / PROTECTED / PUBLIC）控制可见性，
 * 不包含 Kotlin 的 `friend module` 概念；`internal` 的可见性限制在同一 package 内。
 */
abstract class CfirModuleData : CfirSessionComponent {
    /**
     * 模块的逻辑名称。
     */
    abstract val name: Name

    /**
     * 当前模块直接依赖的模块数据。
     */
    abstract val dependencies: List<CfirModuleData>

    /**
     * 当前模块通过 common/refinement 关系依赖的模块数据。
     */
    abstract val refinementDependencies: List<CfirModuleData>

    /**
     * [refinementDependencies] 的传递闭包，按依赖优先的拓扑顺序排列。
     */
    abstract val allRefinementDependencies: List<CfirModuleData>

    /**
     * 前端层面的高层目标平台身份。
     *
     * 对齐 Kotlin FIR 的 `FirModuleData.targetPlatform` 语义，当前仅用于承载
     * `cjnative` / `cjvm` 占位，不替代现有 [platform] 的 native 细节职责。
     */
    abstract val targetPlatform: TargetPlatform

    /**
     * 现有 CFIR 依赖的底层平台细节。
     *
     * 这层目前仍然服务于 native 语义与 OS 相关差异，不能直接拿来表达
     * `cjnative` / `cjvm` 这样的高层后端身份。
     */
    abstract val platform: CfirPlatform

    /**
     * 当前模块是否是 common 模块。
     */
    abstract val isCommon: Boolean

    /**
     * 附加在模块数据上的可选能力集合。
     */
    open val capabilities: CfirModuleCapabilities
        get() = CfirModuleCapabilities.Empty

    /**
     * 当前模块绑定到的 session。
     *
     * 该字段只允许通过 [bindSession] 写入一次，用来保证模块数据和 session 的一一绑定关系。
     */
    protected var boundSession: CfirSession? = null
        private set

    /**
     * 当前模块所属的 session。
     */
    abstract val session: CfirSession

    /**
     * 将当前模块数据绑定到 [session]。
     *
     * 模块数据只能绑定一次；重复绑定表示 session 构造流程破坏了模块所有权不变量。
     */
    fun bindSession(session: CfirSession) {
        if (boundSession != null) {
            error("module data already bound to $this")
        }
        boundSession = session
    }

    /**
     * 返回用于调试输出的模块名称。
     */
    override fun toString(): String = "Module $name"

    /**
     * 用于增量编译或缓存键的稳定模块名称。
     */
    abstract val stableModuleName: String?

    /**
     * 当前模块内的重声明是否应被视为等价声明。
     *
     * 库 session 中的声明来自已编译依赖，允许按库合并语义处理重声明。
     */
    open val areRedeclarationsEquivalent: Boolean
        get() = session.kind == CfirSession.Kind.Library
}

/**
 * 源码模块数据。
 *
 * @property name 模块的逻辑名称。
 * @property dependencies 直接依赖模块。
 * @property refinementDependencies common/refinement 依赖模块。
 * @property targetPlatform 前端高层目标平台。
 * @property platform CFIR 当前保留的底层平台细节。
 * @property isCommon 是否是 common 模块。
 */
class CfirSourceModuleData(
    /** 模块的逻辑名称。 */
    override val name: Name,
    /** 直接依赖模块。 */
    override val dependencies: List<CfirModuleData>,
    /** common/refinement 依赖模块。 */
    override val refinementDependencies: List<CfirModuleData>,
    /** 前端高层目标平台。 */
    override val targetPlatform: TargetPlatform,
    /** CFIR 当前保留的底层平台细节。 */
    override val platform: CfirPlatform,
    /** 是否是 common 模块。 */
    override val isCommon: Boolean = targetPlatform.isCommon(),
) : CfirModuleData() {
    /**
     * 源码模块绑定到的 session。
     */
    override val session: CfirSession
        get() = boundSession ?: sessionNotBoundError()

    /**
     * 源码模块当前没有额外稳定名称。
     */
    override val stableModuleName: String?
        get() = null

    /**
     * refinement 依赖的拓扑排序闭包。
     */
    override val allRefinementDependencies: List<CfirModuleData> =
        topologicallySortedDependsOn(refinementDependencies)
}

/**
 * 二进制依赖模块数据。
 *
 * @property name 合成依赖模块的逻辑名称。
 * @property capabilities 二进制依赖模块携带的能力集合。
 */
class CfirBinaryDependenciesModuleData(
    /** 合成依赖模块的逻辑名称。 */
    override val name: Name,
    /** 二进制依赖模块携带的能力集合。 */
    override val capabilities: CfirModuleCapabilities = CfirModuleCapabilities.Empty,
) : CfirModuleData() {
    /**
     * 合成依赖模块不暴露直接依赖。
     */
    override val dependencies: List<CfirModuleData>
        get() = emptyList()

    /**
     * 合成依赖模块不参与 refinement 依赖。
     */
    override val refinementDependencies: List<CfirModuleData>
        get() = emptyList()

    /**
     * 合成依赖模块的 refinement 闭包始终为空。
     */
    override val allRefinementDependencies: List<CfirModuleData>
        get() = emptyList()

    /**
     * 合成 “all dependencies” 模块没有有意义的高层目标平台。
     */
    override val targetPlatform: TargetPlatform
        get() = shouldNotBeCalled()

    /**
     * 合成 “all dependencies” 模块没有有意义的底层平台。
     */
    override val platform: CfirPlatform
        get() = shouldNotBeCalled()

    /**
     * 合成依赖模块不是 common 模块。
     */
    override val isCommon: Boolean
        get() = false

    /**
     * 二进制依赖模块绑定到的 session。
     */
    override val session: CfirSession
        get() = boundSession ?: sessionNotBoundError()

    /**
     * 合成依赖模块当前没有额外稳定名称。
     */
    override val stableModuleName: String?
        get() = null
}

/**
 * CFIR 当前识别的底层平台类别。
 */
enum class CfirPlatform {
    /**
     * 未指定或默认平台。
     */
    DEFAULT,

    /**
     * OpenHarmony 目标平台。
     */
    OHOS,

    /**
     * Linux 目标平台。
     */
    LINUX,

    /**
     * Windows 目标平台。
     */
    WINDOWS,

    /**
     * macOS 目标平台。
     */
    MACOS,
}

/**
 * 报告模块数据尚未绑定 session 的不变量错误。
 */
private fun CfirModuleData.sessionNotBoundError(): Nothing {
    error("module data ${this::class.simpleName}:$name not bound to session")
}

/**
 * 从 session 中读取可为空的模块数据组件。
 */
val CfirSession.nullableModuleData: CfirModuleData? by CfirSession.nullableSessionComponentAccessor()

/**
 * 从 session 中读取必需的模块数据组件。
 *
 * 未注册模块数据表示 session 构造不完整，调用方应直接失败。
 */
val CfirSession.moduleData: CfirModuleData
    get() = nullableModuleData ?: error("Module data is not registered in $this")

/**
 * 判断当前模块是否可以看到 [otherModule] 的 `internal` 声明。
 * 仓颉的 `internal` 可见性基于包级别，不依赖 `friend module` 机制；
 * 当两个模块相同，或存在 `dependsOn` 关系时，也允许访问。
 */
fun CfirModuleData.canSeeInternalsOf(otherModule: CfirModuleData): Boolean {
    return this == otherModule ||
        otherModule in allRefinementDependencies ||
        this in otherModule.allRefinementDependencies
}

/**
 * 对 refinement 根依赖做拓扑排序并返回传递闭包。
 *
 * 依赖模块总是先于依赖它的模块出现在结果中，便于可见性检查按稳定顺序遍历。
 */
private fun topologicallySortedDependsOn(rootDependencies: List<CfirModuleData>): List<CfirModuleData> {
    val visited = hashSetOf<CfirModuleData>()
    val result = mutableListOf<CfirModuleData>()

    fun visit(moduleData: CfirModuleData) {
        if (!visited.add(moduleData)) return

        for (dependency in moduleData.refinementDependencies) {
            visit(dependency)
        }

        result += moduleData
    }

    for (dependency in rootDependencies) {
        visit(dependency)
    }

    return result
}
