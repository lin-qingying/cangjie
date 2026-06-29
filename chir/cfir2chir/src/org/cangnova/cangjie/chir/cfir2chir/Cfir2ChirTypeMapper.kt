package org.cangnova.cangjie.chir.cfir2chir

import org.cangnova.cangjie.chir.core.type.ChirCPointerType
import org.cangnova.cangjie.chir.core.type.ChirCStringType
import org.cangnova.cangjie.chir.core.type.ChirClassType
import org.cangnova.cangjie.chir.core.type.ChirEnumType
import org.cangnova.cangjie.chir.core.type.ChirFunctionType
import org.cangnova.cangjie.chir.core.type.ChirGenericType
import org.cangnova.cangjie.chir.core.type.ChirNamedType
import org.cangnova.cangjie.chir.core.type.ChirPrimitiveType
import org.cangnova.cangjie.chir.core.type.ChirRawArrayType
import org.cangnova.cangjie.chir.core.type.ChirResolvedTypeRef
import org.cangnova.cangjie.chir.core.type.ChirStructType
import org.cangnova.cangjie.chir.core.type.ChirTupleType
import org.cangnova.cangjie.chir.core.type.ChirType
import org.cangnova.cangjie.chir.core.type.ChirTypeRef
import org.cangnova.cangjie.chir.core.type.ChirVArrayType
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.types.CfirBasicTypeRef
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeAnyType
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeCStringType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.ConeIdealLiteralType
import org.cangnova.cangjie.cfir.types.ConeIntersectionType
import org.cangnova.cangjie.cfir.types.ConePlaceholderType
import org.cangnova.cangjie.cfir.types.ConePointerType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeQuestType
import org.cangnova.cangjie.cfir.types.ConeStubType
import org.cangnova.cangjie.cfir.types.ConeStructType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.ConeTypeAliasType
import org.cangnova.cangjie.cfir.types.ConeTypeVariableType
import org.cangnova.cangjie.cfir.types.ConeUnionType
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
    /**
     * 将 CFIR type ref 映射为 CHIR type ref。
     */
    fun mapTypeRef(typeRef: CfirTypeRef): ChirTypeRef {
        return when (typeRef) {
            is CfirResolvedTypeRef -> ChirResolvedTypeRef(mapConeType(typeRef.coneType))
            is CfirBasicTypeRef -> mapBasicTypeRef(typeRef)
            else -> throw Cfir2ChirConversionException("unresolved CFIR type ref cannot be lowered to CHIR: ${typeRef::class.simpleName}", typeRef)
        }
    }

    /**
     * 将 Cone 类型映射为已解析 CHIR type ref。
     */
    fun mapConeTypeRef(type: ConeCangJieType): ChirResolvedTypeRef =
        ChirResolvedTypeRef(mapConeType(type))

    /**
     * 将已解析 Cone 类型映射为 CHIR 类型。
     */
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
            is ConeVArrayType -> ChirVArrayType(
                elementType = mapConeTypeRef(type.elementType),
                rank = type.size.toIntExact("VArray size"),
            )
            is ConePointerType -> ChirCPointerType(mapConeTypeRef(type.pointeeType))
            is ConeCStringType -> ChirCStringType
            ConeAnyType -> ChirNamedType("Any")
            is ConeTypeParameterType -> ChirGenericType(type.lookupTag.typeParameterSymbol.name.asString())
            is ConeTypeAliasType -> type.expandedType?.let(::mapConeType) ?: ChirNamedType(
                renderName = type.classId.asString(),
                typeArguments = type.typeArguments.map { mapConeTypeRef(it.type) },
            )
            is ConeIntersectionType -> ChirNamedType(
                renderName = type.intersectedTypes.joinToString(prefix = "intersection<", postfix = ">") { it.renderNameForSyntheticType() },
                typeArguments = type.intersectedTypes.map(::mapConeTypeRef),
            )
            is ConeUnionType -> ChirNamedType(
                renderName = type.unionTypes.joinToString(prefix = "union<", postfix = ">") { it.renderNameForSyntheticType() },
                typeArguments = type.unionTypes.map(::mapConeTypeRef),
            )
            is ConeQuestType -> ChirNamedType("Quest")
            is ConeErrorType -> throw Cfir2ChirConversionException("error CFIR Cone type cannot be lowered to CHIR: ${type.diagnostic.reason}")
            is ConeIdealLiteralType,
            is ConePlaceholderType,
            is ConeStubType,
            is ConeTypeVariableType,
            -> throw Cfir2ChirConversionException("inference-only CFIR Cone type ${type::class.simpleName} must be resolved before CHIR lowering")
            else -> throw Cfir2ChirConversionException("unsupported CFIR Cone type for CHIR lowering: ${type::class.qualifiedName}")
        }
    }

    /**
     * 映射尚未形成 Cone 的基础类型引用，主要覆盖内建类型名。
     */
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

    /**
     * 将 CFIR primitive kind 映射为 CHIR primitive type。
     */
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

    /**
     * 将 Long 精确转换为 Int，越界时报告转换异常。
     */
    private fun Long.toIntExact(label: String): Int {
        if (this < Int.MIN_VALUE || this > Int.MAX_VALUE) {
            throw Cfir2ChirConversionException("$label $this cannot be represented by current CHIR VArray rank field")
        }
        return toInt()
    }

    /**
     * 为交叉/联合等合成类型渲染稳定可读名称。
     */
    private fun ConeCangJieType.renderNameForSyntheticType(): String =
        when (this) {
            is ConePrimitiveType -> mapPrimitiveType(kind).renderName
            is ConeClassLikeType -> classId.asString()
            is ConeStructType -> classId.asString()
            is ConeEnumType -> classId.asString()
            is ConeTypeAliasType -> classId.asString()
            ConeAnyType -> "Any"
            is ConeTypeParameterType -> lookupTag.typeParameterSymbol.name.asString()
            else -> this::class.simpleName ?: "anonymous"
        }
}
