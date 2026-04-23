package org.cangnova.cangjie.cfir.resolve.services

import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.resolve.providers.CfirExtendProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirTypeAwareSupertypeProvider
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.extendProvider
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.CfirInterfaceSymbol
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeLookupTagBasedType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeStructType
import org.cangnova.cangjie.cfir.types.CfirTypeSubstitutorByMap
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId
import org.cangnova.cangjie.cfir.types.type
import org.cangnova.cangjie.cfir.types.type

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
        if (!type.isSupportedClassifierType()) return emptyList()

        val declaredSupertypes = resolveDeclaredDirectSupertypes(type)
        val effectiveExtendSupertypes = collectEffectiveExtendSupertypes(type, linkedSetOf())

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
        val targetPattern = (extend.extendedTypeRef as? CfirResolvedTypeRef)?.coneType ?: return emptyList()
        val substitutions = linkedMapOf<String, ConeCangJieType>()
        val extendTypeParameterNames = extend.typeParameters.mapTo(linkedSetOf()) { it.name.asString() }

        if (!matchExtendTargetType(targetPattern, concreteType, extendTypeParameterNames, substitutions)) {
            return emptyList()
        }
        if (extendTypeParameterNames.any { it !in substitutions }) {
            return emptyList()
        }

        val substitutor = substitutions.takeIf { it.isNotEmpty() }?.let(::CfirTypeSubstitutorByMap)
        return extend.superTypeRefs.mapNotNull { superTypeRef ->
            val coneType = (superTypeRef as? CfirResolvedTypeRef)?.coneType ?: return@mapNotNull null
            substitutor?.substituteOrSelf(coneType) ?: coneType
        }
    }

    /**
     * 将 `extend TargetPattern` 与当前 concrete type 进行结构匹配，
     * 推导 extend 自身类型参数在当前 use-site 下应当被替换成什么具体类型。
     */
    private fun matchExtendTargetType(
        pattern: ConeCangJieType,
        actual: ConeCangJieType,
        extendTypeParameterNames: Set<String>,
        substitutions: MutableMap<String, ConeCangJieType>,
    ): Boolean {
        return when (pattern) {
            is ConeTypeParameterType -> {
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

    private fun resolveExtends(type: ConeCangJieType): List<CfirExtend> {
        return when (type) {
            is ConePrimitiveType -> extendProvider.getExtendsForBuiltinType(type.kind)
            else -> {
                val classId = type.classIdOrPrimitiveClassId ?: return emptyList()
                extendProvider.getExtendsForClass(classId)
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

        val replacements: Map<String, ConeCangJieType> = typeParameters.zip(type.typeArguments).associate { (typeParameter, argument) ->
            typeParameter.symbol.name.asString() to argument.type
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
                this is ConePrimitiveType
    }

    private val symbolProvider
        get() = session.symbolProvider

    private val extendProvider: CfirExtendProvider
        get() = session.extendProvider
}
