/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.sessions

import org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirModuleResolveComponents
import org.cangnova.cangjie.analysis.api.projectStructure.CaSourceModule
import org.cangnova.cangjie.cfir.PrivateSessionConstructor
import org.cangnova.cangjie.cfir.session.CfirBuiltinTypes

/**
 * 源码模块的可解析 low-level CFIR session。
 *
 * 该 session 负责源码 lazy resolve，并按需计算模块依赖 session。
 */
internal class LLCfirSourcesSession @PrivateSessionConstructor constructor(
    caModule: CaSourceModule,

    /**
     * 当前源码 session 的模块解析组件。
     */
    override val moduleComponents: LLCfirModuleResolveComponents,
    builtinTypes: CfirBuiltinTypes,
    computeDependencies: () -> List<LLCfirSession>,
) : LLCfirResolvableModuleSession(caModule, builtinTypes) {
    /**
     * Dependencies are lazy to support cyclic dependencies between modules.
     */
    val dependencies: List<LLCfirSession> by lazy(LazyThreadSafetyMode.PUBLICATION, computeDependencies)
}
