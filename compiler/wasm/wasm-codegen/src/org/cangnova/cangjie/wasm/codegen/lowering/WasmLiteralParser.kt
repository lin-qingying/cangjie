package org.cangnova.cangjie.wasm.codegen.lowering

import org.cangnova.cangjie.chir.core.attribute.ChirStringAttribute
import org.cangnova.cangjie.chir.core.declaration.ChirVariableDeclaration
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.type.ChirCStringType
import org.cangnova.cangjie.chir.core.type.ChirGenericType
import org.cangnova.cangjie.chir.core.type.ChirNamedType
import org.cangnova.cangjie.chir.core.type.ChirPrimitiveType
import org.cangnova.cangjie.chir.core.type.ChirResolvedTypeRef
import org.cangnova.cangjie.chir.core.type.ChirType
import org.cangnova.cangjie.chir.core.type.ChirTypeRef
import org.cangnova.cangjie.chir.core.value.ChirConstantValue
import org.cangnova.cangjie.wasm.codegen.diagnostics.WasmCodegenException
import org.cangnova.cangjie.wasm.ir.WasmConstant
import org.cangnova.cangjie.wasm.ir.WasmInstruction
import org.cangnova.cangjie.wasm.ir.WasmValueType
import java.math.BigInteger

/** 将 CHIR 常量解析为带类型的 Wasm Core 常量指令。 */
internal object WasmLiteralParser {
    fun instruction(
        value: ChirConstantValue,
        mapper: WasmTypeMapper,
        dataPool: WasmDataPool,
    ): List<WasmInstruction> {
        val id = value.semanticId
        val type = resolvedType(value.type, id)
        val literal = value.literal.trim()
        if (literal == "null") {
            if (mapper.valueType(value.type, id) != WasmValueType.I32) {
                throw WasmCodegenException("null literal must use the wasm32 reference representation", id)
            }
            return listOf(WasmInstruction.I32Const(0))
        }
        if (type == ChirPrimitiveType.RUNE) {
            return listOf(WasmInstruction.I32Const(parseRune(literal, dataPool, id)))
        }
        if (isStringType(type)) {
            return listOf(WasmInstruction.I32Const(dataPool.address(literal, type == ChirCStringType, id).toInt()))
        }
        val constant = scalarConstant(value.type, literal, mapper, id, dataPool)
        return when (constant) {
            is WasmConstant.I32 -> listOf(WasmInstruction.I32Const(mapper.normalizeI32Constant(constant.value, value.type, id))) +
                mapper.normalize(value.type, id)
            is WasmConstant.I64 -> listOf(WasmInstruction.I64Const(constant.value)) + mapper.normalize(value.type, id)
            is WasmConstant.F32 -> listOf(WasmInstruction.F32Const(constant.value))
            is WasmConstant.F64 -> listOf(WasmInstruction.F64Const(constant.value))
        }
    }

    fun globalInitializer(
        variable: ChirVariableDeclaration,
        wasmType: WasmValueType,
        mapper: WasmTypeMapper,
        dataPool: WasmDataPool,
    ): WasmConstant {
        val id = variable.semanticId
        val literal = variable.attributes.filterIsInstance<ChirStringAttribute>()
            .firstOrNull { it.key == "initializer" }
            ?.value
            ?: return when (wasmType) {
                WasmValueType.I32 -> WasmConstant.I32(0)
                WasmValueType.I64 -> WasmConstant.I64(0)
                WasmValueType.F32 -> WasmConstant.F32(0.0f)
                WasmValueType.F64 -> WasmConstant.F64(0.0)
            }
        val constant = scalarConstant(variable.type, literal, mapper, id, dataPool)
        if (constant.type != wasmType) {
            throw WasmCodegenException(
                "global '${variable.name}' initializer has Wasm type ${constant.type.displayName}, expected ${wasmType.displayName}",
                id,
            )
        }
        return constant
    }

