package org.cangnova.cangjie.cfir.scopes.impl

import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.resolve.substitution.createTypeSubstitutorByTypeConstructor
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.type.model.TypeConstructorMarker

/**
 * 从继承来源 owner 与具体 owner 的同形结构中恢复类型参数替换。
 *
 * 该映射服务于泛型父边来源身份的延续。结构递归必须验证类型节点和构造器；
 * This 视图不改变 owner 的类型参数结构，交叉/联合类型则按成员集合匹配，不能依赖迭代顺序。
 */
internal fun ConeCangJieType.provenanceToConcreteSubstitutor(
    concreteType: ConeCangJieType,
    typeContext: ConeTypeContext,
): ConeSubstitutor {
    val replacements = collectProvenanceReplacements(this, concreteType, emptyMap())
    checkNotNull(replacements) {
        "Inheritance provenance owner shape differs from concrete owner: $this (${this::class.simpleName}) and " +
            "$concreteType (${concreteType::class.simpleName})"
    }
    return createTypeSubstitutorByTypeConstructor(replacements, typeContext, false)
}

private fun collectProvenanceReplacements(
    provenance: ConeCangJieType,
    concrete: ConeCangJieType,
    replacements: Map<TypeConstructorMarker, ConeCangJieType>,
): Map<TypeConstructorMarker, ConeCangJieType>? {
    if (provenance is ConeTypeParameterType) {
        val constructor = provenance.lookupTag as TypeConstructorMarker
        val previous = replacements[constructor]
        if (previous != null && previous != concrete) return null
        return replacements + (constructor to concrete)
    }

    if (!hasSameTypeShape(provenance, concrete)) return null

    val provenanceComponents = provenance.directComponentTypes()
    val concreteComponents = concrete.directComponentTypes()
    if (provenanceComponents.size != concreteComponents.size) return null

    return when (provenance) {
        is ConeIntersectionType -> collectUnorderedComponents(
            provenanceComponents,
            concreteComponents,
            replacements,
        )
        is ConeUnionType -> collectUnorderedComponents(
            provenanceComponents,
            concreteComponents,
            replacements,
        )
        else -> collectOrderedComponents(
            provenanceComponents,
            concreteComponents,
            replacements,
        )
    }
}

private fun collectOrderedComponents(
    provenanceComponents: List<ConeCangJieType>,
    concreteComponents: List<ConeCangJieType>,
    initialReplacements: Map<TypeConstructorMarker, ConeCangJieType>,
): Map<TypeConstructorMarker, ConeCangJieType>? {
    var replacements = initialReplacements
    for ((provenance, concrete) in provenanceComponents.zip(concreteComponents)) {
        replacements = collectProvenanceReplacements(provenance, concrete, replacements) ?: return null
    }
    return replacements
}

/**
 * 对集合语义的类型成员进行一致性匹配；若不同匹配会产生不同的参数替换，来源身份并不唯一。
 */
private fun collectUnorderedComponents(
    provenanceComponents: List<ConeCangJieType>,
    concreteComponents: List<ConeCangJieType>,
    initialReplacements: Map<TypeConstructorMarker, ConeCangJieType>,
): Map<TypeConstructorMarker, ConeCangJieType>? {
    var uniqueReplacements: Map<TypeConstructorMarker, ConeCangJieType>? = null
    var hasAmbiguousReplacements = false

    fun search(
        provenanceIndex: Int,
        remainingConcrete: List<ConeCangJieType>,
        replacements: Map<TypeConstructorMarker, ConeCangJieType>,
    ) {
        if (hasAmbiguousReplacements) return
        if (provenanceIndex == provenanceComponents.size) {
            val previous = uniqueReplacements
            if (previous == null) {
                uniqueReplacements = replacements
            } else if (previous != replacements) {
                hasAmbiguousReplacements = true
            }
            return
        }

        val provenance = provenanceComponents[provenanceIndex]
        for (index in remainingConcrete.indices) {
            val concrete = remainingConcrete[index]
            val candidate = collectProvenanceReplacements(provenance, concrete, replacements) ?: continue
            search(
                provenanceIndex + 1,
                remainingConcrete.filterIndexed { concreteIndex, _ -> concreteIndex != index },
                candidate,
            )
            if (hasAmbiguousReplacements) return
        }
    }

    search(0, concreteComponents, initialReplacements)
    check(!hasAmbiguousReplacements) {
        "Inheritance provenance has ambiguous unordered type-parameter mapping: $provenanceComponents and $concreteComponents"
    }
    return uniqueReplacements
}

