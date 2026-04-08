package org.cangnova.cangjie.analysis.api.platform.projectStructure

import com.intellij.openapi.components.serviceOrNull
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.analysis.api.CaModule

/**
 * 模块内容范围细化器。
 *
 * 平台可以在基础内容范围之上施加额外约束，例如：
 * - 过滤当前不可见的内容根。
 * - 叠加脚本、代码片段、暂存文件的专有可见范围。
 * - 为 LSP 文档快照提供临时覆盖层。
 */
interface CaContentScopeRefiner {
    fun getRefinedContentScope(module: CaModule, baseContentScope: GlobalSearchScope): GlobalSearchScope

    companion object {
        fun getInstance(project: Project): CaContentScopeRefiner? = project.serviceOrNull()
    }
}
