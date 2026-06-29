/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.cfir.diagnostics

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

/**
 * 绑定 PSI 的旧式诊断接口。
 */
interface Diagnostic : UnboundDiagnostic, DiagnosticMarker {
    /**
     * 诊断锚定的 PSI 元素。
     */
    override val psiElement: PsiElement
    /**
     * 诊断所在 PSI 文件。
     */
    val psiFile: PsiFile

    /**
     * 诊断工厂名称。
     */
    override val factoryName: String
        get() = factory.name
}

/**
 * 不要求绑定 PSI 的旧式诊断接口。
 */
interface UnboundDiagnostic {
    /**
     * 创建该诊断的工厂。
     */
    val factory: DiagnosticFactory<*>
    /**
     * 当前诊断严重级别。
     */
    val severity: Severity
    /**
     * 诊断高亮范围集合。
     */
    val textRanges: List<TextRange>
    /**
     * 诊断位置是否有效。
     */
    val isValid: Boolean
}
