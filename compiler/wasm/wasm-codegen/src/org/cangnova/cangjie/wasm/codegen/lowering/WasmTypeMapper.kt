package org.cangnova.cangjie.wasm.codegen.lowering

import org.cangnova.cangjie.chir.core.declaration.ChirClassDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirEnumDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirStructDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirTypeDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirVariableDeclaration
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.model.ChirPackage
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
import org.cangnova.cangjie.chir.core.type.ChirType
import org.cangnova.cangjie.chir.core.type.ChirTypeRef
import org.cangnova.cangjie.chir.core.type.ChirUnresolvedTypeRef
import org.cangnova.cangjie.chir.core.type.ChirVArrayType
import org.cangnova.cangjie.wasm.codegen.diagnostics.WasmCodegenException
import org.cangnova.cangjie.wasm.ir.WasmInstruction
import org.cangnova.cangjie.wasm.ir.WasmLoadKind
import org.cangnova.cangjie.wasm.ir.WasmNumericOperator
import org.cangnova.cangjie.wasm.ir.WasmStoreKind
import org.cangnova.cangjie.wasm.ir.WasmValueType

/** 独立 Wasm backend 的 CHIR 值类型映射与 ILP32 内存布局规则。 */
internal class WasmTypeMapper(chirPackage: ChirPackage) {
    private val declarationsByName = (chirPackage.typeDefinitions + chirPackage.importedTypeDefinitions)
        .associateBy(ChirTypeDeclaration::name)

    /** Map a value type to one Wasm Core stack type; Unit/Void/Nothing have no stack value. */
    fun valueType(typeRef: ChirTypeRef, semanticId: ChirSemanticId): WasmValueType =
        valueTypeOrNull(typeRef, semanticId)
            ?: fail("${typeRef.renderName} has no WebAssembly stack value", semanticId)

    fun valueTypeOrNull(typeRef: ChirTypeRef, semanticId: ChirSemanticId): WasmValueType? {
        val type = resolved(typeRef, semanticId)
        return when (type) {
            ChirPrimitiveType.UNIT,
            ChirPrimitiveType.VOID,
            ChirPrimitiveType.NOTHING,
            -> null
            ChirPrimitiveType.BOOL,
            ChirPrimitiveType.INT8,
            ChirPrimitiveType.INT16,
            ChirPrimitiveType.INT32,
            ChirPrimitiveType.INT_NATIVE,
            ChirPrimitiveType.UINT8,
            ChirPrimitiveType.UINT16,
            ChirPrimitiveType.UINT32,
            ChirPrimitiveType.UINT_NATIVE,
            ChirPrimitiveType.RUNE,
            -> WasmValueType.I32
            ChirPrimitiveType.INT64, ChirPrimitiveType.UINT64 -> WasmValueType.I64
            ChirPrimitiveType.FLOAT32 -> WasmValueType.F32
            ChirPrimitiveType.FLOAT64 -> WasmValueType.F64
            ChirPrimitiveType.FLOAT16 -> WasmValueType.I32
            is ChirCPointerType,
            ChirCStringType,
            is ChirClassType,
            is ChirEnumType,
            is ChirFunctionType,
            is ChirNamedType,
            is ChirRawArrayType,
            is ChirRefType,
            is ChirStructType,
            is ChirThisType,
            is ChirTupleType,
            is ChirVArrayType,
            is ChirBoxType,
            -> WasmValueType.I32
            is ChirGenericType -> WasmValueType.I32
        }
    }

    fun isBoolean(typeRef: ChirTypeRef, semanticId: ChirSemanticId): Boolean =
        resolved(typeRef, semanticId) == ChirPrimitiveType.BOOL

    fun isFloating(typeRef: ChirTypeRef, semanticId: ChirSemanticId): Boolean = when (resolved(typeRef, semanticId)) {
        ChirPrimitiveType.FLOAT16, ChirPrimitiveType.FLOAT32, ChirPrimitiveType.FLOAT64 -> true
        else -> false
    }

    fun isInteger(typeRef: ChirTypeRef, semanticId: ChirSemanticId): Boolean = when (resolved(typeRef, semanticId)) {
        ChirPrimitiveType.INT8, ChirPrimitiveType.INT16, ChirPrimitiveType.INT32, ChirPrimitiveType.INT64,
        ChirPrimitiveType.INT_NATIVE, ChirPrimitiveType.UINT8, ChirPrimitiveType.UINT16, ChirPrimitiveType.UINT32,
        ChirPrimitiveType.UINT64, ChirPrimitiveType.UINT_NATIVE, ChirPrimitiveType.RUNE,
        -> true
        else -> false
    }

