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
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangjieType
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef

/**
 * 澹版槑杩斿洖绫诲瀷璁＄畻鍣ㄣ€? *
 * 璐熻矗璁＄畻鍑芥暟/灞炴€х瓑鍙皟鐢ㄥ０鏄庣殑杩斿洖绫诲瀷锛? * 鏄殣寮忕被鍨嬫帹鏂紙IMPLICIT_TYPES 闃舵锛夌殑鏍稿績鍩虹璁炬柦銆? *
 * Phase 2 鎻愪緵榛樿瀹炵幇锛堢洿鎺ヨ繑鍥炲凡瑙ｆ瀽鐨勬樉寮忕被鍨嬶級锛? * Phase 3 灏嗗疄鐜板畬鏁寸殑闅愬紡杩斿洖绫诲瀷鎺ㄦ柇銆? *
 * 鍙傝€?K2 ReturnTypeCalculator / ReturnTypeCalculatorForFullBodyResolve銆? */
interface CfirReturnTypeCalculator {

    /**
     * 璁＄畻鍙皟鐢ㄥ０鏄庣殑杩斿洖绫诲瀷銆?     *
     * @return 宸茶В鏋愮殑杩斿洖绫诲瀷锛屾垨 null锛堝鏋滄棤娉曡绠楋級
     */
    fun tryCalculateReturnType(declaration: CfirCallableDeclaration): ConeCangjieType?

    /**
     * 璁＄畻鍙皟鐢ㄥ０鏄庣殑杩斿洖绫诲瀷寮曠敤銆?     *
     * 榛樿瀹炵幇鍩轰簬 [tryCalculateReturnType] 鏋勫缓 [CfirResolvedTypeRef]锛?     * 瀛愮被鍙鍐欎互鎻愪緵鏇撮珮鏁堢殑瀹炵幇銆?     *
     * @return 宸茶В鏋愮殑绫诲瀷寮曠敤锛屾垨 null锛堝鏋滄棤娉曡绠楋級
     */
    fun tryCalculateReturnTypeRef(declaration: CfirCallableDeclaration): CfirTypeRef? {
        val type = tryCalculateReturnType(declaration) ?: return null
        val delegatedTypeRef = declaration.returnTypeRefOrNull
        return buildResolvedTypeRef {
            source = delegatedTypeRef?.source
            coneType = type
            this.delegatedTypeRef = delegatedTypeRef
        }
    }

    /**
     * Phase 2 榛樿瀹炵幇锛氫笉鍋氭帹鏂紝鐩存帴浣跨敤宸叉湁鐨勬樉寮忕被鍨嬨€?     */
    object Default : CfirReturnTypeCalculator {
        override fun tryCalculateReturnType(declaration: CfirCallableDeclaration): ConeCangjieType? = null
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
        is CfirVariable -> returnTypeRef
        is CfirPatternVariable -> returnTypeRef
        is CfirValueParameter -> returnTypeRef
    }

