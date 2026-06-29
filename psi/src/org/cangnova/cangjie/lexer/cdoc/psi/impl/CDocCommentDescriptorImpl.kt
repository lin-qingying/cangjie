package org.cangnova.cangjie.lexer.cdoc.psi.impl

import org.cangnova.cangjie.lexer.cdoc.psi.api.CDocCommentDescriptor
import org.cangnova.cangjie.psi.CjImplementationDetail
import org.cangnova.cangjie.psi.CjNonPublicApi

/**
 * 表示 `CDocCommentDescriptorImpl`，承载仓颉词法与文档注释中的语法节点、索引桩或辅助模型。
 */
@CjNonPublicApi
@CjImplementationDetail
class CDocCommentDescriptorImpl(
    /**
     * 暴露 `primaryTag`，实现仓颉词法与文档注释节点对上层接口的属性契约。
     */
    override val primaryTag: CDocTag,
    /**
     * 暴露 `additionalSections`，实现仓颉词法与文档注释节点对上层接口的属性契约。
     */
    override val additionalSections: List<CDocSection>,
) : CDocCommentDescriptor
