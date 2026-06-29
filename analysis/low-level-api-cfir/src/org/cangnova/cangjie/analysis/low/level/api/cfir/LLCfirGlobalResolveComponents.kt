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


/**
 * 按 project 共享的 low-level CFIR 全局解析组件集合。
 */
internal class LLCfirGlobalResolveComponents(
    /**
     * 这些全局组件所属的 IntelliJ project。
     */
    val project: Project,
) {
    companion object {
        fun getInstance(project: Project): LLCfirGlobalResolveComponents {
            return project.getService(LLCfirGlobalResolveComponents::class.java)
        }

        fun getInstance(llCfirSession: CfirSession): LLCfirGlobalResolveComponents {
            return getInstance((llCfirSession as LLCfirSession).project)
        }
    }

    /**
     * 校验 lazy resolve 是否遵守 low-level 阶段推进契约的检查器。
     */
    internal val checker: LLCfirLazyResolveContractChecker = LLCfirLazyResolveContractChecker()

    /**
     * 为 CFIR 文件构建和 lazy resolve 提供阶段锁的 provider。
     */
    internal val lockProvider: LLCfirLockProvider = LLCfirLockProvider(checker)
}
