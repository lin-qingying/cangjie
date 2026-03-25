package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFinalizer
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirMacroDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirMainFunction
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.utils.exceptions.errorWithAttachment
import org.cangnova.cangjie.utils.exceptions.withCfirEntry

/**
 * 声明返回类型计算器。
 * 负责计算函数、属性等可调用声明的返回类型，是 IMPLICIT_TYPES 阶段的基础设施。
 * Phase 2 提供默认实现，直接使用已解析的显式类型；
 * Phase 3 再补全完整的隐式返回类型推断。
 * 参考 K2 `ReturnTypeCalculator` / `ReturnTypeCalculatorForFullBodyResolve`。
 */
abstract class CfirReturnTypeCalculator {
//    abstract val callableCopyTypeCalculator: CallableCopyTypeCalculator

    /**
     * 计算可调用声明的返回类型。
     * @return 已解析的返回类型；若无法计算则返回 `null`
     */

    abstract fun tryCalculateReturnTypeOrNull(declaration: CfirCallableDeclaration): CfirResolvedTypeRef?


    fun tryCalculateReturnType(declaration: CfirCallableDeclaration): CfirResolvedTypeRef {
        return tryCalculateReturnTypeOrNull(declaration)
            ?: errorWithAttachment("${this::class.simpleName}: Return type cannot be calculated for ${declaration::class.simpleName}") {
                withCfirEntry("declaration", declaration)
            }
    }

    companion object {
        val Default: CfirReturnTypeCalculator = CfirReturnTypeCalculatorForFullBodyResolve.Default
    }
}

private val CfirCallableDeclaration.returnTypeRefOrNull: CfirTypeRef?
    get() = when (this) {
        is CfirFunction -> returnTypeRef
        is CfirMainFunction -> returnTypeRef
        is CfirMacroDeclaration -> returnTypeRef
        is CfirFinalizer -> returnTypeRef
        is CfirConstructor -> returnTypeRef
        is CfirEnumConstructor -> returnTypeRef
        is CfirProperty -> returnTypeRef
        is CfirFieldVariable -> returnTypeRef
        is CfirPatternVariable -> returnTypeRef
        is CfirVariable -> null
        is CfirValueParameter -> returnTypeRef
    }
