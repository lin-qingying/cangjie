package org.cangnova.cangjie.cfir.pipeline

import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.resolve.transformers.CfirGlobalResolveProcessor
import org.cangnova.cangjie.cfir.resolve.transformers.CfirTransformerBasedResolveProcessor
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.phaseResolverRegistry

/**
 * CFIR 总解析处理器（对齐 K2 的 FirTotalResolveProcessor）。
 *
 * 职责：
 * - 持有 [ScopeSession] 作用域缓存
 * - 按阶段顺序从 [CfirPhaseResolverRegistry] 获取处理器
 * - 驱动所有文件完成 resolve 流水线
 *
 * 使用方式：
 * ```kotlin
 * val processor = CfirTotalResolveProcessor(session)
 * processor.process(cfirFiles)
 * val scopeSession = processor.scopeSession
 * ```
 */
class CfirTotalResolveProcessor(private val session: CfirSession) {

    val scopeSession: ScopeSession = ScopeSession()

    /**
     * 执行完整 resolve 流水线。
     *
     * 按 [CfirResolvePhase] 顺序处理所有文件：
     * 1. 调用 processor.beforePhase()
     * 2. 根据处理器类型分发：
     *    - [CfirGlobalResolveProcessor]: 全局处理所有文件
     *    - [CfirTransformerBasedResolveProcessor]: 逐文件处理
     * 3. 调用 processor.afterPhase()
     */
    fun process(files: List<CfirFile>) {
        val registry = session.phaseResolverRegistry

        for (phase in CfirResolvePhase.entries) {
            if (phase.noProcessor) continue

            val processor = registry.getProcessor(phase) ?: continue
            processor.beforePhase()
            try {
                when (processor) {
                    is CfirGlobalResolveProcessor -> processor.process(files)
                    is CfirTransformerBasedResolveProcessor -> {
                        for (file in files) {
                            processor.processFile(file)
                        }
                    }
                }
            } finally {
                processor.afterPhase()
            }
        }
    }
}