    fun registerStaticData(value: ChirConstantValue, mapper: WasmTypeMapper, dataPool: WasmDataPool) {
        val type = resolvedType(value.type, value.semanticId)
        if (!isStringType(type) || mapper.valueTypeOrNull(value.type, value.semanticId) != WasmValueType.I32) return
        dataPool.register(value.literal.trim(), type == ChirCStringType, value.semanticId)
    }

    fun registerGlobalStaticData(variable: ChirVariableDeclaration, dataPool: WasmDataPool) {
        val type = resolvedType(variable.type, variable.semanticId)
        if (!isStringType(type)) return
        val literal = variable.attributes.filterIsInstance<ChirStringAttribute>()
            .firstOrNull { it.key == "initializer" }
            ?.value
            ?.trim()
            ?: return
        dataPool.register(literal, type == ChirCStringType, variable.semanticId)
    }

    private fun scalarConstant(
        typeRef: ChirTypeRef,
        literal: String,
        mapper: WasmTypeMapper,
        semanticId: ChirSemanticId,
        dataPool: WasmDataPool,
    ): WasmConstant {
        val type = resolvedType(typeRef, semanticId)
        if (literal == "null") {
            if (mapper.valueType(typeRef, semanticId) != WasmValueType.I32) {
                throw WasmCodegenException("null initializer must use the wasm32 reference representation", semanticId)
            }
            return WasmConstant.I32(0)
        }
        if (type is ChirGenericType && literal != "0") {
            throw WasmCodegenException("generic constants must be null or produced by the generic runtime", semanticId)
        }
        if (type == ChirPrimitiveType.RUNE) {
            return WasmConstant.I32(parseRune(literal, dataPool, semanticId))
        }
        if (isStringType(type)) {
            val wasmType = mapper.valueType(typeRef, semanticId)
            if (wasmType != WasmValueType.I32) {
                throw WasmCodegenException("string literal cannot initialize Wasm ${wasmType.displayName}", semanticId)
            }
            return WasmConstant.I32(dataPool.address(literal, type == ChirCStringType, semanticId).toInt())
        }
        return when (type) {
            ChirPrimitiveType.BOOL -> WasmConstant.I32(
                when (literal.lowercase()) {
                    "true", "1" -> 1
                    "false", "0" -> 0
                    else -> throw WasmCodegenException("invalid Bool literal '$literal'", semanticId)
                },
            )
            ChirPrimitiveType.FLOAT32 -> WasmConstant.F32(parseFloat(literal, semanticId).toFloat())
            ChirPrimitiveType.FLOAT64 -> WasmConstant.F64(parseFloat(literal, semanticId))
            ChirPrimitiveType.FLOAT16 -> WasmConstant.I32(encodeFloat16(parseFloat(literal, semanticId)))
            ChirPrimitiveType.UNIT, ChirPrimitiveType.VOID, ChirPrimitiveType.NOTHING ->
                throw WasmCodegenException("${type.renderName} cannot have a Wasm global initializer", semanticId)
            else -> when (mapper.valueType(typeRef, semanticId)) {
                WasmValueType.I32 -> WasmConstant.I32(
                    mapper.normalizeI32Constant(parseInteger(literal, semanticId).toInt(), typeRef, semanticId),
                )
                WasmValueType.I64 -> WasmConstant.I64(parseInteger(literal, semanticId).toLong())
                WasmValueType.F32 -> WasmConstant.F32(parseFloat(literal, semanticId).toFloat())
                WasmValueType.F64 -> WasmConstant.F64(parseFloat(literal, semanticId))
            }
        }
    }

    private fun parseInteger(raw: String, semanticId: ChirSemanticId): BigInteger {
        return parseIntegerOrNull(raw)
            ?: throw WasmCodegenException("invalid integer literal '$raw'", semanticId)
    }

