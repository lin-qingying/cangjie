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
 * 宏展开编排器
 *
 * 组合 [MacroCollector]、[MacroExecutor]、[MacroReplacer] 完成完整的宏展开流程：
 * 收集 → 执行 → 替换 → 递归检查。
 */
interface MacroExpander {
    /**
     * 完整宏展开流程
     *
     * @param files CFIR 文件列表
     * @param maxIterations 最大递归次数（防止无限展开），默认 16
     * @return 展开后的完整输出
     */
    fun expandAll(files: List<CfirFile>, maxIterations: Int = 16): MacroExpansionOutput
}
