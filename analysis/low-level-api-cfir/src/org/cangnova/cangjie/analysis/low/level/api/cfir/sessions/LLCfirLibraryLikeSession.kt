/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.sessions

import org.cangnova.cangjie.analysis.low.level.api.cfir.util.LLCfirScopeSessionProvider
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.session.CfirBuiltinTypes

/**
 * 库类 low-level CFIR session 的公共基类。
 *
 * 库 session 使用不绑定修改 tracker 的 scope session provider，因为库内容不会通过源码 PSI 修改即时失效。
 */
abstract class LLCfirLibraryLikeSession(
    caModule: CaModule,
    builtinTypes: CfirBuiltinTypes,
) : LLCfirSession(caModule, builtinTypes, Kind.Library) {
    /**
     * 库 session 专用 scope session provider。
     */
    private val scopeSessionProvider = LLCfirScopeSessionProvider.create(project, invalidationTrackers = emptyList())

    /**
     * 返回当前库 session 的 scope session。
     */
    override fun getScopeSession(): ScopeSession {
        return scopeSessionProvider.getScopeSession()
    }
}
