/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.type.AbstractTypeChecker
import org.cangnova.cangjie.type.model.TypeConstructorMarker

/**
 * Override checker aligned to Kotlin FIR order:
 * 1) check override target existence
 * 2) check override-target visibility
 * 3) check visibility compatibility
 * 4) check return type compatibility
 *
 * 中文说明：
 * 这里处理的是“声明级继承规则”，因此应放在 declaration checker 层。
 * 解析阶段只负责把可见性失败保留在引用/候选上，不在这里反向改写解析语义。
 */
object CfirOverrideChecker : CfirClassLikeChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirClassLikeDeclaration) {
        val classScope = context.createUseSiteMemberScope(declaration)

        for (member in declaration.declarations) {
            val callable = member as? CfirCallableDeclaration ?: continue
            if (!callable.isSourceDeclaration || !callable.hasOverrideLikeModifier()) continue
            if (!callable.isValidOverrideLikeDeclaration()) continue

            val overriddenCandidates = when (val symbol = callable.symbol) {
                is CfirNamedFunctionSymbol -> classScope.collectDirectOverriddenFunctions(symbol)
                is CfirFunctionSymbol<*> -> emptyList()
                is CfirPropertySymbol -> classScope.collectDirectOverriddenProperties(symbol)
                else -> emptyList()
            }.filter { it.canParticipateInOverrideTargetSearch(declaration, context) }

            if (overriddenCandidates.isEmpty()) {
                if (callable.hasInheritedSignatureIgnoringStatic(classScope, declaration)) {
                    continue
                }
                reporter.reportOn(
                    source = callable.source,
                    factory = CfirErrors.NOTHING_TO_OVERRIDE,
                )
                continue
            }

            val visibleOverriddenSymbols = overriddenCandidates.filter { it.isVisibleIn(declaration, context) }
            if (visibleOverriddenSymbols.isEmpty()) {
                val invisibleOverridden = overriddenCandidates.first()
                reporter.reportOn(
                    source = callable.source,
                    factory = CfirErrors.CANNOT_OVERRIDE_INVISIBLE_MEMBER,
                    a = invisibleOverridden.name,
                )
                continue
            }

            checkParameterNamingCompatibility(callable, visibleOverriddenSymbols)
            checkGenericConstraintCompatibility(callable, visibleOverriddenSymbols)
            checkVisibilityCompatibility(callable, visibleOverriddenSymbols)
            checkReturnTypeCompatibility(callable, visibleOverriddenSymbols)
        }
    }

    private fun CfirCallableDeclaration.hasOverrideLikeModifier(): Boolean {
        return status.isOverride || status.isRedef
    }

    private fun CfirCallableDeclaration.isValidOverrideLikeDeclaration(): Boolean {
        if (status.isRedef && !status.isStatic) return false
        if (status.isOverride && status.isStatic) return false
        return true
    }

    context(context: CheckerContext)
    private fun CfirCallableDeclaration.hasInheritedSignatureIgnoringStatic(
        classScope: org.cangnova.cangjie.cfir.scopes.CfirTypeScope,
        ownerDeclaration: CfirClassLikeDeclaration,
    ): Boolean {
        return when (val symbol = symbol) {
            is CfirNamedFunctionSymbol -> classScope
                .collectDirectOverriddenFunctionsIgnoringStatic(symbol)
                .any { it.canParticipateInOverrideTargetSearch(ownerDeclaration, context) }

            is CfirPropertySymbol -> classScope
                .collectDirectOverriddenPropertiesIgnoringStatic(symbol)
                .any { it.canParticipateInOverrideTargetSearch(ownerDeclaration, context) }

            else -> false
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkParameterNamingCompatibility(
        declaration: CfirCallableDeclaration,
        overriddenSymbols: List<CfirCallableSymbol<*>>,
    ) {
        val function = declaration as? CfirFunction ?: return
        val overriddenPair = overriddenSymbols.firstNotNullOfOrNull { overridden ->
            (overridden.cfir as? CfirFunction)?.let { it to overridden }
        } ?: return
        val overriddenFunction = overriddenPair.first
        val overriddenSymbol = overriddenPair.second

        if (!function.hasMismatchedParameterNamingAgainst(overriddenFunction)) return

        reporter.reportOn(
            source = declaration.source,
            factory = CfirErrors.PARAM_NAMED_MISMATCHED,
            a = overriddenSymbol.name,
        )
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkVisibilityCompatibility(
        declaration: CfirCallableDeclaration,
        overriddenSymbols: List<CfirCallableSymbol<*>>,
    ) {
        val declarationVisibility = declaration.status.visibility
        val firstIncompatibleOverridden = overriddenSymbols.firstOrNull { overridden ->
            val compareResult = Visibilities.compare(declarationVisibility, overridden.cfir.status.visibility)
            compareResult == null || compareResult < 0
        }
        if (firstIncompatibleOverridden == null) return

        reporter.reportOn(
            source = declaration.source,
            factory = CfirErrors.CANNOT_WEAKEN_ACCESS_PRIVILEGE,
            a = firstIncompatibleOverridden.name,
            b = firstIncompatibleOverridden.cfir.status.visibility,
        )
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkReturnTypeCompatibility(
        declaration: CfirCallableDeclaration,
        overriddenSymbols: List<CfirCallableSymbol<*>>,
    ) {
        val declarationSymbol = declaration.symbol as? CfirCallableSymbol<*> ?: return
        val overridingReturnType = context.returnTypeCalculator.tryCalculateReturnType(declaration).coneType
        if (overridingReturnType is ConeErrorType) return

        for (overridden in overriddenSymbols) {
            if (!overridden.isBound) continue
            val overriddenReturnType = context.returnTypeCalculator.tryCalculateReturnType(overridden.cfir).coneType
                .substituteAllTypeParameters(declarationSymbol, overridden)
            if (overriddenReturnType is ConeErrorType) continue

            val isPropertyOverride = declarationSymbol is CfirPropertySymbol && overridden is CfirPropertySymbol

            if (!isPropertyOverride && overriddenReturnType.isThisType && !overridingReturnType.isThisType) {
                reporter.reportOn(
                    source = declaration.source,
                    factory = CfirErrors.INHERIT_NOT_RETURN_THIS,
                )
                return
            }

            val isCompatible = if (isPropertyOverride) {
                AbstractTypeChecker.equalTypes(
                    context.session.typeContext,
                    overridingReturnType,
                    overriddenReturnType,
                )
            } else {
                AbstractTypeChecker.isSubtypeOf(
                    context.session.typeContext,
                    overridingReturnType,
                    overriddenReturnType,
                )
            }

            if (isCompatible) continue

            if (isPropertyOverride) {
                reporter.reportOn(
                    source = declaration.source?.firstCharacterDiagnosticSource(),
                    factory = CfirErrors.PROPERTY_OVERRIDE_IMPLEMENT_TYPE_DIFF,
                    a = overridingReturnType,
                    b = overriddenReturnType,
                    c = overridden.name,
                )
                return
            }

            reporter.reportOn(
                source = declaration.source,
                factory = CfirErrors.OVERRIDING_RETURN_TYPE_MISMATCH,
                a = overridingReturnType,
                b = overriddenReturnType,
                c = overridden.name,
            )
            return
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkGenericConstraintCompatibility(
        declaration: CfirCallableDeclaration,
        overriddenSymbols: List<CfirCallableSymbol<*>>,
    ) {
        val childTypeParameters = (declaration as? CfirTypeParameterRefsOwner)?.typeParameters.orEmpty()
        if (childTypeParameters.isEmpty()) return

        for (overridden in overriddenSymbols) {
            val parentTypeParameters = (overridden.cfir as? CfirTypeParameterRefsOwner)?.typeParameters.orEmpty()
            if (parentTypeParameters.size != childTypeParameters.size) continue

            val parentToChildSubstitutor = createParentToChildTypeParameterSubstitutor(
                parentTypeParameters,
                childTypeParameters,
            )

            for (index in childTypeParameters.indices) {
                val childBounds = childTypeParameters[index].symbol.resolvedBounds
                    .filterUsableGenericConstraintBounds()
                    .filterNot { it.coneType.isAnyBound() }
                if (childBounds.isEmpty()) continue

                val parentBounds = parentTypeParameters[index].symbol.resolvedBounds
                    .filterUsableGenericConstraintBounds()
                    .map { parentToChildSubstitutor.substituteOrSelf(it.coneType) }

                val nonLooserBound = childBounds.firstOrNull { childBound ->
                    !parentBounds.any { parentBound ->
                        AbstractTypeChecker.isSubtypeOf(context.session.typeContext, parentBound, childBound.coneType)
                    }
                } ?: parentBounds.firstOrNull { parentBound ->
                    childBounds.any { childBound ->
                        childBound.coneType != parentBound &&
                                AbstractTypeChecker.isSubtypeOf(
                                    context.session.typeContext,
                                    childBound.coneType,
                                    parentBound
                                )
                    }
                }?.let { childBounds.first() }

                if (nonLooserBound != null) {
                    reporter.reportOn(
                        source = declaration.genericConstraintDiagnosticSource(childTypeParameters[index])
                            ?: nonLooserBound.source
                            ?: declaration.source,
                        factory = CfirErrors.GENERIC_CONSTRAINT_NOT_LOOSER,
                    )
                    return
                }
            }
        }
    }

    context(context: CheckerContext)
    private fun createParentToChildTypeParameterSubstitutor(
        parentTypeParameters: List<CfirTypeParameterRef>,
        childTypeParameters: List<CfirTypeParameterRef>,
    ) = createTypeSubstitutorByTypeConstructor(
        map = parentTypeParameters.zip(childTypeParameters).associate { (parent, child) ->
            parent.typeConstructorForSubstitution() to ConeTypeParameterTypeImpl(child.symbol.toLookupTag())
        },
        context = context.session.typeContext,
        approximateIntegerLiterals = false,
    )
}

context(context: CheckerContext)
private fun ConeCangJieType.substituteAllTypeParameters(
    overrideDeclaration: CfirCallableSymbol<*>,
    baseDeclaration: CfirCallableSymbol<*>,
): ConeCangJieType {
    val overrideTypeParameters = overrideDeclaration.cfir.typeParameters
    if (overrideTypeParameters.isEmpty()) return this

    val baseTypeParameters = baseDeclaration.cfir.typeParameters
    val size = minOf(overrideTypeParameters.size, baseTypeParameters.size)
    if (size == 0) return this

    val map = LinkedHashMap<TypeConstructorMarker, ConeCangJieType>(size)
    for (index in 0 until size) {
        map[baseTypeParameters[index].typeConstructorForSubstitution()] =
            ConeTypeParameterTypeImpl(overrideTypeParameters[index].symbol.toLookupTag())
    }

    return createTypeSubstitutorByTypeConstructor(
        map = map,
        context = context.session.typeContext,
        approximateIntegerLiterals = false,
    ).substituteOrSelf(this)
}

private fun CfirTypeParameterRef.typeConstructorForSubstitution(): TypeConstructorMarker =
    symbol.toLookupTag() as TypeConstructorMarker

private fun CfirCallableDeclaration.genericConstraintDiagnosticSource(
    typeParameter: CfirTypeParameterRef,
) = attributes.typeConstraintDiagnosticData
    ?.typeConstraints
    ?.firstOrNull { it.parameterName == typeParameter.symbol.name }
    ?.constraintSource

private fun List<CfirResolvedTypeRef>.filterUsableGenericConstraintBounds(): List<CfirResolvedTypeRef> =
    filterNot { it.coneType is ConeErrorType }

private fun ConeCangJieType.isAnyBound(): Boolean =
    this == ConeAnyType || (this as? ConeClassLikeType)?.classId == StdlibClassIds.Any

private val ConeCangJieType.isThisType: Boolean
    get() = (this as? ConeClassLikeType)?.isThisType == true

private fun CfirFunction.hasMismatchedParameterNamingAgainst(overridden: CfirFunction): Boolean {
    if (valueParameters.size != overridden.valueParameters.size) return false

    return valueParameters.zip(overridden.valueParameters).any { (current, parent) ->
        current.isNamed != parent.isNamed ||
                (current.isNamed && parent.isNamed && current.name != parent.name)
    }
}
