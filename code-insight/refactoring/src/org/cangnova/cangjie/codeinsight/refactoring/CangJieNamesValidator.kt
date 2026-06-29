/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package org.cangnova.cangjie.codeinsight.refactoring

import com.intellij.lang.refactoring.NamesValidator
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.KeywordStringsGenerated
import org.cangnova.cangjie.psi.psiUtil.isIdentifier

/**
 * 仓颉语言的重构命名校验器。
 *
 * 对齐 Kotlin `KotlinNamesValidator` 的职责：IDE 与无 UI 重构入口统一通过
 * `NamesValidator` 判断关键字和合法标识符，具体语法判断复用 PSI 层 lexer。
 */
class CangJieNamesValidator : NamesValidator {
    /**
     * 判断名称是否为仓颉关键字。
     */
    override fun isKeyword(name: String, project: Project?): Boolean = name in KeywordStringsGenerated.KEYWORDS

    /**
     * 判断名称是否符合仓颉标识符词法规则。
     */
    override fun isIdentifier(name: String, project: Project?): Boolean = name.isIdentifier()
}
