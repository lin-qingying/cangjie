/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package org.cangnova.cangjie.macro

import org.cangnova.cangjie.cfir.declarations.CfirFile

/**
 * 宏 AST 替换器
 *
 * 将宏展开结果替换回 CFIR 树。
 * 展开文本 → 重新解析为 PSI → 转换为 CFIR 节点 → 替换原 [CfirMacroExpression]。
 */
interface MacroReplacer {
    /**
     * 将展开结果替换回 CFIR 树
     *
     * @param files 当前 CFIR 文件列表
     * @param expansions 宏调用站点到展开结果的映射
     * @return 替换后的输出
     */
    fun replace(
        files: List<CfirFile>,
        expansions: Map<MacroCallSite, MacroExpansionResult>,
    ): MacroReplacementOutput
}
