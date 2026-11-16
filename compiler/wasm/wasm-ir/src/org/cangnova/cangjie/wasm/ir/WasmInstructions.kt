package org.cangnova.cangjie.wasm.ir

/** Wasm Core 类型化指令；structured control flow 以树形结构表达。 */
sealed interface WasmInstruction {
    data object Unreachable : WasmInstruction
    data object Nop : WasmInstruction
    data object Return : WasmInstruction
    data object Drop : WasmInstruction
    data object Select : WasmInstruction
    data object MemorySize : WasmInstruction
    data object MemoryGrow : WasmInstruction

    data class Block(val label: String, val body: List<WasmInstruction>) : WasmInstruction
    data class Loop(val label: String, val body: List<WasmInstruction>) : WasmInstruction
    data class If(
        val label: String,
        val thenBody: List<WasmInstruction>,
        val elseBody: List<WasmInstruction> = emptyList(),
    ) : WasmInstruction

    data class Br(val label: String) : WasmInstruction
    data class BrIf(val label: String) : WasmInstruction
    data class BrTable(val labels: List<String>, val defaultLabel: String) : WasmInstruction
    data class Call(val functionName: String) : WasmInstruction
    data class CallIndirect(val type: WasmFunctionType, val tableName: String) : WasmInstruction

    data class LocalGet(val localName: String) : WasmInstruction
    data class LocalSet(val localName: String) : WasmInstruction
    data class LocalTee(val localName: String) : WasmInstruction
    data class GlobalGet(val globalName: String) : WasmInstruction
    data class GlobalSet(val globalName: String) : WasmInstruction

    data class I32Const(val value: Int) : WasmInstruction
    data class I64Const(val value: Long) : WasmInstruction
    data class F32Const(val value: Float) : WasmInstruction
    data class F64Const(val value: Double) : WasmInstruction
    data class Numeric(val operator: WasmNumericOperator) : WasmInstruction
    data class Load(
        val kind: WasmLoadKind,
        val alignmentExponent: UInt = kind.naturalAlignmentExponent,
        val offset: UInt = 0u,
    ) : WasmInstruction
    data class Store(
        val kind: WasmStoreKind,
        val alignmentExponent: UInt = kind.naturalAlignmentExponent,
        val offset: UInt = 0u,
    ) : WasmInstruction
}