    private fun parseIntegerOrNull(raw: String): BigInteger? {
        val normalized = raw.replace("_", "").removeSuffix("u").removeSuffix("U").removeSuffix("l").removeSuffix("L")
        val negative = normalized.startsWith('-')
        val unsigned = normalized.removePrefix("-").removePrefix("+")
        val radix = when {
            unsigned.startsWith("0x", ignoreCase = true) -> 16
            unsigned.startsWith("0b", ignoreCase = true) -> 2
            unsigned.startsWith("0o", ignoreCase = true) -> 8
            else -> 10
        }
        val digits = if (radix == 10) unsigned else unsigned.drop(2)
        val parsed = digits.toBigIntegerOrNull(radix) ?: return null
        return if (negative) parsed.negate() else parsed
    }

    private fun parseFloat(raw: String, semanticId: ChirSemanticId): Double {
        val normalized = raw.replace("_", "").removeSuffix("f").removeSuffix("F")
        return when (normalized.lowercase()) {
            "nan" -> Double.NaN
            "infinity", "+infinity", "inf", "+inf" -> Double.POSITIVE_INFINITY
            "-infinity", "-inf" -> Double.NEGATIVE_INFINITY
            else -> normalized.toDoubleOrNull()
                ?: throw WasmCodegenException("invalid floating-point literal '$raw'", semanticId)
        }
    }

    private fun parseRune(raw: String, dataPool: WasmDataPool, semanticId: ChirSemanticId): Int {
        val decoded = if (isQuoted(raw)) {
            dataPool.decodeLiteral(raw, semanticId)
        } else {
            parseIntegerOrNull(raw)?.let { integer ->
                if (integer < BigInteger.ZERO || integer > BigInteger.valueOf(0x10ffff)) {
                    throw WasmCodegenException("Rune literal is outside the Unicode scalar range", semanticId)
                }
                String(Character.toChars(integer.toInt()))
            } ?: raw
        }
        if (decoded.codePointCount(0, decoded.length) != 1) {
            throw WasmCodegenException("Rune literal must contain exactly one Unicode scalar value", semanticId)
        }
        return decoded.codePointAt(0)
    }

    private fun encodeFloat16(value: Double): Int {
        val bits = value.toRawBits().toULong()
        val sign = ((bits shr 48) and 0x8000uL).toInt()
        val exponentBits = ((bits shr 52) and 0x7ffuL).toInt()
        val mantissa = bits and 0x000f_ffff_ffff_ffffuL
        if (exponentBits == 0x7ff) return sign or if (mantissa == 0uL) 0x7c00 else 0x7e00
        if (exponentBits == 0) return sign

        val unbiasedExponent = exponentBits - 1023
        val halfExponent = unbiasedExponent + 15
        val significand = mantissa or (1uL shl 52)
        if (halfExponent <= 0) {
            if (halfExponent < -10) return sign
            val rounded = roundRightEven(significand, (43 - halfExponent).toUInt()).toInt()
            return sign or rounded
        }

        var exponent = halfExponent
        var halfMantissa = roundRightEven(mantissa, 42u).toInt()
        if (halfMantissa == 0x400) {
            halfMantissa = 0
            exponent++
        }
        if (exponent >= 31) return sign or 0x7c00
        return sign or (exponent shl 10) or halfMantissa
    }

    private fun roundRightEven(value: ULong, shift: UInt): ULong {
        if (shift == 0u) return value
        val roundedDown = value shr shift.toInt()
        val remainderMask = (1uL shl shift.toInt()) - 1uL
        val remainder = value and remainderMask
        val halfway = 1uL shl (shift.toInt() - 1)
        return if (remainder > halfway || (remainder == halfway && roundedDown and 1uL != 0uL)) {
            roundedDown + 1uL
        } else {
            roundedDown
        }
    }

    private fun resolvedType(typeRef: ChirTypeRef, semanticId: ChirSemanticId): ChirType =
        (typeRef as? ChirResolvedTypeRef)?.type
            ?: throw WasmCodegenException("unresolved literal type '${typeRef.renderName}'", semanticId)

    private fun isQuoted(value: String): Boolean = value.length >= 2 &&
        value.first() in charArrayOf('"', '\'') && value.last() == value.first()

    private fun isStringType(type: ChirType): Boolean =
        type == ChirCStringType || (type is ChirNamedType && type.renderName == "String")
}
