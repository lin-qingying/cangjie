package org.cangnova.cangjie.lexer.cdoc.psi.api

import org.cangnova.cangjie.lexer.cdoc.psi.impl.CDocSection
import org.cangnova.cangjie.lexer.cdoc.psi.impl.CDocTag
import org.cangnova.cangjie.psi.CjImplementationDetail
import org.cangnova.cangjie.psi.CjNonPublicApi

/**
 * CDoc 的结构化视图。
 *
 * 对齐 Kotlin `KDocCommentDescriptor`，用于以主标签和附加分节的形式表达
 * 某个声明或符号最终关联到的文档片段。
 */
@CjNonPublicApi
@SubclassOptInRequired(CjImplementationDetail::class)
interface CDocCommentDescriptor {
    /**
     * 当前声明最相关的主标签或主分节。
     */
    val primaryTag: CDocTag

    /**
     * 与 [primaryTag] 同属一份 CDoc 的其他分节。
     */
    val additionalSections: List<CDocSection>
}
