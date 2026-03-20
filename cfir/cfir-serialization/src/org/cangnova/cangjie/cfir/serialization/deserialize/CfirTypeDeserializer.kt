package org.cangnova.cangjie.cfir.serialization.deserialize

import PackageFormat.ArrayTyInfo
import PackageFormat.CompositeTyInfo
import PackageFormat.FullId
import PackageFormat.FuncTyInfo
import PackageFormat.GenericTyInfo
import PackageFormat.SemaTy
import PackageFormat.TypeKind
import org.cangnova.cangjie.cfir.types.ConeArrayType
import org.cangnova.cangjie.cfir.types.ConeCangjieType
import org.cangnova.cangjie.cfir.types.ConeCStringType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeClassLookupTagImpl
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeFuncType
import org.cangnova.cangjie.cfir.types.ConePointerType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeStructType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.ConeTypeParameterLookupTag
import org.cangnova.cangjie.cfir.types.ConeTypeParameterType
import org.cangnova.cangjie.cfir.types.ConeVArrayType
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * Deserializes flatbuffer SemaTy into ConeCangjieType.
 */
class CfirTypeDeserializer(
    private val context: CfirDeserializationContext,
) {
    private val typesUnderDeserialization = HashSet<Int>()

    /**
     * Deserialize one type index from `allTypes`.
     */
    fun deserializeType(typeIndex: Int): ConeCangjieType {
        context.typeCache[typeIndex]?.let { return it }
        if (!typesUnderDeserialization.add(typeIndex)) {
            return createRecursiveTypeFallback(typeIndex)
        }

        val semaTy = context.pkg.allTypes(typeIndex)
            ?: return ConeErrorType("Cannot read type index $typeIndex")

        return try {
            val result = convertSemaTy(semaTy)
            context.typeCache[typeIndex] = result
            result
        } finally {
            typesUnderDeserialization.remove(typeIndex)
        }
    }

    /**
     * Deserialize type from 1-based type field.
     */
    fun deserializeTypeFromField(typeFieldValue: UInt): ConeCangjieType {
        if (typeFieldValue == 0u) return ConePrimitiveType.UNIT
        return deserializeType((typeFieldValue - 1u).toInt())
    }

    private fun convertSemaTy(semaTy: SemaTy): ConeCangjieType {
        return when (semaTy.kind) {
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

            TypeKind.Class -> convertClassType(semaTy, isInterface = false)
            TypeKind.Interface -> convertClassType(semaTy, isInterface = true)
            TypeKind.Struct -> convertStructType(semaTy)
            TypeKind.Enum -> convertEnumType(semaTy)

            TypeKind.Func -> convertFuncType(semaTy)

            TypeKind.Tuple -> convertTupleType(semaTy)
            TypeKind.Array -> convertArrayType(semaTy)
            TypeKind.VArray -> convertVArrayType(semaTy)

            TypeKind.CPointer -> convertPointerType(semaTy)
            TypeKind.CString -> ConeCStringType()

            TypeKind.Generic -> convertGenericType(semaTy)

            TypeKind.Type -> ConeErrorType("Unsupported type kind: Type")
            else -> ConeErrorType("Unknown TypeKind: ${semaTy.kind}")
        }
    }

    private fun deserializeTypeArgs(semaTy: SemaTy): List<ConeCangjieType> {
        val len = semaTy.typeArgsLength
        if (len == 0) return emptyList()
        return (0 until len).map { deserializeTypeFromField(semaTy.typeArgs(it)) }
    }

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

    private fun convertClassType(semaTy: SemaTy, isInterface: Boolean): ConeCangjieType {
        val info = semaTy.info(CompositeTyInfo()) as? CompositeTyInfo
            ?: return ConeErrorType("Class/Interface missing CompositeTyInfo")
        val fullId = info.declPtr
            ?: return ConeErrorType("CompositeTyInfo missing declPtr")
        val classId = resolveClassId(fullId)
        val typeArgs = deserializeTypeArgs(semaTy)
        return ConeClassLikeType(
            lookupTag = ConeClassLookupTagImpl(classId),
            typeArguments = typeArgs,
            isInterface = isInterface,
            isThisType = info.isThisTy,
        )
    }

    private fun convertStructType(semaTy: SemaTy): ConeCangjieType {
        val info = semaTy.info(CompositeTyInfo()) as? CompositeTyInfo
            ?: return ConeErrorType("Struct missing CompositeTyInfo")
        val fullId = info.declPtr
            ?: return ConeErrorType("CompositeTyInfo missing declPtr")
        val classId = resolveClassId(fullId)
        val typeArgs = deserializeTypeArgs(semaTy)
        return ConeStructType(
            lookupTag = ConeClassLookupTagImpl(classId),
            typeArguments = typeArgs,
        )
    }

    private fun convertEnumType(semaTy: SemaTy): ConeCangjieType {
        val info = semaTy.info(CompositeTyInfo()) as? CompositeTyInfo
            ?: return ConeErrorType("Enum missing CompositeTyInfo")
        val fullId = info.declPtr
            ?: return ConeErrorType("CompositeTyInfo missing declPtr")
        val classId = resolveClassId(fullId)
        val typeArgs = deserializeTypeArgs(semaTy)
        return ConeEnumType(
            lookupTag = ConeClassLookupTagImpl(classId),
            typeArguments = typeArgs,
        )
    }

    private fun convertFuncType(semaTy: SemaTy): ConeCangjieType {
        val info = semaTy.info(FuncTyInfo()) as? FuncTyInfo
            ?: return ConeErrorType("Func missing FuncTyInfo")
        val paramTypes = deserializeTypeArgs(semaTy)
        val returnType = deserializeTypeFromField(info.retType)
        return ConeFuncType(
            parameterTypes = paramTypes,
            returnType = returnType,
            isCFunc = info.isC,
            hasVariableLenArg = info.hasVariableLenArg,
        )
    }

    private fun convertTupleType(semaTy: SemaTy): ConeCangjieType {
        return ConeTupleType(elementTypes = deserializeTypeArgs(semaTy))
    }

    private fun convertArrayType(semaTy: SemaTy): ConeCangjieType {
        val elementType = if (semaTy.typeArgsLength > 0) {
            deserializeTypeFromField(semaTy.typeArgs(0))
        } else {
            ConeErrorType("Array missing element type")
        }
        val info = semaTy.info(ArrayTyInfo()) as? ArrayTyInfo
        val dims = info?.dimsOrSize?.toInt() ?: 1
        return ConeArrayType(elementType = elementType, dims = dims)
    }

    private fun convertVArrayType(semaTy: SemaTy): ConeCangjieType {
        val elementType = if (semaTy.typeArgsLength > 0) {
            deserializeTypeFromField(semaTy.typeArgs(0))
        } else {
            ConeErrorType("VArray missing element type")
        }
        val info = semaTy.info(ArrayTyInfo()) as? ArrayTyInfo
        val size = info?.dimsOrSize ?: 0L
        return ConeVArrayType(elementType = elementType, size = size)
    }

    private fun convertPointerType(semaTy: SemaTy): ConeCangjieType {
        val pointeeType = if (semaTy.typeArgsLength > 0) {
            deserializeTypeFromField(semaTy.typeArgs(0))
        } else {
            ConePrimitiveType.UNIT
        }
        return ConePointerType(pointeeType = pointeeType)
    }

    private fun convertGenericType(semaTy: SemaTy): ConeCangjieType {
        val info = semaTy.info(GenericTyInfo()) as? GenericTyInfo
            ?: return ConeErrorType("Generic missing GenericTyInfo")
        val name = info.declPtr?.decl ?: "T"
        val upperBounds = (0 until info.upperBoundsLength).map {
            deserializeTypeFromField(info.upperBounds(it))
        }
        return ConeTypeParameterType(
            lookupTag = ConeTypeParameterLookupTag(name),
            upperBounds = upperBounds,
        )
    }

    private fun createRecursiveTypeFallback(typeIndex: Int): ConeCangjieType {
        val semaTy = context.pkg.allTypes(typeIndex)
            ?: return ConeErrorType("Recursive type reference: $typeIndex")
        if (semaTy.kind == TypeKind.Generic) {
            val info = semaTy.info(GenericTyInfo()) as? GenericTyInfo
            val name = info?.declPtr?.decl ?: "T$typeIndex"
            return ConeTypeParameterType(
                lookupTag = ConeTypeParameterLookupTag(name),
                upperBounds = emptyList(),
            )
        }
        return ConeErrorType("Recursive type reference: $typeIndex")
    }
}
