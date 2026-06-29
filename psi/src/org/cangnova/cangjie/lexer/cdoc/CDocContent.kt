package org.cangnova.cangjie.lexer.cdoc

import org.cangnova.cangjie.lexer.cdoc.psi.impl.CDocSection
import org.cangnova.cangjie.lexer.cdoc.psi.impl.CDocTag

/**
 * 表示 `CDocContent`，承载仓颉词法与文档注释中的语法节点、索引桩或辅助模型。
 */
data class CDocContent(
    /**
     * 保存 `contentTag`，供仓颉词法与文档注释流程读取节点结构或语义信息。
     */
    val contentTag: CDocTag,
    /**
     * 保存 `sections`，供仓颉词法与文档注释流程读取节点结构或语义信息。
     */
    val sections: List<CDocSection>
)
