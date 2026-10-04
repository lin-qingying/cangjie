package org.cangnova.cangjie.ide.completion.api

import com.intellij.openapi.project.Project
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service

/**
 * 项目级补全服务注册表。
 *
 * 宿主通过本服务取得当前生效的补全实现，使宿主不直接依赖上游 `completion/impl-cfir`，
 * 也不需要在 XML 中为每个内部贡献者注册扩展点。
 */
@Service(Service.Level.PROJECT)
class CaCangJieCompletionServiceRegistry {
    /**
     * 当前生效的补全服务；未注册实现时为 `null`。
     *
     * 平台据此明确「语义补全不可用」，而不是静默回落到词补全。
     */
    @Volatile
    var service: CaCangJieCompletionService? = null
}

/**
 * 取得项目级补全服务注册表。
 */
fun completionServiceRegistry(project: Project): CaCangJieCompletionServiceRegistry =
    project.service()
