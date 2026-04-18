/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.projectStructure

import org.cangnova.cangjie.analysis.api.projectStructure.CaLibrarySourceModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSession
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSessionCache
import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.CfirModuleData
import org.cangnova.cangjie.cfir.CfirSession
import org.cangnova.cangjie.cfir.moduleData
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
    get() = fir.llCfirModuleData

/**
 * The [CfirModuleData] for CFIR elements managed by the Analysis API. In Analysis API mode, all CFIR elements must have [LLCfirModuleData].
 */
open class LLCfirModuleData internal constructor(val ktModule: CaModule) : CfirModuleData() {
    constructor(session: LLCfirSession) : this(session.ktModule) {
        bindSession(session)
    }

    override val name: Name get() = Name.special("<${ktModule.moduleDescription}>")

    override val dependencies: List<CfirModuleData> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        ktModule.directRegularDependencies.map(::LLCfirModuleData)
    }

    override val dependsOnDependencies: List<CfirModuleData> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        ktModule.directDependsOnDependencies.map(::LLCfirModuleData)
    }

    override val allDependsOnDependencies: List<CfirModuleData> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        ktModule.transitiveDependsOnDependencies.map(::LLCfirModuleData)
    }

    override val friendDependencies: List<CfirModuleData> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        ktModule.directFriendDependencies.map(::LLCfirModuleData)
    }

    override val platform: TargetPlatform get() = ktModule.targetPlatform

    override val isCommon: Boolean get() = ktModule.targetPlatform.isCommon()

    override val session: LLCfirSession
        get() = boundSession?.let { it as LLCfirSession }
            ?: LLCfirSessionCache.getInstance(ktModule.project).getSession(ktModule, preferBinary = true)

    override val stableModuleName: String?
        get() = ktModule.stableModuleName

    /**
     * Library sources have [CfirSession.Kind.Source] kind, but should be treated as a binary dependency since we don't expect
     * redeclararions there.
     */
    override val areRedeclarationsEquivalent: Boolean
        get() = super.areRedeclarationsEquivalent || ktModule is CaLibrarySourceModule

    override fun equals(other: Any?): Boolean = this === other || other is LLCfirModuleData && ktModule == other.ktModule
    override fun hashCode(): Int = ktModule.hashCode()
}
