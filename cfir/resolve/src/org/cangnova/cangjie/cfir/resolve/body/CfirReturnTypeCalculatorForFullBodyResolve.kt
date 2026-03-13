package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangjieType

/**
 * BODY_RESOLVE 阶段返回类型计算器。
 *
 * 直接从声明的 returnTypeRef 提取已解析的类型。
 * 用于 BODY_RESOLVE 阶段，此时所有声明的返回类型已经在
 * IMPLICIT_TYPES 阶段被推断完毕。
 *
 * 参考 K2 ReturnTypeCalculatorForFullBodyResolve。
 */
class CfirReturnTypeCalculatorForFullBodyResolve : CfirReturnTypeCalculator {

    override fun tryCalculateReturnType(declaration: CfirCallableDeclaration): ConeCangjieType? {
        val typeRef = extractReturnTypeRef(declaration)
        return if (typeRef is CfirResolvedTypeRef) typeRef.coneType else null
    }

    private fun extractReturnTypeRef(declaration: CfirCallableDeclaration) = when (declaration) {
        is CfirFunction -> declaration.returnTypeRef
        is CfirProperty -> declaration.returnTypeRef
        is CfirVariable -> declaration.returnTypeRef
        else -> null
    }

    companion object {
        val Default = CfirReturnTypeCalculatorForFullBodyResolve()
    }
}
