package org.cangnova.cangjie.analysis.stubs

import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.api.platform.modification.CaModificationTracker

/**
 * `analysis:stubs` 的 project 级状态服务。
 *
 * 它只负责缓存失效边界，不再承担具体的 stub 提取或聚合逻辑。
 */
internal class CaStubPlatformState(
    private val project: Project,
) {
    private val snapshotBuilder = CaStubSnapshotBuilder(
        fileCollector = CaStubFileCollector(project),
        summaryBuilder = CaStubSummaryBuilder(),
    )

    @Volatile
    private var cachedSnapshot: CaStubSnapshot? = null

    fun snapshot(): CaStubSnapshot {
        val modificationCount = project.getService(CaModificationTracker::class.java)?.modificationCount ?: 0L
        cachedSnapshot?.takeIf { it.modificationCount == modificationCount }?.let { return it }

        val rebuiltSnapshot = snapshotBuilder.build(modificationCount)
        cachedSnapshot = rebuiltSnapshot
        return rebuiltSnapshot
    }
}
