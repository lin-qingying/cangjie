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

package org.cangnova.cangjie.cfir.resolve.services

import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.resolve.CfirTypeResolutionConfiguration
import org.cangnova.cangjie.cfir.resolve.SupertypeSupplier
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.resolve.providers.CfirExtendProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirTypeAwareSupertypeProvider
import org.cangnova.cangjie.cfir.resolve.providers.createExtendDeclarationSubstitution
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.extendProvider
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.session.typeResolver
import org.cangnova.cangjie.cfir.symbols.CfirInterfaceSymbol
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.type.model.TypeConstructorMarker

/**
 * 为类型系统提供“具体类型 -> 已实例化父类型”的统一入口。
 *
 * 设计要点：
 * 1. declared supertype 和 extend supertype 都在这里汇总，类型比较不再直接依赖 ClassId 图。
 * 2. extend 的匹配与实例化基于当前 concrete type，因此泛型 extend 不会丢失 use-site 实参。
 * 3. extend 接口会沿 superclass 链继续传播，和官方编译器 `GetAllExtendInterfaceTy` 的语义保持一致。
 */
class CfirTypeAwareSupertypeProviderImpl(
    private val session: CfirSession,
) : CfirTypeAwareSupertypeProvider {
    private val directSupertypesCache = mutableMapOf<ConeCangJieType, List<ConeCangJieType>>()

    override fun getDirectSupertypes(type: ConeCangJieType): List<ConeCangJieType> {
        synchronized(directSupertypesCache) {
            directSupertypesCache[type]?.let { return it }
        }

        val computed = computeDirectSupertypes(type)

        synchronized(directSupertypesCache) {
            return directSupertypesCache.getOrPut(type) { computed }
        }
    }

    private fun computeDirectSupertypes(type: ConeCangJieType): List<ConeCangJieType> {
        val semanticType = type.fullyExpandedType(session)
        if (!semanticType.isSupportedClassifierType()) return emptyList()

        val declaredSupertypes = resolveDeclaredDirectSupertypes(semanticType)
        val effectiveExtendSupertypes = collectEffectiveExtendSupertypes(semanticType, linkedSetOf())

        if (declaredSupertypes.isEmpty() && effectiveExtendSupertypes.isEmpty()) return emptyList()
        return buildList {
            addAll(declaredSupertypes)
            addAll(effectiveExtendSupertypes)
        }.distinct()
    }

    private fun resolveDeclaredDirectSupertypes(type: ConeCangJieType): List<ConeCangJieType> {
        val declaration = resolveClassLikeDeclaration(type) ?: return emptyList()
        val substitutor = declaration.createDeclarationSubstitutor(type)

        return declaration.superTypeRefs.mapNotNull { superTypeRef ->
            val coneType = (superTypeRef as? CfirResolvedTypeRef)?.coneType ?: return@mapNotNull null
            substitutor?.substituteOrSelf(coneType) ?: coneType
        }
    }

    /**
     * 递归收集当前类型及其 superclass 链上所有可继承的 extend 接口。
     *
     * 这里只传播 extend 接口，不展开 superclass 自身的 declared supertype，
     * 避免把“普通名义继承”与“extend 注入接口”这两层语义混在一起。
     */
    private fun collectEffectiveExtendSupertypes(
        type: ConeCangJieType,
        visited: MutableSet<ConeCangJieType>,
    ): List<ConeCangJieType> {
        if (!visited.add(type)) return emptyList()

        val result = linkedSetOf<ConeCangJieType>()
        result += resolveDirectExtendSupertypes(type)

        if (!type.shouldInheritExtendsFromSuperclassChain()) {
            return result.toList()
        }

        for (supertype in resolveDeclaredDirectSupertypes(type)) {
            if (!supertype.shouldParticipateInSuperclassExtendPropagation()) continue
            result += collectEffectiveExtendSupertypes(supertype, visited)
        }

        return result.toList()
    }

    private fun resolveDirectExtendSupertypes(type: ConeCangJieType): List<ConeCangJieType> {
        val extends = resolveExtends(type)
        if (extends.isEmpty()) return emptyList()

        return extends.flatMap { extend ->
            instantiateExtendSupertypes(extend, type)
        }.distinct()
    }

    private fun instantiateExtendSupertypes(
        extend: CfirExtend,
        concreteType: ConeCangJieType,
    ): List<ConeCangJieType> {
        val targetPattern = resolveExtendTypeRef(extend, extend.extendedTypeRef) ?: return emptyList()
        val substitutor = createExtendDeclarationSubstitution(
            session = session,
            extend = extend,
            targetPattern = targetPattern,
            concreteReceiverType = concreteType,
        )?.substitutor ?: return emptyList()
        return extend.superTypeRefs.mapNotNull { superTypeRef ->
            val coneType = resolveExtendTypeRef(extend, superTypeRef) ?: return@mapNotNull null
            substitutor.substituteOrSelf(coneType)
        }
    }

    /**
     * LL source session 可能在 EXTENSIONS 阶段原地替换 extend typeRef 之前查询类型感知父类型。
     *
     * provider 层消费的仍然是同一份 extend 语义，因此未解析的 extend typeRef 要通过当前
     * session 的 typeResolver，并以 extend 声明作为 top container 解析。
     */
    private fun resolveExtendTypeRef(
        extend: CfirExtend,
        typeRef: CfirTypeRef,
    ): ConeCangJieType? {
        if (typeRef is CfirResolvedTypeRef) return typeRef.coneType

        return session.typeResolver.resolveType(
            typeRef = typeRef,
            configuration = CfirTypeResolutionConfiguration.EMPTY
                .withTopContainer(extend)
                .withAdditionalTypeParameters(extend.typeParameters),
            areBareTypesAllowed = false,
            isOperandOfIsOperator = false,
            resolveDeprecations = false,
            supertypeSupplier = SupertypeSupplier.Default,
        ).type
    }

    private fun resolveExtends(type: ConeCangJieType): List<CfirExtend> {
        return when (type) {
            is ConeIdealLiteralType -> type.idealExtendLookupTypes
                .flatMap { extendProvider.getExtendsForBuiltinType(it.kind) }
                .distinct()
            is ConePrimitiveType -> extendProvider.getExtendsForBuiltinType(type.kind)
            else -> {
                val targetKey = type.expandedExtendTargetKey ?: return emptyList()
                extendProvider.getExtendsForTarget(targetKey)
            }
        }
    }

    private fun resolveClassLikeDeclaration(type: ConeCangJieType): CfirClassLikeDeclaration? {
        val classId = type.classIdOrPrimitiveClassId ?: return null
        val symbol = symbolProvider.getClassLikeSymbolByClassId(classId) ?: return null
        if (!symbol.isBound) return null

        symbol.lazyResolveToPhase(CfirResolvePhase.SUPER_TYPES)
        return symbol.cfir as? CfirClassLikeDeclaration
    }

    private fun CfirClassLikeDeclaration.createDeclarationSubstitutor(type: ConeCangJieType): CfirTypeSubstitutorByMap? {
        if (type !is ConeLookupTagBasedType) return null
        if (typeParameters.isEmpty()) return null
        if (typeParameters.size != type.typeArguments.size) return null

        val replacements: Map<TypeConstructorMarker, ConeCangJieType> =
            typeParameters.zip(type.typeArguments).associate { (typeParameter, argument) ->
                typeParameter.symbol.toLookupTag() to argument.type
        }
        return CfirTypeSubstitutorByMap(replacements)
    }

    private fun ConeCangJieType.shouldInheritExtendsFromSuperclassChain(): Boolean {
        return when (this) {
            is ConeClassLikeType -> !isInterface
            is ConeStructType, is ConeEnumType -> true
            else -> false
        }
    }

    private fun ConeCangJieType.shouldParticipateInSuperclassExtendPropagation(): Boolean {
        val classId = classIdOrPrimitiveClassId ?: return false
        val symbol = symbolProvider.getClassLikeSymbolByClassId(classId) ?: return false
        return symbol !is CfirInterfaceSymbol
    }

    private fun ConeCangJieType.isSupportedClassifierType(): Boolean {
        return this is ConeClassLikeType ||
                this is ConeStructType ||
                this is ConeEnumType ||
                this is ConePrimitiveType ||
                this is ConePointerType ||
                this is ConeCStringType
    }

    private val symbolProvider
        get() = session.symbolProvider

    private val extendProvider: CfirExtendProvider
        get() = session.extendProvider
}
