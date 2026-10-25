/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir

import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.low.level.api.cfir.file.builder.LLCfirLockProvider
import org.cangnova.cangjie.analysis.low.level.api.cfir.lazy.resolve.LLCfirLazyResolveContractChecker
import org.cangnova.cangjie.analysis.low.level.api.cfir.macro.LLCfirMacroSemanticSnapshotStore
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSession
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.domains.LLDeserializationStatistics
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.domains.LLDiagnosticsStatistics
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.domains.LLSessionStatistics
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.llDeserializationStatistics
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.llDiagnosticsStatistics
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.llSessionStatistics
import org.cangnova.cangjie.cfir.session.CfirSession
import java.util.Collections
import java.util.WeakHashMap


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

    /**
     * 按模块共享的宏语义快照存储。
     *
     * 快照构建成本远高于普通缓存，且内容只依赖模块输入（PSI / roots / 编译器设置 / SDK），
     * 因此必须跨 session 复用：session 会因内存压力被清理，但模块级快照不应随之重建。
     * 失效由 IDE 层在输入 revision 变化时调用 [LLCfirMacroSemanticSnapshotStore.invalidate]。
     *
     * 使用弱键：模块从项目结构移除且不再被引用后，对应条目自然回收，不需要额外的移除事件。
     * 同步由 [Collections.synchronizedMap] 提供；[MutableMap.computeIfAbsent] 在该包装下是原子的。
     */
    private val macroSemanticSnapshotStores: MutableMap<CaModule, LLCfirMacroSemanticSnapshotStore> =
        Collections.synchronizedMap(WeakHashMap<CaModule, LLCfirMacroSemanticSnapshotStore>())

    /**
     * 取得 [module] 对应的模块级宏语义快照存储。
     */
    internal fun getMacroSemanticSnapshotStore(module: CaModule): LLCfirMacroSemanticSnapshotStore =
        macroSemanticSnapshotStores.computeIfAbsent(module) { LLCfirMacroSemanticSnapshotStore() }
}