    fun isUnsignedInteger(typeRef: ChirTypeRef, semanticId: ChirSemanticId): Boolean = when (resolved(typeRef, semanticId)) {
        ChirPrimitiveType.UINT8, ChirPrimitiveType.UINT16, ChirPrimitiveType.UINT32,
        ChirPrimitiveType.UINT64, ChirPrimitiveType.UINT_NATIVE,
        -> true
        else -> false
    }

    fun functionType(
        parameterTypes: List<ChirTypeRef>,
        returnType: ChirTypeRef,
        semanticId: ChirSemanticId,
    ): org.cangnova.cangjie.wasm.ir.WasmFunctionType =
        org.cangnova.cangjie.wasm.ir.WasmFunctionType(
            parameters = parameterTypes.map { valueType(it, semanticId) },
            results = valueTypeOrNull(returnType, semanticId)?.let(::listOf).orEmpty(),
        )

    /** Return the pointee type used by CHIR pointer/ref memory operations. */
    fun pointeeType(typeRef: ChirTypeRef, semanticId: ChirSemanticId): ChirTypeRef =
        when (val type = resolved(typeRef, semanticId)) {
            is ChirCPointerType -> type.pointeeType
            is ChirRefType -> type.referencedType
            is ChirRawArrayType -> type.elementType
            is ChirVArrayType -> type.elementType
            is ChirBoxType -> type.boxedType
            is ChirTupleType -> typeRef
            is ChirStructType -> typeRef
            is ChirClassType -> typeRef
            is ChirEnumType -> typeRef
            is ChirNamedType -> typeRef
            is ChirCStringType -> ChirResolvedTypeRef(ChirPrimitiveType.INT8)
            else -> fail("${typeRef.renderName} is not a pointer-like CHIR type", semanticId)
        }

    fun byteSize(typeRef: ChirTypeRef, semanticId: ChirSemanticId): Int = when (val type = resolved(typeRef, semanticId)) {
        ChirPrimitiveType.UNIT, ChirPrimitiveType.VOID, ChirPrimitiveType.NOTHING -> 0
        ChirPrimitiveType.BOOL, ChirPrimitiveType.INT8, ChirPrimitiveType.UINT8 -> 1
        ChirPrimitiveType.INT16, ChirPrimitiveType.UINT16 -> 2
        ChirPrimitiveType.FLOAT16 -> 2
        ChirPrimitiveType.INT32, ChirPrimitiveType.UINT32, ChirPrimitiveType.INT_NATIVE,
        ChirPrimitiveType.UINT_NATIVE, ChirPrimitiveType.RUNE, ChirPrimitiveType.FLOAT32,
        -> 4
        ChirPrimitiveType.INT64, ChirPrimitiveType.UINT64, ChirPrimitiveType.FLOAT64 -> 8
        is ChirTupleType -> layout(type.elementTypes, semanticId).size
        is ChirStructType -> layout(type.fieldTypes, semanticId).size
        is ChirClassType -> layout(type.fieldTypes, semanticId).size
        is ChirRawArrayType -> {
            val count = type.size ?: fail("raw array '${type.renderName}' has no fixed Wasm layout size", semanticId)
            checkedMultiply(byteSize(type.elementType, semanticId), count, semanticId)
        }
        is ChirEnumType -> enumSize(type, semanticId)
        is ChirNamedType -> namedTypeSize(type, semanticId)
        is ChirCPointerType, ChirCStringType, is ChirFunctionType, is ChirRefType, is ChirVArrayType,
        is ChirBoxType, is ChirThisType,
        -> 4
        is ChirGenericType -> 4
    }

    fun alignment(typeRef: ChirTypeRef, semanticId: ChirSemanticId): Int = when (byteSize(typeRef, semanticId)) {
        0, 1 -> 1
        2 -> 2
        3, 4 -> 4
        else -> 8
    }

    fun tupleFieldOffset(tuple: ChirTupleType, fieldIndex: Int, semanticId: ChirSemanticId): Int =
        fieldOffset(tuple.elementTypes, fieldIndex, semanticId)

    fun structFieldOffset(struct: ChirStructType, fieldIndex: Int, semanticId: ChirSemanticId): Int =
        fieldOffset(struct.fieldTypes, fieldIndex, semanticId)

    fun classFieldOffset(clazz: ChirClassType, fieldIndex: Int, semanticId: ChirSemanticId): Int =
        fieldOffset(clazz.fieldTypes, fieldIndex, semanticId)

