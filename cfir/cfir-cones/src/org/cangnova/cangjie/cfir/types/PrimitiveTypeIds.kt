package org.cangnova.cangjie.cfir.types

import org.cangnova.cangjie.builtins.StandardNames
import org.cangnova.cangjie.cfir.session.services.CfirExtendTargetKey
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name

/**
 * primitive 类型在符号层暴露时使用的合成 [ClassId]。
 */
val PrimitiveTypeKind.classId: ClassId
    get() = ClassId(StandardNames.BASIC_PACKAGE_FQ_NAME, Name.identifier(typeName))

/**
 * 将标准库 basic 包下的 [ClassId] 还原为 primitive 类型种类。
 */
fun ClassId.toPrimitiveTypeKindOrNull(): PrimitiveTypeKind? {
    if (packageFqName != StandardNames.BASIC_PACKAGE_FQ_NAME) return null
    return PrimitiveTypeKind.entries.firstOrNull { it.typeName == shortClassName.asString() }
}

/**
 * 当前 primitive 是否作为可见内建 classifier 暴露给解析和 extend 查询。
 */
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

/**
 * 当前 primitive 在 extend 成员查找中需要尝试的真实 primitive 种类。
 */
val PrimitiveTypeKind.extendLookupKinds: List<PrimitiveTypeKind>
    get() = idealExtendLookupKinds.ifEmpty { listOf(this) }

/**
 * 当前类型为 ideal primitive 或 ideal literal 时对应的真实 extend 查找类型集合。
 */
val ConeCangJieType.idealExtendLookupTypes: List<ConePrimitiveType>
    get() = when (this) {
        is ConeIdealIntLiteralType -> PrimitiveTypeKind.IDEAL_INT.idealExtendLookupKinds.map(::ConePrimitiveType)
        is ConeIdealFloatLiteralType -> PrimitiveTypeKind.IDEAL_FLOAT.idealExtendLookupKinds.map(::ConePrimitiveType)
        is ConePrimitiveType -> kind.idealExtendLookupKinds.map(::ConePrimitiveType)
        else -> emptyList()
    }

/**
 * 提取类型的 nominal [ClassId] 或 primitive 合成 [ClassId]。
 */
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
 * extend 目标索引用的语义键。
 *
 * primitive 仍复用其合成 ClassId；`CPointer` / `CString` 没有 ClassId，
 * 但官方 `TypeManager::builtinTyToExtendMap` 会把它们作为 built-in type
 * 参与 extend 查询，因此这里用专门 key 表示。
 */
val ConeCangJieType.extendTargetKey: CfirExtendTargetKey?
    get() = when (this) {
        is ConePointerType -> CfirExtendTargetKey.CPointer
        is ConeCStringType -> CfirExtendTargetKey.CString
        else -> classIdOrPrimitiveClassId?.let(CfirExtendTargetKey::ClassLike)
    }

/**
 * extend 声明侧的目标索引键。
 *
 * typealias 目标必须保留别名声明身份：`extend C<T> <: I<T>` 不是
 * 对 `C` 最终展开 class 的 extend。展开类型上的 abbreviation 只用于
 * 还原声明侧 alias 视图，不能在索引时丢掉这层声明映射。
 */
val ConeCangJieType.declaredExtendTargetKey: CfirExtendTargetKey?
    get() {
        val abbreviatedAlias = abbreviatedType as? ConeTypeAliasType
        if (abbreviatedAlias != null) return CfirExtendTargetKey.ClassLike(abbreviatedAlias.classId)
        return when (this) {
            is ConeTypeAliasType -> CfirExtendTargetKey.ClassLike(classId)
            else -> extendTargetKey
        }
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

/**
 * typealias 展开后的 extend 目标键。
 */
val ConeCangJieType.expandedExtendTargetKey: CfirExtendTargetKey?
    get() = when (this) {
        is ConeTypeAliasType -> expandedType?.expandedExtendTargetKey ?: CfirExtendTargetKey.ClassLike(classId)
        else -> extendTargetKey
    }
