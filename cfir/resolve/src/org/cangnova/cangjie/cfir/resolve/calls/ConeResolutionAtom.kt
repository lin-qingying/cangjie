package org.cangnova.cangjie.cfir.resolve.calls

import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.semantics.AbstractConeResolutionAtom
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.type.model.CangJieTypeMarker

//  -------------------------- Atoms --------------------------

/**
 * [ConeResolutionAtom] is an abstract representation calls component (like argument or receiver) which is needed
 *   for convenient representation of all different kinds of arguments in the original order. Regular expressions cannot be
 *   used for this purpose, as we have postponed arguments in the resolution logic (callable references and lambdas), which
 *   require some additional information for resolution, which is provided on different steps of the resolution pipeline. So atoms
 *   allow tracking all those transformations and keep the original structure of the call at the same time.
 *
 * There are multiple different kinds of atoms:
 * - [ConeAtomWithCandidate] used for arguments, which are not completed yet and contain [Candidate] inside
 * - [ConeResolutionAtomWithSingleChild] used for expressions, which wrap some other expression. Atom of underlying expression
 *     is stored inside [ConeResolutionAtomWithSingleChild.subAtom]
 * - [ConeResolutionAtomWithPostponedChild] is created for postponed arguments at the first stage of argument processing
 *     and contain mutable [subAtom] property for more specific [ConePostponedResolvedAtom] which will be initialized later
 * - [ConePostponedResolvedAtom] and its inheritors are special atoms for proper lambda and callable reference resolution
 * - [ConeSimpleLeafResolutionAtom] is an atom for regular already completed expressions
 */
sealed class ConeResolutionAtom : AbstractConeResolutionAtom() {
    abstract override val expression: CfirExpression

    companion object {
        @JvmName("createRawAtomNullable")
        fun createRawAtom(expression: CfirExpression?): ConeResolutionAtom? {
            return createRawAtom(expression, allowUnresolvedExpression = false)
        }

        fun createRawAtom(expression: CfirExpression): ConeResolutionAtom {
            return createRawAtom(expression, allowUnresolvedExpression = false)!!
        }

        /**
         * Creation of atoms based on potentially unresolved arguments is unsafe, as most of the atoms consumers
         *   expect that `ConeResolutionAtom.expression.resolvedType` is initialized. This way of atom creation
         *   is allowed to use only for creating arguments mapping with a condition that those atoms won't be used
         *   in the further resolution pipeline (like in `CheckArguments` stage or in call completion)
         *
         * The only known case for such potentially unresolved atoms is annotation resolution, which pipeline
         *   is following:
         * - create argument/parameter mapping
         * - analyze arguments with an expected type from corresponding parameters
         * - run proper resolution sequence for annotation constructor call (with creation of new atoms)
         */
        @UnsafeExpressionUtility
        fun createRawAtomForPotentiallyUnresolvedExpression(expression: CfirExpression): ConeResolutionAtom {
            return createRawAtom(expression, allowUnresolvedExpression = true)!!
        }

        private fun createRawAtom(expression: CfirExpression?, allowUnresolvedExpression: Boolean): ConeResolutionAtom? {
            fun CfirExpression.createConeResolutionAtomWithSingleChild(subExpression: CfirExpression?): ConeResolutionAtomWithSingleChild {
                return ConeResolutionAtomWithSingleChild(this, createRawAtom(subExpression, allowUnresolvedExpression))
            }
            return when (expression) {
                null -> null
                is CfirAnonymousFunctionExpression -> ConeResolutionAtomWithPostponedChild(expression)
                is CfirCallableReferenceAccess -> when {
                    expression.hasResolvedType -> ConeSimpleLeafResolutionAtom(expression, allowUnresolvedExpression)
                    else -> ConeResolutionAtomWithPostponedChild(expression)
                }
                is CfirPropertyAccessExpression -> when {
                    expression.shouldBeResolvedInContextSensitiveMode() || expression.shouldAlternativeBeResolved() ->
                        ConeResolutionAtomWithPostponedChild(
                            expression,
                            fallbackSubAtom = createRawAtomForResolvable(expression, allowUnresolvedExpression),
                        )
                    else -> createRawAtomForResolvable(expression, allowUnresolvedExpression)
                }
                is CfirResolvedQualifier if expression.shouldAlternativeBeResolved() -> {
                    ConeResolutionAtomWithPostponedChild(
                        expression,
                        fallbackSubAtom = createRawAtomForResolvable(expression, allowUnresolvedExpression),
                    )
                }
                is CfirCollectionLiteral -> ConeResolutionAtomWithPostponedChild(expression)
                is CfirResolvable -> createRawAtomForResolvable(expression, allowUnresolvedExpression)
                is CfirSafeCallExpression -> expression.createConeResolutionAtomWithSingleChild(
                    (expression.selector as? CfirExpression)?.unwrapSmartcastExpression()
                )
                is CfirWrappedArgumentExpression -> expression.createConeResolutionAtomWithSingleChild(expression.expression)
                is CfirErrorExpression -> expression.createConeResolutionAtomWithSingleChild(expression.expression)
                is CfirQualifiedErrorAccessExpression -> expression.createConeResolutionAtomWithSingleChild(expression.selector)
                is CfirBlock -> expression.createConeResolutionAtomWithSingleChild(expression.lastExpression)
                else -> ConeSimpleLeafResolutionAtom(expression, allowUnresolvedExpression)
            }
        }

        private fun CfirQualifierWithContextSensitiveAlternative.shouldAlternativeBeResolved(): Boolean {
            // It's ok to opt in here because it's only not-null in ideMode
            return (@OptIn(CfirIdeOnly::class) contextSensitiveAlternative) != null
        }

        private fun createRawAtomForResolvable(
            expression: CfirExpression,
            allowUnresolvedExpression: Boolean,
        ): ConeResolutionAtom =
            when (val candidate = (expression as? CfirResolvable)?.candidate()) {
                null -> ConeSimpleLeafResolutionAtom(expression, allowUnresolvedExpression)
                else -> ConeAtomWithCandidate(expression, candidate)
            }
    }
}

