package org.cangnova.cangjie.codegen.types

import org.cangnova.cangjie.chir.core.type.ChirBoxType
import org.cangnova.cangjie.chir.core.type.ChirCPointerType
import org.cangnova.cangjie.chir.core.type.ChirCStringType
import org.cangnova.cangjie.chir.core.type.ChirClassType
import org.cangnova.cangjie.chir.core.type.ChirEnumType
import org.cangnova.cangjie.chir.core.type.ChirFunctionType
import org.cangnova.cangjie.chir.core.type.ChirGenericType
import org.cangnova.cangjie.chir.core.type.ChirNamedType
import org.cangnova.cangjie.chir.core.type.ChirPrimitiveType
import org.cangnova.cangjie.chir.core.type.ChirRawArrayType
import org.cangnova.cangjie.chir.core.type.ChirRefType
import org.cangnova.cangjie.chir.core.type.ChirResolvedTypeRef
import org.cangnova.cangjie.chir.core.type.ChirStructType
import org.cangnova.cangjie.chir.core.type.ChirThisType
import org.cangnova.cangjie.chir.core.type.ChirTupleType
import org.cangnova.cangjie.chir.core.type.ChirTypeRef
import org.cangnova.cangjie.chir.core.type.ChirUnresolvedTypeRef
import org.cangnova.cangjie.chir.core.type.ChirVArrayType

interface TypeLowering {
    fun lower(type: ChirTypeRef): String
}

class DefaultTypeLowering : TypeLowering {
    override fun lower(type: ChirTypeRef): String {
        return when (type) {
            is ChirResolvedTypeRef -> lowerResolved(type)
            is ChirUnresolvedTypeRef -> "%${type.symbol}"
            else -> "ptr"
        }
    }

    private fun lowerResolved(type: ChirResolvedTypeRef): String {
        return when (val resolved = type.type) {
            ChirPrimitiveType.UNIT -> "void"
            ChirPrimitiveType.BOOL -> "i1"
            ChirPrimitiveType.INT8 -> "i8"
            ChirPrimitiveType.INT16 -> "i16"
            ChirPrimitiveType.INT32 -> "i32"
            ChirPrimitiveType.INT64 -> "i64"
            ChirPrimitiveType.INT_NATIVE -> "i64"
            ChirPrimitiveType.UINT8 -> "i8"
            ChirPrimitiveType.UINT16 -> "i16"
            ChirPrimitiveType.UINT32 -> "i32"
            ChirPrimitiveType.UINT64 -> "i64"
            ChirPrimitiveType.UINT_NATIVE -> "i64"
            ChirPrimitiveType.FLOAT16 -> "half"
            ChirPrimitiveType.FLOAT32 -> "float"
            ChirPrimitiveType.FLOAT64 -> "double"
            ChirPrimitiveType.RUNE -> "i32"
            ChirPrimitiveType.NOTHING -> "void"
            ChirPrimitiveType.VOID -> "void"
            is ChirNamedType -> "%${resolved.renderName}"
            is ChirTupleType -> "{ ${resolved.elementTypes.joinToString(", ") { lower(it) }} }"
            is ChirFunctionType -> "ptr"
            is ChirStructType -> "%struct.${resolved.name}"
            is ChirClassType -> "%class.${resolved.name}"
            is ChirEnumType -> "%enum.${resolved.name}"
            is ChirRawArrayType -> "[${resolved.size ?: 0} x ${lower(resolved.elementType)}]"
            is ChirVArrayType -> "%varray.${resolved.rank}.${sanitize(resolved.elementType.renderName)}"
            is ChirCPointerType -> "ptr"
            is ChirCStringType -> "ptr"
            is ChirGenericType -> "%generic.${resolved.identifier}"
            is ChirRefType -> "ptr"
            is ChirBoxType -> "%box.${sanitize(resolved.boxedType.renderName)}"
            is ChirThisType -> "%this.${sanitize(resolved.ownerTypeName)}"
            else -> "%${sanitize(resolved.renderName)}"
        }
    }

    private fun sanitize(raw: String): String = raw.replace(':', '_').replace('<', '_').replace('>', '_').replace(',', '_')
}

