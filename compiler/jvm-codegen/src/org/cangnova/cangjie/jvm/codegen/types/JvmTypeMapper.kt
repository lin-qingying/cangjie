package org.cangnova.cangjie.jvm.codegen.types

import org.cangnova.cangjie.chir.core.type.ChirClassType
import org.cangnova.cangjie.chir.core.type.ChirCStringType
import org.cangnova.cangjie.chir.core.type.ChirEnumType
import org.cangnova.cangjie.chir.core.type.ChirFunctionType
import org.cangnova.cangjie.chir.core.type.ChirNamedType
import org.cangnova.cangjie.chir.core.type.ChirPrimitiveType
import org.cangnova.cangjie.chir.core.type.ChirRawArrayType
import org.cangnova.cangjie.chir.core.type.ChirResolvedTypeRef
import org.cangnova.cangjie.chir.core.type.ChirStructType
import org.cangnova.cangjie.chir.core.type.ChirType
import org.cangnova.cangjie.chir.core.type.ChirTypeRef
import org.cangnova.cangjie.chir.core.type.ChirUnresolvedTypeRef
import org.cangnova.cangjie.jvm.codegen.api.JvmCodegenOptions
import org.cangnova.cangjie.jvm.codegen.diagnostics.JvmCodegenException
import org.cangnova.cangjie.jvm.codegen.naming.JvmNamePolicy
import org.objectweb.asm.Type

/**
 * CHIR 类型到 JVM descriptor 的映射器。这里表达 JVM ABI，不在 CHIR 核心模型里塞平台细节。
 */
class JvmTypeMapper(
    private val defaultPackageName: String = "",
    private val namePolicy: JvmNamePolicy = JvmNamePolicy(JvmCodegenOptions()),
) {
    fun mapReturnType(type: ChirTypeRef): Type {
        val resolved = resolvedType(type)
        return if (resolved == ChirPrimitiveType.UNIT || resolved == ChirPrimitiveType.VOID || resolved == ChirPrimitiveType.NOTHING) {
            Type.VOID_TYPE
        } else {
            mapValueType(type)
        }
    }

    fun mapValueType(type: ChirTypeRef): Type {
        return when (val resolved = resolvedType(type)) {
            ChirPrimitiveType.BOOL -> Type.BOOLEAN_TYPE
            ChirPrimitiveType.INT8, ChirPrimitiveType.UINT8 -> Type.BYTE_TYPE
            ChirPrimitiveType.INT16, ChirPrimitiveType.UINT16 -> Type.SHORT_TYPE
            ChirPrimitiveType.INT32, ChirPrimitiveType.UINT32, ChirPrimitiveType.INT_NATIVE, ChirPrimitiveType.UINT_NATIVE,
            ChirPrimitiveType.RUNE,
            -> Type.INT_TYPE
            ChirPrimitiveType.INT64, ChirPrimitiveType.UINT64 -> Type.LONG_TYPE
            ChirPrimitiveType.FLOAT32 -> Type.FLOAT_TYPE
            ChirPrimitiveType.FLOAT64 -> Type.DOUBLE_TYPE
            ChirPrimitiveType.UNIT, ChirPrimitiveType.VOID, ChirPrimitiveType.NOTHING -> throw JvmCodegenException(
                "type ${resolved.renderName} cannot be used as a JVM value type",
            )
            ChirPrimitiveType.FLOAT16 -> throw JvmCodegenException("JVM backend does not support float16 value type")
            is ChirCStringType -> Type.getObjectType("java/lang/String")
            is ChirNamedType -> mapNamedType(resolved)
            is ChirClassType -> Type.getObjectType(namePolicy.typeInternalName(defaultPackageName, resolved.name))
            is ChirStructType -> Type.getObjectType(namePolicy.typeInternalName(defaultPackageName, resolved.name))
            is ChirEnumType -> Type.getObjectType(namePolicy.typeInternalName(defaultPackageName, resolved.name))
            is ChirRawArrayType -> Type.getType("[${mapValueType(resolved.elementType).descriptor}")
            is ChirFunctionType -> Type.getObjectType("java/lang/Object")
            else -> throw JvmCodegenException("unsupported JVM value type ${resolved.renderName}")
        }
    }

    fun methodDescriptor(returnType: ChirTypeRef, parameterTypes: List<ChirTypeRef>): String {
        return Type.getMethodDescriptor(
            mapReturnType(returnType),
            *parameterTypes.map(::mapValueType).toTypedArray(),
        )
    }

    private fun mapNamedType(type: ChirNamedType): Type {
        return when (type.renderName) {
            "String", "std.core.String", "std/String", "java.lang.String" -> Type.getObjectType("java/lang/String")
            else -> Type.getObjectType(namePolicy.typeInternalName(defaultPackageName, type.renderName))
        }
    }

    private fun resolvedType(type: ChirTypeRef): ChirType {
        return when (type) {
            is ChirResolvedTypeRef -> type.type
            is ChirUnresolvedTypeRef -> throw JvmCodegenException("unresolved type ${type.symbol} cannot be lowered to JVM")
        }
    }

}