class ConeResolutionAtomWithSingleChild(override val expression: CfirExpression, val subAtom: ConeResolutionAtom?) : ConeResolutionAtom()

class ConeSimpleLeafResolutionAtom(override val expression: CfirExpression, allowUnresolvedExpression: Boolean) : ConeResolutionAtom() {
    init {
        if (AbstractTypeChecker.RUN_SLOW_ASSERTIONS) {
            checkWithAttachment(
                allowUnresolvedExpression ||
                        expression.unwrapArgument() is CfirFakeArgumentForCallableReference ||
                        expression.hasResolvedType,
                { "ConeResolvedAtom should be created only for resolved expressions" }
            ) {
                withCfirEntry("expression", expression)
            }
        }
    }
}

//  -------------------------- Not-resolved atoms --------------------------

class ConeAtomWithCandidate(override val expression: CfirExpression, val candidate: Candidate) : ConeResolutionAtom()

class ConeResolutionAtomWithPostponedChild(
    override val expression: CfirExpression,
    // Used for cases like when simple name access doesn't need context-sensitive resolution
    val fallbackSubAtom: ConeResolutionAtom? = null,
) : ConeResolutionAtom() {

    var subAtom: ConeResolutionAtom? = null
        private set(value) {
            require(field == null) { "subAtom already initialized" }
            field = value
        }

    fun setPostponedSubAtom(atom: ConePostponedResolvedAtom) {
        subAtom = atom
    }

    fun useFallbackSubAtom() {
        subAtom = fallbackSubAtom
    }

    fun useFallbackForDisabledCollectionLiterals() {
        require(expression is CfirCollectionLiteral) {
            "expected atom with ${CfirCollectionLiteral::class.simpleName}, got ${expression::class.simpleName}"
        }
        subAtom = ConeSimpleLeafResolutionAtom(expression, allowUnresolvedExpression = false)
    }

    fun makeFreshCopy(): ConeResolutionAtomWithPostponedChild = ConeResolutionAtomWithPostponedChild(expression, fallbackSubAtom)
}

sealed class ConePostponedResolvedAtom : ConeResolutionAtom(), PostponedResolvedAtomMarker {
    abstract override val inputTypes: Collection<ConeCangJieType>
    abstract override val outputType: ConeCangJieType?
    override var analyzed: Boolean = false
    abstract override val expectedType: ConeCangJieType?
}

//  ------------- Lambdas -------------

// A lambda or a callable reference.
// We separate this kind of atom because for them, we might fix earlier type variables contained inside the parameter
// type of the relevant function expected type.
sealed class ConeFunctionTypeRelatedPostponedResolvedAtom : ConePostponedResolvedAtom()