    fun loadKind(typeRef: ChirTypeRef, semanticId: ChirSemanticId): WasmLoadKind = when (resolved(typeRef, semanticId)) {
        ChirPrimitiveType.BOOL, ChirPrimitiveType.UINT8 -> WasmLoadKind.I32_8_U
        ChirPrimitiveType.INT8 -> WasmLoadKind.I32_8_S
        ChirPrimitiveType.UINT16 -> WasmLoadKind.I32_16_U
        ChirPrimitiveType.INT16 -> WasmLoadKind.I32_16_S
        ChirPrimitiveType.INT32, ChirPrimitiveType.UINT32, ChirPrimitiveType.INT_NATIVE,
        ChirPrimitiveType.UINT_NATIVE, ChirPrimitiveType.RUNE,
        -> WasmLoadKind.I32
        ChirPrimitiveType.INT64, ChirPrimitiveType.UINT64 -> WasmLoadKind.I64
        ChirPrimitiveType.FLOAT32 -> WasmLoadKind.F32
        ChirPrimitiveType.FLOAT64 -> WasmLoadKind.F64
        ChirPrimitiveType.FLOAT16 -> WasmLoadKind.I32_16_U
        is ChirCPointerType, ChirCStringType, is ChirClassType, is ChirEnumType, is ChirFunctionType,
        is ChirNamedType, is ChirRawArrayType, is ChirRefType, is ChirStructType, is ChirThisType,
        is ChirTupleType, is ChirVArrayType, is ChirBoxType,
        -> WasmLoadKind.I32
        is ChirGenericType -> WasmLoadKind.I32
        ChirPrimitiveType.UNIT, ChirPrimitiveType.VOID, ChirPrimitiveType.NOTHING ->
            fail("${typeRef.renderName} cannot be loaded as a Wasm value", semanticId)
    }

    fun storeKind(typeRef: ChirTypeRef, semanticId: ChirSemanticId): WasmStoreKind = when (resolved(typeRef, semanticId)) {
        ChirPrimitiveType.BOOL, ChirPrimitiveType.INT8, ChirPrimitiveType.UINT8 -> WasmStoreKind.I32_8
        ChirPrimitiveType.INT16, ChirPrimitiveType.UINT16 -> WasmStoreKind.I32_16
        ChirPrimitiveType.INT32, ChirPrimitiveType.UINT32, ChirPrimitiveType.INT_NATIVE,
        ChirPrimitiveType.UINT_NATIVE, ChirPrimitiveType.RUNE,
        -> WasmStoreKind.I32
        ChirPrimitiveType.INT64, ChirPrimitiveType.UINT64 -> WasmStoreKind.I64
        ChirPrimitiveType.FLOAT32 -> WasmStoreKind.F32
        ChirPrimitiveType.FLOAT64 -> WasmStoreKind.F64
        ChirPrimitiveType.FLOAT16 -> WasmStoreKind.I32_16
        is ChirCPointerType, ChirCStringType, is ChirClassType, is ChirEnumType, is ChirFunctionType,
        is ChirNamedType, is ChirRawArrayType, is ChirRefType, is ChirStructType, is ChirThisType,
        is ChirTupleType, is ChirVArrayType, is ChirBoxType,
        -> WasmStoreKind.I32
        is ChirGenericType -> WasmStoreKind.I32
        ChirPrimitiveType.UNIT, ChirPrimitiveType.VOID, ChirPrimitiveType.NOTHING ->
            fail("${typeRef.renderName} cannot be stored as a Wasm value", semanticId)
    }

    /** Normalize narrow Cangjie integer values after arithmetic and casts. */
    fun normalize(typeRef: ChirTypeRef, semanticId: ChirSemanticId): List<WasmInstruction> = when (resolved(typeRef, semanticId)) {
        ChirPrimitiveType.INT8 -> listOf(WasmInstruction.Numeric(WasmNumericOperator.I32_EXTEND8_S))
        ChirPrimitiveType.UINT8 -> listOf(WasmInstruction.I32Const(0xff), WasmInstruction.Numeric(WasmNumericOperator.I32_AND))
        ChirPrimitiveType.INT16 -> listOf(WasmInstruction.Numeric(WasmNumericOperator.I32_EXTEND16_S))
        ChirPrimitiveType.UINT16 -> listOf(WasmInstruction.I32Const(0xffff), WasmInstruction.Numeric(WasmNumericOperator.I32_AND))
        ChirPrimitiveType.BOOL -> listOf(WasmInstruction.I32Const(1), WasmInstruction.Numeric(WasmNumericOperator.I32_AND))
        ChirPrimitiveType.FLOAT16 -> listOf(WasmInstruction.I32Const(0xffff), WasmInstruction.Numeric(WasmNumericOperator.I32_AND))
        else -> emptyList()
    }

