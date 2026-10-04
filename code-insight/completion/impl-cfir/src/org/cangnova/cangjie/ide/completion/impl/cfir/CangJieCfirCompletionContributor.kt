package org.cangnova.cangjie.ide.completion.impl.cfir

import com.intellij.codeInsight.completion.CompletionParameters
import org.cangnova.cangjie.ide.completion.api.CangJieCompletionService
import org.cangnova.cangjie.ide.completion.impl.shared.AbstractCangJieCompletionContributor

/**
 * 仓颉 CFIR 补全的平台入口。
 *
 * 这是注册进生产描述符的唯一 contributor：内部区段由服务显式列表组织，
 * 不为每个区段增加扩展点，也不在宿主侧重建补全逻辑。
 */
class CangJieCfirCompletionContributor : AbstractCangJieCompletionContributor() {
    /**
     * 取得项目当前生效的补全服务；未装配实现时返回 `null`。
     */
    override fun completionService(parameters: CompletionParameters): CangJieCompletionService? =
        cfirCompletionServiceOrNull(parameters.position.project)
}
