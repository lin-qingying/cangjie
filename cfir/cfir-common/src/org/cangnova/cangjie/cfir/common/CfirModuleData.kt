package org.cangnova.cangjie.cfir.common

import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.utils.shouldNotBeCalled

/**
 * CFIR 妯″潡鏁版嵁鎶借薄銆? *
 * 浠撻鍖?妯″潡绯荤粺浣跨敤鍥涚骇 AccessLevel锛圥RIVATE/INTERNAL/PROTECTED/PUBLIC锛夋帶鍒跺彲瑙佹€э紝
 * 涓嶅惈 Kotlin 鐨?friend module 姒傚康銆俰nternal 鍙鎬ч檺瀹氬湪鍚屼竴 package 鍐呫€? */
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
 * 婧愮爜妯″潡鏁版嵁銆? */
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
 * 浜岃繘鍒朵緷璧栨ā鍧楁暟鎹€? */
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
 * 鍒ゆ柇褰撳墠妯″潡鏄惁鍙互鐪嬪埌 [otherModule] 鐨?internal 澹版槑銆? *
 * 浠撻鐨?internal 鍙鎬у熀浜庡寘绾у埆锛堝悓鍖呭彲瑙侊級锛屼笉渚濊禆 friend module 鏈哄埗銆? * 褰撲袱涓ā鍧楀睘浜庡悓涓€妯″潡鎴栧瓨鍦?dependsOn 鍏崇郴鏃跺彲瑙併€? */
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

