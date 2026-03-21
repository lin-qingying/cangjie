package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType

/**
 * BODY_RESOLVE 阶段的返回类型计算器。
 * 直接从声明的 `returnTypeRef` 中提取已解析类型。
 * 进入 BODY_RESOLVE 时，所有声明的返回类型都应已在 IMPLICIT_TYPES 阶段完成推断。
 * 参考 K2 `ReturnTypeCalculatorForFullBodyResolve`。
 */
class CfirReturnTypeCalculatorForFullBodyResolve : CfirReturnTypeCalculator {

    override fun tryCalculateReturnType(declaration: CfirCallableDeclaration): ConeCangJieType? {
        val typeRef = extractReturnTypeRef(declaration)
        return if (typeRef is CfirResolvedTypeRef) typeRef.coneType else null
    }

    private fun extractReturnTypeRef(declaration: CfirCallableDeclaration) = when (declaration) {
        is CfirFunction -> declaration.returnTypeRef
        is CfirProperty -> declaration.returnTypeRef
        is CfirFieldVariable -> declaration.returnTypeRef
        is CfirPatternVariable -> declaration.returnTypeRef
        else -> null
    }

    companion object {
        val Default = CfirReturnTypeCalculatorForFullBodyResolve()
    }
}

