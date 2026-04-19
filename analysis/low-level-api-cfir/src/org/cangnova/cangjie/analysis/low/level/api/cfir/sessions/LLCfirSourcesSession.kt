/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.sessions

import org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirModuleResolveComponents
import org.cangnova.cangjie.analysis.api.projectStructure.CaSourceModule
import org.cangnova.cangjie.cfir.PrivateSessionConstructor
import org.cangnova.cangjie.cfir.session.CfirBuiltinTypes

internal class LLCfirSourcesSession @PrivateSessionConstructor constructor(
    ktModule: CaSourceModule,
    override val moduleComponents: LLCfirModuleResolveComponents,
    builtinTypes: CfirBuiltinTypes,
    computeDependencies: () -> List<LLCfirSession>,
) : LLCfirResolvableModuleSession(ktModule, builtinTypes) {
    /**
     * Dependencies are lazy to support cyclic dependencies between modules.
     */
    val dependencies: List<LLCfirSession> by lazy(LazyThreadSafetyMode.PUBLICATION, computeDependencies)
}
