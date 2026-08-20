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

import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.resolve.CfirTypeResolutionConfiguration
import org.cangnova.cangjie.cfir.resolve.SupertypeSupplier
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.resolve.providers.CfirExtendProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirInstantiatedSupertypeDescriptor
import org.cangnova.cangjie.cfir.resolve.providers.CfirInstantiatedSupertypeOrigin
import org.cangnova.cangjie.cfir.resolve.providers.CfirTypeAwareSupertypeProvider
import org.cangnova.cangjie.cfir.resolve.providers.classifyDeclaredSupertype
import org.cangnova.cangjie.cfir.resolve.providers.createExtendDeclarationSubstitution
import org.cangnova.cangjie.cfir.resolve.providers.getDeclarationPackage
import org.cangnova.cangjie.cfir.resolve.providers.ordinarySupertypeTypeOrNull
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.extendIndexStoreOrNull
import org.cangnova.cangjie.cfir.session.extendProvider
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.session.typeResolver
import org.cangnova.cangjie.cfir.symbols.CfirInterfaceSymbol
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.symbols.toLookupTag
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
    /**
     * 当前解析会话。
     */
    private val session: CfirSession,
) : CfirTypeAwareSupertypeProvider {
    /**
     * 具体类型到来源保留父边列表的缓存。
     *
     * 类型系统的去重父类型视图由该缓存投影得到，禁止反向用去重类型重建成员来源。
     */
    private val directSupertypeDescriptorsCache =
        mutableMapOf<ConeCangJieType, List<CfirInstantiatedSupertypeDescriptor>>()

    /**
     * 缓存对应的 extend 索引版本。
     *
     * 类型系统可能在 EXTENSIONS 阶段之前查询父类型；这时 extend 索引尚未完成，
     * 不能把空结果带到后续 BODY_RESOLVE。索引 rebuild 后必须整体失效。
     */
    private var cachedExtendIndexVersion: Long = Long.MIN_VALUE

    /**
     * 当前调用栈上正在计算直接父类型的类型集合。
     *
     * 父类型计算本身是可重入的：官方 `TypeManager::GetAllExtendInterfaceTyHelper`
     * 在收集 extend 接口之前先用 `CheckGenericDeclInstantiation` 求值 extend 的 where 约束，
     * 后者再调用 `IsSubtype`，而子类型判定又会回来查询父类型。递归只发生在同一调用栈内，
     * 因此用线程封闭状态记录“计算中”标记，既能覆盖所有调用入口，
     * 又不改变 [directSupertypeDescriptorsCache] 既有的 `synchronized` 语义。
     */
    private val computingSupertypes: ThreadLocal<MutableSet<ConeCangJieType>> =
        ThreadLocal.withInitial { linkedSetOf() }

    /**
     * 获取具体类型的直接父类型列表。
     *
     * 已完成计算的类型仍然优先返回缓存结果；只有在对“计算中”的类型重入时，
     * 才按最小不动点返回空父类型列表，即“该 extend 在这条回边上不贡献自己的接口”。
     * 这与官方 `TypeManager::IsSubtype` 先把 `(leaf, root, ...)` 以 `false`
     * 预置进 `subtypeCache`、再用真实结果覆盖的做法语义一致。
     */
    override fun getDirectSupertypeDescriptors(
        type: ConeCangJieType,
    ): List<CfirInstantiatedSupertypeDescriptor> {
        ensureCacheFresh()
        synchronized(directSupertypeDescriptorsCache) {
            directSupertypeDescriptorsCache[type]?.let { return it }
        }

        val semanticType = type.fullyExpandedType(session)
        val computingTypes = computingSupertypes.get()
        if (semanticType.isSupertypeComputationReentry(computingTypes)) return emptyList()

        computingTypes += semanticType
        val computed = try {
            computeDirectSupertypeDescriptors(semanticType)
        } finally {
            computingTypes -= semanticType
        }

        synchronized(directSupertypeDescriptorsCache) {
            return directSupertypeDescriptorsCache.getOrPut(type) { computed }
        }
    }

    /**
     * 判断当前类型是否构成对进行中父类型计算的递归回边。
     *
     * 判定分两级，命中任意一级都视为重入，从而在该回边上取最小不动点（空父类型列表）：
     *
     * 1. 精确回边：结构完全相同的类型再次进入。`class A<X>` 配合
     *    `extend<V> A<V> <: I where V <: I` 与 `T <: A<T>` 就是这种形状：
     *    计算 `A<T>` 的父类型要求 `T <: I`，而 `T` 的唯一上界又是 `A<T>`，
     *    于是重新查询 `A<T>`。官方编译器在这里读到的正是预置的 `false`。
     * 2. 膨胀回边：类型构造器相同，且新查询把某个进行中的类型整体包含为真子项。
     *    这类回边每层都生成结构更大的类型，精确相等永远不会再次成立，
     *    是唯一可能让“计算中”键空间无界增长的形状，必须一并截断。
     *    截断不会改变不动点：这条更大的查询要成立，仍必须在下一层回来判定被包含的进行中类型，
     *    那一步会命中第 1 级，同样得到“不贡献”。
     *
     * 反过来，构造器相同但结构更小或互不包含的实例化必须照常计算：官方 `subtypeCache`
     * 以完整实参类型（而非类型构造器）为键，若只按构造器截断，
     * 计算 `A<A<B>>` 期间对 `A<B>` 的查询会被误判为重入，
     * 从而把官方判定为成立的 `A<A<B>> <: I` 错误地变成不成立。
     *
     * 终止性：固定任一类型构造器，其嵌套查询链上的类型两两不等（第 1 级），
     * 且后出现的类型不包含先出现的类型（第 2 级）。链上所有类型都由当前程序中有限的
     * 源码类型表达式经代换得到，而代换引起结构增长的唯一形状就是
     * “把进行中的类型代入它自身的上界”，已被第 2 级截断，因此嵌套链必然有限。
     */
    private fun ConeCangJieType.isSupertypeComputationReentry(
        computingTypes: Set<ConeCangJieType>,
    ): Boolean {
        if (computingTypes.isEmpty()) return false
        val queriedType = this
        val classifierKey = queriedType.expandedExtendTargetKey
        return computingTypes.any { computingType ->
            computingType == queriedType ||
                    classifierKey != null &&
                    computingType.expandedExtendTargetKey == classifierKey &&
                    queriedType.contains { nestedType -> nestedType == computingType }
        }
    }

    /**
     * 根据 session 级 extend 索引版本清理本地父类型缓存。
     */
    private fun ensureCacheFresh() {
        val currentVersion = session.extendIndexStoreOrNull?.modificationCount ?: Long.MIN_VALUE
        synchronized(directSupertypeDescriptorsCache) {
            if (cachedExtendIndexVersion == currentVersion) return
            directSupertypeDescriptorsCache.clear()
            cachedExtendIndexVersion = currentVersion
        }
    }

    /**
     * 计算已完全展开的语义类型的声明父类型与 extend 父类型。
     *
     * typealias 展开由 [getDirectSupertypeDescriptors] 统一完成，保证“计算中”标记与这里消费的
     * 是同一份语义类型，别名拼写不会绕开递归回边判定。
     */
    private fun computeDirectSupertypeDescriptors(
        semanticType: ConeCangJieType,
    ): List<CfirInstantiatedSupertypeDescriptor> {
        if (!semanticType.isSupportedClassifierType()) return emptyList()

        val declaredSupertypes = resolveDeclaredDirectSupertypeDescriptors(semanticType)
        val effectiveExtendSupertypes = collectEffectiveExtendSupertypeDescriptors(
            type = semanticType,
            visited = linkedSetOf(),
            propagationPath = emptyList(),
        )

        if (declaredSupertypes.isEmpty() && effectiveExtendSupertypes.isEmpty()) return emptyList()
        return buildList {
            addAll(declaredSupertypes)
            addAll(effectiveExtendSupertypes)
        }.distinctBy { descriptor -> descriptor.type to descriptor.origin }
    }

    /**
     * 解析声明头中写出的直接父类型，并代入 use-site 类型实参。
     */
    private fun resolveDeclaredDirectSupertypeDescriptors(
        type: ConeCangJieType,
    ): List<CfirInstantiatedSupertypeDescriptor> {
        val declaration = resolveClassLikeDeclaration(type) ?: return emptyList()
        val substitutor = declaration.createDeclarationSubstitutor(type)

        val declaredSupertypes = declaration.superTypeRefs.mapNotNull { superTypeRef ->
            val classification = superTypeRef.classifyDeclaredSupertype(session)
            val semanticType = classification.ordinarySupertypeTypeOrNull() ?: return@mapNotNull null
            CfirInstantiatedSupertypeDescriptor(
                type = substitutor?.substituteOrSelf(semanticType) ?: semanticType,
                origin = CfirInstantiatedSupertypeOrigin.Declared(superTypeRef),
            )
        }
        return declaredSupertypes.withImplicitObjectSuperclass(declaration)
    }

    /**
     * 官方 `PreCheck::AddSuperClassObjectForClassDecl` 会在 class 没有显式父类时补
     * `std.core.Object`。这里保持同一语义在类型系统的统一父类型入口中生效：
     * 显式接口不占用 class superclass 槽位，只有显式 concrete superclass 才阻止补父类。
     */
    private fun List<CfirInstantiatedSupertypeDescriptor>.withImplicitObjectSuperclass(
        declaration: CfirClassLikeDeclaration,
    ): List<CfirInstantiatedSupertypeDescriptor> {
        if (declaration !is CfirClass) return this
        if (declaration.symbol.classId == StdlibClassIds.Any) return this
        if (any { it.type.isConcreteSuperclassCandidate() }) return this

        val implicitSuperclass = if (declaration.symbol.classId == StdlibClassIds.Object) {
            ConeClassLikeType(StdlibClassIds.Any.toLookupTag(), isInterface = true)
        } else {
            ConeClassLikeType(StdlibClassIds.Object.toLookupTag())
        }
        return this + CfirInstantiatedSupertypeDescriptor(
            type = implicitSuperclass,
            origin = CfirInstantiatedSupertypeOrigin.ImplicitObject(declaration.symbol.classId),
        )
    }

    /**
     * 判断类型是否可占用 class 的 concrete superclass 槽位。
     */
    private fun ConeCangJieType.isConcreteSuperclassCandidate(): Boolean = when (this) {
        is ConeClassLikeType -> !isInterface
        is ConeStructType, is ConeEnumType -> true
        else -> false
    }

    /**
     * 递归收集当前类型及其 superclass 链上所有可继承的 extend 接口。
     *
     * 这里只传播 extend 接口，不展开 superclass 自身的 declared supertype，
     * 避免把“普通名义继承”与“extend 注入接口”这两层语义混在一起。
     */
    private fun collectEffectiveExtendSupertypeDescriptors(
        type: ConeCangJieType,
        visited: MutableSet<ConeCangJieType>,
        propagationPath: List<CfirTypeRef>,
    ): List<CfirInstantiatedSupertypeDescriptor> {
        if (!visited.add(type)) return emptyList()

        val result = mutableListOf<CfirInstantiatedSupertypeDescriptor>()
        result += resolveDirectExtendSupertypeDescriptors(type, propagationPath)

        if (!type.shouldInheritExtendsFromSuperclassChain()) {
            return result
        }

        for (supertype in resolveDeclaredDirectSupertypeDescriptors(type)) {
            if (!supertype.type.shouldParticipateInSuperclassExtendPropagation()) continue
            val nextPropagationPath = when (val origin = supertype.origin) {
                is CfirInstantiatedSupertypeOrigin.Declared -> propagationPath + origin.sourceTypeRef
                is CfirInstantiatedSupertypeOrigin.ImplicitObject -> propagationPath
                is CfirInstantiatedSupertypeOrigin.Extend -> error(
                    "Declared-supertype traversal produced an extend edge: $supertype",
                )
            }
            result += collectEffectiveExtendSupertypeDescriptors(
                type = supertype.type,
                visited = visited,
                propagationPath = nextPropagationPath,
            )
        }

        return result
    }

    /**
     * 解析当前类型直接匹配到的 extend 父类型。
     */
    private fun resolveDirectExtendSupertypeDescriptors(
        type: ConeCangJieType,
        propagationPath: List<CfirTypeRef>,
    ): List<CfirInstantiatedSupertypeDescriptor> {
        val extends = resolveExtends(type)
        if (extends.isEmpty()) return emptyList()

        return extends.flatMap { extend ->
            instantiateExtendSupertypeDescriptors(extend, type, propagationPath)
        }
    }

    /**
     * 将 extend 声明中的父类型按具体接收者类型实例化。
     */
    private fun instantiateExtendSupertypeDescriptors(
        extend: CfirExtend,
        concreteType: ConeCangJieType,
        propagationPath: List<CfirTypeRef>,
    ): List<CfirInstantiatedSupertypeDescriptor> {
        val targetPattern = resolveExtendTypeRef(extend, extend.extendedTypeRef) ?: return emptyList()
        val substitutor = createExtendDeclarationSubstitution(
            session = session,
            extend = extend,
            targetPattern = targetPattern,
            concreteReceiverType = concreteType,
        )?.substitutor ?: return emptyList()
        return extend.superTypeRefs.mapNotNull { superTypeRef ->
            val coneType = resolveExtendTypeRef(extend, superTypeRef) ?: return@mapNotNull null
            CfirInstantiatedSupertypeDescriptor(
                type = substitutor.substituteOrSelf(coneType),
                origin = CfirInstantiatedSupertypeOrigin.Extend(
                    sourceExtend = extend,
                    declarationPackage = requireNotNull(extend.getDeclarationPackage()) {
                        "Extend declaration package is not indexed: ${extend.symbol}"
                    },
                    sourceTypeRef = superTypeRef,
                    propagationPath = propagationPath,
                ),
            )
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

    /**
     * 查询当前类型可匹配的 extend 声明。
     */
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

    /**
     * 解析类型对应的 class-like 声明。
     */
    private fun resolveClassLikeDeclaration(type: ConeCangJieType): CfirClassLikeDeclaration? {
        val classId = type.classIdOrPrimitiveClassId ?: return null
        val symbol = symbolProvider.getClassLikeSymbolByClassId(classId) ?: return null
        if (!symbol.isBound) return null

        symbol.lazyResolveToPhase(CfirResolvePhase.SUPER_TYPES)
        return symbol.cfir as? CfirClassLikeDeclaration
    }

    /**
     * 为 class-like 声明类型参数创建 use-site 替换器。
     */
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

    /**
     * 判断当前类型是否应从 superclass 链继承 extend 接口。
     */
    private fun ConeCangJieType.shouldInheritExtendsFromSuperclassChain(): Boolean {
        return when (this) {

            is ConeClassLikeType -> !isInterface
            is ConeStructType, is ConeEnumType -> true
            else -> false
        }
    }

    /**
     * 判断父类型是否继续参与 superclass extend 传播。
     */
    private fun ConeCangJieType.shouldParticipateInSuperclassExtendPropagation(): Boolean {
        val classId = classIdOrPrimitiveClassId ?: return false
        val symbol = symbolProvider.getClassLikeSymbolByClassId(classId) ?: return false
        return symbol !is CfirInterfaceSymbol
    }

    /**
     * 判断类型是否属于该 provider 支持的分类器类型集合。
     */
    private fun ConeCangJieType.isSupportedClassifierType(): Boolean {
        return this is ConeClassLikeType ||
                this is ConeStructType ||
                this is ConeEnumType ||
                this is ConePrimitiveType ||
                this is ConePointerType ||
                this is ConeCStringType
    }

    /**
     * 当前会话的符号 provider。
     */
    private val symbolProvider
        get() = session.symbolProvider

    /**
     * 当前会话的 extend provider。
     */
    private val extendProvider: CfirExtendProvider
        get() = session.extendProvider
}
