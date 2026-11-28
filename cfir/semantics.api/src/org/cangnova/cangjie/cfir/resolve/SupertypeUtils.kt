package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.declarations.isInReferenceCycle
import org.cangnova.cangjie.cfir.declarations.util.expandedConeType
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.resolve.substitution.chain
import org.cangnova.cangjie.cfir.resolve.substitution.substitutorByMap
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.resolve.providers.symbolProvider
import org.cangnova.cangjie.cfir.session.directSupertypeProviderOrNull
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassifierSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeAliasSymbol
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.types.*

/**
 * 共享父类型查询的来源接口。
 *
 * class、struct、enum 与 interface 共用 class-like 声明；查询保留各自实际 cone 类型，
 * typealias 的展开仍由其声明所属的正常 SUPER_TYPES 产品提供。
 */
abstract class SupertypeSupplier {
    abstract fun forClass(firClass: CfirClassLikeDeclaration, useSiteSession: CfirSession): List<ConeCangJieType>
    abstract fun expansionForTypeAlias(typeAlias: CfirTypeAlias, useSiteSession: CfirSession): ConeCangJieType?

    object Default : SupertypeSupplier() {
        override fun forClass(firClass: CfirClassLikeDeclaration, useSiteSession: CfirSession): List<ConeCangJieType> {
            // 仓颉 class/struct/interface/enum 只能定义在包顶层，均由正常 SUPER_TYPES 产品提供父类型。
            firClass.lazyResolveToPhase(CfirResolvePhase.SUPER_TYPES)
            return firClass.superTypeRefs.mapNotNull { it.coneTypeOrNull }
        }

        override fun expansionForTypeAlias(typeAlias: CfirTypeAlias, useSiteSession: CfirSession): ConeCangJieType? {
            return typeAlias.expandedConeTypeWithEnsuredPhase()
        }
    }
}

/** 普通继承查询只排除真正的循环目标，诊断承载节点和当前声明的自身成员保持各自语义。 */
fun ConeCangJieType.isReferenceCycleTarget(useSiteSession: CfirSession): Boolean =
    isReferenceCycleTarget(useSiteSession, useSiteSession.symbolProvider)

/** 显式来源查询保留自己的 classifier 归属，不能从默认 symbol provider 读取另一声明的循环位。 */
fun ConeCangJieType.isReferenceCycleTarget(useSiteSession: CfirSession, symbolProvider: CfirSymbolProvider): Boolean {
    val symbol = if (this is ConeLookupTagBasedType) lookupTag.toSymbol(useSiteSession, symbolProvider) as? CfirClassLikeSymbol<*>
        else classIdOrPrimitiveClassId?.let(symbolProvider::getClassLikeSymbolByClassId)
    symbol ?: return false
    if (!symbol.cfir.origin.fromSource) return symbol.cfir.isInReferenceCycle
    val graph = checkNotNull(symbol.cfir.moduleData.session.directSupertypeProviderOrNull) {
        "Source cycle membership requires its original publication provider"
    }
    // 确定的 marker-only 循环无需解析其尚未完成的其它引用。
    graph.getPublishedReferenceCycleState(symbol)?.let { return it }
    symbol.lazyResolveToPhase(CfirResolvePhase.SUPER_TYPES)
    return checkNotNull(checkNotNull(symbol.cfir.moduleData.session.directSupertypeProviderOrNull).getPublishedReferenceCycleState(symbol)) {
        "Source cycle membership has not been published"
    }
}

/** 按真实 classifier 身份收集类型对应的名义声明；结构类型的组成类型独立递归。 */
fun collectSymbolsForType(type: ConeCangJieType, useSiteSession: CfirSession): List<CfirClassLikeSymbol<*>> {
    val lookupTags = linkedSetOf<ConeClassLikeLookupTag>()
    val visitedParameters = hashSetOf<org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol>()
    fun collect(type: ConeCangJieType) {
        when (val expanded = type.fullyExpandedType(useSiteSession)) {
            is ConeClassifierType -> lookupTags += expanded.lookupTag
            is ConeIntersectionType -> expanded.intersectedTypes.forEach(::collect)
            is ConeUnionType -> expanded.unionTypes.forEach(::collect)
            is ConeTypeParameterType -> {
                val parameter = expanded.lookupTag.typeParameterSymbol
                if (visitedParameters.add(parameter)) parameter.resolvedBounds.forEach { it.coneTypeOrNull?.let(::collect) }
            }
            else -> Unit
        }
    }
    collect(type)
    return lookupTags.mapNotNull { it.toClassLikeSymbol(useSiteSession) }
}

