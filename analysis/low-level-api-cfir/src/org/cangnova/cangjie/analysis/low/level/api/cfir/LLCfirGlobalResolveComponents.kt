/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir

import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.low.level.api.cfir.file.builder.LLCfirLockProvider
import org.cangnova.cangjie.analysis.low.level.api.cfir.lazy.resolve.LLCfirLazyResolveContractChecker
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSession
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.domains.LLDeserializationStatistics
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.domains.LLDiagnosticsStatistics
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.domains.LLSessionStatistics
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.llDeserializationStatistics
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.llDiagnosticsStatistics
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.llSessionStatistics
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
     * 诊断收集统计域；统计未启用时为 `null`。
     *
     * 三个"不走 session 观察者"的域集中在这里解析一次：它们的位置都在 low-level 模块内，
     * 调用点分散在模块组件、session 缓存与符号提供器上，各自去工程级取一次服务是多余的
     * service 查找。延迟解析是因为本组件在工程服务注册完成前就可能被拿到。
     */
    internal val diagnosticsStatistics: LLDiagnosticsStatistics? by lazy(LazyThreadSafetyMode.PUBLICATION) {
        project.llDiagnosticsStatistics()
    }

    /**
     * session 创建统计域；统计未启用时为 `null`。
     */
    internal val sessionStatistics: LLSessionStatistics? by lazy(LazyThreadSafetyMode.PUBLICATION) {
        project.llSessionStatistics()
    }

    /**
     * stub 反序列化统计域；统计未启用时为 `null`。
     */
    internal val deserializationStatistics: LLDeserializationStatistics? by lazy(LazyThreadSafetyMode.PUBLICATION) {
        project.llDeserializationStatistics()
    }

    /**
     * 为 CFIR 文件构建和 lazy resolve 提供阶段锁的 provider。
     */
    internal val lockProvider: LLCfirLockProvider = LLCfirLockProvider(checker)
}
