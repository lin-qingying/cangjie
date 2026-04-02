package org.cangnova.cangjie.analysis.api.platform.modification

import com.intellij.openapi.components.serviceOrNull
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.api.CaModule

/**
 * Analysis API 修改追踪器。
 *
 * 平台需要通过该接口把文件、模块、工作区层面的变化统一投递给分析层，
 * 以便 session 缓存、pointer 恢复和测试框架都能基于同一套版本视图工作。
 */
interface CaModificationTracker {
    val modificationCount: Long

    fun getModuleModificationCount(module: CaModule): Long = modificationCount

    companion object {
        fun getInstance(project: Project): CaModificationTracker? = project.serviceOrNull()
    }
}
