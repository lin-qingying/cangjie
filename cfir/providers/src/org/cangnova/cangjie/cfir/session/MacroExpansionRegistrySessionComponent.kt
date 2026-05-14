package org.cangnova.cangjie.cfir.session

import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroExpansionRegistry

/**
 * Session 上可选挂载的 [MacroExpansionRegistry] —— baseline 第 10 节
 * "session/analysis 长生命周期 registry"。
 *
 * 由 macro construction step（`MacroConstructionService.expand`）在产出诊断后
 * 通过 [CfirSession.register] 写入；ordinary checker / IDE / LSP 通过本
 * accessor 读取，禁止反向回写。
 *
 * 缺省时为 `null`：尚未运行过 construction step 或无宏文件的 identity path。
 */
val CfirSession.macroExpansionRegistry: MacroExpansionRegistry? by CfirSession.nullableSessionComponentAccessor()
