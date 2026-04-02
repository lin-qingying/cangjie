package org.cangnova.cangjie.cfir.resolve.transformers

import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.session.CfirSession

/**
 * 宏展开动作抽象。
 *
 * 定义在 resolve 模块中，避免 resolve 模块依赖 macro 模块。
 * 由 CLI 编译器或其他入口点提供具体实现。
 *
 * 无宏调用时实现应直接返回原文件列表。
 */
fun interface MacroExpandAction {
    /**
     * 对给定文件列表执行宏展开。
     *
     * @param session 当前编译会话
     * @param files 待展开的 CFIR 文件列表
     * @return 展开后的 CFIR 文件列表（可能包含替换后的文件）
     */
    fun expand(session: CfirSession, files: List<CfirFile>): List<CfirFile>
}
