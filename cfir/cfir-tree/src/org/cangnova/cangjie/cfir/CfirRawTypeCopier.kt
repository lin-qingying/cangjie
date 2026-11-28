package org.cangnova.cangjie.cfir

import java.util.IdentityHashMap
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.utils.WeakPair

/**
 * Cone 只是 metadata；其中的 classifier/type-parameter 句柄仍须使用同一个 symbol map。
 * 类型壳独立构造，class-like tag 的旧 session 弱缓存不被复制。
 */
internal class CfirRawTypeCopier(
    private val state: CfirRawCopyState,
    private val copyDiagnostic: (ConeDiagnostic) -> ConeDiagnostic,
    private val borrowInferenceConstructor: (ConeTypeVariableTypeConstructor) -> ConeTypeVariableTypeConstructor,
) {
    private val copies = IdentityHashMap<ConeCangJieType, ConeCangJieType>()
    private val active = java.util.Collections.newSetFromMap(IdentityHashMap<ConeCangJieType, Boolean>())
    private val classifierSymbols = IdentityHashMap<CfirClassLikeSymbol<*>, Pair<CfirSession, CfirClassLikeSymbol<*>>>()
    private val classifierTags = IdentityHashMap<ConeClassLikeLookupTag, Pair<CfirSession, CfirClassLikeSymbol<*>>>()

    fun registerClassifier(source: CfirClassLikeSymbol<*>, target: CfirClassLikeSymbol<*>, session: CfirSession) {
        val binding = session to target
        check(classifierSymbols.put(source, binding) == null)
        classifierTags[source.toLookupTag()] = binding
        val canonicalTargetTag = target.toLookupTag() as? ConeClassLikeLookupTagImpl
            ?: error("Fresh classifier has no canonical class-like tag")
        canonicalTargetTag.boundSymbol = WeakPair(session, target)
    }

    fun lookupTag(value: ConeClassLikeLookupTag): ConeClassLikeLookupTag {
        return when (value) {
            is ConeClassLikeLookupTagImpl -> ConeClassLikeLookupTagImpl(value.classId).also { target ->
                val cachedBinding = value.boundSymbol
                val cached = cachedBinding?.second
                val useSiteSession = cachedBinding?.first
                val exact = classifierTags[value] ?: cached?.let(classifierSymbols::get)
                val binding = when {
                    exact != null -> exact
                    cached != null && useSiteSession != null -> {
                        val borrowed = state.referencedSymbol(cached)
                        useSiteSession to borrowed
                    }
                    // 无精确 tag/symbol 身份时保持未绑定，由正常 resolver 在 use-site session 查找。
                    else -> null
                }
                binding?.let { (session, symbol) -> target.boundSymbol = WeakPair(session, symbol) }
            }
            is ConeClassLikeErrorLookupTag -> ConeClassLikeErrorLookupTag(
                value.classId, copyDiagnostic(value.diagnostic), value.delegatedType?.let { copy(it) },
            )
            else -> error("Class-like lookup tag has no RAW copy policy")
        }
    }

    /** 扫描不推进 symbol、不读取 type-parameter bounds；其 owning 声明由 generated scanner 管理。 */
    fun collect(value: ConeCangJieType) {
        val seen = java.util.Collections.newSetFromMap(IdentityHashMap<ConeCangJieType, Boolean>())
        fun inspect(type: ConeCangJieType) {
            if (!seen.add(type)) return
            when (type) {
                is ConePrimitiveType, is ConeCStringType, is ConeQuestType, ConeAnyType -> Unit
                is ConeTypeParameterTypeImpl -> Unit
                is ConeTypeVariableType -> borrowInferenceConstructor(type.typeConstructor)
                is ConeErrorType -> {
                    copyDiagnostic(type.diagnostic)
                    type.delegatedType?.let(::inspect)
                }
                is ConeClassLikeType, is ConeStructType, is ConeEnumType -> Unit
                is ConeTypeAliasType -> type.expandedType?.let(::inspect)
                is ConeTupleType -> type.elementTypes.forEach(::inspect)
                is ConeFunctionType -> { type.parameterTypes.forEach(::inspect); inspect(type.returnType) }
                is ConePointerType -> inspect(type.pointeeType)
                is ConeVArrayType -> inspect(type.elementType)
                is ConeUnionType -> type.unionTypes.forEach(::inspect)
                is ConeIdealIntConstantType, is ConeIdealFloatConstantType -> Unit
                else -> error("RAW cannot import stub/capture/intersection/operator-result type metadata")
            }
            type.typeArguments.forEach { inspect(it.type) }
            type.attributes.forEach { attribute ->
                when (attribute) {
                    is AbbreviatedTypeAttribute -> inspect(attribute.coneType)
                    else -> error("Cone attribute has no RAW copy policy")
                }
            }
        }
        inspect(value)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : ConeCangJieType> copy(value: T): T {
        copies[value]?.let { return it as T }
        check(active.add(value)) { "Owning cycle in RAW Cone metadata" }
        try {
            val attributes = ConeAttributes.create(value.attributes.map { attribute ->
                when (attribute) {
                    is AbbreviatedTypeAttribute -> AbbreviatedTypeAttribute(copy(attribute.coneType))
                    else -> error("Cone attribute has no RAW copy policy")
                }
            })
            val arguments = value.typeArguments.map { copy(it.type) }
            val target: ConeCangJieType = when (value) {
                is ConePrimitiveType -> ConePrimitiveType(value.kind, attributes)
                is ConeTypeParameterTypeImpl -> ConeTypeParameterTypeImpl(
                    ConeTypeParameterLookupTag(state.referencedSymbol(value.lookupTag.typeParameterSymbol)), attributes,
                )
                // active-parent 推断构造器是显式 borrowed handle；只复制引用壳，不能分配新的 T。
                is ConeTypeVariableType -> ConeTypeVariableType(borrowInferenceConstructor(value.typeConstructor), attributes)
                is ConeErrorType -> ConeErrorType(
                    copyDiagnostic(value.diagnostic), value.isUninferredParameter,
                    value.delegatedType?.let { copy(it) }, arguments, attributes,
                    ConeClassLikeErrorLookupTag(value.lookupTag.classId, copyDiagnostic(value.diagnostic), value.delegatedType?.let { copy(it) }),
                )
                is ConeClassLikeType -> ConeClassLikeType(lookupTag(value.lookupTag), arguments, attributes, value.isInterface, value.isThisType)
                is ConeStructType -> ConeStructType(lookupTag(value.lookupTag), arguments, attributes)
                is ConeEnumType -> ConeEnumType(lookupTag(value.lookupTag), arguments, attributes, value.isRefEnum)
                is ConeTypeAliasType -> ConeTypeAliasType(value.classId, value.expandedType?.let { copy(it) }, arguments, attributes)
                is ConeTupleType -> ConeTupleType(value.elementTypes.map { copy(it) }, attributes)
                is ConeFunctionType -> ConeFunctionType(
                    value.parameterTypes.map { copy(it) }, copy(value.returnType), value.isCFunc,
                    value.isClosureType, value.hasVariableLenArg, attributes,
                )
                is ConePointerType -> ConePointerType(copy(value.pointeeType), attributes)
                is ConeCStringType -> ConeCStringType(attributes)
                is ConeVArrayType -> ConeVArrayType(copy(value.elementType), value.size, attributes)
                is ConeQuestType -> ConeQuestType(attributes)
                is ConeUnionType -> ConeUnionType(value.unionTypes.map { copy(it) }.toSet(), attributes)
                is ConeIdealIntConstantType -> ConeIdealIntConstantType(value.value)
                is ConeIdealFloatConstantType -> ConeIdealFloatConstantType(value.value)
                ConeAnyType -> ConeAnyType
                else -> error("RAW cannot import stub/capture/intersection/operator-result type metadata")
            }
            check(target !== value || value === ConeAnyType) { "Mutable Cone metadata was shared" }
            copies[value] = target
            return target as T
        } finally {
            active.remove(value)
        }
    }
}
