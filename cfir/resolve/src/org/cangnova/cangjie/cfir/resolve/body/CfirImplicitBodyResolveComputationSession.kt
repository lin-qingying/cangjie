package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.types.ConeCangjieType

/**
 * 隐式返回类型推断的计算状态。
 * 三态状态机 `NotComputed -> Computing -> Computed` 用于检测递归依赖并缓存结果。
 */
sealed class CfirImplicitBodyResolveComputationStatus {
    /** 尚未开始计算。 */
    object NotComputed : CfirImplicitBodyResolveComputationStatus()

    /** 正在计算中，用于递归检测。 */
    object Computing : CfirImplicitBodyResolveComputationStatus()

    /** 已完成计算，并缓存解析后的类型与声明。 */
    class Computed(
        val resolvedType: ConeCangjieType,
        val transformedDeclaration: CfirCallableDeclaration,
    ) : CfirImplicitBodyResolveComputationStatus()
}

/**
 * 隐式类型推断计算会话。
 * 负责管理所有可调用声明的计算状态，并提供递归保护与结果缓存。
 */
class CfirImplicitBodyResolveComputationSession {

    private val statusMap = HashMap<CfirCallableSymbol<*>, CfirImplicitBodyResolveComputationStatus>()

    /** 当前正在计算的符号栈，用于调试和错误报告。 */
    private val computingSymbolsStack = mutableListOf<CfirCallableSymbol<*>>()

    /** 查询符号当前的计算状态。 */
    fun getStatus(symbol: CfirCallableSymbol<*>): CfirImplicitBodyResolveComputationStatus {
        return statusMap[symbol] ?: CfirImplicitBodyResolveComputationStatus.NotComputed
    }

    /**
     * 执行计算并缓存结果。
     * @param symbol 被计算的符号
     * @param transformation 实际执行 body resolve 的变换闭包
     * @return 变换后的声明
     */
    fun <D : CfirCallableDeclaration> compute(
        symbol: CfirCallableSymbol<*>,
        transformation: () -> D,
    ): D {
        statusMap[symbol] = CfirImplicitBodyResolveComputationStatus.Computing
        computingSymbolsStack.add(symbol)
        return try {
            val result = transformation()
            val resolvedType = extractResolvedType(result)
            statusMap[symbol] = CfirImplicitBodyResolveComputationStatus.Computed(resolvedType, result)
            result
        } finally {
            computingSymbolsStack.removeLastOrNull()
        }
    }

    /** 从变换后的声明中提取已解析的返回类型。 */
    private fun extractResolvedType(declaration: CfirCallableDeclaration): ConeCangjieType {
        val typeRef = when (declaration) {
            is org.cangnova.cangjie.cfir.declarations.CfirFunction -> declaration.returnTypeRef
            is org.cangnova.cangjie.cfir.declarations.CfirProperty -> declaration.returnTypeRef
            is org.cangnova.cangjie.cfir.declarations.CfirFieldVariable -> declaration.returnTypeRef
            is org.cangnova.cangjie.cfir.declarations.CfirPatternVariable -> declaration.returnTypeRef
            else -> return org.cangnova.cangjie.cfir.types.ConeErrorType("unsupported declaration for implicit type")
        }
        return if (typeRef is org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef) {
            typeRef.coneType
        } else {
            org.cangnova.cangjie.cfir.types.ConeErrorType("type not resolved after transformation")
        }
    }
}

