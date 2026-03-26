package org.cangnova.cangjie.cfir.resolve.calls

import org.cangnova.cangjie.cfir.CfirResolvable
import org.cangnova.cangjie.cfir.declarations.CfirAnonymousFunction
import org.cangnova.cangjie.cfir.expressions.CfirAnonymousFunctionExpression
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirNamedReferenceWithCandidate
import org.cangnova.cangjie.cfir.resovle.calls.ConeTypeVariableForLambdaReturnType
import org.cangnova.cangjie.cfir.semantics.AbstractConeResolutionAtom
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.resolve.calls.model.LambdaWithTypeVariableAsExpectedTypeMarker
import org.cangnova.cangjie.resolve.calls.model.PostponedAtomWithRevisableExpectedType
import org.cangnova.cangjie.resolve.calls.model.PostponedCallableReferenceMarker
import org.cangnova.cangjie.resolve.calls.model.PostponedResolvedAtomMarker
import org.cangnova.cangjie.type.model.CangJieTypeMarker

sealed class ConeResolutionAtom : AbstractConeResolutionAtom() {
    abstract override val expression: CfirExpression

    companion object {
        @JvmName("createRawAtomNullable")
        fun createRawAtom(expression: CfirExpression?): ConeResolutionAtom? {
            return expression?.let(::createRawAtom)
        }

        fun createRawAtom(expression: CfirExpression): ConeResolutionAtom {
            return when (expression) {
                is CfirAnonymousFunctionExpression -> ConeResolutionAtomWithPostponedChild(expression)
                is CfirBlock -> {
                    val childExpression = expression.statements.lastOrNull() as? CfirExpression
                    ConeResolutionAtomWithSingleChild(
                        expression = expression,
                        subAtom = childExpression?.let { createRawAtom(it) },
                    )
                }
                is CfirResolvable -> createRawAtomForResolvable(expression)
                else -> ConeSimpleLeafResolutionAtom(expression)
            }
        }

        private fun createRawAtomForResolvable(expression: CfirResolvable): ConeResolutionAtom {
            val candidate = (expression.calleeReference as? CfirNamedReferenceWithCandidate)?.candidate
            val cfirExpression = expression as? CfirExpression
                ?: error("Resolvable argument is expected to be an expression: ${expression::class}")
            return if (candidate != null) {
                ConeAtomWithCandidate(cfirExpression, candidate)
            } else {
                ConeSimpleLeafResolutionAtom(cfirExpression)
            }
        }
    }
}

class ConeSimpleLeafResolutionAtom(
    override val expression: CfirExpression,
    @Suppress("UNUSED_PARAMETER") allowUnresolvedExpression: Boolean = true,
) : ConeResolutionAtom()

class ConeAtomWithCandidate(
    override val expression: CfirExpression,
    val candidate: Candidate,
) : ConeResolutionAtom()

class ConeResolutionAtomWithSingleChild(
    override val expression: CfirExpression,
    val subAtom: ConeResolutionAtom?,
) : ConeResolutionAtom()

sealed class ConePostponedResolvedAtom : ConeResolutionAtom(), PostponedResolvedAtomMarker {
    abstract override val inputTypes: Collection<ConeCangJieType>
    abstract override val outputType: ConeCangJieType?
    abstract override val expectedType: ConeCangJieType?
    final override var analyzed: Boolean = false
}

class ConeResolutionAtomWithPostponedChild(
    override val expression: CfirExpression,
    val fallbackSubAtom: ConeResolutionAtom? = null,
) : ConeResolutionAtom() {
    var subAtom: ConeResolutionAtom? = null
        internal set

    fun setPostponedSubAtom(atom: ConePostponedResolvedAtom) {
        require(subAtom == null) { "subAtom already initialized" }
        subAtom = atom
    }

    fun useFallbackSubAtom() {
        subAtom = fallbackSubAtom
    }
}

object ConeResolutionAtomFactory {
    fun create(expression: CfirExpression): ConeResolutionAtom = ConeResolutionAtom.createRawAtom(expression)

    fun createWithCandidate(expression: CfirExpression, candidate: Candidate): ConeResolutionAtom {
        return ConeAtomWithCandidate(expression, candidate)
    }
}

