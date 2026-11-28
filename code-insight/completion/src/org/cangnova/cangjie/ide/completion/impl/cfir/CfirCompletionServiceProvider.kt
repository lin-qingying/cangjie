package org.cangnova.cangjie.ide.completion.impl.cfir

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.ide.completion.api.CangJieCompletionService
import org.cangnova.cangjie.ide.completion.api.completionServiceRegistry
import org.cangnova.cangjie.ide.completion.impl.cfir.resolve.CfirCompletionDeclarationResolver
import org.cangnova.cangjie.ide.completion.impl.shared.resolve.completionDeclarationResolverRegistry

/** 装配失败的日志出口：见 [cfirCompletionServiceOrNull] 的失败语义说明。 */
private val LOG = logger<CfirCompletionServiceProvider>()

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
     *
     * 同时装入**候选声明解析器**：它按候选身份重进分析恢复符号，供延迟文档（`resolve`）使用。
     * 两者必须同源装配——解析器用的身份算法与生成候选的是同一套，缺了它 `resolve` 只能空手而归。
     */
    fun service(project: Project): CangJieCompletionService {
        val registry = completionServiceRegistry(project)
        completionDeclarationResolverRegistry(project)?.service = CfirCompletionDeclarationResolver()
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
}.onFailure {
    // 装配**失败**与「实现不在 classpath」必须可区分：后者由 `getService` 返回 null（不抛），
    // 前者被 `runCatching` 吞掉后只剩「零候选」这一种表现——正是本模块 KDoc 点名的
    // 最难定位的失败形态。因此这里必须留下可检索的日志。
    LOG.warn("CangJie completion service assembly failed; this completion will produce no candidates.", it)
}.getOrNull()
