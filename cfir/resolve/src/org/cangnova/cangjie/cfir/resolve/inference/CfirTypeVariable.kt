package org.cangnova.cangjie.cfir.resolve.inference

import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.types.ConeCangjieType
import org.cangnova.cangjie.cfir.types.ConeTypeParameterLookupTag

/**
 * 绫诲瀷鎺ㄦ柇涓殑绫诲瀷鍙橀噺銆? *
 * 涓烘硾鍨嬭皟鐢ㄦ帹鏂垱寤虹殑涓存椂鍙橀噺锛屾瘡涓被鍨嬪彉閲忓搴斾竴涓０鏄庣殑绫诲瀷鍙傛暟銆? * 绾︽潫绯荤粺鏀堕泦璇ュ彉閲忕殑涓婄晫/涓嬬晫绾︽潫锛屾渶缁堝浐瀹氫负鍏蜂綋绫诲瀷銆? *
 * 瀵归綈 K2 TypeVariable锛堢畝鍖栦负鍗曚竴绫诲瀷锛屽幓鎺?5 绉嶅瓙绫伙級銆? */
data class CfirTypeVariable(
    /** 瀵瑰簲鐨勭被鍨嬪弬鏁扮鍙?*/
    val typeParameter: CfirTypeParameterSymbol,
    /** 鍞竴鏍囪瘑锛堝湪鍚屼竴绾︽潫绯荤粺鍐呭敮涓€锛?*/
    val freshTypeId: Int,
    /** 绫诲瀷鍙傛暟鐨勬煡鎵炬爣绛?*/
    val lookupTag: ConeTypeParameterLookupTag,
) {
    /** 绫诲瀷鍙傛暟鍚嶇О */
    val name: String get() = lookupTag.name

    /** 鏀堕泦鍒扮殑涓婄晫绾︽潫 */
    val upperBounds: MutableList<ConeCangjieType> = mutableListOf()

    /** 鏀堕泦鍒扮殑涓嬬晫绾︽潫 */
    val lowerBounds: MutableList<ConeCangjieType> = mutableListOf()

    /** 宸插浐瀹氱殑鍏蜂綋绫诲瀷锛坣ull 琛ㄧず灏氭湭鍥哄畾锛?*/
    var fixedType: ConeCangjieType? = null

    /** 鏄惁宸插浐瀹?*/
    val isFixed: Boolean get() = fixedType != null

    override fun toString(): String = buildString {
        append("CfirTypeVariable($name#$freshTypeId")
        if (isFixed) append(" = $fixedType")
        else {
            if (lowerBounds.isNotEmpty()) append(" >: ${lowerBounds.joinToString(" | ")}")
            if (upperBounds.isNotEmpty()) append(" <: ${upperBounds.joinToString(" & ")}")
        }
        append(")")
    }
}

