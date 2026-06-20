package org.cangnova.cangjie.cfir.types

import org.cangnova.cangjie.builtins.StandardNames
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name

val PrimitiveTypeKind.classId: ClassId
    get() = ClassId(StandardNames.BASIC_PACKAGE_FQ_NAME, Name.identifier(typeName))

fun ClassId.toPrimitiveTypeKindOrNull(): PrimitiveTypeKind? {
    if (packageFqName != StandardNames.BASIC_PACKAGE_FQ_NAME) return null
    return PrimitiveTypeKind.entries.firstOrNull { it.typeName == shortClassName.asString() }
}

val PrimitiveTypeKind.isExposedBuiltinClassifier: Boolean
    get() = this != PrimitiveTypeKind.IDEAL_INT && this != PrimitiveTypeKind.IDEAL_FLOAT

/**
 * Ideal primitive 在 extend 成员查找中的真实候选类型集合。
 *
 * 对齐官方 `TypeCheckUtil::GetIdealTypesByKind`：IdealInt / IdealFloat 本身不是可扩展的
 * 暴露类型，成员访问需要在所有可落地的具体 primitive 类型上查找 extend。
 */
val PrimitiveTypeKind.idealExtendLookupKinds: List<PrimitiveTypeKind>
    get() = when (this) {
        PrimitiveTypeKind.IDEAL_INT -> listOf(
            PrimitiveTypeKind.INT8,
            PrimitiveTypeKind.INT16,
            PrimitiveTypeKind.INT32,
            PrimitiveTypeKind.INT_NATIVE,
            PrimitiveTypeKind.INT64,
            PrimitiveTypeKind.UINT8,
            PrimitiveTypeKind.UINT16,
            PrimitiveTypeKind.UINT32,
            PrimitiveTypeKind.UINT64,
            PrimitiveTypeKind.UINT_NATIVE,
        )
        PrimitiveTypeKind.IDEAL_FLOAT -> listOf(
            PrimitiveTypeKind.FLOAT16,
            PrimitiveTypeKind.FLOAT32,
            PrimitiveTypeKind.FLOAT64,
        )
        else -> emptyList()
    }

val PrimitiveTypeKind.extendLookupKinds: List<PrimitiveTypeKind>
    get() = idealExtendLookupKinds.ifEmpty { listOf(this) }

val ConeCangJieType.idealExtendLookupTypes: List<ConePrimitiveType>
    get() = when (this) {
        is ConeIdealIntLiteralType -> PrimitiveTypeKind.IDEAL_INT.idealExtendLookupKinds.map(::ConePrimitiveType)
        is ConeIdealFloatLiteralType -> PrimitiveTypeKind.IDEAL_FLOAT.idealExtendLookupKinds.map(::ConePrimitiveType)
        is ConePrimitiveType -> kind.idealExtendLookupKinds.map(::ConePrimitiveType)
        else -> emptyList()
    }

val ConeCangJieType.classIdOrPrimitiveClassId: ClassId?
    get() = when (this) {
        is ConePrimitiveType -> kind.classId
        is ConeClassLikeType -> classId
        is ConeStructType -> classId
        is ConeEnumType -> classId
        is ConeTypeAliasType -> classId
        else -> null
    }

/**
 * extend 目标索引用的语义 ClassId。
 *
 * 仓颉官方 `Ty::IsExtendable()` 对 type alias 递归检查其展开类型，因此
 * `extend Alias <: I` 必须按 Alias 展开后的真实可扩展类型参与索引和查找。
 * 普通符号解析仍应使用 [classIdOrPrimitiveClassId] 保留别名身份。
 */
val ConeCangJieType.expandedClassIdOrPrimitiveClassId: ClassId?
    get() = when (this) {
        is ConeTypeAliasType -> expandedType?.expandedClassIdOrPrimitiveClassId ?: classId
        else -> classIdOrPrimitiveClassId
    }
