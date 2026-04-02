package org.cangnova.cangjie.cfir.pipeline

import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.resolve.transformers.CfirFileReplacingResolveProcessor
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
 * val resolvedFiles = processor.process(cfirFiles)
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
     *    - [CfirFileReplacingResolveProcessor]: 处理并替换文件列表
     *    - [CfirGlobalResolveProcessor]: 全局处理所有文件
     *    - [CfirTransformerBasedResolveProcessor]: 逐文件处理
     * 3. 调用 processor.afterPhase()
     *
     * @return 经过所有阶段处理后的文件列表（可能因文件替换型处理器而与输入不同）
     */
    fun process(files: List<CfirFile>): List<CfirFile> {
        val registry = session.phaseResolverRegistry
        var currentFiles = files

        for (phase in CfirResolvePhase.entries) {
            if (phase.noProcessor) continue

            val processor = registry.getProcessor(phase) ?: continue
            processor.beforePhase()
            try {
                when (processor) {
                    is CfirFileReplacingResolveProcessor -> {
                        currentFiles = processor.processAndReplace(currentFiles)
                    }
                    is CfirGlobalResolveProcessor -> processor.process(currentFiles)
                    is CfirTransformerBasedResolveProcessor -> {
                        for (file in currentFiles) {
                            processor.processFile(file)
                        }
                    }
                }
            } finally {
                processor.afterPhase()
            }
        }

        return currentFiles
    }
}