private fun hasSameTypeShape(
    provenance: ConeCangJieType,
    concrete: ConeCangJieType,
): Boolean {
    val provenanceClassifier = provenance.nominalClassifierIdentity()
    val concreteClassifier = concrete.nominalClassifierIdentity()
    if (provenanceClassifier != null || concreteClassifier != null) {
        if (provenanceClassifier == null || concreteClassifier == null) return false
        if (provenanceClassifier.classId != concreteClassifier.classId) return false
        if (!areCompatibleNominalOwnerViews(
                provenance,
                concrete,
                provenanceClassifier,
                concreteClassifier,
            )
        ) {
            return false
        }
        return provenance.directComponentTypes().size == concrete.directComponentTypes().size
    }

    return when {
        provenance is ConeFunctionType && concrete is ConeFunctionType ->
            provenance.parameterTypes.size == concrete.parameterTypes.size &&
                provenance.isCFunc == concrete.isCFunc &&
                provenance.isClosureType == concrete.isClosureType &&
                provenance.hasVariableLenArg == concrete.hasVariableLenArg
        provenance is ConeTupleType && concrete is ConeTupleType ->
            provenance.elementTypes.size == concrete.elementTypes.size
        provenance is ConeVArrayType && concrete is ConeVArrayType -> provenance.size == concrete.size
        provenance is ConeIntersectionType && concrete is ConeIntersectionType ->
            provenance.intersectedTypes.size == concrete.intersectedTypes.size
        provenance is ConeUnionType && concrete is ConeUnionType -> provenance.unionTypes.size == concrete.unionTypes.size
        provenance is ConeErrorType && concrete is ConeErrorType -> true
        provenance is ConeErrorType || concrete is ConeErrorType -> false
        else -> provenance == concrete && provenance.directComponentTypes().isEmpty()
    }
}

/** 仅用于恢复 owner 类型参数替换的名义声明身份。 */
private data class NominalClassifierIdentity(
    val classId: ClassId,
    val form: NominalClassifierForm,
)

private enum class NominalClassifierForm {
    CLASS_LIKE,
    STRUCT,
    ENUM,
    TYPE_ALIAS,
    PRIMITIVE,
    POINTER,
    C_STRING,
}

/** 按声明身份获取名义分类器，不将 Cone 子类判等当成声明身份判定。 */
private fun ConeCangJieType.nominalClassifierIdentity(): NominalClassifierIdentity? = when (this) {
    is ConeClassLikeType -> NominalClassifierIdentity(classId, NominalClassifierForm.CLASS_LIKE)
    is ConeStructType -> NominalClassifierIdentity(classId, NominalClassifierForm.STRUCT)
    is ConeEnumType -> NominalClassifierIdentity(classId, NominalClassifierForm.ENUM)
    is ConeTypeAliasType -> NominalClassifierIdentity(classId, NominalClassifierForm.TYPE_ALIAS)
    is ConePrimitiveType -> NominalClassifierIdentity(kind.classId, NominalClassifierForm.PRIMITIVE)
    is ConePointerType -> NominalClassifierIdentity(StdlibClassIds.CPointer, NominalClassifierForm.POINTER)
    is ConeCStringType -> NominalClassifierIdentity(StdlibClassIds.CString, NominalClassifierForm.C_STRING)
    else -> null
}

/**
 * 对 owner 类型参数槽位使用声明身份配对。普通 class-like 视图可承载 struct、enum 或内建 Cone
 * 类型的同一声明实参；此判断只建立替换映射，不会改变原类型种类或类型等价关系。
 */
private fun areCompatibleNominalOwnerViews(
    provenance: ConeCangJieType,
    concrete: ConeCangJieType,
    provenanceClassifier: NominalClassifierIdentity,
    concreteClassifier: NominalClassifierIdentity,
): Boolean = when {
    provenanceClassifier.form == concreteClassifier.form -> when (provenanceClassifier.form) {
        NominalClassifierForm.CLASS_LIKE ->
            (provenance as ConeClassLikeType).isInterface == (concrete as ConeClassLikeType).isInterface
        NominalClassifierForm.ENUM ->
            (provenance as ConeEnumType).isRefEnum == (concrete as ConeEnumType).isRefEnum
        else -> true
    }
    provenanceClassifier.form == NominalClassifierForm.CLASS_LIKE &&
        concreteClassifier.form.isRepresentableByClassLikeView ->
        (provenance as ConeClassLikeType).let { !it.isInterface && !it.isThisType }
    provenanceClassifier.form.isRepresentableByClassLikeView &&
        concreteClassifier.form == NominalClassifierForm.CLASS_LIKE ->
        (concrete as ConeClassLikeType).let { !it.isInterface && !it.isThisType }
    else -> false
}

private val NominalClassifierForm.isRepresentableByClassLikeView: Boolean
    get() = this == NominalClassifierForm.STRUCT ||
        this == NominalClassifierForm.ENUM ||
        this == NominalClassifierForm.PRIMITIVE ||
        this == NominalClassifierForm.POINTER ||
        this == NominalClassifierForm.C_STRING
