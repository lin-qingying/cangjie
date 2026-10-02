package org.cangnova.cangjie.cfir.pipeline

import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.resolve.transformers.CfirFileReplacingResolveProcessor
import org.cangnova.cangjie.cfir.resolve.transformers.CfirGlobalResolveProcessor
import org.cangnova.cangjie.cfir.resolve.transformers.CfirTransformerBasedResolveProcessor
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.resolve.transformers.measurePhase
import org.cangnova.cangjie.cfir.session.phaseResolverRegistry
import org.cangnova.cangjie.cfir.session.resolvePhaseTimingObserverOrNull

/**
 * 语义解析总处理器：按 [CfirResolvePhase] 顺序驱动各阶段处理器。
 *
 * 阶段耗时观察是可选能力：宿主注册 [org.cangnova.cangjie.cfir.resolve.transformers.CfirResolvePhaseTimingObserver]
 * 后，每个阶段会回调开始与结束（含异常路径），未注册时不取单调时钟、解析路径零额外开销。
 */
class CfirTotalResolveProcessor(private val session: CfirSession) {
    /**
     * 本次处理共享的作用域会话。
     */
    val scopeSession: ScopeSession = ScopeSession()

    /**
     * 依次执行所有已注册处理器的阶段，返回处理后的文件列表。
     *
     * @param files 待执行 total resolve 的 CFIR 文件列表。
     * @return 经过所有阶段处理后的文件列表（可能因文件替换型处理器而与输入不同）
     */
    fun process(files: List<CfirFile>): List<CfirFile> {
        val registry = session.phaseResolverRegistry
        val timingObserver = session.resolvePhaseTimingObserverOrNull
        var currentFiles = files

        for (phase in CfirResolvePhase.entries) {
            if (phase.noProcessor) continue

            val processor = registry.getProcessor(phase) ?: continue
            val fileCount = currentFiles.size
            timingObserver.measurePhase(phase, fileCount) {
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
        }

        return currentFiles
    }
}