/** 所有消费方共用相同的 alias 展开、接口策略、替换链与父边访问协议。 */
fun lookupSuperTypes(
    symbols: List<CfirClassLikeSymbol<*>>,
    lookupInterfaces: Boolean,
    deep: Boolean,
    useSiteSession: CfirSession,
    substituteTypes: Boolean,
    supertypeSupplier: SupertypeSupplier = SupertypeSupplier.Default,
    visitedSymbols: MutableSet<CfirClassifierSymbol<*>> = mutableSetOf(),
): List<ConeCangJieType> = buildList {
    for (symbol in symbols) {
        symbol.collectSuperTypes(this, visitedSymbols, deep, lookupInterfaces, substituteTypes, useSiteSession, supertypeSupplier)
    }
}

fun lookupSuperTypes(
    klass: CfirClassLikeDeclaration,
    lookupInterfaces: Boolean,
    deep: Boolean,
    useSiteSession: CfirSession,
    substituteTypes: Boolean,
    supertypeSupplier: SupertypeSupplier = SupertypeSupplier.Default,
): List<ConeCangJieType> = buildList {
    klass.symbol.collectSuperTypes(this, mutableSetOf(), deep, lookupInterfaces, substituteTypes, useSiteSession, supertypeSupplier)
}

fun lookupSuperTypes(
    symbol: CfirClassLikeSymbol<*>,
    lookupInterfaces: Boolean,
    deep: Boolean,
    useSiteSession: CfirSession,
): List<ConeCangJieType> = buildList {
    symbol.collectSuperTypes(this, mutableSetOf(), deep, lookupInterfaces, false, useSiteSession, SupertypeSupplier.Default)
}

/** 继承判据比较 lookup tag；是否代入父类型实参由共同查询入口统一决定。 */
fun CfirClassLikeSymbol<*>.isSubclassOf(
    ownerLookupTag: ConeClassLikeLookupTag,
    session: CfirSession,
    isStrict: Boolean,
    lookupInterfaces: Boolean,
): Boolean {
    lazyResolveToPhase(CfirResolvePhase.SUPER_TYPES)
    return cfir.isSubclassOf(ownerLookupTag, session, isStrict, SupertypeSupplier.Default, lookupInterfaces)
}

fun CfirClassLikeDeclaration.isSubclassOf(
    ownerLookupTag: ConeClassLikeLookupTag,
    session: CfirSession,
    isStrict: Boolean,
    supertypeSupplier: SupertypeSupplier = SupertypeSupplier.Default,
    lookupInterfaces: Boolean = true,
): Boolean {
    if (symbol.toLookupTag() == ownerLookupTag) return !isStrict
    return lookupSuperTypes(this, lookupInterfaces, true, session, false, supertypeSupplier)
        .any { (it as? ConeClassifierType)?.lookupTag == ownerLookupTag }
}

/** 使用同一 supplier 展开当前父边的 alias，结构类型与错误类型保留真实表示。 */
fun ConeCangJieType.computePartialExpansion(useSiteSession: CfirSession, supertypeSupplier: SupertypeSupplier): ConeCangJieType =
    fullyExpandedType(useSiteSession) { supertypeSupplier.expansionForTypeAlias(it, useSiteSession) }

private fun CfirClassLikeSymbol<*>.collectSuperTypes(
    list: MutableList<ConeCangJieType>,
    visitedSymbols: MutableSet<CfirClassifierSymbol<*>>,
    deep: Boolean,
    lookupInterfaces: Boolean,
    substituteSuperTypes: Boolean,
    useSiteSession: CfirSession,
    supertypeSupplier: SupertypeSupplier,
): Unit = forEachSupertype(
    visitedSymbols, deep, lookupInterfaces, substituteSuperTypes, useSiteSession, supertypeSupplier,
    onType = { list += it },
)

