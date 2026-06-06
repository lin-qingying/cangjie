/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package org.cangnova.cangjie.codeinsight.refactoring

import com.intellij.refactoring.suggested.SuggestedRefactoringProvider

/**
 * LSP/headless 环境使用的 suggested-refactoring project service。
 *
 * IntelliJ 平台默认实现会监听 `EditorFactory`，属于 IDE 编辑器子系统。LSP 的 rename
 * 只需要满足 Kotlin `Renamer` 末尾的 `reset()` 清理协议，不应因此初始化插件层或 UI 编辑器。
 */
class CangJieHeadlessSuggestedRefactoringProvider : SuggestedRefactoringProvider {
    override fun reset() = Unit
}