sealed class ConeFunctionTypeRelatedPostponedResolvedAtom : ConePostponedResolvedAtom()

class ConeResolvedLambdaAtom(
    override val expression: CfirExpression,
    val anonymousFunction: CfirAnonymousFunction,
    expectedType: ConeCangJieType?,
    val parameterTypes: List<ConeCangJieType>,
    returnType: ConeCangJieType,
    typeVariableForLambdaReturnType: ConeTypeVariableForLambdaReturnType? = null,
) : ConeFunctionTypeRelatedPostponedResolvedAtom() {
    override val inputTypes: Collection<ConeCangJieType>
        get() = parameterTypes

    override var outputType: ConeCangJieType = returnType
        private set

    override var expectedType: ConeCangJieType? = expectedType
        private set

    var typeVariableForLambdaReturnType: ConeTypeVariableForLambdaReturnType? = typeVariableForLambdaReturnType
        private set

    var returnStatements: Collection<ConeResolutionAtom> = emptyList()
        internal set

    val returnType: ConeCangJieType
        get() = outputType

    fun replaceExpectedType(expectedType: ConeCangJieType, newReturnType: ConeCangJieType? = null) {
        this.expectedType = expectedType
        if (newReturnType != null) {
            outputType = newReturnType
        }
    }

    fun replaceTypeVariableForLambdaReturnType(variable: ConeTypeVariableForLambdaReturnType) {
        typeVariableForLambdaReturnType = variable
    }
}

sealed class ConePostponedAtomWithRevisableExpectedType :
    ConeFunctionTypeRelatedPostponedResolvedAtom(),
    PostponedAtomWithRevisableExpectedType

class ConeLambdaWithTypeVariableAsExpectedTypeAtom(
    override val expression: CfirExpression,
    val anonymousFunction: CfirAnonymousFunction,
    override val expectedType: ConeCangJieType,
    val candidateOfOuterCall: Candidate,
    val anonymousFunctionIfReturnExpression: CfirAnonymousFunction? = null,
) : ConePostponedAtomWithRevisableExpectedType(), LambdaWithTypeVariableAsExpectedTypeMarker {
    override var revisedExpectedType: CangJieTypeMarker? = null
        private set

    override var parameterTypesFromDeclaration: List<CangJieTypeMarker?>? = null
        private set

    override val inputTypes: Collection<ConeCangJieType> = emptyList()
    override val outputType: ConeCangJieType? = null

    var subAtom: ConeResolvedLambdaAtom? = null
        internal set

    override fun reviseExpectedType(expectedType: CangJieTypeMarker) {
        revisedExpectedType = expectedType
    }

    override fun updateParameterTypesFromDeclaration(types: List<CangJieTypeMarker?>?) {
        parameterTypesFromDeclaration = types
    }
}

class ConeResolvedCallableReferenceAtom(
    override val expression: CfirExpression,
    override val expectedType: ConeCangJieType?,
) : ConePostponedAtomWithRevisableExpectedType(), PostponedCallableReferenceMarker {
    override val inputTypes: Collection<ConeCangJieType> = emptyList()
    override val outputType: ConeCangJieType? = null

    override var revisedExpectedType: CangJieTypeMarker? = null
        private set

    override fun reviseExpectedType(expectedType: CangJieTypeMarker) {
        revisedExpectedType = expectedType
    }
}

class ConeSimpleNameForContextSensitiveResolution(
    override val expression: CfirExpression,
    val containingCallCandidate: Candidate,
    val fallbackSubAtom: ConeResolutionAtom? = null,
) : ConePostponedResolvedAtom() {
    override val inputTypes: Collection<ConeCangJieType> = emptyList()
    override val outputType: ConeCangJieType? = null
    override val expectedType: ConeCangJieType? = null
}

class ConeContextSensitiveAlternativeForQualifierAtom(
    override val expression: CfirExpression,
) : ConePostponedResolvedAtom() {
    override val inputTypes: Collection<ConeCangJieType> = emptyList()
    override val outputType: ConeCangJieType? = null
    override val expectedType: ConeCangJieType? = null
}