private inline fun CfirClassLikeSymbol<*>.forEachSupertype(
    visitedSymbols: MutableSet<CfirClassifierSymbol<*>>,
    deep: Boolean,
    lookupInterfaces: Boolean,
    substituteSuperTypes: Boolean,
    useSiteSession: CfirSession,
    supertypeSupplier: SupertypeSupplier,
    onType: (ConeCangJieType) -> Unit,
): Unit = forEachSupertypeWithInheritor(
    deep, lookupInterfaces, substituteSuperTypes, useSiteSession, supertypeSupplier, visitedSymbols,
    onSupertypeAndInheritor = { type, _ -> onType(type) },
)

/** 父边同时保存其实际 inheritor；去重只作用于声明身份，不按 ClassId 合并继承路径。 */
inline fun CfirClassLikeSymbol<*>.forEachSupertypeWithInheritor(
    deep: Boolean,
    lookupInterfaces: Boolean,
    substituteSuperTypes: Boolean,
    useSiteSession: CfirSession,
    supertypeSupplier: SupertypeSupplier,
    visitedSymbols: MutableSet<CfirClassifierSymbol<*>> = mutableSetOf(),
    onSupertypeAndInheritor: (ConeCangJieType, CfirClassLikeSymbol<*>) -> Unit,
) {
    val substitutor: ConeSubstitutor = ConeSubstitutor.Empty
    (this to substitutor).traverseDepthFirstWithoutDuplicates(
        getSubsequent = act@{ (next, currentSubstitutor) ->
            if (next is CfirTypeAliasSymbol) {
                val expansion = supertypeSupplier.expansionForTypeAlias(next.cfir, useSiteSession)
                    ?.computePartialExpansion(useSiteSession, supertypeSupplier)
                    ?: return@act emptyList()
                return@act listOf(expansion to currentSubstitutor)
            }
            val superClassTypes = supertypeSupplier.forClass(next.cfir, useSiteSession).mapNotNull { type ->
                type.computePartialExpansion(useSiteSession, supertypeSupplier)
                    .takeIf { lookupInterfaces || it.isClassBasedType(useSiteSession) }
            }
            superClassTypes.forEach { onSupertypeAndInheritor(currentSubstitutor.substituteOrSelf(it), next) }
            if (!deep) return@act emptyList()
            superClassTypes.mapNotNull { type ->
                if (type is ConeErrorType) return@mapNotNull null
                type to if (substituteSuperTypes) createSubstitutionForSupertype(type, useSiteSession).chain(currentSubstitutor)
                else currentSubstitutor
            }
        },
        toStackElement = { _, (type, currentSubstitutor) -> type.toClassLikeSymbol(useSiteSession)?.let { it to currentSubstitutor } },
        visit = { (symbol, _) -> visitedSymbols.add(symbol) },
    )
}

/** 与共享父类型查询共用的 DFS；只访问 visit 接受的真实身份。 */
inline fun <T, K> T.traverseDepthFirstWithoutDuplicates(
    getSubsequent: (T) -> List<K>,
    toStackElement: (T, K) -> T?,
    visit: (T) -> Boolean,
) {
    val stack = mutableListOf(this)
    while (stack.isNotEmpty()) {
        val current = stack.removeAt(stack.lastIndex)
        if (visit(current)) {
            getSubsequent(current).forEach { next -> toStackElement(current, next)?.let(stack::add) }
        }
    }
}

fun ConeCangJieType?.isClassBasedType(useSiteSession: CfirSession): Boolean =
    this !is ConeErrorType && this?.toClassLikeSymbol(useSiteSession)?.cfir is CfirClass

/** 仓颉泛型实参是 invariant 类型；不存在 Kotlin variance/capture/projection 的额外转换。 */
fun createSubstitutionForSupertype(superType: ConeCangJieType, session: CfirSession): ConeSubstitutor {
    val klass = superType.toClassLikeSymbol(session)?.cfir ?: return ConeSubstitutor.Empty
    return substitutorByMap(klass.typeParameters.map { it.symbol }.zip(superType.typeArguments.map { it.type }).toMap(), session)
}

fun CfirClassLikeSymbol<*>.getSuperTypes(
    useSiteSession: CfirSession,
    recursive: Boolean = true,
    lookupInterfaces: Boolean = true,
    substituteSuperTypes: Boolean = true,
    supertypeSupplier: SupertypeSupplier = SupertypeSupplier.Default,
): List<ConeCangJieType> = buildList {
    collectSuperTypes(this, mutableSetOf(), recursive, lookupInterfaces, substituteSuperTypes, useSiteSession, supertypeSupplier)
}