/** CHIR lowering 使用的 Wasm 数值运算及其二进制编码和栈类型。 */
enum class WasmNumericOperator(
    val opcode: Int,
    val inputs: List<WasmValueType>,
    val result: WasmValueType,
) {
    I32_EQZ(0x45, listOf(WasmValueType.I32), WasmValueType.I32),
    I32_EQ(0x46, listOf(WasmValueType.I32, WasmValueType.I32), WasmValueType.I32),
    I32_NE(0x47, listOf(WasmValueType.I32, WasmValueType.I32), WasmValueType.I32),
    I32_LT_S(0x48, listOf(WasmValueType.I32, WasmValueType.I32), WasmValueType.I32),
    I32_LT_U(0x49, listOf(WasmValueType.I32, WasmValueType.I32), WasmValueType.I32),
    I32_GT_S(0x4a, listOf(WasmValueType.I32, WasmValueType.I32), WasmValueType.I32),
    I32_GT_U(0x4b, listOf(WasmValueType.I32, WasmValueType.I32), WasmValueType.I32),
    I32_LE_S(0x4c, listOf(WasmValueType.I32, WasmValueType.I32), WasmValueType.I32),
    I32_LE_U(0x4d, listOf(WasmValueType.I32, WasmValueType.I32), WasmValueType.I32),
    I32_GE_S(0x4e, listOf(WasmValueType.I32, WasmValueType.I32), WasmValueType.I32),
    I32_GE_U(0x4f, listOf(WasmValueType.I32, WasmValueType.I32), WasmValueType.I32),
    I64_EQZ(0x50, listOf(WasmValueType.I64), WasmValueType.I32),
    I64_EQ(0x51, listOf(WasmValueType.I64, WasmValueType.I64), WasmValueType.I32),
    I64_NE(0x52, listOf(WasmValueType.I64, WasmValueType.I64), WasmValueType.I32),
    I64_LT_S(0x53, listOf(WasmValueType.I64, WasmValueType.I64), WasmValueType.I32),
    I64_LT_U(0x54, listOf(WasmValueType.I64, WasmValueType.I64), WasmValueType.I32),
    I64_GT_S(0x55, listOf(WasmValueType.I64, WasmValueType.I64), WasmValueType.I32),
    I64_GT_U(0x56, listOf(WasmValueType.I64, WasmValueType.I64), WasmValueType.I32),
    I64_LE_S(0x57, listOf(WasmValueType.I64, WasmValueType.I64), WasmValueType.I32),
    I64_LE_U(0x58, listOf(WasmValueType.I64, WasmValueType.I64), WasmValueType.I32),
    I64_GE_S(0x59, listOf(WasmValueType.I64, WasmValueType.I64), WasmValueType.I32),
    I64_GE_U(0x5a, listOf(WasmValueType.I64, WasmValueType.I64), WasmValueType.I32),
    F32_EQ(0x5b, listOf(WasmValueType.F32, WasmValueType.F32), WasmValueType.I32),
    F32_NE(0x5c, listOf(WasmValueType.F32, WasmValueType.F32), WasmValueType.I32),
    F32_LT(0x5d, listOf(WasmValueType.F32, WasmValueType.F32), WasmValueType.I32),
    F32_GT(0x5e, listOf(WasmValueType.F32, WasmValueType.F32), WasmValueType.I32),
    F32_LE(0x5f, listOf(WasmValueType.F32, WasmValueType.F32), WasmValueType.I32),
    F32_GE(0x60, listOf(WasmValueType.F32, WasmValueType.F32), WasmValueType.I32),
    F64_EQ(0x61, listOf(WasmValueType.F64, WasmValueType.F64), WasmValueType.I32),
    F64_NE(0x62, listOf(WasmValueType.F64, WasmValueType.F64), WasmValueType.I32),
    F64_LT(0x63, listOf(WasmValueType.F64, WasmValueType.F64), WasmValueType.I32),
    F64_GT(0x64, listOf(WasmValueType.F64, WasmValueType.F64), WasmValueType.I32),
    F64_LE(0x65, listOf(WasmValueType.F64, WasmValueType.F64), WasmValueType.I32),
    F64_GE(0x66, listOf(WasmValueType.F64, WasmValueType.F64), WasmValueType.I32),
    I32_CLZ(0x67, listOf(WasmValueType.I32), WasmValueType.I32),
    I32_CTZ(0x68, listOf(WasmValueType.I32), WasmValueType.I32),
    I32_POPCNT(0x69, listOf(WasmValueType.I32), WasmValueType.I32),
    I32_ADD(0x6a, listOf(WasmValueType.I32, WasmValueType.I32), WasmValueType.I32),
    I32_SUB(0x6b, listOf(WasmValueType.I32, WasmValueType.I32), WasmValueType.I32),
    I32_MUL(0x6c, listOf(WasmValueType.I32, WasmValueType.I32), WasmValueType.I32),
    I32_DIV_S(0x6d, listOf(WasmValueType.I32, WasmValueType.I32), WasmValueType.I32),
    I32_DIV_U(0x6e, listOf(WasmValueType.I32, WasmValueType.I32), WasmValueType.I32),
    I32_REM_S(0x6f, listOf(WasmValueType.I32, WasmValueType.I32), WasmValueType.I32),
    I32_REM_U(0x70, listOf(WasmValueType.I32, WasmValueType.I32), WasmValueType.I32),
    I32_AND(0x71, listOf(WasmValueType.I32, WasmValueType.I32), WasmValueType.I32),
    I32_OR(0x72, listOf(WasmValueType.I32, WasmValueType.I32), WasmValueType.I32),
    I32_XOR(0x73, listOf(WasmValueType.I32, WasmValueType.I32), WasmValueType.I32),
    I32_SHL(0x74, listOf(WasmValueType.I32, WasmValueType.I32), WasmValueType.I32),
    I32_SHR_S(0x75, listOf(WasmValueType.I32, WasmValueType.I32), WasmValueType.I32),
    I32_SHR_U(0x76, listOf(WasmValueType.I32, WasmValueType.I32), WasmValueType.I32),
    I32_ROTL(0x77, listOf(WasmValueType.I32, WasmValueType.I32), WasmValueType.I32),
    I32_ROTR(0x78, listOf(WasmValueType.I32, WasmValueType.I32), WasmValueType.I32),
    I64_CLZ(0x79, listOf(WasmValueType.I64), WasmValueType.I64),
    I64_CTZ(0x7a, listOf(WasmValueType.I64), WasmValueType.I64),
    I64_POPCNT(0x7b, listOf(WasmValueType.I64), WasmValueType.I64),
    I64_ADD(0x7c, listOf(WasmValueType.I64, WasmValueType.I64), WasmValueType.I64),
    I64_SUB(0x7d, listOf(WasmValueType.I64, WasmValueType.I64), WasmValueType.I64),
    I64_MUL(0x7e, listOf(WasmValueType.I64, WasmValueType.I64), WasmValueType.I64),
    I64_DIV_S(0x7f, listOf(WasmValueType.I64, WasmValueType.I64), WasmValueType.I64),
    I64_DIV_U(0x80, listOf(WasmValueType.I64, WasmValueType.I64), WasmValueType.I64),
    I64_REM_S(0x81, listOf(WasmValueType.I64, WasmValueType.I64), WasmValueType.I64),
    I64_REM_U(0x82, listOf(WasmValueType.I64, WasmValueType.I64), WasmValueType.I64),
    I64_AND(0x83, listOf(WasmValueType.I64, WasmValueType.I64), WasmValueType.I64),
    I64_OR(0x84, listOf(WasmValueType.I64, WasmValueType.I64), WasmValueType.I64),
    I64_XOR(0x85, listOf(WasmValueType.I64, WasmValueType.I64), WasmValueType.I64),
    I64_SHL(0x86, listOf(WasmValueType.I64, WasmValueType.I64), WasmValueType.I64),
    I64_SHR_S(0x87, listOf(WasmValueType.I64, WasmValueType.I64), WasmValueType.I64),
    I64_SHR_U(0x88, listOf(WasmValueType.I64, WasmValueType.I64), WasmValueType.I64),
    I64_ROTL(0x89, listOf(WasmValueType.I64, WasmValueType.I64), WasmValueType.I64),
    I64_ROTR(0x8a, listOf(WasmValueType.I64, WasmValueType.I64), WasmValueType.I64),
    F32_ABS(0x8b, listOf(WasmValueType.F32), WasmValueType.F32),
    F32_NEG(0x8c, listOf(WasmValueType.F32), WasmValueType.F32),
    F32_CEIL(0x8d, listOf(WasmValueType.F32), WasmValueType.F32),
    F32_FLOOR(0x8e, listOf(WasmValueType.F32), WasmValueType.F32),
    F32_TRUNC(0x8f, listOf(WasmValueType.F32), WasmValueType.F32),
    F32_NEAREST(0x90, listOf(WasmValueType.F32), WasmValueType.F32),
    F32_SQRT(0x91, listOf(WasmValueType.F32), WasmValueType.F32),
    F32_ADD(0x92, listOf(WasmValueType.F32, WasmValueType.F32), WasmValueType.F32),
    F32_SUB(0x93, listOf(WasmValueType.F32, WasmValueType.F32), WasmValueType.F32),
    F32_MUL(0x94, listOf(WasmValueType.F32, WasmValueType.F32), WasmValueType.F32),
    F32_DIV(0x95, listOf(WasmValueType.F32, WasmValueType.F32), WasmValueType.F32),
    F32_MIN(0x96, listOf(WasmValueType.F32, WasmValueType.F32), WasmValueType.F32),
    F32_MAX(0x97, listOf(WasmValueType.F32, WasmValueType.F32), WasmValueType.F32),
    F32_COPYSIGN(0x98, listOf(WasmValueType.F32, WasmValueType.F32), WasmValueType.F32),
    F64_ABS(0x99, listOf(WasmValueType.F64), WasmValueType.F64),
    F64_NEG(0x9a, listOf(WasmValueType.F64), WasmValueType.F64),
    F64_CEIL(0x9b, listOf(WasmValueType.F64), WasmValueType.F64),
    F64_FLOOR(0x9c, listOf(WasmValueType.F64), WasmValueType.F64),
    F64_TRUNC(0x9d, listOf(WasmValueType.F64), WasmValueType.F64),
    F64_NEAREST(0x9e, listOf(WasmValueType.F64), WasmValueType.F64),
    F64_SQRT(0x9f, listOf(WasmValueType.F64), WasmValueType.F64),
    F64_ADD(0xa0, listOf(WasmValueType.F64, WasmValueType.F64), WasmValueType.F64),
    F64_SUB(0xa1, listOf(WasmValueType.F64, WasmValueType.F64), WasmValueType.F64),
    F64_MUL(0xa2, listOf(WasmValueType.F64, WasmValueType.F64), WasmValueType.F64),
    F64_DIV(0xa3, listOf(WasmValueType.F64, WasmValueType.F64), WasmValueType.F64),
    F64_MIN(0xa4, listOf(WasmValueType.F64, WasmValueType.F64), WasmValueType.F64),
    F64_MAX(0xa5, listOf(WasmValueType.F64, WasmValueType.F64), WasmValueType.F64),
    F64_COPYSIGN(0xa6, listOf(WasmValueType.F64, WasmValueType.F64), WasmValueType.F64),
    I32_WRAP_I64(0xa7, listOf(WasmValueType.I64), WasmValueType.I32),
    I32_TRUNC_F32_S(0xa8, listOf(WasmValueType.F32), WasmValueType.I32),
    I32_TRUNC_F32_U(0xa9, listOf(WasmValueType.F32), WasmValueType.I32),
    I32_TRUNC_F64_S(0xaa, listOf(WasmValueType.F64), WasmValueType.I32),
    I32_TRUNC_F64_U(0xab, listOf(WasmValueType.F64), WasmValueType.I32),
    I64_EXTEND_I32_S(0xac, listOf(WasmValueType.I32), WasmValueType.I64),
    I64_EXTEND_I32_U(0xad, listOf(WasmValueType.I32), WasmValueType.I64),
    I64_TRUNC_F32_S(0xae, listOf(WasmValueType.F32), WasmValueType.I64),
    I64_TRUNC_F32_U(0xaf, listOf(WasmValueType.F32), WasmValueType.I64),
    I64_TRUNC_F64_S(0xb0, listOf(WasmValueType.F64), WasmValueType.I64),
    I64_TRUNC_F64_U(0xb1, listOf(WasmValueType.F64), WasmValueType.I64),
    F32_CONVERT_I32_S(0xb2, listOf(WasmValueType.I32), WasmValueType.F32),
    F32_CONVERT_I32_U(0xb3, listOf(WasmValueType.I32), WasmValueType.F32),
    F32_CONVERT_I64_S(0xb4, listOf(WasmValueType.I64), WasmValueType.F32),
    F32_CONVERT_I64_U(0xb5, listOf(WasmValueType.I64), WasmValueType.F32),
    F32_DEMOTE_F64(0xb6, listOf(WasmValueType.F64), WasmValueType.F32),
    F64_CONVERT_I32_S(0xb7, listOf(WasmValueType.I32), WasmValueType.F64),
    F64_CONVERT_I32_U(0xb8, listOf(WasmValueType.I32), WasmValueType.F64),
    F64_CONVERT_I64_S(0xb9, listOf(WasmValueType.I64), WasmValueType.F64),
    F64_CONVERT_I64_U(0xba, listOf(WasmValueType.I64), WasmValueType.F64),
    F64_PROMOTE_F32(0xbb, listOf(WasmValueType.F32), WasmValueType.F64),
    I32_REINTERPRET_F32(0xbc, listOf(WasmValueType.F32), WasmValueType.I32),
    I64_REINTERPRET_F64(0xbd, listOf(WasmValueType.F64), WasmValueType.I64),
    F32_REINTERPRET_I32(0xbe, listOf(WasmValueType.I32), WasmValueType.F32),
    F64_REINTERPRET_I64(0xbf, listOf(WasmValueType.I64), WasmValueType.F64),
    I32_EXTEND8_S(0xc0, listOf(WasmValueType.I32), WasmValueType.I32),
    I32_EXTEND16_S(0xc1, listOf(WasmValueType.I32), WasmValueType.I32),
    I64_EXTEND8_S(0xc2, listOf(WasmValueType.I64), WasmValueType.I64),
    I64_EXTEND16_S(0xc3, listOf(WasmValueType.I64), WasmValueType.I64),
    I64_EXTEND32_S(0xc4, listOf(WasmValueType.I64), WasmValueType.I64),
}