    /** 将 i32 常量规整为仓颉窄整数或 Bool 的位表示。 */
    fun normalizeI32Constant(value: Int, typeRef: ChirTypeRef, semanticId: ChirSemanticId): Int =
        when (resolved(typeRef, semanticId)) {
            ChirPrimitiveType.BOOL -> value and 1
            ChirPrimitiveType.INT8 -> value.toByte().toInt()
            ChirPrimitiveType.UINT8 -> value and 0xff
            ChirPrimitiveType.INT16 -> value.toShort().toInt()
            ChirPrimitiveType.UINT16 -> value and 0xffff
            ChirPrimitiveType.FLOAT16 -> value and 0xffff
            else -> value
        }

    fun fieldLayout(types: List<ChirTypeRef>, semanticId: ChirSemanticId): WasmAggregateLayout = layout(types, semanticId)

    private fun enumSize(type: ChirEnumType, semanticId: ChirSemanticId): Int {
        val payloadLayouts = type.cases.map { case -> layout(case.payloadTypes, semanticId) }
        val payloadAlignment = payloadLayouts.maxOfOrNull(WasmAggregateLayout::alignment) ?: 1
        val payloadOffset = alignTo(4, payloadAlignment)
        val payloadSize = payloadLayouts.maxOfOrNull(WasmAggregateLayout::size) ?: 0
        return checkedAdd(payloadOffset, payloadSize, semanticId)
    }

    private fun namedTypeSize(type: ChirNamedType, semanticId: ChirSemanticId): Int {
        val declaration = declarationsByName[type.renderName] ?: return 4
        if (declaration.typeParameters.isNotEmpty()) {
            if (type.typeArguments.size != declaration.typeParameters.size) {
                fail("generic named type '${type.renderName}' has incomplete type arguments", semanticId)
            }
            fail("generic named type '${type.renderName}' requires concrete field substitution before Wasm layout", semanticId)
        }
        return when (declaration) {
            is ChirStructDeclaration -> layout(declaration.fieldDeclarations.map(ChirVariableDeclaration::type), semanticId).size
            is ChirClassDeclaration -> layout(
                declaration.memberDeclarations.filterIsInstance<ChirVariableDeclaration>().map(ChirVariableDeclaration::type),
                semanticId,
            ).size
            is ChirEnumDeclaration -> 4
            else -> 4
        }
    }

    private fun layout(types: List<ChirTypeRef>, semanticId: ChirSemanticId): WasmAggregateLayout {
        var offset = 0
        var aggregateAlignment = 1
        val offsets = ArrayList<Int>(types.size)
        for (type in types) {
            val fieldAlignment = alignment(type, semanticId)
            aggregateAlignment = maxOf(aggregateAlignment, fieldAlignment)
            offset = alignTo(offset, fieldAlignment)
            offsets += offset
            offset = checkedAdd(offset, byteSize(type, semanticId), semanticId)
        }
        return WasmAggregateLayout(alignTo(offset, aggregateAlignment), aggregateAlignment, offsets)
    }

    private fun fieldOffset(types: List<ChirTypeRef>, fieldIndex: Int, semanticId: ChirSemanticId): Int {
        if (fieldIndex !in types.indices) fail("field index $fieldIndex is outside aggregate of ${types.size} fields", semanticId)
        return layout(types, semanticId).fieldOffsets[fieldIndex]
    }

    private fun resolved(typeRef: ChirTypeRef, semanticId: ChirSemanticId): ChirType = when (typeRef) {
        is ChirResolvedTypeRef -> typeRef.type
        is ChirUnresolvedTypeRef -> fail("unresolved CHIR type '${typeRef.symbol}' reached Wasm lowering", semanticId)
    }

    private fun alignTo(value: Int, alignment: Int): Int {
        require(alignment > 0 && alignment.countOneBits() == 1) { "alignment must be a power of two" }
        return Math.addExact(value, alignment - 1) and -alignment
    }

    private fun checkedAdd(left: Int, right: Int, semanticId: ChirSemanticId): Int = try {
        Math.addExact(left, right)
    } catch (failure: ArithmeticException) {
        fail("aggregate Wasm memory layout exceeds the 32-bit address model", semanticId, failure)
    }

    private fun checkedMultiply(left: Int, right: Int, semanticId: ChirSemanticId): Int = try {
        Math.multiplyExact(left, right)
    } catch (failure: ArithmeticException) {
        fail("array Wasm memory layout exceeds the 32-bit address model", semanticId, failure)
    }

    private fun fail(message: String, semanticId: ChirSemanticId, cause: Throwable? = null): Nothing =
        throw WasmCodegenException(message, semanticId, cause)
}

/** Target ABI layout for an inline aggregate. */
internal data class WasmAggregateLayout(
    val size: Int,
    val alignment: Int,
    val fieldOffsets: List<Int>,
)
