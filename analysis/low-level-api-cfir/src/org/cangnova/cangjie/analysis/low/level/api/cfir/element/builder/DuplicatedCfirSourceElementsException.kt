/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.element.builder

import org.cangnova.cangjie.psi.psiUtil.getElementTextWithContext
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.render
import org.cangnova.cangjie.psi.CjElement

/**
 * 表示同一个 PSI source 被多个 CFIR 元素作为真实来源复用的结构错误。
 */
class DuplicatedCfirSourceElementsException(
    existingCfir: CfirElement,
    newCfir: CfirElement,
    psi: CjElement
) : IllegalStateException() {
    /**
     * 包含已有/新增 CFIR 文本和 PSI 上下文的详细诊断消息。
     */
    override val message: String? = """|The PSI element should be used only once as a real PSI source of CfirElement,
       |the elements ${if (existingCfir.source === newCfir.source) "HAVE" else "DON'T HAVE"} the same instances of source elements 
       |
       |existing CFIR element is $existingCfir with text:
       |${existingCfir.render().trim()}
       |
       |new CFIR element is $newCfir with text:
       | ${newCfir.render().trim()}
       |
       |PSI element is $psi with text in context:
       |${psi.getElementTextWithContext()}""".trimMargin()


    companion object {
        // The are some cases which are still generates CFIR elements with duplicated source elements
        // Then such case is met, it's better to be fixed
        // but exception reporting can be easily disabled by setting this to false
        var IS_ENABLED = false
    }
}
