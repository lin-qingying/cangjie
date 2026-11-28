package org.cangnova.cangjie.analysis.low.level.api.cfir.lazy.resolve

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.CfirDesignation
import org.cangnova.cangjie.analysis.low.level.api.cfir.transformers.PreservedState
import org.cangnova.cangjie.analysis.low.level.api.cfir.transformers.StateKeeper
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirAnnotationMetadataBuilder
import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirRawMacroUsageSlot
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurface
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceExpr
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.annotationMetadataRegistryOrNull
import org.cangnova.cangjie.cfir.session.rawMacroSurfaceUsageStoreOrNull

/**
 * 一次 body 解析的私有发布状态。Kotlin 没有仓颉 macro side products；这些状态显式加入原 keeper，
 * 等根 postProcess 完成旧节点复用后才选择最终产品，并与原图共用失败恢复边界。
 */
internal class CfirLazyBodyPublications(private val session: CfirSession) : PreservedState, AutoCloseable {
    private class Entry(
        val target: CfirDeclaration,
        val file: CfirFile,
        val metadata: CfirAnnotationMetadataBuilder,
        val slots: List<CfirRawMacroUsageSlot>,
        val surfaces: List<MacroSurfaceExpr>,
    ) {
        private enum class WriterState { OPEN, SEALED, ABORTED }
        private var writerState = WriterState.OPEN

        fun seal() = metadata.seal(listOf(target)).also { writerState = WriterState.SEALED }

        fun abortOpen() {
            if (writerState == WriterState.OPEN) {
                metadata.abort()
                writerState = WriterState.ABORTED
            }
        }
    }

    private val entries = mutableListOf<Entry>()
    private val restorations = mutableListOf<() -> Unit>()
    private var postProcessed = false
    private var restored = false
    private var closed = false

    /** 在采用前验证全部真实槽位，再显式接管仍 OPEN 的 writer；不向 session 发布候选。 */
    fun stage(
        target: CfirDeclaration,
        file: CfirFile,
        metadata: CfirAnnotationMetadataBuilder,
        slots: List<CfirRawMacroUsageSlot>,
        surfaces: List<MacroSurface>,
    ) {
        check(!postProcessed && !restored && !closed)
        require(target.moduleData.session === session && file.moduleData.session === session)
        require(slots.none { slot -> entries.any { slot in it.slots } }) { "A body operation cannot adopt the same slot twice." }
        checkNotNull(session.rawMacroSurfaceUsageStoreOrNull) {
            "Body publication requires a registered raw file usage store."
        }.validateAdoptionSlots(file, slots)
        entries += Entry(target, file, metadata, slots.toList(), surfaces.filterIsInstance<MacroSurfaceExpr>())
    }

    /** 根 keeper 的所有子后处理先完成；恢复时先还原原图，再恢复本操作的 side tables。 */
    fun <T : Any> withStateKeeper(keeper: StateKeeper<T, CfirDesignation>): StateKeeper<T, CfirDesignation> =
        StateKeeper { target, designation ->
            val original = keeper.prepare(target, designation)
            object : PreservedState {
                private var postProcessed = false
                private var restored = false

                override fun postProcess() {
                    if (postProcessed) return
                    postProcessed = true
                    synchronized(session) {
                        try {
                            original.postProcess()
                            this@CfirLazyBodyPublications.postProcess()
                        } catch (failure: Throwable) {
                            // 发布异常不能先释放锁；原图及两类索引在读者重新进入前恢复。
                            try {
                                restore()
                            } catch (restorationFailure: Throwable) {
                                failure.addSuppressed(restorationFailure)
                            }
                            throw failure
                        }
                    }
                }

                override fun restore() {
                    if (restored) return
                    restored = true
                    synchronized(session) {
                        try {
                            original.restore()
                        } finally {
                            this@CfirLazyBodyPublications.restore()
                        }
                    }
                }
            }
        }

    override fun postProcess() {
        if (postProcessed) return
        check(!restored && !closed)
        postProcessed = true
        if (entries.isEmpty()) return

        // 所有查询、验证、提交和撤销共用该锁；锁内只遍历已采用图，不执行解析或插件逻辑。
        synchronized(session) {
            val metadataProducts = entries.map { it.seal() }
            val usageStore = checkNotNull(session.rawMacroSurfaceUsageStoreOrNull)
            val usagePublications = entries.map { entry ->
                usageStore.prepareExpressionSurfacePublication(entry.file, entry.slots, entry.surfaces)
            }
            val metadataPublication = checkNotNull(session.annotationMetadataRegistryOrNull).preparePublication(metadataProducts)

            usagePublications.forEach { it.validate() }
            metadataPublication.validate()
            // 撤销依据在首次写入前全部登记；COMMITTING handle 也可恢复部分写入。
            usagePublications.forEach { restorations += it::restore }
            restorations += metadataPublication::restore
            usagePublications.forEach { it.commit() }
            metadataPublication.commit()
        }
    }

    override fun restore() {
        if (restored) return
        restored = true
        synchronized(session) {
            var failure: Throwable? = null
            for (restoration in restorations.asReversed()) {
                try {
                    restoration()
                } catch (current: Throwable) {
                    if (failure == null) failure = current else failure.addSuppressed(current)
                }
            }
            entries.forEach { it.abortOpen() }
            restorations.clear()
            if (failure != null) throw failure
        }
    }

    /** 正常返回及 partial suspension 都只释放临时引用；普通失败已通过 keeper 恢复。 */
    override fun close() {
        if (closed) return
        closed = true
        try {
            entries.forEach { it.abortOpen() }
        } finally {
            entries.clear()
            restorations.clear()
        }
    }
}