class ConeResolvedLambdaAtom(
    override val expression: CfirAnonymousFunctionExpression,
    expectedType: ConeCangJieType?,
    val expectedFunctionTypeKind: FunctionTypeKind?,
    internal val receiverType: ConeCangJieType?,
    internal val contextParameterTypes: List<ConeCangJieType>,
    internal val parameterTypes: List<ConeCangJieType>,
    var returnType: ConeCangJieType,
    typeVariableForLambdaReturnType: ConeTypeVariableForLambdaReturnType?,
    val coerceCfirstParameterToExtensionReceiver: Boolean,
    // NB: It's not null right now only for lambdas inside the calls
    // TODO: Handle somehow that kind of lack of information once KT-67961 is fixed
    val sourceForFunctionExpression: CjSourceElement?,
) : ConeFunctionTypeRelatedPostponedResolvedAtom() {
    val anonymousFunction: CfirAnonymousFunction = expression.anonymousFunction

    var typeVariableForLambdaReturnType: ConeTypeVariableForLambdaReturnType? = typeVariableForLambdaReturnType
        private set

    override var expectedType: ConeCangJieType? = expectedType
        private set

    lateinit var returnStatements: Collection<ConeResolutionAtom>

    override val inputTypes: Collection<ConeCangJieType>
        get() {
            if (receiverType == null && contextParameterTypes.isEmpty()) return parameterTypes
            return ArrayList<ConeCangJieType>(parameterTypes.size + contextParameterTypes.size + (if (receiverType != null) 1 else 0)).apply {
                addAll(parameterTypes)
                addIfNotNull(receiverType)
                addAll(contextParameterTypes)
            }
        }

    override val outputType: ConeCangJieType get() = returnType

    fun replaceExpectedType(expectedType: ConeCangJieType, newReturnType: ConeTypeVariableType) {
        this.expectedType = expectedType
        this.returnType = newReturnType
    }

    fun replaceTypeVariableForLambdaReturnType(typeVariableForLambdaReturnType: ConeTypeVariableForLambdaReturnType) {
        this.typeVariableForLambdaReturnType = typeVariableForLambdaReturnType
    }
}

sealed class ConePostponedAtomWithRevisableExpectedType(
    /**
     * If the atom is created for a return statement of the lambda, its anonymous function is stored
     * to report RETURN_TYPE_MISMATCH in case of a new constraint error.
     * Note that for other kinds of resolution atoms, the new constraint error, if any, can be reported right away
     * when creating the atom, hence no need to store this field.
     */
    val anonymousFunctionIfReturnExpression: CfirAnonymousFunction?
) : ConeFunctionTypeRelatedPostponedResolvedAtom(), PostponedAtomWithRevisableExpectedType

class ConeLambdaWithTypeVariableAsExpectedTypeAtom(
    override val expression: CfirAnonymousFunctionExpression,
    private val initialExpectedTypeType: ConeCangJieType,
    val candidateOfOuterCall: Candidate,
    anonymousFunctionIfReturnExpression: CfirAnonymousFunction? = null,
) : ConePostponedAtomWithRevisableExpectedType(anonymousFunctionIfReturnExpression), LambdaWithTypeVariableAsExpectedTypeMarker {
    val anonymousFunction: CfirAnonymousFunction = expression.anonymousFunction

    var subAtom: ConeResolvedLambdaAtom? = null
        set(value) {
            require(field == null) { "subAtom already initialized" }
            field = value
        }

    override var parameterTypesFromDeclaration: List<ConeCangJieType?>? = null
        private set

    override fun updateParameterTypesFromDeclaration(types: List<CangJieTypeMarker?>?) {
        @Suppress("UNCHECKED_CAST")
        types as List<ConeCangJieType?>?
        parameterTypesFromDeclaration = types
    }

    override val expectedType: ConeCangJieType
        get() = revisedExpectedType ?: initialExpectedTypeType

    override val inputTypes: Collection<ConeCangJieType> get() = listOf(initialExpectedTypeType)
    override val outputType: ConeCangJieType? get() = null
    override var revisedExpectedType: ConeCangJieType? = null
        private set

    override fun reviseExpectedType(expectedType: CangJieTypeMarker) {
        require(expectedType is ConeCangJieType)
        revisedExpectedType = expectedType
    }
}

//  ------------- References -------------

