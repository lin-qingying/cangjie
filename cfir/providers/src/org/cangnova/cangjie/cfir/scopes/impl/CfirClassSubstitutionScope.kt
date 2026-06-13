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

package org.cangnova.cangjie.cfir.scopes.impl

import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.builder.*
import org.cangnova.cangjie.cfir.originalForSubstitutionOverride
import org.cangnova.cangjie.cfir.originalForSubstitutionOverrideAttr
import org.cangnova.cangjie.cfir.resolve.providers.findExtendDeclarationSubstitution
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.scopes.*
import org.cangnova.cangjie.cfir.session.*
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.unwrapSubstitutionOverrides
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.type.model.TypeConstructorMarker

/**
 * 对齐 Kotlin `FirClassSubstitutionScope` 的 use-site substitution scope。
 *
 * 这层负责两件事：
 * 1. 基于接收者具体类型，把来自 class/supertype/extend 的成员声明复制为 substitution override。
 * 2. 让后续调用解析直接看到“已经替换过 owner 类型实参”的成员签名。
 *
 * 这里禁止再把 owner substitutor 透传给 candidate 兜底；substitution override 必须在 providers
 * 层完成，解析层只消费最终成员签名。
 */
class CfirClassSubstitutionScope(
    private val session: CfirSession,
    private val useSiteMemberScope: CfirTypeScope,
    private val dispatchReceiverType: ConeCangJieType,
    private val substitutionOwnerType: ConeCangJieType? = null,
) : CfirTypeScope() {
    private val functionOverrideCache = mutableMapOf<CfirNamedFunctionSymbol, CfirNamedFunctionSymbol>()
    private val propertyOverrideCache = mutableMapOf<CfirPropertySymbol, CfirPropertySymbol>()
    private val fieldOverrideCache = mutableMapOf<CfirFieldVariableSymbol, CfirFieldVariableSymbol>()
    private val enumConstructorOverrideCache = mutableMapOf<CfirEnumConstructorSymbol, CfirEnumConstructorSymbol>()
    private val wrappedBaseScopeCache = mutableMapOf<CfirTypeScope, CfirTypeScope>()
    private val concreteSupertypeCache = mutableMapOf<ClassId, ConeCangJieType?>()

    override fun getCallableNames(): Set<Name> = useSiteMemberScope.getCallableNames()

    override fun getClassifierNames(): Set<Name> = useSiteMemberScope.getClassifierNames()

    override fun processFunctionsByName(name: Name, processor: (CfirNamedFunctionSymbol) -> Unit) {
        val selected = linkedMapOf<String, CfirNamedFunctionSymbol>()
        useSiteMemberScope.processFunctionsByName(name) { original ->
            val substituted = substituteFunctionSymbol(original)
            selected.putIfAbsent(substituted.overrideSignatureKey(), substituted)
        }
        selected.values.forEach(processor)
    }

    override fun processPropertiesByName(name: Name, processor: (CfirPropertySymbol) -> Unit) {
        useSiteMemberScope.processPropertiesByName(name) { original ->
            processor(substitutePropertySymbol(original))
        }
    }

    override fun processClassifiersByName(name: Name, processor: (CfirClassLikeSymbol<*>) -> Unit) {
        useSiteMemberScope.processClassifiersByName(name, processor)
    }

    override fun processCallablesByName(name: Name, processor: (CfirCallableSymbol<*>) -> Unit) {
        useSiteMemberScope.processCallablesByName(name) { original ->
            processor(substituteCallableSymbol(original))
        }
    }

    override fun processDirectOverriddenFunctionsWithBaseScope(
        functionSymbol: CfirNamedFunctionSymbol,
        processor: (CfirNamedFunctionSymbol, CfirTypeScope) -> ProcessorAction
    ): ProcessorAction {
        val original = functionSymbol.originalForSubstitutionOverride as? CfirNamedFunctionSymbol
        return when {
            original == null || original !in functionOverrideCache -> {
                useSiteMemberScope.processDirectOverriddenFunctionsWithBaseScope(functionSymbol, processor)
            }
            processor(original, useSiteMemberScope) == ProcessorAction.STOP -> ProcessorAction.STOP
            else -> ProcessorAction.NONE
        }
    }

    override fun processDirectOverriddenPropertiesWithBaseScope(
        propertySymbol: CfirPropertySymbol,
        processor: (CfirPropertySymbol, CfirTypeScope) -> ProcessorAction,
    ): ProcessorAction {
        val original = propertySymbol.originalForSubstitutionOverride as? CfirPropertySymbol
        return when {
            original == null || original !in propertyOverrideCache -> {
                useSiteMemberScope.processDirectOverriddenPropertiesWithBaseScope(propertySymbol, processor)
            }
            processor(original, useSiteMemberScope) == ProcessorAction.STOP -> ProcessorAction.STOP
            else -> ProcessorAction.NONE
        }
    }

    override fun withReplacedSessionOrNull(newSession: CfirSession, newScopeSession: ScopeSession): CfirTypeScope? {
        val replacedScope = useSiteMemberScope.withReplacedSessionOrNull(newSession, newScopeSession) ?: return null
        return CfirClassSubstitutionScope(newSession, replacedScope, dispatchReceiverType, substitutionOwnerType)
    }

    private fun substitutedBaseScope(baseScope: CfirTypeScope): CfirTypeScope {
        if (baseScope === useSiteMemberScope) return this
        return synchronized(wrappedBaseScopeCache) {
            wrappedBaseScopeCache.getOrPut(baseScope) {
                CfirClassSubstitutionScope(session, baseScope, dispatchReceiverType, substitutionOwnerType)
            }
        }
    }

    private fun substituteCallableSymbol(symbol: CfirCallableSymbol<*>): CfirCallableSymbol<*> {
        return when (symbol) {
            is CfirNamedFunctionSymbol -> substituteFunctionSymbol(symbol)
            is CfirPropertySymbol -> substitutePropertySymbol(symbol)
            is CfirFieldVariableSymbol -> substituteFieldSymbol(symbol)
            is CfirEnumConstructorSymbol -> substituteEnumConstructorSymbol(symbol)
            else -> symbol
        }
    }

    private fun substituteFunctionSymbol(symbol: CfirNamedFunctionSymbol): CfirNamedFunctionSymbol {
        return synchronized(functionOverrideCache) {
            functionOverrideCache.getOrPut(symbol) {
                createSubstitutedFunctionSymbol(symbol)
            }
        }
    }

    private fun substitutePropertySymbol(symbol: CfirPropertySymbol): CfirPropertySymbol {
        return synchronized(propertyOverrideCache) {
            propertyOverrideCache.getOrPut(symbol) {
                createSubstitutedPropertySymbol(symbol)
            }
        }
    }

    private fun substituteFieldSymbol(symbol: CfirFieldVariableSymbol): CfirFieldVariableSymbol {
        return synchronized(fieldOverrideCache) {
            fieldOverrideCache.getOrPut(symbol) {
                createSubstitutedFieldSymbol(symbol)
            }
        }
    }

    private fun substituteEnumConstructorSymbol(symbol: CfirEnumConstructorSymbol): CfirEnumConstructorSymbol {
        return synchronized(enumConstructorOverrideCache) {
            enumConstructorOverrideCache.getOrPut(symbol) {
                createSubstitutedEnumConstructorSymbol(symbol)
            }
        }
    }

    private fun createSubstitutedFunctionSymbol(symbol: CfirNamedFunctionSymbol): CfirNamedFunctionSymbol {
        val substitutor = computeCallableSubstitutor(symbol)
        if (substitutor === ConeSubstitutor.Empty || substitutor == null) return symbol

        symbol.lazyResolveToPhase(CfirResolvePhase.TYPES)
        val declaration = symbol.cfir as? CfirNamedFunction ?: return symbol
        val copiedSymbol = CfirNamedFunctionSymbol(symbol.callableId)
        val returnTypeData = declaration.substitutedReturnTypeData(symbol, substitutor)
        val copiedDeclaration = buildNamedFunctionCopy(declaration) {
            origin = substitutionOverrideOrigin(symbol)
            this.symbol = copiedSymbol
            dispatchReceiverType = substituteDispatchReceiverType(declaration.dispatchReceiverType, substitutor)
            returnTypeRef = returnTypeData.typeRef
            valueParameters.clear()
            valueParameters += substituteValueParameters(declaration.valueParameters, substitutor)
        }
        copiedDeclaration.attributes.deferredCallableCopyReturnType = returnTypeData.deferredReturnType
        copiedDeclaration.originalForSubstitutionOverrideAttr = declaration
        return copiedSymbol
    }

    private fun createSubstitutedPropertySymbol(symbol: CfirPropertySymbol): CfirPropertySymbol {
        val substitutor = computeCallableSubstitutor(symbol)
        if (substitutor === ConeSubstitutor.Empty || substitutor == null) return symbol

        symbol.lazyResolveToPhase(CfirResolvePhase.TYPES)
        val declaration = symbol.cfir
        val copiedSymbol = CfirPropertySymbol(symbol.callableId)
        val returnTypeData = declaration.substitutedReturnTypeData(symbol, substitutor)
        val copiedDeclaration = buildPropertyCopy(declaration) {
            origin = substitutionOverrideOrigin(symbol)
            this.symbol = copiedSymbol
            dispatchReceiverType = substituteDispatchReceiverType(declaration.dispatchReceiverType, substitutor)
            returnTypeRef = returnTypeData.typeRef
            getter = substituteAccessorFunction(declaration.getter, substitutor)
            setter = substituteAccessorFunction(declaration.setter, substitutor)
        }
        copiedDeclaration.attributes.deferredCallableCopyReturnType = returnTypeData.deferredReturnType
        copiedDeclaration.originalForSubstitutionOverrideAttr = declaration
        return copiedSymbol
    }

    private fun createSubstitutedFieldSymbol(symbol: CfirFieldVariableSymbol): CfirFieldVariableSymbol {
        val substitutor = computeCallableSubstitutor(symbol)
        if (substitutor === ConeSubstitutor.Empty || substitutor == null) return symbol

        symbol.lazyResolveToPhase(CfirResolvePhase.TYPES)
        val declaration = symbol.cfir
        val copiedSymbol = CfirFieldVariableSymbol(symbol.callableId)
        val returnTypeData = declaration.substitutedReturnTypeData(symbol, substitutor)
        val copiedDeclaration = buildFieldVariableCopy(declaration) {
            origin = substitutionOverrideOrigin(symbol)
            this.symbol = copiedSymbol
            dispatchReceiverType = substituteDispatchReceiverType(declaration.dispatchReceiverType, substitutor)
            returnTypeRef = returnTypeData.typeRef
        }
        copiedDeclaration.attributes.deferredCallableCopyReturnType = returnTypeData.deferredReturnType
        copiedDeclaration.originalForSubstitutionOverrideAttr = declaration
        return copiedSymbol
    }

    private fun createSubstitutedEnumConstructorSymbol(symbol: CfirEnumConstructorSymbol): CfirEnumConstructorSymbol {
        val substitutor = computeCallableSubstitutor(symbol)
        if (substitutor === ConeSubstitutor.Empty || substitutor == null) return symbol

        symbol.lazyResolveToPhase(CfirResolvePhase.TYPES)
        val declaration = symbol.cfir
        val copiedSymbol = CfirEnumConstructorSymbol(symbol.callableId)
        val returnTypeData = declaration.substitutedReturnTypeData(symbol, substitutor)
        val copiedDeclaration = buildEnumConstructorCopy(declaration) {
            origin = substitutionOverrideOrigin(symbol)
            this.symbol = copiedSymbol
            dispatchReceiverType = substituteDispatchReceiverType(declaration.dispatchReceiverType, substitutor)
            returnTypeRef = returnTypeData.typeRef
            valueParameters.clear()
            valueParameters += substituteValueParameters(declaration.valueParameters, substitutor)
        }
        copiedDeclaration.attributes.deferredCallableCopyReturnType = returnTypeData.deferredReturnType
        copiedDeclaration.originalForSubstitutionOverrideAttr = declaration
        return copiedSymbol
    }

    private fun substituteAccessorFunction(function: CfirPropertyAccessor?, substitutor: ConeSubstitutor): CfirPropertyAccessor? {
        function ?: return null
        val symbol = function.symbol
        val copiedSymbol = CfirPropertyAccessorSymbol()
        val returnTypeData = function.substitutedReturnTypeData(symbol, substitutor)
        val copiedDeclaration = buildPropertyAccessorCopy(function) {
            origin = substitutionOverrideOrigin(symbol)
            this.symbol = copiedSymbol
            dispatchReceiverType = substituteDispatchReceiverType(function.dispatchReceiverType, substitutor)
            returnTypeRef = returnTypeData.typeRef
            valueParameters.clear()
            valueParameters += substituteValueParameters(function.valueParameters, substitutor)
        }
        copiedDeclaration.attributes.deferredCallableCopyReturnType = returnTypeData.deferredReturnType
        copiedDeclaration.originalForSubstitutionOverrideAttr = function
        return copiedDeclaration
    }

    private fun substituteValueParameters(
        valueParameters: List<CfirValueParameter>,
        substitutor: ConeSubstitutor,
    ): List<CfirValueParameter> {
        return valueParameters.map { valueParameter ->
            val copiedSymbol = org.cangnova.cangjie.cfir.symbols.CfirValueParameterSymbol(valueParameter.symbol.callableId)
            buildValueParameterCopy(valueParameter) {
                symbol = copiedSymbol
                returnTypeRef = substituteTypeRef(valueParameter.symbol.resolvedReturnTypeRef, substitutor)
            }
        }
    }

    private data class ReturnTypeData(
        val typeRef: CfirTypeRef,
        val deferredReturnType: DeferredReturnTypeOfSubstitution?,
    )

    private fun CfirCallableDeclaration.substitutedReturnTypeData(
        symbol: CfirCallableSymbol<*>,
        substitutor: ConeSubstitutor,
    ): ReturnTypeData {
        val resolvedTypeRef = returnTypeRef as? CfirResolvedTypeRef
        if (resolvedTypeRef != null) {
            return ReturnTypeData(substituteTypeRef(resolvedTypeRef, substitutor), deferredReturnType = null)
        }

        return ReturnTypeData(
            typeRef = returnTypeRef,
            deferredReturnType = DeferredReturnTypeOfSubstitution(substitutor, symbol),
        )
    }

    private fun substituteDispatchReceiverType(
        type: ConeSimpleCangJieType?,
        substitutor: ConeSubstitutor,
    ): ConeSimpleCangJieType? {
        if (type == null) return null
        return substitutor.substituteOrSelf(type) as? ConeSimpleCangJieType ?: type
    }

    private fun substituteTypeRef(
        typeRef: CfirResolvedTypeRef,
        substitutor: ConeSubstitutor,
    ): CfirResolvedTypeRef {
        val substitutedType = substitutor.substituteOrSelf(typeRef.coneType)
        return typeRef.withReplacedSourceAndType(typeRef.source, substitutedType)
    }

    private fun substitutionOverrideOrigin(symbol: CfirCallableSymbol<*>): CfirDeclarationOrigin {
        val ownerExtend = session.extendProvider.getContainingExtend(symbol)
        if (ownerExtend != null) {
            val ownerClassId = (ownerExtend.extendedTypeRef as? CfirResolvedTypeRef)
                ?.coneType
                ?.classIdOrPrimitiveClassId
            return if (ownerClassId != null && ownerClassId == dispatchReceiverType.classIdOrPrimitiveClassId) {
                CfirDeclarationOrigin.SubstitutionOverride.CallSite
            } else {
                CfirDeclarationOrigin.SubstitutionOverride.DeclarationSite
            }
        }

        val ownerClassId = session.cfirProvider.getContainingClass(symbol)?.classId
        return if (ownerClassId != null && ownerClassId == dispatchReceiverType.classIdOrPrimitiveClassId) {
            CfirDeclarationOrigin.SubstitutionOverride.CallSite
        } else {
            CfirDeclarationOrigin.SubstitutionOverride.DeclarationSite
        }
    }

    private fun computeCallableSubstitutor(symbol: CfirCallableSymbol<*>): ConeSubstitutor? {
        val ownerExtend = session.extendProvider.getContainingExtend(symbol)
            ?.takeIf(session.extendProvider::isExtendAccessible)
        if (ownerExtend != null) {
            return findExtendDeclarationSubstitution(session, ownerExtend, dispatchReceiverType)?.substitutor
        }

        val ownerClassId = session.cfirProvider.getContainingClass(symbol)?.classId
        if (ownerClassId != null) {
            val concreteOwnerType = concreteTypeForOwner(ownerClassId) ?: return null
            val ownerDeclaration = session.symbolProvider.getClassLikeSymbolByClassId(ownerClassId)?.cfir ?: return null
            return createClassLikeDeclarationSubstitutor(ownerDeclaration, concreteOwnerType)
        }
        return null
    }

    private fun concreteTypeForOwner(ownerClassId: ClassId): ConeCangJieType? {
        substitutionOwnerType
            ?.takeIf { it.classIdOrPrimitiveClassId == ownerClassId }
            ?.let { return it }
        substitutionOwnerType
            ?.let { findConcreteTypeInHierarchy(it, ownerClassId) }
            ?.let { return it }

        return synchronized(concreteSupertypeCache) {
            concreteSupertypeCache.getOrPut(ownerClassId) {
                findConcreteTypeInHierarchy(dispatchReceiverType, ownerClassId)
            }
        }
    }

    private fun findConcreteTypeInHierarchy(rootType: ConeCangJieType, targetClassId: ClassId): ConeCangJieType? {
        if (rootType.classIdOrPrimitiveClassId == targetClassId) return rootType

        val queue = ArrayDeque<ConeCangJieType>()
        val visited = linkedSetOf<ConeCangJieType>()
        queue += rootType

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (!visited.add(current)) continue
            if (current.classIdOrPrimitiveClassId == targetClassId) return current
            queue.addAll(session.typeAwareSupertypeProviderOrNull?.getDirectSupertypes(current).orEmpty())
        }

        return null
    }

    private fun createClassLikeDeclarationSubstitutor(
        declaration: CfirClassLikeDeclaration,
        concreteType: ConeCangJieType,
    ): ConeSubstitutor? {
        if (concreteType !is ConeLookupTagBasedType) return null
        if (declaration.typeParameters.isEmpty()) return ConeSubstitutor.Empty
        if (declaration.typeParameters.size != concreteType.typeArguments.size) return null

        val replacements: Map<TypeConstructorMarker, ConeCangJieType> =
            declaration.typeParameters.zip(concreteType.typeArguments).associate { (typeParameter, argument) ->
                typeParameter.symbol.toLookupTag() to argument.type
            }
        return replacements.takeIf { it.isNotEmpty() }?.let(::CfirTypeSubstitutorByMap) ?: ConeSubstitutor.Empty
    }

    override fun toString(): String {
        return "Substitution scope for [$useSiteMemberScope] on $dispatchReceiverType"
    }
}

