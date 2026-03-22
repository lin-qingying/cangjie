package org.cangnova.cangjie.cfir.resolve.calls.stages

import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.constraints.CfirConstraintPosition
import org.cangnova.cangjie.cfir.constraints.CfirConstraintSystem
import org.cangnova.cangjie.cfir.constraints.CfirTypeVariable
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirPropertyAccess
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccess
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirCandidate
import org.cangnova.cangjie.cfir.diagnostic.InferenceConstraintError
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeClassLookupTagImpl
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeTypeParameterLookupTag
import org.cangnova.cangjie.cfir.types.ConeTypeParameterType
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.cfir.diagnostic.ConeCannotInferTypeParameterType
import org.cangnova.cangjie.cfir.resolve.collectTypeVariableNames
import org.cangnova.cangjie.cfir.resolve.inference.inferenceLogger


/**
 * Completes the candidate constraint system after fresh variables have been created.
 *
 * Compared with the previous implementation, this stage no longer owns:
 * - creating the constraint system
 * - registering fresh variables
 * - directly applying explicit type arguments as a standalone substitutor shortcut
 *
 * Those responsibilities now live in [CfirCreateFreshTypeVariableSubstitutorStage],
 * matching Kotlin FIR's candidate lifecycle more closely.
 */
object CfirInferTypeArguments : CfirResolutionStage() {

    override fun check(
        candidate: CfirCandidate,
        sink: CfirCheckerSink,
        context: CfirResolutionContext,
    ) {
        val typeParameters = candidate.effectiveTypeParameters() ?: return
        if (typeParameters.isEmpty()) return

        val constraintSystem = candidate.constraintSystem ?: return
        val typeVariableMap = candidate.freshVariables.associateBy { it.lookupTag.name }
        candidate.callInfo.session.inferenceLogger?.apply {
            logCandidate(candidate)
            logStage("InferTypeArguments", constraintSystem)
        }

        // Enum constructors may consume expected type information, but this must be done
        // through the same constraint system as every other source of information.
        bindEnumTypeParametersFromExpectedType(candidate, typeParameters, context.expectedType)

        // 参数约束已由 CfirCheckArguments 通过约束系统事务性添加，这里只收集返回类型约束。
        collectReturnTypeConstraint(candidate, typeVariableMap, constraintSystem, context.expectedType)

        constraintSystem.fixAllVariables()
        val result = constraintSystem.buildResult()
        if (constraintSystem.hasErrors) {
            constraintSystem.errors.forEach { error ->
                candidate.callInfo.session.inferenceLogger?.logError(error, constraintSystem)
                sink.reportDiagnostic(InferenceConstraintError(error.message))
            }
        }

        candidate.updateSubstitutor(result.substitutor)
        applyInferredTypeArgumentsToCallSite(candidate, typeParameters, typeVariableMap)
    }

    private fun bindEnumTypeParametersFromExpectedType(
        candidate: CfirCandidate,
        typeParameters: List<CfirTypeParameter>,
        expectedType: ConeCangJieType?,
    ) {
        if (candidate.callInfo.typeArguments.isNotEmpty()) return
        if (!candidate.symbol.isBound) return

        val enumDecl = candidate.symbol.cfir as? CfirEnumConstructor ?: return
        val enumSymbol = enumDecl.symbol as? org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol ?: return
        val ownerClassId = candidate.callInfo.session.symbolProvider.getEnumConstructorOwnerClassId(enumSymbol)
            ?: candidate.callInfo.session.cfirProvider.getEnumConstructorOwnerClassId(enumSymbol)
            ?: return

        val expectedArgs = when (expectedType) {
            is ConeEnumType -> if (expectedType.classId == ownerClassId) expectedType.typeArguments else return
            is ConeClassLikeType -> if (expectedType.classId == ownerClassId) expectedType.typeArguments else return
            else -> return
        }
        if (expectedArgs.size != typeParameters.size) return

        val constraintSystem = candidate.constraintSystem ?: return
        for (index in typeParameters.indices) {
            constraintSystem.addEqualityConstraint(
                ConeTypeParameterType(ConeTypeParameterLookupTag(typeParameters[index].name.asString())),
                expectedArgs[index],
                CfirConstraintPosition.ExpectedType,
            )
        }
    }

