package org.cangnova.cangjie.cfir.pipeline

import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.resolve.ScopeSession
import org.cangnova.cangjie.cfir.session.CfirSession

/**
 * 单模块前端输出（对齐 K2 的 SingleModuleFrontendOutput）。
 *
 * 包含单个模块完成 resolve + check 后的产物：
 * - [session]: 编译会话
 * - [scopeSession]: 作用域缓存会话
 * - [fir]: 已解析的 CFIR 文件列表
 */
data class SingleModuleFrontendOutput(
    val session: CfirSession,
    val scopeSession: ScopeSession,
    val fir: List<CfirFile>,
)

/**
 * 所有模块前端输出（对齐 K2 的 AllModulesFrontendOutput）。
 *
 * 仓颉简化：当前仅支持单模块编译。
 * 使用 value class 保持与 K2 的结构对齐，为未来扩展预留空间。
 */
@JvmInline
value class AllModulesFrontendOutput(val outputs: List<SingleModuleFrontendOutput>)
