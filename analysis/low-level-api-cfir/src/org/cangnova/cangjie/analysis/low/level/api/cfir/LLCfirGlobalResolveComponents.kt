/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir

import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.low.level.api.cfir.file.builder.LLCfirLockProvider
import org.cangnova.cangjie.analysis.low.level.api.cfir.lazy.resolve.LLCfirLazyResolveContractChecker
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSession
import org.cangnova.cangjie.cfir.session.CfirSession


internal class LLCfirGlobalResolveComponents(val project: Project) {
    companion object {
        fun getInstance(project: Project): LLCfirGlobalResolveComponents {
            return project.getService(LLCfirGlobalResolveComponents::class.java)
        }

        fun getInstance(llCfirSession: CfirSession): LLCfirGlobalResolveComponents {
            return getInstance((llCfirSession as LLCfirSession).project)
        }
    }

    internal val checker: LLCfirLazyResolveContractChecker = LLCfirLazyResolveContractChecker()
    internal val lockProvider: LLCfirLockProvider = LLCfirLockProvider(checker)
}
