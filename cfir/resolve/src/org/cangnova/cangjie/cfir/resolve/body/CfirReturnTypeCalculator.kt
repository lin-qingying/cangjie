package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangjieType
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef

/**
 * 声明返回类型计算器。
 *
 * 负责计算函数/属性等可调用声明的返回类型，
 * 是隐式类型推断（IMPLICIT_TYPES 阶段）的核心基础设施。
 *
 * Phase 2 提供默认实现（直接返回已解析的显式类型），
 * Phase 3 将实现完整的隐式返回类型推断。
 *
 * 参考 K2 ReturnTypeCalculator / ReturnTypeCalculatorForFullBodyResolve。
 */
interface CfirReturnTypeCalculator {

    /**
     * 计算可调用声明的返回类型。
     *
     * @return 已解析的返回类型，或 null（如果无法计算）
     */
    fun tryCalculateReturnType(declaration: CfirCallableDeclaration): ConeCangjieType?

    /**
     * 计算可调用声明的返回类型引用。
     *
     * 默认实现基于 [tryCalculateReturnType] 构建 [CfirResolvedTypeRef]，
     * 子类可覆写以提供更高效的实现。
     *
     * @return 已解析的类型引用，或 null（如果无法计算）
     */
    fun tryCalculateReturnTypeRef(declaration: CfirCallableDeclaration): CfirTypeRef? {
        val type = tryCalculateReturnType(declaration) ?: return null
        return buildResolvedTypeRef { coneType = type }
    }

    /**
     * Phase 2 默认实现：不做推断，直接使用已有的显式类型。
     */
    object Default : CfirReturnTypeCalculator {
        override fun tryCalculateReturnType(declaration: CfirCallableDeclaration): ConeCangjieType? = null
    }
}
