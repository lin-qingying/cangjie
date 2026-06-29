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
import org.cangnova.cangjie.codegen.diagnostics.CodegenLoweringException

/**
 * CHIR 类型到 LLVM IR 类型的唯一 lowering 入口。
 *
 * 后端只接受已经解析完成的 CHIR 类型；未解析类型必须在 CHIR 校验阶段被拦截，
 * 不能在这里生成看似可用的占位 LLVM 类型。
 */
interface TypeLowering {
    /**
     * 将 CHIR 类型引用降低为 LLVM textual type。
     */
    fun lower(type: ChirTypeRef): String
}

/**
 * 默认 CHIR 到 LLVM 类型 lowering 实现。
 */
class DefaultTypeLowering : TypeLowering {
    /**
     * 降低已解析 CHIR 类型；未解析类型直接失败。
     */
    override fun lower(type: ChirTypeRef): String {
        return when (type) {
            is ChirResolvedTypeRef -> lowerResolved(type)
            is ChirUnresolvedTypeRef -> throw CodegenLoweringException(
                "cannot lower unresolved CHIR type '${type.symbol}' to LLVM IR",
                null,
            )
        }
    }

    /**
     * 降低具体已解析 CHIR 类型到 LLVM textual type。
     */
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
            is ChirNamedType -> llvmNominalTypeName(resolved)
            is ChirTupleType -> "{ ${resolved.elementTypes.joinToString(", ") { lower(it) }} }"
            is ChirFunctionType -> "ptr"
            is ChirStructType -> llvmNominalTypeName(resolved)
            is ChirClassType -> llvmNominalTypeName(resolved)
            is ChirEnumType -> llvmNominalTypeName(resolved)
            is ChirRawArrayType -> {
                val size = resolved.size ?: throw CodegenLoweringException(
                    "raw array type '${resolved.renderName}' must carry a fixed LLVM array size",
                    null,
                )
                "[$size x ${lower(resolved.elementType)}]"
            }
            is ChirVArrayType -> llvmNominalTypeName(resolved)
            is ChirCPointerType -> "ptr"
            is ChirCStringType -> "ptr"
            is ChirGenericType -> llvmNominalTypeName(resolved)
            is ChirRefType -> "ptr"
            is ChirBoxType -> llvmNominalTypeName(resolved)
            is ChirThisType -> llvmNominalTypeName(resolved)
        }
    }
}