class ConeResolvedCallableReferenceAtom(
    override val expression: CfirCallableReferenceAccess,
    private val initialExpectedType: ConeCangJieType?,
    val lhs: DoubleColonLHS?,
    private val session: CfirSession,
    anonymousFunctionIfReturnExpression: CfirAnonymousFunction? = null,
) : ConePostponedAtomWithRevisableExpectedType(anonymousFunctionIfReturnExpression), PostponedCallableReferenceMarker {
    var subAtom: ConeAtomWithCandidate? = null
        private set

    enum class State(val needsResolution: Boolean) {
        // Regularly, the first time we resolve `::bar` of `foo(::bar)` at `EagerResolveOfCallableReferences` of `foo`
        // Might be transformed both to `POSTPONED_BECAUSE_OF_AMBIGUITY` or `RESOLVED`
        NOT_RESOLVED_YET(needsResolution = true),

        // Means that we should try resolving again at the completion stage when the expected type is ready
        // Might be transformed only to `RESOLVED`
        POSTPONED_BECAUSE_OF_AMBIGUITY(needsResolution = true),

        // That would correspond both to successful and failed results (including final ambiguity)
        RESOLVED(needsResolution = false),
    }

    var state: State = State.NOT_RESOLVED_YET

    val isPostponedBecauseOfAmbiguity: Boolean get() = state == State.POSTPONED_BECAUSE_OF_AMBIGUITY

    override val needsResolution: Boolean get() = state.needsResolution

    var resultingReference: CfirNamedReference? = null
        private set

    fun initializeResultingReference(resultingReference: CfirNamedReference) {
        require(this.resultingReference == null) { "resultingReference already initialized" }
        this.resultingReference = resultingReference
        this.state = State.RESOLVED
        val candidate = (resultingReference as? CfirNamedReferenceWithCandidate)?.candidate
        if (candidate != null) {
            subAtom = ConeAtomWithCandidate(expression, candidate)
        }
    }

    var resultingTypeForCallableReference: ConeCangJieType? = null

    override val inputTypes: Collection<ConeCangJieType>
        get() {
            // For not resolved references we don't expose input types because for the first time,
            // we should try resolving them immediately (effectively, they're not fully blown postponed atoms)
            if (state == State.NOT_RESOLVED_YET) return emptyList()
            return extractInputOutputTypesFromCallableReferenceExpectedType(expectedType, session)?.inputTypes
                ?: listOfNotNull(expectedType)
        }
    override val outputType: ConeCangJieType?
        get() {
            // For not resolved references we don't expose the output type because for the first time,
            // we should try resolving them immediately (effectively, they're not fully blown postponed atoms)
            if (state == State.NOT_RESOLVED_YET) return null
            return extractInputOutputTypesFromCallableReferenceExpectedType(expectedType, session)?.outputType
        }

    override val expectedType: ConeCangJieType?
        // TODO: Consider changing `!isPostponedBecauseOfAmbiguity` to `state == State.NOT_RESOLVED_YET` (KT-74021)
        get() = if (!isPostponedBecauseOfAmbiguity)
            initialExpectedType
        else
            revisedExpectedType ?: initialExpectedType

    override var revisedExpectedType: ConeCangJieType? = null
        // TODO: Consider simplifying this (KT-74021)
        get() = if (isPostponedBecauseOfAmbiguity) field else expectedType
        private set

    override fun reviseExpectedType(expectedType: CangJieTypeMarker) {
        require(expectedType is ConeCangJieType)
        revisedExpectedType = expectedType
    }
}

class ConeSimpleNameForContextSensitiveResolution(
    override val expression: CfirPropertyAccessExpression,
    override val expectedType: ConeCangJieType,
    val containingCallCandidate: Candidate,
    val fallbackSubAtom: ConeResolutionAtom,
) : ConePostponedResolvedAtom() {
    override val inputTypes: Collection<ConeCangJieType> = listOf(expectedType)
    override val outputType: ConeCangJieType?
        get() = null
}

class ConeContextSensitiveAlternativeForQualifierAtom @CfirIdeOnly constructor(
    val originalExpression: CfirQualifierWithContextSensitiveAlternative,
    val alternative: CfirPropertyAccessExpression,
    override val expectedType: ConeCangJieType,
) : ConePostponedResolvedAtom() {
    override val inputTypes: Collection<ConeCangJieType> = listOf(expectedType)
    override val outputType: ConeCangJieType?
        get() = null

    override val expression: CfirExpression
        get() = originalExpression as CfirExpression

    // Generally, all the call-site might just assign `analyzed = true` themselves, but this method might help to highlight the places
    // where we discard the alternative
    fun markDiscarded() {
        analyzed = true
        originalExpression.replaceContextSensitiveAlternative(null)
    }
}

class ConeCollectionLiteralAtom(
    override val expression: CfirCollectionLiteral,
    override val expectedType: ConeCangJieType?,
    val containingCallCandidate: Candidate,
) : ConePostponedResolvedAtom(), CollectionLiteralAtomMarker {
    override val inputTypes: Collection<ConeCangJieType> = listOfNotNull(expectedType)
    override val outputType: ConeCangJieType?
        get() = null

    var subAtom: ConeAtomWithCandidate? = null
        set(value) {
            require(field == null) { "subAtom already initialized" }
            field = value
        }
}

//  -------------------------- Utils --------------------------

internal data class InputOutputTypes(val inputTypes: List<ConeCangJieType>, val outputType: ConeCangJieType)

internal fun extractInputOutputTypesFromCallableReferenceExpectedType(
    expectedType: ConeCangJieType?,
    session: CfirSession
): InputOutputTypes? {
    val expectedClassLikeType = expectedType?.lowerBoundIfFlexible() as? ConeClassLikeType ?: return null

    return when {
        expectedClassLikeType.isSomeFunctionType(session) ->
            InputOutputTypes(expectedClassLikeType.valueParameterTypesIncludingReceiver(session), expectedClassLikeType.returnType(session))

        else -> null
    }
}
