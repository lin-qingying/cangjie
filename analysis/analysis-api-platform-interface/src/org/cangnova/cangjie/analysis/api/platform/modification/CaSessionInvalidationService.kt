package org.cangnova.cangjie.analysis.api.platform.modification

import com.intellij.openapi.components.serviceOrNull
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.api.CaModule
import org.cangnova.cangjie.analysis.api.session.CaSessionProvider

/**
 * Analysis session 失效服务。
 *
 * 平台把模块或文件变化转换成失效请求，通过该接口通知具体的 session provider
 * 清理缓存、重建低层 facade 或刷新快照。
 */
interface CaSessionInvalidationService {
    fun invalidate(modules: Set<CaModule>)

    companion object {
        fun getInstance(project: Project): CaSessionInvalidationService? =
            project.serviceOrNull()
                ?: (CaSessionProvider.getInstance(project) as? CaSessionInvalidationService)
    }
}
