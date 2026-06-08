package org.cangnova.cangjie.chir.cfir2chir

import org.cangnova.cangjie.chir.core.type.ChirCPointerType
import org.cangnova.cangjie.chir.core.type.ChirCStringType
import org.cangnova.cangjie.chir.core.type.ChirClassType
import org.cangnova.cangjie.chir.core.type.ChirEnumType
import org.cangnova.cangjie.chir.core.type.ChirFunctionType
import org.cangnova.cangjie.chir.core.type.ChirGenericType
import org.cangnova.cangjie.chir.core.type.ChirNamedType
import org.cangnova.cangjie.chir.core.type.ChirPrimitiveType
import org.cangnova.cangjie.chir.core.type.ChirResolvedTypeRef
import org.cangnova.cangjie.chir.core.type.ChirStructType
import org.cangnova.cangjie.chir.core.type.ChirTupleType
import org.cangnova.cangjie.chir.core.type.ChirType
import org.cangnova.cangjie.chir.core.type.ChirTypeRef
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.types.CfirBasicTypeRef
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeCStringType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.ConePointerType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeStructType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.ConeVArrayType
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind
import org.cangnova.cangjie.cfir.types.type

/**
 * CFIR 类型到 CHIR 类型的映射器。
 *
 * 类型映射只接受已经完成 resolve 的真实 Cone 类型；理想字面量类型、错误类型和占位推断类型
 * 不在 backend 阶段兜底成其他类型。
 */
class Cfir2ChirTypeMapper {
    fun mapTypeRef(typeRef: CfirTypeRef): ChirTypeRef {
        return when (typeRef) {
            is CfirResolvedTypeRef -> ChirResolvedTypeRef(mapConeType(typeRef.coneType))
            is CfirBasicTypeRef -> mapBasicTypeRef(typeRef)
            else -> throw Cfir2ChirConversionException("unresolved CFIR type ref cannot be lowered to CHIR: ${typeRef::class.simpleName}", typeRef)
        }
    }

    fun mapConeTypeRef(type: ConeCangJieType): ChirResolvedTypeRef =
        ChirResolvedTypeRef(mapConeType(type))

    fun mapConeType(type: ConeCangJieType): ChirType {
        return when (type) {
            is ConePrimitiveType -> mapPrimitiveType(type.kind)
            is ConeClassLikeType -> ChirClassType(
                name = type.classId.asString(),
                typeArguments = type.typeArguments.map { mapConeTypeRef(it.type) },
            )
            is ConeStructType -> ChirStructType(
                name = type.classId.asString(),
                typeArguments = type.typeArguments.map { mapConeTypeRef(it.type) },
            )
            is ConeEnumType -> ChirEnumType(
                name = type.classId.asString(),
                typeArguments = type.typeArguments.map { mapConeTypeRef(it.type) },
            )
            is ConeFunctionType -> {
                if (type.isCFunc || type.hasVariableLenArg) {
                    throw Cfir2ChirConversionException("C function types require dedicated CHIR ABI lowering before JVM backend")
                }
                ChirFunctionType(
                    parameterTypes = type.parameterTypes.map(::mapConeTypeRef),
                    returnType = mapConeTypeRef(type.returnType),
                )
            }
            is ConeTupleType -> ChirTupleType(type.elementTypes.map(::mapConeTypeRef))
            is ConeVArrayType -> throw Cfir2ChirConversionException("VArray size ${type.size} has no CHIR carrier yet; extend CHIR type model before lowering")
            is ConePointerType -> ChirCPointerType(mapConeTypeRef(type.pointeeType))
            is ConeCStringType -> ChirCStringType
            is ConeTypeParameterType -> ChirGenericType(type.lookupTag.typeParameterSymbol.name.asString())
            else -> throw Cfir2ChirConversionException("unsupported CFIR Cone type for CHIR lowering: ${type::class.qualifiedName}")
        }
    }

    private fun mapBasicTypeRef(typeRef: CfirBasicTypeRef): ChirTypeRef {
        return ChirResolvedTypeRef(
            when (typeRef.name.asString()) {
                "Unit" -> ChirPrimitiveType.UNIT
                "Bool" -> ChirPrimitiveType.BOOL
                "Int8" -> ChirPrimitiveType.INT8
                "Int16" -> ChirPrimitiveType.INT16
                "Int32" -> ChirPrimitiveType.INT32
                "Int64" -> ChirPrimitiveType.INT64
                "IntNative" -> ChirPrimitiveType.INT_NATIVE
                "UInt8" -> ChirPrimitiveType.UINT8
                "UInt16" -> ChirPrimitiveType.UINT16
                "UInt32" -> ChirPrimitiveType.UINT32
                "UInt64" -> ChirPrimitiveType.UINT64
                "UIntNative" -> ChirPrimitiveType.UINT_NATIVE
                "Float16" -> ChirPrimitiveType.FLOAT16
                "Float32" -> ChirPrimitiveType.FLOAT32
                "Float64" -> ChirPrimitiveType.FLOAT64
                "Rune" -> ChirPrimitiveType.RUNE
                "Nothing" -> ChirPrimitiveType.NOTHING
                "String" -> ChirNamedType("String")
                else -> ChirNamedType(typeRef.name.asString())
            },
        )
    }

    private fun mapPrimitiveType(kind: PrimitiveTypeKind): ChirPrimitiveType {
        return when (kind) {
            PrimitiveTypeKind.UNIT -> ChirPrimitiveType.UNIT
            PrimitiveTypeKind.BOOLEAN -> ChirPrimitiveType.BOOL
            PrimitiveTypeKind.INT8 -> ChirPrimitiveType.INT8
            PrimitiveTypeKind.INT16 -> ChirPrimitiveType.INT16
            PrimitiveTypeKind.INT32 -> ChirPrimitiveType.INT32
            PrimitiveTypeKind.INT64 -> ChirPrimitiveType.INT64
            PrimitiveTypeKind.INT_NATIVE -> ChirPrimitiveType.INT_NATIVE
            PrimitiveTypeKind.UINT8 -> ChirPrimitiveType.UINT8
            PrimitiveTypeKind.UINT16 -> ChirPrimitiveType.UINT16
            PrimitiveTypeKind.UINT32 -> ChirPrimitiveType.UINT32
            PrimitiveTypeKind.UINT64 -> ChirPrimitiveType.UINT64
            PrimitiveTypeKind.UINT_NATIVE -> ChirPrimitiveType.UINT_NATIVE
            PrimitiveTypeKind.FLOAT16 -> ChirPrimitiveType.FLOAT16
            PrimitiveTypeKind.FLOAT32 -> ChirPrimitiveType.FLOAT32
            PrimitiveTypeKind.FLOAT64 -> ChirPrimitiveType.FLOAT64
            PrimitiveTypeKind.RUNE -> ChirPrimitiveType.RUNE
            PrimitiveTypeKind.NOTHING -> ChirPrimitiveType.NOTHING
            PrimitiveTypeKind.IDEAL_INT,
            PrimitiveTypeKind.IDEAL_FLOAT,
            -> throw Cfir2ChirConversionException("ideal literal type $kind must be resolved before CHIR lowering")
        }
    }
}
