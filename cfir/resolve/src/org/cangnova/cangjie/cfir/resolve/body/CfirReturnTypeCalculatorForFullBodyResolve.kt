package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangjieType

/**
 * BODY_RESOLVE 闃舵杩斿洖绫诲瀷璁＄畻鍣ㄣ€? *
 * 鐩存帴浠庡０鏄庣殑 returnTypeRef 鎻愬彇宸茶В鏋愮殑绫诲瀷銆? * 鐢ㄤ簬 BODY_RESOLVE 闃舵锛屾鏃舵墍鏈夊０鏄庣殑杩斿洖绫诲瀷宸茬粡鍦? * IMPLICIT_TYPES 闃舵琚帹鏂畬姣曘€? *
 * 鍙傝€?K2 ReturnTypeCalculatorForFullBodyResolve銆? */
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

