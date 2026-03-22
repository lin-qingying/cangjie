package org.cangnova.cangjie.cfir.resolve.calls.stages

import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.constraints.CfirConstraintPosition
import org.cangnova.cangjie.cfir.constraints.CfirConstraintSystem
import org.cangnova.cangjie.cfir.constraints.CfirTypeSubstitutor
import org.cangnova.cangjie.cfir.constraints.CfirTypeVariable
import org.cangnova.cangjie.cfir.resolve.CfirTypeResolutionConfiguration
import org.cangnova.cangjie.cfir.resolve.CfirConstraintSystemImpl
import org.cangnova.cangjie.cfir.resolve.SupertypeSupplier
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirCandidate
import org.cangnova.cangjie.cfir.diagnostic.InferenceConstraintError
import org.cangnova.cangjie.cfir.resolve.inference.inferenceLogger
import org.cangnova.cangjie.cfir.session.cfirProvider

import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.session.typeResolver
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeTypeParameterLookupTag
import org.cangnova.cangjie.cfir.types.ConeTypeParameterType

/**
 * Mirrors Kotlin FIR's `CreateFreshTypeVariableSubstitutorStage` at a simplified level.
 *
 * This stage owns initialization of:
 * - candidate constraint system
 * - fresh inference variables for declared type parameters
 * - declaration upper-bound constraints
 * - explicit type-argument equality constraints
 *
 * Later stages may then add argument/expected-type constraints and complete the system.
 */
object CfirCreateFreshTypeVariableSubstitutorStage : CfirResolutionStage() {

    override fun check(
        candidate: CfirCandidate,
        sink: CfirCheckerSink,
        context: CfirResolutionContext,
    ) {
        if (candidate.constraintSystem != null) return

        val typeParameters = candidate.effectiveTypeParameters() ?: run {
            candidate.initializeSubstitutorAndVariables(CfirTypeSubstitutor.Empty, emptyList())
            return
        }

        if (typeParameters.isEmpty()) {
            candidate.initializeSubstitutorAndVariables(CfirTypeSubstitutor.Empty, emptyList())
            return
        }

        val inferenceComponents = context.inferenceComponents ?: return
        val constraintSystem = inferenceComponents.createConstraintSystem()
        candidate.constraintSystem = constraintSystem
        candidate.callInfo.session.inferenceLogger?.apply {
            logCandidate(candidate)
            logStage("CreateFreshTypeVariableSubstitutor", constraintSystem)
        }

        val freshVariables = buildFreshVariables(typeParameters, constraintSystem)
        candidate.initializeSubstitutorAndVariables(CfirTypeSubstitutor.Empty, freshVariables)

        registerDeclaredUpperBounds(typeParameters, constraintSystem)
        registerExplicitTypeArgumentConstraints(candidate, typeParameters, constraintSystem, context)

        if (constraintSystem.hasErrors) {
            constraintSystem.errors.forEach { error ->
                candidate.callInfo.session.inferenceLogger?.logError(error, constraintSystem)
                sink.reportDiagnostic(InferenceConstraintError(error.message))
            }
        }
    }

    private fun buildFreshVariables(
        typeParameters: List<CfirTypeParameter>,
        constraintSystem: CfirConstraintSystem,
    ): List<CfirTypeVariable> {
        return buildList {
            for (typeParameter in typeParameters) {
                val symbol = typeParameter.symbol as? CfirTypeParameterSymbol ?: continue
                val variable = CfirTypeVariable(
                    typeParameter = symbol,
                    freshTypeId = (constraintSystem as? CfirConstraintSystemImpl)
                        ?.nextFreshTypeId() ?: addAndReturnSyntheticFreshId(this),
                    lookupTag = ConeTypeParameterLookupTag(typeParameter.name.asString()),
                )
                constraintSystem.registerTypeVariable(variable)
                add(variable)
            }
        }
    }

    private fun addAndReturnSyntheticFreshId(
        currentVariables: List<CfirTypeVariable>,
    ): Int = currentVariables.size + 1

    private fun registerDeclaredUpperBounds(
        typeParameters: List<CfirTypeParameter>,
        constraintSystem: CfirConstraintSystem,
    ) {
        for (typeParameter in typeParameters) {
            val variableType = ConeTypeParameterType(ConeTypeParameterLookupTag(typeParameter.name.asString()))
            for (bound in typeParameter.bounds) {
                val boundType = (bound as? CfirResolvedTypeRef)?.coneType ?: continue
                if (boundType is ConeErrorType) continue
                constraintSystem.addSubtypeConstraint(
                    variableType,
                    boundType,
                    CfirConstraintPosition.UpperBound,
                )
            }
        }
    }

    private fun registerExplicitTypeArgumentConstraints(
        candidate: CfirCandidate,
        typeParameters: List<CfirTypeParameter>,
        constraintSystem: CfirConstraintSystem,
        context: CfirResolutionContext,
    ) {
        val explicitTypeArguments = candidate.callInfo.typeArguments
        if (explicitTypeArguments.isEmpty()) return
        if (explicitTypeArguments.size != typeParameters.size) return

        for (index in typeParameters.indices) {
            val argumentType = resolveExplicitTypeArgument(candidate, explicitTypeArguments[index], context)
                ?: continue
            if (argumentType is ConeErrorType) continue

            constraintSystem.addEqualityConstraint(
                ConeTypeParameterType(ConeTypeParameterLookupTag(typeParameters[index].name.asString())),
                argumentType,
                CfirConstraintPosition.ExplicitTypeArgument(index),
            )
        }
    }

    private fun resolveExplicitTypeArgument(
        candidate: CfirCandidate,
        typeRef: CfirTypeRef,
        context: CfirResolutionContext?,
    ): ConeCangJieType? {
        val resolved = (typeRef as? CfirResolvedTypeRef)?.coneType
        if (resolved != null) return resolved

        val bodyResolveContext = context?.bodyResolveContext
            ?: return null
        val configuration = CfirTypeResolutionConfiguration(
            useSiteFile = bodyResolveContext.file,
            topContainer = bodyResolveContext.containers.lastOrNull(),
        )
        return candidate.callInfo.session.typeResolver.resolveType(
            typeRef = typeRef,
            configuration = configuration,
            areBareTypesAllowed = false,
            isOperandOfIsOperator = false,
            resolveDeprecations = true,
            supertypeSupplier = SupertypeSupplier.Default,
            expandTypeAliases = true,
        ).type
    }

    private fun CfirCandidate.effectiveTypeParameters(): List<CfirTypeParameter>? {
        if (!symbol.isBound) return null
        return when (val decl = symbol.cfir) {
            is CfirFunction -> decl.typeParameters
            is CfirConstructor -> decl.typeParameters
            is CfirEnumConstructor -> {
                decl.typeParameters.ifEmpty {
                    val enumSymbol = decl.symbol as? org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol
                        ?: return decl.typeParameters
                    val ownerClassId = callInfo.session.symbolProvider.getEnumConstructorOwnerClassId(enumSymbol)
                        ?: callInfo.session.cfirProvider.getEnumConstructorOwnerClassId(enumSymbol)
                        ?: return decl.typeParameters
                    val ownerSymbol = callInfo.session.symbolProvider.getClassLikeSymbolByClassId(ownerClassId)
                        ?: return decl.typeParameters
                    ownerSymbol.cfir.typeParameters
                }
            }
            else -> null
        }
    }
}
