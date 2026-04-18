/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.sessions

import org.cangnova.cangjie.analysis.low.level.api.cfir.util.LLCfirScopeSessionProvider
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.cfir.BuiltinTypes
import org.cangnova.cangjie.cfir.resolve.ScopeSession

abstract class LLCfirLibraryLikeSession(
    ktModule: CaModule,
    builtinTypes: BuiltinTypes,
) : LLCfirSession(ktModule, builtinTypes, Kind.Library) {
    private val scopeSessionProvider = LLCfirScopeSessionProvider.create(project, invalidationTrackers = emptyList())

    override fun getScopeSession(): ScopeSession {
        return scopeSessionProvider.getScopeSession()
    }
}
