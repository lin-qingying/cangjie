package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.types.ConeCangjieType

/**
 * 隐式返回类型推断的计算状态。
 *
 * 三状态机：NotComputed → Computing → Computed，
 * 用于递归依赖检测和缓存已计算的结果。
 *
 * 参考 K2 ImplicitBodyResolveComputationStatus。
 */
sealed class CfirImplicitBodyResolveComputationStatus {
    /** 尚未开始计算 */
    object NotComputed : CfirImplicitBodyResolveComputationStatus()

    /** 正在计算中（用于递归检测） */
    object Computing : CfirImplicitBodyResolveComputationStatus()

    /** 已完成计算，缓存解析后的类型和变换后的声明 */
    class Computed(
        val resolvedType: ConeCangjieType,
        val transformedDeclaration: CfirCallableDeclaration,
    ) : CfirImplicitBodyResolveComputationStatus()
}

/**
 * 隐式类型推断计算会话。
 *
 * 管理所有可调用声明的计算状态，提供递归保护：
 * - 首次计算：NotComputed → Computing → Computed
 * - 递归访问：检测到 Computing 状态 → 返回错误类型
 * - 重复访问：Computed 状态 → 直接返回缓存结果
 *
 * 参考 K2 ImplicitBodyResolveComputationSession。
 */
class CfirImplicitBodyResolveComputationSession {

    private val statusMap = HashMap<CfirCallableSymbol<*>, CfirImplicitBodyResolveComputationStatus>()

    /** 当前正在计算的符号栈，用于调试和错误报告 */
    private val computingSymbolsStack = mutableListOf<CfirCallableSymbol<*>>()

    /** 查询符号的当前计算状态 */
    fun getStatus(symbol: CfirCallableSymbol<*>): CfirImplicitBodyResolveComputationStatus {
        return statusMap[symbol] ?: CfirImplicitBodyResolveComputationStatus.NotComputed
    }

    /**
     * 执行计算并缓存结果。
     *
     * 1. 标记为 Computing 并压栈
     * 2. 执行 [transformation] 获得变换后的声明
     * 3. 提取返回类型并缓存为 Computed
     * 4. 弹栈
     *
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

    /** 从变换后的声明中提取已解析的返回类型 */
    private fun extractResolvedType(declaration: CfirCallableDeclaration): ConeCangjieType {
        val typeRef = when (declaration) {
            is org.cangnova.cangjie.cfir.declarations.CfirFunction -> declaration.returnTypeRef
            is org.cangnova.cangjie.cfir.declarations.CfirProperty -> declaration.returnTypeRef
            is org.cangnova.cangjie.cfir.declarations.CfirVariable -> declaration.returnTypeRef
            else -> return org.cangnova.cangjie.cfir.types.ConeErrorType("unsupported declaration for implicit type")
        }
        return if (typeRef is org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef) {
            typeRef.coneType
        } else {
            org.cangnova.cangjie.cfir.types.ConeErrorType("type not resolved after transformation")
        }
    }
}
