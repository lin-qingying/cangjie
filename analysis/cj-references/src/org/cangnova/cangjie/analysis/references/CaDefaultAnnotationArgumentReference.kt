package org.cangnova.cangjie.analysis.references

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.idea.references.AbstractCangJieReference
import org.cangnova.cangjie.psi.CjAnnotation
import org.cangnova.cangjie.psi.CjValueArgument
import org.cangnova.cangjie.references.CangJiePsiReferenceProviderContributor

/**
 * 对齐 Kotlin `KtDefaultAnnotationArgumentReference`。
 *
 * 当注解调用只有一个未命名实参时，该实参位置应直接指向构造器的目标形参。
 */
internal class CaDefaultAnnotationArgumentReference(
    element: CjValueArgument,
) : AbstractCangJieReference<CjValueArgument>(element) {
    override fun getRangeInElement(): TextRange = TextRange.EMPTY_RANGE

    override fun resolveTargetElements(): Collection<PsiElement> {
        val annotation = element.containingAnnotation() ?: return emptyList()
        return annotation.resolveMappedValueParameters(argumentIndex = 0)
    }

    class Provider : CangJiePsiReferenceProviderContributor<CjValueArgument> {
        override val elementClass: Class<CjValueArgument>
            get() = CjValueArgument::class.java

        override val referenceProvider: CangJiePsiReferenceProviderContributor.ReferenceProvider<CjValueArgument>
            get() = CangJiePsiReferenceProviderContributor.ReferenceProvider { argument ->
                if (argument.shouldProduceReference()) {
                    listOf(CaDefaultAnnotationArgumentReference(argument))
                } else {
                    emptyList()
                }
            }
    }
}

private fun CjValueArgument.shouldProduceReference(): Boolean {
    if (isNamed()) return false
    val annotation = containingAnnotation() ?: return false
    return annotation.valueArguments.size == 1
}

private fun CjValueArgument.containingAnnotation(): CjAnnotation? {
    return generateSequence(parent) { current -> current.parent }
        .filterIsInstance<CjAnnotation>()
        .firstOrNull()
}
