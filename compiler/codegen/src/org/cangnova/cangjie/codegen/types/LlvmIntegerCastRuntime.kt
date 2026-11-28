package org.cangnova.cangjie.codegen.types

import org.cangnova.cangjie.chir.core.declaration.ChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.expression.ChirOtherExpression
import org.cangnova.cangjie.chir.core.expression.ChirOtherOperation
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.type.ChirPrimitiveType
import org.cangnova.cangjie.chir.core.type.ChirResolvedTypeRef
import org.cangnova.cangjie.chir.core.type.ChirType
import org.cangnova.cangjie.chir.core.type.ChirTypeRef
import org.cangnova.cangjie.codegen.ir.LlvmFunctionAbi
import java.math.BigInteger

/** 定宽整数的数学值域；上下界不受 JVM Long 表示范围限制。 */
internal data class LlvmIntegerDomain(
    val bitWidth: Int,
    val isSigned: Boolean,
) {
    val minimum: BigInteger
        get() = if (isSigned) BigInteger.ONE.shiftLeft(bitWidth - 1).negate() else BigInteger.ZERO

    val maximum: BigInteger
        get() = if (isSigned) BigInteger.ONE.shiftLeft(bitWidth - 1).subtract(BigInteger.ONE)
        else BigInteger.ONE.shiftLeft(bitWidth).subtract(BigInteger.ONE)
}

/**
 * 统一提供 Cangjie 整数转换的值域关系，供 LLVM lowering 与 module 级异常字面量收集共用。
 */
internal object LlvmIntegerCastSemantics {
    fun domain(typeRef: ChirTypeRef): LlvmIntegerDomain? =
        (typeRef as? ChirResolvedTypeRef)?.type?.let(::domain)

    fun domain(type: ChirType): LlvmIntegerDomain? = when (type) {
        ChirPrimitiveType.INT8 -> LlvmIntegerDomain(8, isSigned = true)
        ChirPrimitiveType.INT16 -> LlvmIntegerDomain(16, isSigned = true)
        ChirPrimitiveType.INT32 -> LlvmIntegerDomain(32, isSigned = true)
        ChirPrimitiveType.INT64, ChirPrimitiveType.INT_NATIVE -> LlvmIntegerDomain(64, isSigned = true)
        ChirPrimitiveType.UINT8 -> LlvmIntegerDomain(8, isSigned = false)
        ChirPrimitiveType.UINT16 -> LlvmIntegerDomain(16, isSigned = false)
        ChirPrimitiveType.UINT32 -> LlvmIntegerDomain(32, isSigned = false)
        ChirPrimitiveType.UINT64, ChirPrimitiveType.UINT_NATIVE -> LlvmIntegerDomain(64, isSigned = false)
        else -> null
    }

    fun canCastWithoutOverflow(source: LlvmIntegerDomain, target: LlvmIntegerDomain): Boolean =
        source.minimum >= target.minimum && source.maximum <= target.maximum

    fun lowerBoundCanOverflow(source: LlvmIntegerDomain, target: LlvmIntegerDomain): Boolean =
        source.minimum < target.minimum

    fun upperBoundCanOverflow(source: LlvmIntegerDomain, target: LlvmIntegerDomain): Boolean =
        source.maximum > target.maximum

    fun lowerBoundOverflowMessage(targetType: ChirTypeRef): String =
        "The result would be less than ${boundaryTypeName(targetType)}.Min."

    fun upperBoundOverflowMessage(targetType: ChirTypeRef): String =
        "The result would be greater than ${boundaryTypeName(targetType)}.Max."

    /** 收集可能抛出整数溢出异常的所有目标边界文本，供 module 统一创建静态 String 对象。 */
    fun throwingMessages(functions: Collection<ChirFunctionDeclaration>): Set<String> = buildSet {
        functions.forEach { function ->
            function.blocks.forEach { block ->
                block.expressions.filterIsInstance<ChirOtherExpression>().forEach { expression ->
                    if (ChirOtherOperation.parse(expression.operation) != ChirOtherOperation.CANGJIE_TYPE_CAST_THROWING) {
                        return@forEach
                    }
                    val source = expression.operands.singleOrNull()?.type?.let(::domain) ?: return@forEach
                    val resultType = expression.resultType ?: return@forEach
                    val target = domain(resultType) ?: return@forEach
                    if (lowerBoundCanOverflow(source, target)) {
                        add(lowerBoundOverflowMessage(resultType))
                    }
                    if (upperBoundCanOverflow(source, target)) {
                        add(upperBoundOverflowMessage(resultType))
                    }
                }
            }
        }
    }

    private fun boundaryTypeName(typeRef: ChirTypeRef): String =
        ((typeRef as? ChirResolvedTypeRef)?.type as? ChirPrimitiveType)
            ?.let(LlvmRawArrayRuntime::primitiveTypeInfoName)
            ?: typeRef.renderName
}

/** 整数 throwing cast 所需的静态 Cangjie String 和异常 runtime ABI。 */
internal data class LlvmOverflowExceptionRuntimePlan(
    val globalDeclarations: List<String>,
    val functionDeclarations: List<String>,
    val runtimeFunctionAbis: List<LlvmFunctionAbi>,
    val overriddenImportedFunctionSymbols: Set<String>,
)

/** 按官方 Cangjie String/RawArray 布局准备溢出异常消息与抛出调用。 */
internal object LlvmOverflowExceptionRuntime {
    const val CREATE_OVERFLOW_EXCEPTION = "rt\$CreateOverflowException_msg"
    const val THROW_EXCEPTION_INTRINSIC = "llvm.cj.throw.exception"

    fun plan(messages: Set<String>): LlvmOverflowExceptionRuntimePlan {
        if (messages.isEmpty()) {
            return LlvmOverflowExceptionRuntimePlan(emptyList(), emptyList(), emptyList(), emptySet())
        }

        return LlvmOverflowExceptionRuntimePlan(
            globalDeclarations = emptyList(),
            functionDeclarations = listOf(
                "declare ptr addrspace(1) @\"$CREATE_OVERFLOW_EXCEPTION\"(" +
                    "ptr addrspace(1))",
                "declare void @$THROW_EXCEPTION_INTRINSIC(ptr addrspace(1))",
            ),
            runtimeFunctionAbis = listOf(
                LlvmFunctionAbi(CREATE_OVERFLOW_EXCEPTION, "ptr addrspace(1)", listOf("ptr addrspace(1)")),
                LlvmFunctionAbi(THROW_EXCEPTION_INTRINSIC, "void", listOf("ptr addrspace(1)")),
            ),
            overriddenImportedFunctionSymbols = setOf(CREATE_OVERFLOW_EXCEPTION),
        )
    }

    fun throwInstructions(
        function: org.cangnova.cangjie.codegen.function.CGFunction,
        sourceId: ChirSemanticId,
        temporaryOrdinal: Int,
        message: String,
    ): List<String> {
        val exception = function.intermediateRef(sourceId, temporaryOrdinal)
        val stringLiteral = LlvmStringLiteralRuntime.literalReference(message)
        return listOf(
            "  $exception = call ptr addrspace(1) @\"$CREATE_OVERFLOW_EXCEPTION\"(" +
                "ptr addrspace(1) addrspacecast (ptr $stringLiteral to ptr addrspace(1)))",
            "  call void @$THROW_EXCEPTION_INTRINSIC(ptr addrspace(1) $exception)",
            "  unreachable",
        )
    }

}