/** 标量 load 指令的二进制编码、结果类型与自然对齐。 */
enum class WasmLoadKind(
    val opcode: Int,
    val resultType: WasmValueType,
    val naturalAlignmentExponent: UInt,
) {
    I32(0x28, WasmValueType.I32, 2u),
    I64(0x29, WasmValueType.I64, 3u),
    F32(0x2a, WasmValueType.F32, 2u),
    F64(0x2b, WasmValueType.F64, 3u),
    I32_8_S(0x2c, WasmValueType.I32, 0u),
    I32_8_U(0x2d, WasmValueType.I32, 0u),
    I32_16_S(0x2e, WasmValueType.I32, 1u),
    I32_16_U(0x2f, WasmValueType.I32, 1u),
    I64_8_S(0x30, WasmValueType.I64, 0u),
    I64_8_U(0x31, WasmValueType.I64, 0u),
    I64_16_S(0x32, WasmValueType.I64, 1u),
    I64_16_U(0x33, WasmValueType.I64, 1u),
    I64_32_S(0x34, WasmValueType.I64, 2u),
    I64_32_U(0x35, WasmValueType.I64, 2u),
}

/** 标量 store 指令的二进制编码、输入类型与自然对齐。 */
enum class WasmStoreKind(
    val opcode: Int,
    val valueType: WasmValueType,
    val naturalAlignmentExponent: UInt,
) {
    I32(0x36, WasmValueType.I32, 2u),
    I64(0x37, WasmValueType.I64, 3u),
    F32(0x38, WasmValueType.F32, 2u),
    F64(0x39, WasmValueType.F64, 3u),
    I32_8(0x3a, WasmValueType.I32, 0u),
    I32_16(0x3b, WasmValueType.I32, 1u),
    I64_8(0x3c, WasmValueType.I64, 0u),
    I64_16(0x3d, WasmValueType.I64, 1u),
    I64_32(0x3e, WasmValueType.I64, 2u),
}
