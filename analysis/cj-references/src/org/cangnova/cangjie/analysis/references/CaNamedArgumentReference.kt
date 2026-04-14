package org.cangnova.cangjie.analysis.references

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.analyze
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaValueParameterOwnerSymbol
import org.cangnova.cangjie.idea.references.AbstractCjReference
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjCallableDeclaration
import org.cangnova.cangjie.psi.CjTypeStatement
import org.cangnova.cangjie.psi.CjValueArgument
import org.cangnova.cangjie.psi.CjValueArgumentName
import org.cangnova.cangjie.references.CangJiePsiReferenceProviderContributor

/**
 * 命名实参名本身应直接指向其映射到的形参声明。
 */
internal class CaNamedArgumentReference(
    element: CjValueArgumentName,
) : AbstractCjReference<CjValueArgumentName>(element) {
    override val resolvesByNames: Collection<Name>
        get() = listOf(element.asName)

    override fun getRangeInElement(): TextRange {
        return element.referenceExpression.referencedNameElement.textRange.shiftRight(-element.textOffset)
    }

    override fun canRename(): Boolean = true

    override fun resolveTargetElements(): Collection<PsiElement> {
        val valueArgument = element.parent as? CjValueArgument ?: return emptyList()
        val argumentIndex = valueArgument.argumentIndexInCall() ?: return emptyList()
        val callOwner = valueArgument.containingCallOwner() ?: return emptyList()
        val mappedParameters = callOwner.resolveMappedValueParameters(argumentIndex)
        if (mappedParameters.isNotEmpty()) return mappedParameters

        val parameterName = element.asName
        return analyze(callOwner) {
            val callInfo = callOwner.resolveToCall() ?: return@analyze emptyList()
            val candidateCalls = buildList {
                callInfo.successfulCall?.let(::add)
                if (callInfo.successfulCall == null) {
                    addAll(callInfo.calls)
                }
            }.ifEmpty { callInfo.calls }

            candidateCalls.asSequence()
                .mapNotNull { call -> call.target as? CaValueParameterOwnerSymbol }
                .flatMap { owner -> owner.valueParameters.asSequence() }
                .filter { parameter -> parameter.name == parameterName }
                .mapNotNull { parameter -> (parameter as? CaDeclarationSymbol)?.psi }
                .toCollection(linkedSetOf())
                .toList()
        }.ifEmpty {
            callOwner.resolveCallTargetPsis()
                .asSequence()
                .flatMap { declaration ->
                    when (declaration) {
                        is CjCallableDeclaration -> declaration.valueParameters.asSequence()
                        is CjTypeStatement -> declaration.constructors
                            .asSequence()
                            .flatMap { constructor -> constructor.valueParameters.asSequence() }
                        else -> emptySequence()
                    }
                }
                .filter { parameter -> parameter.nameAsSafeName == parameterName }
                .distinct()
                .toList()
        }
    }

    override fun getVariants(): Array<Any> {
        val valueArgument = element.parent as? CjValueArgument ?: return emptyArray()
        val callOwner = valueArgument.containingCallOwner() ?: return emptyArray()
        return callOwner.resolveCallTargetPsis()
            .asSequence()
            .filterIsInstance<CjCallableDeclaration>()
            .flatMap { declaration -> declaration.valueParameters.asSequence() }
            .mapNotNull { parameter -> parameter.name }
            .distinct()
            .toList()
            .toTypedArray()
    }

    class Provider : CangJiePsiReferenceProviderContributor<CjValueArgumentName> {
        override val elementClass: Class<CjValueArgumentName>
            get() = CjValueArgumentName::class.java

        override val referenceProvider: CangJiePsiReferenceProviderContributor.ReferenceProvider<CjValueArgumentName>
            get() = CangJiePsiReferenceProviderContributor.ReferenceProvider { argumentName ->
                listOf(CaNamedArgumentReference(argumentName))
            }
    }
}
