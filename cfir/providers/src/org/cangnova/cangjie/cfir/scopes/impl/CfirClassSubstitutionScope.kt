package org.cangnova.cangjie.cfir.scopes.impl

import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.originalForSubstitutionOverrideAttr
import org.cangnova.cangjie.cfir.originalForSubstitutionOverride
import org.cangnova.cangjie.cfir.unwrapSubstitutionOverrides
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirPropertyAccessor
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.declarations.builder.buildFieldVariableCopy
import org.cangnova.cangjie.cfir.declarations.builder.buildNamedFunctionCopy
import org.cangnova.cangjie.cfir.declarations.builder.buildPropertyAccessorCopy
import org.cangnova.cangjie.cfir.declarations.builder.buildPropertyCopy
import org.cangnova.cangjie.cfir.declarations.builder.buildValueParameterCopy
import org.cangnova.cangjie.cfir.scopes.CfirTypeScope
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.ProcessorAction
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.session.extendProvider
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.session.typeAwareSupertypeProviderOrNull
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFieldVariableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertyAccessorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeSubstitutorByMap
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeLookupTagBasedType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeSimpleCangJieType
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId
import org.cangnova.cangjie.cfir.types.type
import org.cangnova.cangjie.cfir.types.withReplacedSourceAndType
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name

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
) : CfirTypeScope() {
    private val functionOverrideCache = mutableMapOf<CfirNamedFunctionSymbol, CfirNamedFunctionSymbol>()
    private val propertyOverrideCache = mutableMapOf<CfirPropertySymbol, CfirPropertySymbol>()
    private val fieldOverrideCache = mutableMapOf<CfirFieldVariableSymbol, CfirFieldVariableSymbol>()
    private val wrappedBaseScopeCache = mutableMapOf<CfirTypeScope, CfirTypeScope>()
    private val concreteSupertypeCache = mutableMapOf<ClassId, ConeCangJieType?>()

    override fun getCallableNames(): Set<Name> = useSiteMemberScope.getCallableNames()

    override fun getClassifierNames(): Set<Name> = useSiteMemberScope.getClassifierNames()

    override fun processFunctionsByName(name: Name, processor: (CfirNamedFunctionSymbol) -> Unit) {
        useSiteMemberScope.processFunctionsByName(name) { original ->
            processor(substituteFunctionSymbol(original))
        }
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
        return CfirClassSubstitutionScope(newSession, replacedScope, dispatchReceiverType)
    }

    private fun substitutedBaseScope(baseScope: CfirTypeScope): CfirTypeScope {
        if (baseScope === useSiteMemberScope) return this
        return synchronized(wrappedBaseScopeCache) {
            wrappedBaseScopeCache.getOrPut(baseScope) {
                CfirClassSubstitutionScope(session, baseScope, dispatchReceiverType)
            }
        }
    }

    private fun substituteCallableSymbol(symbol: CfirCallableSymbol<*>): CfirCallableSymbol<*> {
        val originalSymbol = symbol.originalForSubstitutionOverride ?: symbol
        return when (originalSymbol) {
            is CfirNamedFunctionSymbol -> substituteFunctionSymbol(originalSymbol)
            is CfirPropertySymbol -> substitutePropertySymbol(originalSymbol)
            is CfirFieldVariableSymbol -> substituteFieldSymbol(originalSymbol)
            else -> originalSymbol
        }
    }

    private fun substituteFunctionSymbol(symbol: CfirNamedFunctionSymbol): CfirNamedFunctionSymbol {
        val originalSymbol = symbol.unwrapOriginalForSubstitutionOverride()
        return synchronized(functionOverrideCache) {
            functionOverrideCache.getOrPut(originalSymbol) {
                createSubstitutedFunctionSymbol(originalSymbol)
            }
        }
    }

    private fun substitutePropertySymbol(symbol: CfirPropertySymbol): CfirPropertySymbol {
        val originalSymbol = symbol.unwrapOriginalForSubstitutionOverride()
        return synchronized(propertyOverrideCache) {
            propertyOverrideCache.getOrPut(originalSymbol) {
                createSubstitutedPropertySymbol(originalSymbol)
            }
        }
    }

    private fun substituteFieldSymbol(symbol: CfirFieldVariableSymbol): CfirFieldVariableSymbol {
        val originalSymbol = symbol.unwrapOriginalForSubstitutionOverride()
        return synchronized(fieldOverrideCache) {
            fieldOverrideCache.getOrPut(originalSymbol) {
                createSubstitutedFieldSymbol(originalSymbol)
            }
        }
    }

    private fun createSubstitutedFunctionSymbol(symbol: CfirNamedFunctionSymbol): CfirNamedFunctionSymbol {
        val substitutor = computeCallableSubstitutor(symbol)
        if (substitutor === ConeSubstitutor.Empty || substitutor == null) return symbol

        val declaration = symbol.cfir as? CfirNamedFunction ?: return symbol
        val copiedSymbol = CfirNamedFunctionSymbol(symbol.callableId)
        val copiedDeclaration = buildNamedFunctionCopy(declaration) {
            origin = substitutionOverrideOrigin(symbol)
            this.symbol = copiedSymbol
            dispatchReceiverType = substituteDispatchReceiverType(declaration.dispatchReceiverType, substitutor)
            returnTypeRef = substituteTypeRef(symbol.resolvedReturnTypeRef, substitutor)
            valueParameters.clear()
            valueParameters += substituteValueParameters(declaration.valueParameters, substitutor)
        }
        copiedDeclaration.originalForSubstitutionOverrideAttr = declaration
        return copiedSymbol
    }

    private fun createSubstitutedPropertySymbol(symbol: CfirPropertySymbol): CfirPropertySymbol {
        val substitutor = computeCallableSubstitutor(symbol)
        if (substitutor === ConeSubstitutor.Empty || substitutor == null) return symbol

        val declaration = symbol.cfir
        val copiedSymbol = CfirPropertySymbol(symbol.callableId)
        val copiedDeclaration = buildPropertyCopy(declaration) {
            origin = substitutionOverrideOrigin(symbol)
            this.symbol = copiedSymbol
            dispatchReceiverType = substituteDispatchReceiverType(declaration.dispatchReceiverType, substitutor)
            returnTypeRef = substituteTypeRef(symbol.resolvedReturnTypeRef, substitutor)
            getter = substituteAccessorFunction(declaration.getter, substitutor)
            setter = substituteAccessorFunction(declaration.setter, substitutor)
        }
        copiedDeclaration.originalForSubstitutionOverrideAttr = declaration
        return copiedSymbol
    }

    private fun createSubstitutedFieldSymbol(symbol: CfirFieldVariableSymbol): CfirFieldVariableSymbol {
        val substitutor = computeCallableSubstitutor(symbol)
        if (substitutor === ConeSubstitutor.Empty || substitutor == null) return symbol

        val declaration = symbol.cfir
        val copiedSymbol = CfirFieldVariableSymbol(symbol.callableId)
        val copiedDeclaration = buildFieldVariableCopy(declaration) {
            origin = substitutionOverrideOrigin(symbol)
            this.symbol = copiedSymbol
            dispatchReceiverType = substituteDispatchReceiverType(declaration.dispatchReceiverType, substitutor)
            returnTypeRef = substituteTypeRef(symbol.resolvedReturnTypeRef, substitutor)
        }
        copiedDeclaration.originalForSubstitutionOverrideAttr = declaration
        return copiedSymbol
    }

    private fun substituteAccessorFunction(function: CfirPropertyAccessor?, substitutor: ConeSubstitutor): CfirPropertyAccessor? {
        function ?: return null
        val symbol = function.symbol
        val copiedSymbol = CfirPropertyAccessorSymbol()
        val copiedDeclaration = buildPropertyAccessorCopy(function) {
            origin = substitutionOverrideOrigin(symbol)
            this.symbol = copiedSymbol
            dispatchReceiverType = substituteDispatchReceiverType(function.dispatchReceiverType, substitutor)
            returnTypeRef = substituteTypeRef(symbol.resolvedReturnTypeRef, substitutor)
            valueParameters.clear()
            valueParameters += substituteValueParameters(function.valueParameters, substitutor)
        }
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
        val ownerClassId = session.cfirProvider.getContainingClass(symbol)?.classId
        return if (ownerClassId != null && ownerClassId == dispatchReceiverType.classIdOrPrimitiveClassId) {
            CfirDeclarationOrigin.SubstitutionOverride.CallSite
        } else {
            CfirDeclarationOrigin.SubstitutionOverride.DeclarationSite
        }
    }

    private fun computeCallableSubstitutor(symbol: CfirCallableSymbol<*>): ConeSubstitutor? {
        val ownerClassId = session.cfirProvider.getContainingClass(symbol)?.classId
        if (ownerClassId != null) {
            val concreteOwnerType = concreteTypeForOwner(ownerClassId) ?: return null
            val ownerDeclaration = session.symbolProvider.getClassLikeSymbolByClassId(ownerClassId)?.cfir ?: return null
            return createClassLikeDeclarationSubstitutor(ownerDeclaration, concreteOwnerType)
        }

        val ownerExtend = session.extendProvider.getContainingExtend(symbol)
            ?.takeIf(session.extendProvider::isExtendAccessible)
            ?: return null
        return findExtendDeclarationSubstitutor(ownerExtend)
    }

    private fun concreteTypeForOwner(ownerClassId: ClassId): ConeCangJieType? {
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

        val replacements = declaration.typeParameters.zip(concreteType.typeArguments).associate { (typeParameter, argument) ->
            typeParameter.symbol.name.asString() to argument.type
        }
        return replacements.takeIf { it.isNotEmpty() }?.let(::CfirTypeSubstitutorByMap) ?: ConeSubstitutor.Empty
    }

    private fun findExtendDeclarationSubstitutor(extend: CfirExtend): ConeSubstitutor? {
        val queue = ArrayDeque<ConeCangJieType>()
        val visited = linkedSetOf<ConeCangJieType>()
        queue += dispatchReceiverType

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (!visited.add(current)) continue

            createExtendDeclarationSubstitutor(extend, current)?.let { return it }

            queue.addAll(session.typeAwareSupertypeProviderOrNull?.getDirectSupertypes(current).orEmpty())
        }

        return null
    }

    private fun createExtendDeclarationSubstitutor(
        extend: CfirExtend,
        concreteReceiverType: ConeCangJieType,
    ): ConeSubstitutor? {
        val targetPattern = (extend.extendedTypeRef as? CfirResolvedTypeRef)?.coneType ?: return null
        val substitutions = linkedMapOf<String, ConeCangJieType>()
        val extendTypeParameterNames = extend.typeParameters.mapTo(linkedSetOf()) { it.name.asString() }

        if (!matchExtendTargetType(targetPattern, concreteReceiverType, extendTypeParameterNames, substitutions)) {
            return null
        }
        if (extendTypeParameterNames.any { it !in substitutions }) {
            return null
        }

        return substitutions.takeIf { it.isNotEmpty() }?.let(::CfirTypeSubstitutorByMap) ?: ConeSubstitutor.Empty
    }

    private fun matchExtendTargetType(
        pattern: ConeCangJieType,
        actual: ConeCangJieType,
        extendTypeParameterNames: Set<String>,
        substitutions: MutableMap<String, ConeCangJieType>,
    ): Boolean {
        return when (pattern) {
            is org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType -> {
                val typeParameterName = pattern.lookupTag.name.asString()
                if (typeParameterName !in extendTypeParameterNames) {
                    pattern == actual
                } else {
                    val existing = substitutions[typeParameterName]
                    existing == null || existing == actual
                }.also { matches ->
                    if (matches) {
                        substitutions.putIfAbsent(typeParameterName, actual)
                    }
                }
            }

            is ConePrimitiveType -> actual is ConePrimitiveType && pattern.kind == actual.kind

            is ConeLookupTagBasedType -> {
                val actualClassifier = actual as? ConeLookupTagBasedType ?: return false
                if (pattern.classIdOrPrimitiveClassId != actualClassifier.classIdOrPrimitiveClassId) return false
                if (pattern.typeArguments.size != actualClassifier.typeArguments.size) return false

                pattern.typeArguments.indices.all { index ->
                    matchExtendTargetType(
                        pattern = pattern.typeArguments[index].type,
                        actual = actualClassifier.typeArguments[index].type,
                        extendTypeParameterNames = extendTypeParameterNames,
                        substitutions = substitutions,
                    )
                }
            }

            else -> pattern == actual
        }
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
