package org.cangnova.cangjie.lexer.cdoc.psi.impl

import org.cangnova.cangjie.lexer.cdoc.psi.api.CDocCommentDescriptor
import org.cangnova.cangjie.psi.CjImplementationDetail
import org.cangnova.cangjie.psi.CjNonPublicApi

@CjNonPublicApi
@CjImplementationDetail
class CDocCommentDescriptorImpl(
    override val primaryTag: CDocTag,
    override val additionalSections: List<CDocSection>,
) : CDocCommentDescriptor
