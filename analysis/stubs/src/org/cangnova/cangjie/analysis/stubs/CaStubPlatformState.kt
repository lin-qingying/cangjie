package org.cangnova.cangjie.analysis.stubs

import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.api.platform.modification.CaModificationTracker

/**
 * `analysis:stubs` 的 project 级状态服务。
 *
 * 它只负责缓存失效边界，不再承担具体的 stub 提取或聚合逻辑。
 */
internal class CaStubPlatformState(
    /**
     * 提供修改计数和文件收集上下文的 IntelliJ project。
     */
    private val project: Project,
) {
    /**
     * 负责从当前项目重新构建 stub 快照的组合 builder。
     */
    private val snapshotBuilder = CaStubSnapshotBuilder(
        fileCollector = CaStubFileCollector(project),
        summaryBuilder = CaStubSummaryBuilder(),
    )

    /**
     * 与最近一次项目修改计数匹配的快照缓存。
     */
    @Volatile
    private var cachedSnapshot: CaStubSnapshot? = null

    /**
     * 返回当前项目结构修改计数对应的 stub 快照。
     *
     * 修改计数未变化时直接复用缓存；变化后重新收集文件并构建聚合索引。
     */
    fun snapshot(): CaStubSnapshot {
        val modificationCount = project.getService(CaModificationTracker::class.java)?.modificationCount ?: 0L
        cachedSnapshot?.takeIf { it.modificationCount == modificationCount }?.let { return it }

        val rebuiltSnapshot = snapshotBuilder.build(modificationCount)
        cachedSnapshot = rebuiltSnapshot
        return rebuiltSnapshot
    }
}