    private fun collectReturnTypeConstraint(
        candidate: CfirCandidate,
        typeVariableMap: Map<String, CfirTypeVariable>,
        constraintSystem: CfirConstraintSystem,
        expectedReturnType: ConeCangJieType?,
    ) {
        if (expectedReturnType == null) return

        val returnType = extractReturnType(candidate) ?: return
        if (returnType is ConeErrorType) return

        if (!shouldAddExpectedTypeConstraint(returnType, typeVariableMap)) return

        constraintSystem.addSubtypeConstraint(
            returnType,
            expectedReturnType,
            CfirConstraintPosition.ReturnType,
        )
    }

    private fun extractReturnType(candidate: CfirCandidate): ConeCangJieType? {
        if (!candidate.symbol.isBound) return null
        val typeRef = when (val decl = candidate.symbol.cfir) {
            is CfirFunction -> decl.returnTypeRef
            is CfirConstructor -> decl.returnTypeRef
            is CfirEnumConstructor -> {
                val symbol = decl.symbol as? org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol ?: return null
                val classId = candidate.callInfo.session.symbolProvider.getEnumConstructorOwnerClassId(symbol)
                    ?: candidate.callInfo.session.cfirProvider.getEnumConstructorOwnerClassId(symbol)
                    ?: return null
                val typeArguments = (candidate.effectiveTypeParameters() ?: decl.typeParameters).map {
                    ConeTypeParameterType(ConeTypeParameterLookupTag(it.name.asString()))
                }
                return ConeEnumType(ConeClassLookupTagImpl(classId), typeArguments)
            }
            else -> return null
        }
        return (typeRef as? CfirResolvedTypeRef)?.coneType
    }

    private fun shouldAddExpectedTypeConstraint(
        returnType: ConeCangJieType,
        typeVariableMap: Map<String, CfirTypeVariable>,
    ): Boolean {
        val namesInReturnType = mutableSetOf<String>()
        returnType.collectTypeVariableNames(namesInReturnType)
        if (namesInReturnType.isEmpty()) return true

        return namesInReturnType.none { variableName ->
            val variable = typeVariableMap[variableName] ?: return@none false
            variable.lowerBounds.isNotEmpty()
        }
    }

    private fun applyInferredTypeArgumentsToCallSite(
        candidate: CfirCandidate,
        typeParameters: List<CfirTypeParameter>,
        typeVariableMap: Map<String, CfirTypeVariable>,
    ) {
        if (candidate.callInfo.typeArguments.isNotEmpty()) return

        val callSite = candidate.callInfo.callSite
        val callSiteSource = callSite.source
        val inferredTypeArguments = typeParameters.map { typeParameter ->
            val parameterName = typeParameter.name.asString()
            val parameterSymbol = typeParameter.symbol as? CfirTypeParameterSymbol
            check(parameterSymbol != null) {
                "Expected CfirTypeParameterSymbol for type parameter '$parameterName', got: ${typeParameter.symbol::class.simpleName}"
            }
            val variable = typeVariableMap[parameterName]
            val inferredType = variable?.fixedType
                ?: candidate.substitutor.substituteOrSelf(ConeTypeParameterType(ConeTypeParameterLookupTag(parameterName)))

            val finalType = when {
                variable?.fixedType == null ->
                    ConeErrorType(ConeCannotInferTypeParameterType(parameterSymbol))

                inferredType is ConeTypeParameterType && inferredType.lookupTag.name == parameterName ->
                    ConeErrorType(ConeCannotInferTypeParameterType(parameterSymbol))

                else -> inferredType
            }

            buildResolvedTypeRef {
                source = callSiteSource
                coneType = finalType
                delegatedTypeRef = null
            }
        }

        when (callSite) {
            is CfirFunctionCall -> callSite.replaceTypeArguments(inferredTypeArguments)
            is CfirQualifiedAccess -> callSite.replaceTypeArguments(inferredTypeArguments)
            is CfirPropertyAccess -> Unit
        }
    }

    private fun CfirCandidate.effectiveTypeParameters(): List<CfirTypeParameter>? {
        if (!symbol.isBound) return null
        return when (val decl = symbol.cfir) {
            is CfirFunction -> decl.typeParameters
            is CfirConstructor -> decl.typeParameters
            is CfirEnumConstructor -> {
                if (decl.typeParameters.isNotEmpty()) {
                    decl.typeParameters
                } else {
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
