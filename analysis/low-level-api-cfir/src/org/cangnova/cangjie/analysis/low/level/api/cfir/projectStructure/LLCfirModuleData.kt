

package org.cangnova.cangjie.analysis.low.level.api.cfir.projectStructure

import org.cangnova.cangjie.analysis.api.projectStructure.CaLibrarySourceModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSession
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSessionCache
import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.common.moduleData
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.platform.TargetPlatform
import org.cangnova.cangjie.platform.isCommon

val CfirElementWithResolveState.llCfirModuleData: LLCfirModuleData
    get() = moduleData as LLCfirModuleData

val CfirSession.llCfirModuleData: LLCfirModuleData
    get() = moduleData as LLCfirModuleData

val LLCfirSession.moduleData: LLCfirModuleData
    get() = llCfirModuleData

val CfirBasedSymbol<*>.llCfirModuleData: LLCfirModuleData
    get() = cfir.llCfirModuleData

/**
 * The [CfirModuleData] for CFIR elements managed by the Analysis API. In Analysis API mode, all CFIR elements must have [LLCfirModuleData].
 */
open class LLCfirModuleData internal constructor(val caModule: CaModule) : CfirModuleData() {
    constructor(session: LLCfirSession) : this(session.caModule) {
        bindSession(session)
    }

    override val name: Name get() = Name.special("<${caModule.moduleDescription}>")

    override val dependencies: List<CfirModuleData> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        caModule.directRegularDependencies.map(::LLCfirModuleData)
    }

    override val refinementDependencies: List<CfirModuleData> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        caModule.directDependsOnDependencies.map(::LLCfirModuleData)
    }

    override val allRefinementDependencies: List<CfirModuleData> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        caModule.transitiveDependsOnDependencies.map(::LLCfirModuleData)
    }

    override val targetPlatform: TargetPlatform
        get() = caModule.targetPlatform

    override val platform: CfirPlatform
        get() = targetPlatform.toCfirPlatform()

    override val isCommon: Boolean
        get() = targetPlatform.isCommon()

    override val session: LLCfirSession
        get() = boundSession?.let { it as LLCfirSession }
            ?: LLCfirSessionCache.getInstance(caModule.project).getSession(caModule, preferBinary = true)

    override val stableModuleName: String?
        get() = caModule.stableModuleName

    /**
     * Library sources have [CfirSession.Kind.Source] kind, but should be treated as a binary dependency since we don't expect
     * redeclararions there.
     */
    override val areRedeclarationsEquivalent: Boolean
        get() = super.areRedeclarationsEquivalent || caModule is CaLibrarySourceModule

    override fun equals(other: Any?): Boolean = this === other || other is LLCfirModuleData && caModule == other.caModule
    override fun hashCode(): Int = caModule.hashCode()
}

/**
 * 当前仓颉前端尚未按后端细分 CFIR 平台实现。
 *
 * 因此 `cjnative` 与 `cjvm` 在 frontend / analysis 层都会先落到同一套默认 CFIR 平台，
 * 只保留高层 `targetPlatform` 身份，等待未来真正的后端分流接入。
 */
private fun TargetPlatform.toCfirPlatform(): CfirPlatform = CfirPlatform.DEFAULT
