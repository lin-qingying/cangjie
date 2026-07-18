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
import org.cangnova.cangjie.cfir.resolve.providers.createCallableOwnerUseSiteSubstitutor
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
    /**
     * 当前 use-site session。
     */
    private val session: CfirSession,
    /**
     * 被包装的 use-site 成员 scope。
     */
    private val useSiteMemberScope: CfirTypeScope,
    /**
     * 当前 dispatch receiver 具体类型。
     */
    private val dispatchReceiverType: ConeCangJieType,
    /**
     * 可选的替换 owner 类型；父类型替换时与 dispatch receiver 区分。
     */
    private val substitutionOwnerType: ConeCangJieType? = null,
) : CfirTypeScope() {
    /**
     * owner 类型参数替换器。
     */
    val substitutor: ConeSubstitutor by lazy(LazyThreadSafetyMode.PUBLICATION) {
        val ownerClassId = dispatchReceiverType.classIdOrPrimitiveClassId
        val concreteOwnerType = ownerClassId?.let(::concreteTypeForOwner)
        val ownerDeclaration = ownerClassId
            ?.let { session.symbolProvider.getClassLikeSymbolByClassId(it)?.cfir }
        if (ownerDeclaration != null && concreteOwnerType != null) {
            createClassLikeDeclarationSubstitutor(ownerDeclaration, concreteOwnerType) ?: ConeSubstitutor.Empty
        } else {
            ConeSubstitutor.Empty
        }
    }

    /**
     * 返回当前 scope 对指定 owner 类型参数的实例化结果。
     *
     * 从该 scope 产出的 callable 已经按 [dispatchReceiverType] 构造完成；调用推断阶段必须复用
     * 同一 owner 映射，不能通过 substitution override 的原始声明再次创建 owner fresh variable。
     * 即使结果仍是外层声明的类型参数，也表示该参数已由当前构造类型固定，而不是本次调用待推断参数。
     */
    fun substitutedOwnerTypeParameterOrNull(typeParameterSymbol: CfirTypeParameterSymbol): ConeCangJieType? =
        substitutor.substituteOrNull(typeParameterSymbol.constructType())

    /**
     * 函数 substitution override 缓存。
     */
    private val functionOverrideCache = mutableMapOf<CfirNamedFunctionSymbol, CfirNamedFunctionSymbol>()
    /**
     * 属性 substitution override 缓存。
     */
    private val propertyOverrideCache = mutableMapOf<CfirPropertySymbol, CfirPropertySymbol>()
    /**
     * 字段变量 substitution override 缓存。
     */
    private val fieldOverrideCache = mutableMapOf<CfirFieldVariableSymbol, CfirFieldVariableSymbol>()
    /**
     * 构造器 substitution override 缓存。
     */
    private val constructorOverrideCache = mutableMapOf<CfirConstructorSymbol, CfirConstructorSymbol>()
    /**
     * enum constructor substitution override 缓存。
     */
    private val enumConstructorOverrideCache = mutableMapOf<CfirEnumConstructorSymbol, CfirEnumConstructorSymbol>()
    /**
     * base scope 替换 wrapper 缓存。
     */
    private val wrappedBaseScopeCache = mutableMapOf<CfirTypeScope, CfirTypeScope>()
    /**
     * owner ClassId 到具体 supertype 的缓存。
     */
    private val concreteSupertypeCache = mutableMapOf<ClassId, ConeCangJieType?>()

    /**
     * 返回委托 scope callable 名称。
     */
    override fun getCallableNames(): Set<Name> = useSiteMemberScope.getCallableNames()

    /**
     * 返回委托 scope classifier 名称。
     */
    override fun getClassifierNames(): Set<Name> = useSiteMemberScope.getClassifierNames()

    /**
     * 按名称处理替换后的函数。
     */
    override fun processFunctionsByName(name: Name, processor: (CfirNamedFunctionSymbol) -> Unit) {
        useSiteMemberScope.processFunctionsByName(name) { original ->
            processor(substituteFunctionSymbol(original))
        }
    }

    /**
     * 按名称处理替换后的属性。
     */
    override fun processPropertiesByName(name: Name, processor: (CfirPropertySymbol) -> Unit) {
        useSiteMemberScope.processPropertiesByName(name) { original ->
            processor(substitutePropertySymbol(original))
        }
    }

    /**
     * classifier 不需要 substitution override，直接透传。
     */
    override fun processClassifiersByName(name: Name, processor: (CfirClassLikeSymbol<*>) -> Unit) {
        useSiteMemberScope.processClassifiersByName(name, processor)
    }

    /**
     * 透传嵌套 classifier 时合并底层 scope 与当前具体 owner 的替换器。
     */
    override fun processClassifiersByNameWithSubstitution(
        name: Name,
        processor: (CfirClassifierSymbol<*>, ConeSubstitutor) -> Unit,
    ) {
        useSiteMemberScope.processClassifiersByNameWithSubstitution(name) { classifier, delegateSubstitutor ->
            val effectiveSubstitutor = when {
                delegateSubstitutor === ConeSubstitutor.Empty -> substitutor
                substitutor === ConeSubstitutor.Empty -> delegateSubstitutor
                else -> ChainedCfirSubstitutor(delegateSubstitutor, substitutor)
            }
            processor(classifier, effectiveSubstitutor)
        }
    }

    /**
     * 处理替换后的声明构造器。
     */
    override fun processDeclaredConstructors(processor: (CfirConstructorSymbol) -> Unit) {
        useSiteMemberScope.processDeclaredConstructors { original ->
            processor(substituteConstructorSymbol(original))
        }
    }

    /**
     * 按名称处理替换后的 callable。
     */
    override fun processCallablesByName(name: Name, processor: (CfirCallableSymbol<*>) -> Unit) {
        useSiteMemberScope.processCallablesByName(name) { original ->
            processor(substituteCallableSymbol(original))
        }
    }

    /**
     * 处理替换后函数的直接覆盖链。
     */
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

    /**
     * 处理替换后属性的直接覆盖链。
     */
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

    /**
     * 替换委托 scope 的 session 后重建 substitution scope。
     */
    override fun withReplacedSessionOrNull(newSession: CfirSession, newScopeSession: ScopeSession): CfirTypeScope? {
        val replacedScope = useSiteMemberScope.withReplacedSessionOrNull(newSession, newScopeSession) ?: return null
        return CfirClassSubstitutionScope(newSession, replacedScope, dispatchReceiverType, substitutionOwnerType)
    }

    /**
     * 将 base scope 包装成当前 receiver 类型下的 substitution scope。
     */
    private fun substitutedBaseScope(baseScope: CfirTypeScope): CfirTypeScope {
        if (baseScope === useSiteMemberScope) return this
        return synchronized(wrappedBaseScopeCache) {
            wrappedBaseScopeCache.getOrPut(baseScope) {
                CfirClassSubstitutionScope(session, baseScope, dispatchReceiverType, substitutionOwnerType)
            }
        }
    }

    /**
     * 根据 callable symbol 具体类型创建或返回 substitution override。
     */
    private fun substituteCallableSymbol(symbol: CfirCallableSymbol<*>): CfirCallableSymbol<*> {
        return when (symbol) {
            is CfirNamedFunctionSymbol -> substituteFunctionSymbol(symbol)
            is CfirPropertySymbol -> substitutePropertySymbol(symbol)
            is CfirFieldVariableSymbol -> substituteFieldSymbol(symbol)
            is CfirEnumConstructorSymbol -> substituteEnumConstructorSymbol(symbol)
            else -> symbol
        }
    }

    /**
     * 替换函数 symbol。
     */
    private fun substituteFunctionSymbol(symbol: CfirNamedFunctionSymbol): CfirNamedFunctionSymbol {
        return synchronized(functionOverrideCache) {
            functionOverrideCache.getOrPut(symbol) {
                createSubstitutedFunctionSymbol(symbol)
            }
        }
    }

    /**
     * 替换属性 symbol。
     */
    private fun substitutePropertySymbol(symbol: CfirPropertySymbol): CfirPropertySymbol {
        return synchronized(propertyOverrideCache) {
            propertyOverrideCache.getOrPut(symbol) {
                createSubstitutedPropertySymbol(symbol)
            }
        }
    }

    /**
     * 替换字段变量 symbol。
     */
    private fun substituteFieldSymbol(symbol: CfirFieldVariableSymbol): CfirFieldVariableSymbol {
        return synchronized(fieldOverrideCache) {
            fieldOverrideCache.getOrPut(symbol) {
                createSubstitutedFieldSymbol(symbol)
            }
        }
    }

    /**
     * 替换构造器 symbol。
     */
    private fun substituteConstructorSymbol(symbol: CfirConstructorSymbol): CfirConstructorSymbol {
        return synchronized(constructorOverrideCache) {
            constructorOverrideCache.getOrPut(symbol) {
                createSubstitutedConstructorSymbol(symbol)
            }
        }
    }

    /**
     * 替换 enum constructor symbol。
     */
    private fun substituteEnumConstructorSymbol(symbol: CfirEnumConstructorSymbol): CfirEnumConstructorSymbol {
        return synchronized(enumConstructorOverrideCache) {
            enumConstructorOverrideCache.getOrPut(symbol) {
                createSubstitutedEnumConstructorSymbol(symbol)
            }
        }
    }

    /**
     * 创建函数 substitution override。
     */
    private fun createSubstitutedFunctionSymbol(symbol: CfirNamedFunctionSymbol): CfirNamedFunctionSymbol {
        val ownerSubstitutor = computeCallableSubstitutor(symbol)
        if (ownerSubstitutor === ConeSubstitutor.Empty || ownerSubstitutor == null) return symbol

        symbol.lazyResolveToPhase(CfirResolvePhase.TYPES)
        val declaration = symbol.cfir as? CfirNamedFunction ?: return symbol
        val copiedSymbol = CfirNamedFunctionSymbol(symbol.callableId)
        val typeParameterSubstitution = declaration.createSubstitutedTypeParameters(copiedSymbol, ownerSubstitutor)
        val substitutor = typeParameterSubstitution.substitutor
        val returnTypeData = declaration.substitutedReturnTypeData(symbol, substitutor)
        val copiedDeclaration = buildNamedFunctionCopy(declaration) {
            origin = substitutionOverrideOrigin(symbol)
            this.symbol = copiedSymbol
            dispatchReceiverType = substituteDispatchReceiverType(declaration.dispatchReceiverType, substitutor)
            typeParameters.clear()
            typeParameters += typeParameterSubstitution.typeParameters
            returnTypeRef = returnTypeData.typeRef
            valueParameters.clear()
            valueParameters += substituteValueParameters(declaration.valueParameters, substitutor)
        }
        copiedDeclaration.attributes.deferredCallableCopyReturnType = returnTypeData.deferredReturnType
        copiedDeclaration.originalForSubstitutionOverrideAttr = declaration
        return copiedSymbol
    }

    /**
     * 创建属性 substitution override。
     */
    private fun createSubstitutedPropertySymbol(symbol: CfirPropertySymbol): CfirPropertySymbol {
        val ownerSubstitutor = computeCallableSubstitutor(symbol)
        if (ownerSubstitutor === ConeSubstitutor.Empty || ownerSubstitutor == null) return symbol

        symbol.lazyResolveToPhase(CfirResolvePhase.TYPES)
        val declaration = symbol.cfir
        val copiedSymbol = CfirPropertySymbol(symbol.callableId)
        val typeParameterSubstitution = declaration.createSubstitutedTypeParameters(copiedSymbol, ownerSubstitutor)
        val substitutor = typeParameterSubstitution.substitutor
        val returnTypeData = declaration.substitutedReturnTypeData(symbol, substitutor)
        val copiedDeclaration = buildPropertyCopy(declaration) {
            origin = substitutionOverrideOrigin(symbol)
            this.symbol = copiedSymbol
            dispatchReceiverType = substituteDispatchReceiverType(declaration.dispatchReceiverType, substitutor)
            typeParameters.clear()
            typeParameters += typeParameterSubstitution.typeParameters
            returnTypeRef = returnTypeData.typeRef
            getter = substituteAccessorFunction(declaration.getter, substitutor)
            setter = substituteAccessorFunction(declaration.setter, substitutor)
        }
        copiedDeclaration.attributes.deferredCallableCopyReturnType = returnTypeData.deferredReturnType
        copiedDeclaration.originalForSubstitutionOverrideAttr = declaration
        return copiedSymbol
    }

    /**
     * 创建字段变量 substitution override。
     */
    private fun createSubstitutedFieldSymbol(symbol: CfirFieldVariableSymbol): CfirFieldVariableSymbol {
        val ownerSubstitutor = computeCallableSubstitutor(symbol)
        if (ownerSubstitutor === ConeSubstitutor.Empty || ownerSubstitutor == null) return symbol

        symbol.lazyResolveToPhase(CfirResolvePhase.TYPES)
        val declaration = symbol.cfir
        val copiedSymbol = CfirFieldVariableSymbol(symbol.callableId)
        val typeParameterSubstitution = declaration.createSubstitutedTypeParameters(copiedSymbol, ownerSubstitutor)
        val substitutor = typeParameterSubstitution.substitutor
        val returnTypeData = declaration.substitutedReturnTypeData(symbol, substitutor)
        val copiedDeclaration = buildFieldVariableCopy(declaration) {
            origin = substitutionOverrideOrigin(symbol)
            this.symbol = copiedSymbol
            dispatchReceiverType = substituteDispatchReceiverType(declaration.dispatchReceiverType, substitutor)
            typeParameters.clear()
            typeParameters += typeParameterSubstitution.typeParameters
            returnTypeRef = returnTypeData.typeRef
        }
        copiedDeclaration.attributes.deferredCallableCopyReturnType = returnTypeData.deferredReturnType
        copiedDeclaration.originalForSubstitutionOverrideAttr = declaration
        return copiedSymbol
    }

    /**
     * 创建构造器 substitution override。
     */
    private fun createSubstitutedConstructorSymbol(symbol: CfirConstructorSymbol): CfirConstructorSymbol {
        val ownerSubstitutor = computeCallableSubstitutor(symbol)
        if (ownerSubstitutor === ConeSubstitutor.Empty || ownerSubstitutor == null) return symbol

        symbol.lazyResolveToPhase(CfirResolvePhase.TYPES)
        val declaration = symbol.cfir
        val copiedSymbol = CfirConstructorSymbol(symbol.callableId)
        val typeParameterSubstitution = declaration.createSubstitutedTypeParameters(copiedSymbol, ownerSubstitutor)
        val substitutor = typeParameterSubstitution.substitutor
        val returnTypeData = declaration.substitutedReturnTypeData(symbol, substitutor)
        val copiedDeclaration = buildConstructorCopy(declaration) {
            origin = substitutionOverrideOrigin(symbol)
            this.symbol = copiedSymbol
            dispatchReceiverType = substituteDispatchReceiverType(declaration.dispatchReceiverType, substitutor)
            typeParameters.clear()
            typeParameters += typeParameterSubstitution.typeParameters
            returnTypeRef = returnTypeData.typeRef
            valueParameters.clear()
            valueParameters += substituteValueParameters(declaration.valueParameters, substitutor)
        }
        copiedDeclaration.attributes.deferredCallableCopyReturnType = returnTypeData.deferredReturnType
        copiedDeclaration.originalForSubstitutionOverrideAttr = declaration
        return copiedSymbol
    }

    /**
     * 创建 enum constructor substitution override。
     */
    private fun createSubstitutedEnumConstructorSymbol(symbol: CfirEnumConstructorSymbol): CfirEnumConstructorSymbol {
        val ownerSubstitutor = computeCallableSubstitutor(symbol)
        if (ownerSubstitutor === ConeSubstitutor.Empty || ownerSubstitutor == null) return symbol

        symbol.lazyResolveToPhase(CfirResolvePhase.TYPES)
        val declaration = symbol.cfir
        val copiedSymbol = CfirEnumConstructorSymbol(symbol.callableId)
        val typeParameterSubstitution = declaration.createSubstitutedTypeParameters(copiedSymbol, ownerSubstitutor)
        val substitutor = typeParameterSubstitution.substitutor
        val returnTypeData = declaration.substitutedReturnTypeData(symbol, substitutor)
        val copiedDeclaration = buildEnumConstructorCopy(declaration) {
            origin = substitutionOverrideOrigin(symbol)
            this.symbol = copiedSymbol
            dispatchReceiverType = substituteDispatchReceiverType(declaration.dispatchReceiverType, substitutor)
            typeParameters.clear()
            typeParameters += typeParameterSubstitution.typeParameters
            returnTypeRef = returnTypeData.typeRef
            valueParameters.clear()
            valueParameters += substituteValueParameters(declaration.valueParameters, substitutor)
        }
        copiedDeclaration.attributes.deferredCallableCopyReturnType = returnTypeData.deferredReturnType
        copiedDeclaration.originalForSubstitutionOverrideAttr = declaration
        return copiedSymbol
    }

    /**
     * 替换属性访问器函数。
     */
    private fun substituteAccessorFunction(function: CfirPropertyAccessor?, substitutor: ConeSubstitutor): CfirPropertyAccessor? {
        function ?: return null
        val symbol = function.symbol
        val copiedSymbol = CfirPropertyAccessorSymbol()
        val typeParameterSubstitution = function.createSubstitutedTypeParameters(copiedSymbol, substitutor)
        val accessorSubstitutor = typeParameterSubstitution.substitutor
        val returnTypeData = function.substitutedReturnTypeData(symbol, accessorSubstitutor)
        val copiedDeclaration = buildPropertyAccessorCopy(function) {
            origin = substitutionOverrideOrigin(symbol)
            this.symbol = copiedSymbol
            dispatchReceiverType = substituteDispatchReceiverType(function.dispatchReceiverType, accessorSubstitutor)
            typeParameters.clear()
            typeParameters += typeParameterSubstitution.typeParameters
            returnTypeRef = returnTypeData.typeRef
            valueParameters.clear()
            valueParameters += substituteValueParameters(function.valueParameters, accessorSubstitutor)
        }
        copiedDeclaration.attributes.deferredCallableCopyReturnType = returnTypeData.deferredReturnType
        copiedDeclaration.originalForSubstitutionOverrideAttr = function
        return copiedDeclaration
    }

    /**
     * 替换 value parameter 列表。
     */
    private fun substituteValueParameters(
        valueParameters: List<CfirValueParameter>,
        substitutor: ConeSubstitutor,
    ): List<CfirValueParameter> {
        return valueParameters.map { valueParameter ->
            val copiedSymbol = org.cangnova.cangjie.cfir.symbols.CfirValueParameterSymbol(valueParameter.symbol.callableId)
            buildValueParameterCopy(valueParameter) {
                symbol = copiedSymbol
                returnTypeRef = substituteTypeRef(valueParameter.returnTypeRef, substitutor)
            }
        }
    }

    /**
     * 返回类型替换结果。
     *
     * @property typeRef 替换后的 return type ref。
     * @property deferredReturnType 若原返回类型未解析，则保存延迟替换任务。
     */
    private data class ReturnTypeData(
        /**
         * 替换后的 return type ref。
         */
        val typeRef: CfirTypeRef,
        /**
         * 原返回类型尚未解析时保留的延迟替换任务。
         */
        val deferredReturnType: DeferredReturnTypeOfSubstitution?,
    )

    /**
     * callable 自身类型参数替换结果。
     *
     * @property typeParameters 新建的类型参数列表。
     * @property substitutor 同时覆盖 callable 类型参数和 owner 类型参数的替换器。
     */
    private data class TypeParameterSubstitutionData(
        /**
         * 为 substitution override 新建的 callable 类型参数列表。
         */
        val typeParameters: List<CfirTypeParameter>,
        /**
         * 同时覆盖 callable 自身类型参数与 owner 类型参数的替换器。
         */
        val substitutor: ConeSubstitutor,
    )

    /**
     * 对齐 Kotlin `FirClassSubstitutionScope.createSubstitutedData`：
     * substitution override 必须重新创建 callable 自身类型参数，并把旧类型参数、
     * owner 类型参数同时替换到 bounds、形参和返回类型中。
     */
    private fun CfirCallableDeclaration.createSubstitutedTypeParameters(
        copiedSymbol: CfirBasedSymbol<*>,
        ownerSubstitutor: ConeSubstitutor,
    ): TypeParameterSubstitutionData {
        if (typeParameters.isEmpty()) {
            return TypeParameterSubstitutionData(emptyList(), ownerSubstitutor)
        }

        val typeParameterDeclarations = typeParameters.map { it as CfirTypeParameter }
        val newSymbols = typeParameterDeclarations.map { CfirTypeParameterSymbol() }
        val callableTypeParameterSubstitutor = CfirTypeSubstitutorByMap(
            typeParameterDeclarations.zip(newSymbols).associate { (typeParameter, newSymbol) ->
                typeParameter.symbol.toLookupTag() to newSymbol.constructType()
            }
        )
        val substitutor = ChainedCfirSubstitutor(callableTypeParameterSubstitutor, ownerSubstitutor)
        val newTypeParameters = typeParameterDeclarations.zip(newSymbols).map { (typeParameter, newSymbol) ->
            buildTypeParameterCopy(typeParameter) {
                origin = substitutionOverrideOrigin(this@createSubstitutedTypeParameters.symbol)
                containingDeclarationSymbol = copiedSymbol
                symbol = newSymbol
                bounds.clear()
                bounds += typeParameter.bounds.map { bound -> substituteTypeRef(bound, substitutor) }
            }
        }

        return TypeParameterSubstitutionData(newTypeParameters, substitutor)
    }

    /**
     * 计算 callable 返回类型替换数据。
     */
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

    /**
     * 替换 dispatch receiver 类型。
     */
    private fun substituteDispatchReceiverType(
        type: ConeSimpleCangJieType?,
        substitutor: ConeSubstitutor,
    ): ConeSimpleCangJieType? {
        if (type == null) return null
        return substitutor.substituteOrSelf(type) as? ConeSimpleCangJieType ?: type
    }

    /**
     * 替换 resolved type ref。
     */
    private fun substituteTypeRef(
        typeRef: CfirResolvedTypeRef,
        substitutor: ConeSubstitutor,
    ): CfirResolvedTypeRef {
        val substitutedType = substitutor.substituteOrSelf(typeRef.coneType)
        return typeRef.withReplacedSourceAndType(typeRef.source, substitutedType)
    }

    /**
     * 替换普通 type ref；未 resolved 的 type ref 保持原样。
     */
    private fun substituteTypeRef(
        typeRef: CfirTypeRef,
        substitutor: ConeSubstitutor,
    ): CfirTypeRef {
        return when (typeRef) {
            is CfirResolvedTypeRef -> substituteTypeRef(typeRef, substitutor)
            else -> typeRef
        }
    }

    /**
     * 顺序应用两个 substitutor 的链式 substitutor。
     */
    private class ChainedCfirSubstitutor(
        /**
         * 先执行的替换器。
         */
        private val first: ConeSubstitutor,
        /**
         * 后执行的替换器。
         */
        private val second: ConeSubstitutor,
    ) : ConeSubstitutor() {
        /**
         * 替换类型，若无变化则返回原类型。
         */
        override fun substituteOrSelf(type: ConeCangJieType): ConeCangJieType {
            return second.substituteOrSelf(first.substituteOrSelf(type))
        }

        /**
         * 替换类型；无变化时返回 `null`。
         */
        override fun substituteOrNull(type: ConeCangJieType): ConeCangJieType? {
            val afterFirst = first.substituteOrNull(type)
            val afterSecond = second.substituteOrNull(afterFirst ?: type)
            return afterSecond ?: afterFirst
        }

        /**
         * 替换类型实参。
         */
        override fun substituteArgument(projection: ConeTypeProjection, index: Int): ConeTypeProjection? {
            val afterFirst = first.substituteArgument(projection, index)
            val afterSecond = second.substituteArgument(afterFirst ?: projection, index)
            return afterSecond ?: afterFirst
        }
    }

    /**
     * 计算 substitution override 的声明 origin。
     */
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

        val ownerClassId = ownerClassIdForCallable(symbol)
        return if (ownerClassId != null && ownerClassId == dispatchReceiverType.classIdOrPrimitiveClassId) {
            CfirDeclarationOrigin.SubstitutionOverride.CallSite
        } else {
            CfirDeclarationOrigin.SubstitutionOverride.DeclarationSite
        }
    }

    /**
     * 计算 callable 使用的 owner substitutor。
     */
    private fun computeCallableSubstitutor(symbol: CfirCallableSymbol<*>): ConeSubstitutor? {
        val receiverType = substitutionOwnerType ?: dispatchReceiverType
        return createCallableOwnerUseSiteSubstitutor(session, symbol, receiverType)
    }

    /**
     * 返回 callable 所属 owner class id。
     */
    private fun ownerClassIdForCallable(symbol: CfirCallableSymbol<*>): ClassId? {
        return session.cfirProvider.getContainingClass(symbol)?.classId
            ?: (symbol as? CfirEnumConstructorSymbol)?.let {
                dispatchReceiverType.expandedClassIdOrPrimitiveClassId
            }
    }

    /**
     * 返回 [ownerClassId] 在当前 receiver 继承链中的具体类型。
     */
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

    /**
     * 在继承链中查找目标 class id 对应的具体类型。
     */
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

    /**
     * 为 class-like declaration 创建类型参数替换器。
     */
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

    /**
     * 返回调试文本。
     */
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
    /**
     * use-site substitution override 需要应用到原始返回类型上的替换器。
     */
    private val substitutor: ConeSubstitutor,
    /**
     * 原始 callable symbol，延迟计算时通过它推进真实返回类型。
     */
    private val baseSymbol: CfirCallableSymbol<*>,
) : DeferredCallableCopyReturnType() {
    /**
     * 推进原始返回类型计算并替换为当前 use-site 类型。
     */
    override fun computeReturnType(calc: CallableCopyTypeCalculator): ConeCangJieType? {
        val baseDeclaration = baseSymbol.cfir
        val baseReturnType = calc.computeReturnTypeOrNull(baseDeclaration) ?: return null
        return substitutor.substituteOrSelf(baseReturnType)
    }

    /**
     * 返回调试文本。
     */
    override fun toString(): String {
        return "DeferredReturnTypeOfSubstitution(substitutor=$substitutor, baseSymbol=$baseSymbol)"
    }
}