/**
 * 对 substitution override 做统一“回到原始声明”的入口。
 *
 * 解析阶段允许看到替换后的签名壳，但 owner/file/visibility 等元数据
 * 必须回到原始声明上计算，否则同一成员会在不同前端路径下得到不同语义。
 */
@Suppress("UNCHECKED_CAST")
internal inline fun <reified S : CfirCallableSymbol<*>> S.unwrapOriginalForSubstitutionOverride(): S = unwrapSubstitutionOverrides()

/**
 * substitution override 的返回类型延迟替换。
 *
 * 原始成员返回类型可能仍处于隐式推断阶段；copy 创建阶段只记录替换规则，
 * 后续由当前阶段的 [CallableCopyTypeCalculator] 推进原始声明并完成替换。
 */
private class DeferredReturnTypeOfSubstitution(
    private val substitutor: ConeSubstitutor,
    private val baseSymbol: CfirCallableSymbol<*>,
) : DeferredCallableCopyReturnType() {
    override fun computeReturnType(calc: CallableCopyTypeCalculator): ConeCangJieType? {
        val baseDeclaration = baseSymbol.cfir
        val baseReturnType = calc.computeReturnTypeOrNull(baseDeclaration) ?: return null
        return substitutor.substituteOrSelf(baseReturnType)
    }

    override fun toString(): String {
        return "DeferredReturnTypeOfSubstitution(substitutor=$substitutor, baseSymbol=$baseSymbol)"
    }
}
