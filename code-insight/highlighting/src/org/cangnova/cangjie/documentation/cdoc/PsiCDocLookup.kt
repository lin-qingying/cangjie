package org.cangnova.cangjie.documentation.cdoc

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.lexer.cdoc.CDocContent
import org.cangnova.cangjie.lexer.cdoc.parser.CDocKnownTag
import org.cangnova.cangjie.lexer.cdoc.psi.CDoc
import org.cangnova.cangjie.lexer.cdoc.psi.impl.CDocSection
import org.cangnova.cangjie.lexer.cdoc.psi.impl.CDocTag
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjDeclarationWithBody
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjParameter
import org.cangnova.cangjie.psi.CjPrimaryConstructor
import org.cangnova.cangjie.psi.CjProperty
import org.cangnova.cangjie.psi.CjTypeParameter
import org.cangnova.cangjie.psi.CjTypeStatement
import org.cangnova.cangjie.psi.psiUtil.findDescendantOfType
import org.cangnova.cangjie.psi.psiUtil.getChildOfType
import org.cangnova.cangjie.psi.psiUtil.getChildrenOfType
import org.cangnova.cangjie.psi.psiUtil.isPropertyParameter
import org.cangnova.cangjie.utils.toLowerCaseAsciiOnly

/** 按现有 PSI 所有权规则查找声明、参数或属性对应的 CDoc。 */
fun CjElement.findCDocByPsi(): CDocContent? =
    lookupOwnedCDoc() ?: lookupCDocInContainer()

private fun CjElement.lookupCDocInContainer(): CDocContent? {
    val subjectName = name
    val containingDeclaration = PsiTreeUtil.findFirstParent(this, true) {
        it is CjDeclarationWithBody && it !is CjPrimaryConstructor || it is CjTypeStatement
    }
    val containerCDoc = containingDeclaration?.getChildOfType<CDoc>() ?: return null
    if (subjectName == null) return null

    val propertySection = containerCDoc.findSectionByTag(CDocKnownTag.PROPERTY, subjectName)
    val paramTag = containerCDoc.findDescendantOfType<CDocTag> {
        it.knownTag == CDocKnownTag.PARAM && it.getSubjectName() == subjectName
    }
    val primaryContent = when {
        this is CjParameter && isPropertyParameter() -> propertySection ?: paramTag
        this is CjParameter || this is CjTypeParameter -> paramTag
        this is CjProperty && containingDeclaration is CjTypeStatement -> propertySection
        else -> null
    }
    return primaryContent?.let { CDocContent(it, sections = emptyList()) }
}

private fun CjElement.lookupOwnedCDoc(): CDocContent? {
    val psiDeclaration = when (this) {
        is CjPrimaryConstructor -> getContainingTypeStatement()
        else -> this
    }
    if (psiDeclaration !is CjDeclaration) return null
    val cdoc = psiDeclaration.docComment ?: return null

    if (this is CjPrimaryConstructor) {
        val constructorSection = cdoc.findSectionByTag(CDocKnownTag.CONSTRUCTOR)
        if (constructorSection != null) {
            val parameterSections = cdoc.findSectionsContainingTag(CDocKnownTag.PARAM)
            return CDocContent(constructorSection, parameterSections)
        }
    }
    return CDocContent(cdoc.getDefaultSection(), cdoc.getAllSections())
}

private fun CDoc.findSectionsContainingTag(tag: CDocKnownTag): List<CDocSection> =
    getChildrenOfType<CDocSection>()
        .filter { it.findTagByName(tag.name.toLowerCaseAsciiOnly()) != null }
