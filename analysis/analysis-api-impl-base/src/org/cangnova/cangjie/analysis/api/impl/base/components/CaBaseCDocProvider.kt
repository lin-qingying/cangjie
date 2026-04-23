package org.cangnova.cangjie.analysis.api.impl.base.components

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.CaNonPublicApi
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.components.CaCDocProvider
import org.cangnova.cangjie.analysis.api.components.allOverriddenSymbols
import org.cangnova.cangjie.analysis.api.components.getExpectsForActual
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaNamedFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPropertyAccessorSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPropertySymbol
import org.cangnova.cangjie.analysis.api.symbols.CaValueParameterSymbol
import org.cangnova.cangjie.lexer.cdoc.parser.CDocKnownTag
import org.cangnova.cangjie.lexer.cdoc.psi.CDoc
import org.cangnova.cangjie.lexer.cdoc.psi.api.CDocCommentDescriptor
import org.cangnova.cangjie.lexer.cdoc.psi.impl.CDocCommentDescriptorImpl
import org.cangnova.cangjie.lexer.cdoc.psi.impl.CDocSection
import org.cangnova.cangjie.lexer.cdoc.psi.impl.CDocTag
import org.cangnova.cangjie.psi.CjConstructor
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjDeclarationWithBody
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjParameter
import org.cangnova.cangjie.psi.CjPrimaryConstructor
import org.cangnova.cangjie.psi.CjProperty
import org.cangnova.cangjie.psi.CjTypeParameter
import org.cangnova.cangjie.psi.CjTypeStatement
import org.cangnova.cangjie.psi.CjNonPublicApi
import org.cangnova.cangjie.psi.CjImplementationDetail
import org.cangnova.cangjie.psi.psiUtil.findDescendantOfType
import org.cangnova.cangjie.psi.psiUtil.getChildOfType
import org.cangnova.cangjie.psi.psiUtil.getChildrenOfType
import org.cangnova.cangjie.psi.psiUtil.isPropertyParameter
import org.cangnova.cangjie.utils.toLowerCaseAsciiOnly

@CaImplementationDetail
@OptIn(CjNonPublicApi::class, CjImplementationDetail::class, CaNonPublicApi::class)
abstract class CaBaseCDocProvider<T : CaSession> : CaBaseSessionComponent<T>(), CaCDocProvider {
    override fun CjDeclaration.findCDoc(): CDocCommentDescriptor? = this.lookupOwnedCDoc() ?: this.lookupCDocInParent()

    override fun CaDeclarationSymbol.findCDoc(): CDocCommentDescriptor? = with(analysisSession) {
        val cjElement = psi?.navigationElement as? CjDeclaration
        cjElement?.findCDoc()?.let { return it }

        if (this@findCDoc is CaCallableSymbol) {
            allOverriddenSymbols.forEach { overridden ->
                overridden.findCDoc()?.let { return it }
            }
        }

        if (this@findCDoc is CaValueParameterSymbol) {
            val containingSymbol = containingDeclaration as? CaNamedFunctionSymbol
            if (containingSymbol != null) {
                val index = containingSymbol.valueParameters.indexOf(this@findCDoc)
                containingSymbol.getExpectsForActual()
                    .filterIsInstance<CaNamedFunctionSymbol>()
                    .firstNotNullOfOrNull { expectFunction ->
                        expectFunction.valueParameters.getOrNull(index)?.findCDoc()
                    }
                    ?.let { return it }
            }
        }

        if (this@findCDoc is CaPropertyAccessorSymbol) {
            val containingProperty = containingDeclaration as? CaPropertySymbol
            containingProperty?.findCDoc()?.let { return it }
        }

        getExpectsForActual().firstNotNullOfOrNull { expectSymbol ->
            expectSymbol.findCDoc()
        }?.let { return it }

        null
    }
    private fun CjElement.lookupOwnedCDoc(): CDocCommentDescriptor? {
        val psiDeclaration = when (this) {
            is CjPrimaryConstructor -> getContainingTypeStatement()
            else -> this
        }

        if (psiDeclaration is CjDeclaration) {
            val cdoc = psiDeclaration.docComment
            if (cdoc != null) {
                if (this is CjConstructor<*>) {
                    val constructorSection = cdoc.findSectionByTag(CDocKnownTag.CONSTRUCTOR)
                    if (constructorSection != null) {
                        val paramSections = cdoc.findSectionsContainingTag(CDocKnownTag.PARAM)
                        return CDocCommentDescriptorImpl(constructorSection, paramSections)
                    }
                }

                return CDocCommentDescriptorImpl(cdoc.getDefaultSection(), cdoc.getAllSections())
            }
        }

        return null
    }

    private fun CDoc.findSectionsContainingTag(tag: CDocKnownTag): List<CDocSection> {
        return getChildrenOfType<CDocSection>()
            .filter { it.findTagByName(tag.name.toLowerCaseAsciiOnly()) != null }
    }

    private fun CjDeclaration.lookupCDocInParent(): CDocCommentDescriptor? {
        val subjectName = name
        val containingDeclaration = PsiTreeUtil.findFirstParent(this, true) {
            (it is CjDeclarationWithBody && it !is CjPrimaryConstructor) || it is CjTypeStatement
        }

        val containerCDoc = containingDeclaration?.getChildOfType<CDoc>()
        if (containerCDoc == null || subjectName == null) return null

        val propertySection = containerCDoc.findSectionByTag(CDocKnownTag.PROPERTY, subjectName)
        val paramTag = containerCDoc.findDescendantOfType<CDocTag> {
            it.knownTag == CDocKnownTag.PARAM && it.getSubjectName() == subjectName
        }

        val primaryContent = when (this) {
            is CjParameter if this.isPropertyParameter() -> propertySection ?: paramTag
            is CjParameter, is CjTypeParameter -> paramTag
            is CjProperty if containingDeclaration is CjTypeStatement -> propertySection
            else -> null
        }

        return primaryContent?.let {
            CDocCommentDescriptorImpl(it, additionalSections = emptyList())
        }
    }
}
