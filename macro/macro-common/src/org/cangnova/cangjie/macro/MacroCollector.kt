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
 * 宏调用收集器
 *
 * 从 CFIR 文件中收集所有宏调用站点（[CfirMacroExpression] 节点）。
 */
interface MacroCollector {
    /** 从 CFIR 文件中收集所有宏调用站点 */
    fun collect(files: List<CfirFile>): List<MacroCallSite>
}
