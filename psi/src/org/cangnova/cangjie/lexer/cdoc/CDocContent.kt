package org.cangnova.cangjie.lexer.cdoc

import org.cangnova.cangjie.lexer.cdoc.psi.impl.CDocSection
import org.cangnova.cangjie.lexer.cdoc.psi.impl.CDocTag

data class CDocContent(
    val contentTag: CDocTag,
    val sections: List<CDocSection>
)