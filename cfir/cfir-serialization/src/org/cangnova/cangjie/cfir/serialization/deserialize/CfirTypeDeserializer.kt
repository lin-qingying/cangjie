package org.cangnova.cangjie.cfir.serialization.deserialize

import PackageFormat.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.MutableOrEmptyList
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.impl.CfirTypeParameterImpl
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.symbols.ConeClassLikeLookupTagImpl
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterTypeImpl
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.types.impl.CfirResolvedTypeRefImpl
import org.cangnova.cangjie.name.Name

private fun simpleDiagnostic(reason: String): ConeDiagnostic = object : ConeDiagnostic {
    override val reason: String = reason
}

private fun errorType(reason: String, delegatedType: ConeCangJieType? = null): ConeErrorType =
    ConeErrorType(simpleDiagnostic(reason), delegatedType = delegatedType)

/**
 * Deserializes flatbuffer SemaTy into ConeCangJieType.
 */
@OptIn(CfirImplementationDetail::class)
class CfirTypeDeserializer(
    private val context: CfirDeserializationContext,
) {
    private val typesUnderDeserialization = HashSet<Int>()

    /**
     * Deserialize one type index from `allTypes`.
     */
    fun deserializeType(typeIndex: Int): ConeCangJieType {
        context.typeCache[typeIndex]?.let { return it }
        if (!typesUnderDeserialization.add(typeIndex)) {
            return createRecursiveTypeFallback(typeIndex)
        }

        val semaTy = context.pkg.allTypes(typeIndex)
            ?: return errorType("Cannot read type index $typeIndex")

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
    fun deserializeTypeFromField(typeFieldValue: UInt): ConeCangJieType {
        if (typeFieldValue == 0u) return ConePrimitiveType.UNIT
        return deserializeType((typeFieldValue - 1u).toInt())
    }

    private fun convertSemaTy(semaTy: SemaTy): ConeCangJieType {
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

            TypeKind.Type -> errorType("Unsupported type kind: Type")
            else -> errorType("Unknown TypeKind: ${semaTy.kind}")
        }
    }

    private fun deserializeTypeArgs(semaTy: SemaTy): List<ConeTypeProjection> {
        val len = semaTy.typeArgsLength
        if (len == 0) return emptyList()
        return (0 until len).map { index ->
            ConeTypeProjection(deserializeTypeFromField(semaTy.typeArgs(index)))
        }
    }

    private fun convertClassType(semaTy: SemaTy, isInterface: Boolean): ConeCangJieType {
        val info = semaTy.info(CompositeTyInfo()) as? CompositeTyInfo
            ?: return errorType("Class/Interface missing CompositeTyInfo")
        val fullId = info.declPtr
            ?: return errorType("CompositeTyInfo missing declPtr")
        val classId = context.fullIdResolver.resolveClassId(fullId)
            ?: return errorType("Cannot resolve class FullId: ${context.fullIdResolver.describe(fullId)}")
        return ConeClassLikeType(
            lookupTag = ConeClassLikeLookupTagImpl(classId),
            typeArguments = deserializeTypeArgs(semaTy),
            isInterface = isInterface,
            isThisType = info.isThisTy,
        )
    }

    private fun convertStructType(semaTy: SemaTy): ConeCangJieType {
        val info = semaTy.info(CompositeTyInfo()) as? CompositeTyInfo
            ?: return errorType("Struct missing CompositeTyInfo")
        val fullId = info.declPtr
            ?: return errorType("CompositeTyInfo missing declPtr")
        val classId = context.fullIdResolver.resolveClassId(fullId)
            ?: return errorType("Cannot resolve struct FullId: ${context.fullIdResolver.describe(fullId)}")
        return ConeStructType(
            lookupTag = ConeClassLikeLookupTagImpl(classId),
            typeArguments = deserializeTypeArgs(semaTy),
        )
    }

    private fun convertEnumType(semaTy: SemaTy): ConeCangJieType {
        val info = semaTy.info(CompositeTyInfo()) as? CompositeTyInfo
            ?: return errorType("Enum missing CompositeTyInfo")
        val fullId = info.declPtr
            ?: return errorType("CompositeTyInfo missing declPtr")
        val classId = context.fullIdResolver.resolveClassId(fullId)
            ?: return errorType("Cannot resolve enum FullId: ${context.fullIdResolver.describe(fullId)}")
        return ConeEnumType(
            lookupTag = ConeClassLikeLookupTagImpl(classId),
            typeArguments = deserializeTypeArgs(semaTy),
        )
    }

    private fun convertFuncType(semaTy: SemaTy): ConeCangJieType {
        val info = semaTy.info(FuncTyInfo()) as? FuncTyInfo
            ?: return errorType("Func missing FuncTyInfo")
        val paramTypes = deserializeTypeArgs(semaTy).map { it.type }
        val returnType = deserializeTypeFromField(info.retType)
        return ConeFuncType(
            parameterTypes = paramTypes,
            returnType = returnType,
            isCFunc = info.isC,
            hasVariableLenArg = info.hasVariableLenArg,
        )
    }

    private fun convertTupleType(semaTy: SemaTy): ConeCangJieType {
        return ConeTupleType(elementTypes = deserializeTypeArgs(semaTy).map { it.type })
    }

    private fun convertArrayType(semaTy: SemaTy): ConeCangJieType {
        val elementProjection = deserializeTypeArgs(semaTy).singleOrNull()
            ?: ConeTypeProjection(errorType("Array missing element type"))
        return ConeStructType(
            lookupTag = ConeClassLikeLookupTagImpl(StdlibClassIds.Array),
            typeArguments = listOf(elementProjection),
        )
    }

    private fun convertVArrayType(semaTy: SemaTy): ConeCangJieType {
        val elementType = deserializeTypeArgs(semaTy).singleOrNull()?.type
            ?: errorType("VArray missing element type")
        val info = semaTy.info(ArrayTyInfo()) as? ArrayTyInfo
        val size = info?.dimsOrSize ?: 0L
        return ConeVArrayType(elementType = elementType, size = size)
    }

    private fun convertPointerType(semaTy: SemaTy): ConeCangJieType {
        val pointeeType = deserializeTypeArgs(semaTy).singleOrNull()?.type ?: ConePrimitiveType.UNIT
        return ConePointerType(pointeeType = pointeeType)
    }

    private fun convertGenericType(semaTy: SemaTy): ConeCangJieType {
        val info = semaTy.info(GenericTyInfo()) as? GenericTyInfo
            ?: return errorType("Generic missing GenericTyInfo")
        val fullId = info.declPtr
            ?: return errorType("GenericTyInfo missing declPtr")
        val name = context.fullIdResolver.resolveDeclarationName(fullId)
            ?: return errorType("Cannot resolve generic parameter FullId: ${context.fullIdResolver.describe(fullId)}")
        val upperBounds = (0 until info.upperBoundsLength).map {
            deserializeTypeFromField(info.upperBounds(it))
        }
        return ConeTypeParameterTypeImpl(createSyntheticTypeParameterSymbol(name, upperBounds).toLookupTag())
    }

    private fun createRecursiveTypeFallback(typeIndex: Int): ConeCangJieType {
        val semaTy = context.pkg.allTypes(typeIndex)
            ?: return errorType("Recursive type reference: $typeIndex")
        if (semaTy.kind == TypeKind.Generic) {
            val info = semaTy.info(GenericTyInfo()) as? GenericTyInfo
            val name = Name.identifier(info?.declPtr?.decl ?: "T$typeIndex")
            return ConeTypeParameterTypeImpl(createSyntheticTypeParameterSymbol(name, emptyList()).toLookupTag())
        }
        return errorType("Recursive type reference: $typeIndex")
    }

    private fun createSyntheticTypeParameterSymbol(
        name: Name,
        upperBounds: List<ConeCangJieType>,
    ): CfirTypeParameterSymbol {
        val symbol = CfirTypeParameterSymbol()
        val boundRefs = upperBounds.mapTo(mutableListOf<CfirTypeRef>()) { upperBound ->
            CfirResolvedTypeRefImpl(
                source = null,
                annotations = MutableOrEmptyList.empty(),
                coneType = upperBound,
                delegatedTypeRef = null,
            )
        }
        val declaration = CfirTypeParameterImpl(
            source = null,
            moduleData = context.moduleData,
            resolvePhase = CfirResolvePhase.BODY_RESOLVE,
            annotations = MutableOrEmptyList.empty(),
            origin = CfirDeclarationOrigin.Library,
            attributes = CfirDeclarationAttributes.EMPTY,
            containingDeclarationSymbol = symbol,
            symbol = symbol,
            name = name,
            bounds = boundRefs,
        )
        symbol.bind(declaration)
        return symbol
    }
}
