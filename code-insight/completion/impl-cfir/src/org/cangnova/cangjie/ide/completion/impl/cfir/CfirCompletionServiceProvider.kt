package org.cangnova.cangjie.ide.completion.impl.cfir

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.ide.completion.api.CangJieCompletionService
import org.cangnova.cangjie.ide.completion.api.completionServiceRegistry

/**
 * 仓颉补全服务的项目级提供者。
 *
 * 提供者在首次访问时把 [CfirCompletionService] 装入注册表：
 * 这样「上游实现是否随插件一起分发」成为唯一的装配开关，
 * 宿主不需要知道任何内部区段，也不需要在 XML 中逐个注册。
 */
@Service(Service.Level.PROJECT)
class CfirCompletionServiceProvider {
    /**
     * 当前项目可用的补全服务。
     */
    fun service(project: Project): CangJieCompletionService {
        val registry = completionServiceRegistry(project)
        return registry.service ?: CfirCompletionService().also { registry.service = it }
    }
}

/**
 * 取得仓颉补全服务；未随插件分发实现时返回 `null`。
 *
 * 调用方必须容忍 `null`：那表示上游补全实现不在 classpath 上，
 * 此时不得退回通用词补全冒充语义补全。
 */
fun cfirCompletionServiceOrNull(project: Project): CangJieCompletionService? = runCatching {
    project.getService(CfirCompletionServiceProvider::class.java)?.service(project)
}.getOrNull()
