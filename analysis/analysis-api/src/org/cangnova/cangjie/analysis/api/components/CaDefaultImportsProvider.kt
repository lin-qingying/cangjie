package org.cangnova.cangjie.analysis.api.components

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.api.imports.CaDefaultImports
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner

/**
 * 默认导入信息提供协议。
 *
 * 设计要点/职责:
 * - 暴露当前项目中 Cangjie 默认隐式导入(类似 Kotlin `kotlin.*`/`kotlin.collections.*`)的集合视图。
 * - 通过项目级 service 取得,跨 session 共享,故不依赖生命周期 token。
 *
 * 对齐 Kotlin Analysis API 的 `KaDefaultImportsProvider`。
 */
interface CaDefaultImportsProvider  {
    /**
     * 项目当前生效的默认导入集合。
     */
    val defaultImports: CaDefaultImports

    companion object {
        /**
         * 在指定 [Project] 上获取该服务实例。
         */
        fun getService(project: Project): CaDefaultImportsProvider =
            project.service()
    }
}
