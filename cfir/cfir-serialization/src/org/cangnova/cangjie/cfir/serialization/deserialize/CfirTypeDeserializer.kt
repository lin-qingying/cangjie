package org.cangnova.cangjie.cfir.serialization.deserialize

import PackageFormat.*
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * 类型反序列化器：SemaTy → ConeCangjieType。
 *
 * 将 .cjo 中的 FlatBuffers SemaTy 表转换为 CFIR 类型系统中的 ConeCangjieType。
 * 使用 CfirDeserializationContext 中的 typeCache 避免重复反序列化。
 */
class CfirTypeDeserializer(
    private val context: CfirDeserializationContext,
) {
    /**
     * 反序列化指定索引的类型。
     * .cjo 中类型索引从 0 开始，但 type 字段值 0 表示无类型。
     * 实际索引 = type 字段值 - 1（当 type > 0 时）。
     *
     * @param typeIndex allTypes 中的索引（0-based）
     */
    fun deserializeType(typeIndex: Int): ConeCangjieType {
        context.typeCache[typeIndex]?.let { return it }

        val semaTy = context.pkg.allTypes(typeIndex)
            ?: return ConeErrorType("无法读取类型索引 $typeIndex")

        val result = convertSemaTy(semaTy)
        context.typeCache[typeIndex] = result
        return result
    }

    /**
     * 从 type 字段值反序列化类型。
     * type 字段值 0 表示无类型，返回 Unit。
     * 实际索引 = typeFieldValue - 1。
     */
    fun deserializeTypeFromField(typeFieldValue: UInt): ConeCangjieType {
        if (typeFieldValue == 0u) return ConePrimitiveType.UNIT
        return deserializeType((typeFieldValue - 1u).toInt())
    }

    /**
     * 将 SemaTy 转换为 ConeCangjieType。
     * 根据 TypeKind 分派到不同的转换逻辑。
     */
    private fun convertSemaTy(semaTy: SemaTy): ConeCangjieType {
        return when (semaTy.kind) {
            // 原始类型
            TypeKind.Unit -> ConePrimitiveType.UNIT
            TypeKind.Int8 -> ConePrimitiveType.INT8
            TypeKind.Int16 -> ConePrimitiveType.INT16
            TypeKind.Int32 -> ConePrimitiveType.INT32
            TypeKind.Int64 -> ConePrimitiveType.INT64
            TypeKind.IntNative -> ConePrimitiveType.INT_NATIVE
            TypeKind.UInt8 -> ConePrimitiveType.UINT8
            TypeKind.UInt16 -> ConePrimitiveType.UINT16
            TypeKind.UInt32 -> ConePrimitiveType.UINT32
            TypeKind.UInt64 -> ConePrimitiveType.UINT64
            TypeKind.UIntNative -> ConePrimitiveType.UINT_NATIVE
            TypeKind.Float16 -> ConePrimitiveType.FLOAT16
            TypeKind.Float32 -> ConePrimitiveType.FLOAT32
            TypeKind.Float64 -> ConePrimitiveType.FLOAT64
            TypeKind.Rune -> ConePrimitiveType.RUNE
            TypeKind.Nothing -> ConePrimitiveType.NOTHING
            TypeKind.Bool -> ConePrimitiveType.BOOLEAN

            // 复合类型
            TypeKind.Class -> convertClassType(semaTy, isInterface = false)
            TypeKind.Interface -> convertClassType(semaTy, isInterface = true)
            TypeKind.Struct -> convertStructType(semaTy)
            TypeKind.Enum -> convertEnumType(semaTy)

            // 函数类型
            TypeKind.Func -> convertFuncType(semaTy)

            // 容器类型
            TypeKind.Tuple -> convertTupleType(semaTy)
            TypeKind.Array -> convertArrayType(semaTy)
            TypeKind.VArray -> convertVArrayType(semaTy)

            // C 互操作类型
            TypeKind.CPointer -> convertPointerType(semaTy)
            TypeKind.CString -> ConeCStringType()

            // 泛型类型参数
            TypeKind.Generic -> convertGenericType(semaTy)

            // Type（类型的类型，暂映射为 error）
            TypeKind.Type -> ConeErrorType("Type kind 暂不支持")

            else -> ConeErrorType("未知 TypeKind: ${semaTy.kind}")
        }
    }

    /** 反序列化 typeArgs 为 ConeCangjieType 列表 */
    private fun deserializeTypeArgs(semaTy: SemaTy): List<ConeCangjieType> {
        val len = semaTy.typeArgsLength
        if (len == 0) return emptyList()
        return (0 until len).map { deserializeTypeFromField(semaTy.typeArgs(it)) }
    }

    /** 从 CompositeTyInfo 的 FullId 解析 ClassId */
    private fun resolveClassId(fullId: FullId): ClassId {
        val pkgName = if (fullId.pkgId == 0) {
            context.header.fullPkgName
        } else {
            val importIndex = fullId.pkgId - 1
            context.header.imports.getOrElse(importIndex) { "" }
        }
        val declName = fullId.decl ?: "???"
        return ClassId(FqName(pkgName), Name.identifier(declName))
    }

    /** Class/Interface → ConeClassLikeType */
    private fun convertClassType(semaTy: SemaTy, isInterface: Boolean): ConeCangjieType {
        val info = semaTy.info(CompositeTyInfo()) as? CompositeTyInfo
            ?: return ConeErrorType("Class/Interface 缺少 CompositeTyInfo")
        val fullId = info.declPtr
            ?: return ConeErrorType("CompositeTyInfo 缺少 declPtr")
        val classId = resolveClassId(fullId)
        val typeArgs = deserializeTypeArgs(semaTy)
        return ConeClassLikeType(
            lookupTag = ConeClassLookupTagImpl(classId),
            typeArguments = typeArgs,
            isInterface = isInterface,
            isThisType = info.isThisTy,
        )
    }

    /** Struct → ConeStructType */
    private fun convertStructType(semaTy: SemaTy): ConeCangjieType {
        val info = semaTy.info(CompositeTyInfo()) as? CompositeTyInfo
            ?: return ConeErrorType("Struct 缺少 CompositeTyInfo")
        val fullId = info.declPtr
            ?: return ConeErrorType("CompositeTyInfo 缺少 declPtr")
        val classId = resolveClassId(fullId)
        val typeArgs = deserializeTypeArgs(semaTy)
        return ConeStructType(
            lookupTag = ConeClassLookupTagImpl(classId),
            typeArguments = typeArgs,
        )
    }

    /** Enum → ConeEnumType */
    private fun convertEnumType(semaTy: SemaTy): ConeCangjieType {
        val info = semaTy.info(CompositeTyInfo()) as? CompositeTyInfo
            ?: return ConeErrorType("Enum 缺少 CompositeTyInfo")
        val fullId = info.declPtr
            ?: return ConeErrorType("CompositeTyInfo 缺少 declPtr")
        val classId = resolveClassId(fullId)
        val typeArgs = deserializeTypeArgs(semaTy)
        return ConeEnumType(
            lookupTag = ConeClassLookupTagImpl(classId),
            typeArguments = typeArgs,
        )
    }

    /** Func → ConeFuncType */
    private fun convertFuncType(semaTy: SemaTy): ConeCangjieType {
        val info = semaTy.info(FuncTyInfo()) as? FuncTyInfo
            ?: return ConeErrorType("Func 缺少 FuncTyInfo")
        // typeArgs 中存放参数类型
        val paramTypes = deserializeTypeArgs(semaTy)
        // retType 是返回类型的字段值
        val returnType = deserializeTypeFromField(info.retType)
        return ConeFuncType(
            parameterTypes = paramTypes,
            returnType = returnType,
            isCFunc = info.isC,
            hasVariableLenArg = info.hasVariableLenArg,
        )
    }

    /** Tuple → ConeTupleType */
    private fun convertTupleType(semaTy: SemaTy): ConeCangjieType {
        val elementTypes = deserializeTypeArgs(semaTy)
        return ConeTupleType(elementTypes = elementTypes)
    }

    /** Array → ConeArrayType */
    private fun convertArrayType(semaTy: SemaTy): ConeCangjieType {
        val elementType = if (semaTy.typeArgsLength > 0) {
            deserializeTypeFromField(semaTy.typeArgs(0))
        } else {
            ConeErrorType("Array 缺少元素类型")
        }
        val info = semaTy.info(ArrayTyInfo()) as? ArrayTyInfo
        val dims = info?.dimsOrSize?.toInt() ?: 1
        return ConeArrayType(elementType = elementType, dims = dims)
    }

    /** VArray → ConeVArrayType */
    private fun convertVArrayType(semaTy: SemaTy): ConeCangjieType {
        val elementType = if (semaTy.typeArgsLength > 0) {
            deserializeTypeFromField(semaTy.typeArgs(0))
        } else {
            ConeErrorType("VArray 缺少元素类型")
        }
        val info = semaTy.info(ArrayTyInfo()) as? ArrayTyInfo
        val size = info?.dimsOrSize ?: 0L
        return ConeVArrayType(elementType = elementType, size = size)
    }

    /** CPointer → ConePointerType */
    private fun convertPointerType(semaTy: SemaTy): ConeCangjieType {
        val pointeeType = if (semaTy.typeArgsLength > 0) {
            deserializeTypeFromField(semaTy.typeArgs(0))
        } else {
            ConePrimitiveType.UNIT // void* 等价
        }
        return ConePointerType(pointeeType = pointeeType)
    }

    /** Generic → ConeTypeParameterType */
    private fun convertGenericType(semaTy: SemaTy): ConeCangjieType {
        val info = semaTy.info(GenericTyInfo()) as? GenericTyInfo
            ?: return ConeErrorType("Generic 缺少 GenericTyInfo")
        val fullId = info.declPtr
        val name = fullId?.decl ?: "T"
        val upperBounds = (0 until info.upperBoundsLength).map {
            deserializeTypeFromField(info.upperBounds(it))
        }
        return ConeTypeParameterType(
            lookupTag = ConeTypeParameterLookupTag(name),
            upperBounds = upperBounds,
        )
    }
}
