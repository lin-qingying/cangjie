package org.cangnova.cangjie.cfir.common

import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.utils.shouldNotBeCalled

/**
 * CFIR 模块数据抽象。
 * 仓颉模块系统使用四级 AccessLevel（PRIVATE / INTERNAL / PROTECTED / PUBLIC）控制可见性，
 * 不包含 Kotlin 的 `friend module` 概念；`internal` 的可见性限制在同一 package 内。
 */
abstract class CfirModuleData : CfirSessionComponent {
    abstract val name: Name
    abstract val dependencies: List<CfirModuleData>
    abstract val refinementDependencies: List<CfirModuleData>

    /** Transitive closure over [refinementDependencies]. */
    abstract val allRefinementDependencies: List<CfirModuleData>

    abstract val platform: CfirPlatform
    abstract val isCommon: Boolean

    open val capabilities: CfirModuleCapabilities
        get() = CfirModuleCapabilities.Empty

    protected var boundSession: CfirSession? = null
        private set

    abstract val session: CfirSession

    fun bindSession(session: CfirSession) {
        if (boundSession != null) {
            error("module data already bound to $this")
        }
        boundSession = session
    }

    override fun toString(): String = "Module $name"

    abstract val stableModuleName: String?

    open val areRedeclarationsEquivalent: Boolean
        get() = session.kind == CfirSession.Kind.Library
}

/**
 * 源码模块数据。
 */
class CfirSourceModuleData(
    override val name: Name,
    override val dependencies: List<CfirModuleData>,
    override val refinementDependencies: List<CfirModuleData>,
    override val platform: CfirPlatform,
    override val isCommon: Boolean = platform.isCommon(),
) : CfirModuleData() {
    override val session: CfirSession
        get() = boundSession ?: sessionNotBoundError()

    override val stableModuleName: String?
        get() = null

    override val allRefinementDependencies: List<CfirModuleData> =
        topologicallySortedDependsOn(refinementDependencies)
}

/**
 * 二进制依赖模块数据。
 */
class CfirBinaryDependenciesModuleData(
    override val name: Name,
    override val capabilities: CfirModuleCapabilities = CfirModuleCapabilities.Empty,
) : CfirModuleData() {
    override val dependencies: List<CfirModuleData>
        get() = emptyList()
    override val refinementDependencies: List<CfirModuleData>
        get() = emptyList()
    override val allRefinementDependencies: List<CfirModuleData>
        get() = emptyList()

    // platform info is meaningless for synthetic "all dependencies" module
    override val platform: CfirPlatform
        get() = shouldNotBeCalled()
    override val isCommon: Boolean
        get() = false
    override val session: CfirSession
        get() = boundSession ?: sessionNotBoundError()
    override val stableModuleName: String?
        get() = null
}

enum class CfirPlatform {
    DEFAULT,
    OHOS,
    LINUX,
    WINDOWS,
    MACOS,
}

private fun CfirPlatform.isCommon(): Boolean = this == CfirPlatform.DEFAULT

private fun CfirModuleData.sessionNotBoundError(): Nothing {
    error("module data ${this::class.simpleName}:$name not bound to session")
}

val CfirSession.nullableModuleData: CfirModuleData? by CfirSession.nullableSessionComponentAccessor()

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

