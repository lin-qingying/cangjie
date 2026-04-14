package org.cangnova.cangjie.analysis.references

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import org.cangnova.cangjie.analysis.api.analyze
import org.cangnova.cangjie.idea.references.AbstractCjReference
import org.cangnova.cangjie.idea.references.CjSimpleNameReference
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjBasicType
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjImportAlias
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.cangnova.cangjie.references.CangJiePsiReferenceProviderContributor

/**
 * 对齐 Kotlin `KtSimpleNameReference` / `KaFirSimpleNameReference` 的 simple-name 引用实现。
 */
internal class CaSimpleNameReference(
    expression: CjSimpleNameExpression,
) : CjSimpleNameReference(expression) {
    override val resolvesByNames: Collection<Name>
        get() = listOf(expression.referencedNameAsName)

    override fun getRangeInElement(): TextRange {
        return expression.referencedNameElement.textRange.shiftRight(-expression.textOffset)
    }

    override fun canRename(): Boolean = true

    override fun resolveTargetElements(): Collection<PsiElement> {
        return analyze(element) {
            element.resolveToSymbols()
                .asSequence()
                .mapNotNull { symbol -> symbol.getOriginalPsi() }
                .toList()
        }
    }

    override fun getVariants(): Array<Any> {
        val containingFile = element.containingFile as? CjFile ?: return emptyArray()
        return analyze(element) {
            containingFile.getFileScope()
                .availableNames
                .map(Name::asString)
                .toTypedArray()
        }
    }

    override fun getImportAlias(): CjImportAlias? {
        val containingFile = element.containingFile as? CjFile ?: return null
        return containingFile.findImportByAlias(element.referencedName)
            ?.let { importInfo -> importInfo as? org.cangnova.cangjie.psi.CjImportItem }
            ?.alias
    }

    class Provider : CangJiePsiReferenceProviderContributor<CjSimpleNameExpression> {
        override val elementClass: Class<CjSimpleNameExpression>
            get() = CjSimpleNameExpression::class.java

        override val referenceProvider: CangJiePsiReferenceProviderContributor.ReferenceProvider<CjSimpleNameExpression>
            get() = CangJiePsiReferenceProviderContributor.ReferenceProvider { expression ->
                if (expression.isDeclarationNameReference()) {
                    emptyList()
                } else {
                    listOf(CaSimpleNameReference(expression))
                }
            }
    }
}

/**
 * 仓颉基础类型在词法上是关键字，但在语义上仍然属于可导航的类型引用。
 */
internal class CaBasicTypeReference(
    element: CjBasicType,
) : AbstractCjReference<CjBasicType>(element) {
    override val resolvesByNames: Collection<Name>
        get() = listOf(Name.identifier(element.name))

    override fun getRangeInElement(): TextRange = element.textRange.shiftRight(-element.textOffset)

    override fun canRename(): Boolean = true

    override fun resolveTargetElements(): Collection<PsiElement> {
        val containingFile = element.containingFile as? CjFile ?: return emptyList()
        val typeName = element.name

        return analyze(element) {
            containingFile.getFileScope()
                .getSymbols(Name.identifier(typeName))
                .asSequence()
                .mapNotNull { symbol -> symbol.getOriginalPsi() }
                .toList()
        }
    }

    override fun getVariants(): Array<Any> {
        val containingFile = element.containingFile as? CjFile ?: return emptyArray()
        return analyze(element) {
            containingFile.getFileScope()
                .availableNames
                .map(Name::asString)
                .toTypedArray()
        }
    }

    class Provider : CangJiePsiReferenceProviderContributor<CjBasicType> {
        override val elementClass: Class<CjBasicType>
            get() = CjBasicType::class.java

        override val referenceProvider: CangJiePsiReferenceProviderContributor.ReferenceProvider<CjBasicType>
            get() = CangJiePsiReferenceProviderContributor.ReferenceProvider { basicType ->
                listOf(CaBasicTypeReference(basicType))
            }
    }
}

private fun CjSimpleNameExpression.isDeclarationNameReference(): Boolean {
    if (this is org.cangnova.cangjie.psi.CjBindingPattern) {
        return true
    }
    val owner = (this as? PsiNameIdentifierOwner) ?: (parent as? PsiNameIdentifierOwner) ?: return false
    val identifier = identifier ?: return false
    return owner.nameIdentifier == identifier
}
