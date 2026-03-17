package org.cangnova.cangjie.cfir.resolve.inference

import org.cangnova.cangjie.cfir.resolve.calls.CfirTypeSubstitutor
import org.cangnova.cangjie.cfir.types.ConeCangjieType

/**
 * 娉涘瀷绾︽潫绯荤粺鎺ュ彛銆? *
 * 绠＄悊绫诲瀷鍙橀噺鐨勬敞鍐屻€佺害鏉熸敹闆嗐€佸彉閲忓浐瀹氬拰鏈€缁堟浛鎹㈠櫒鏋勫缓銆? * 浣跨敤 2 绉嶇姸鎬侊紙BUILDING 鈫?COMPLETED锛夛紝绠€鍖?K2 鐨?4 绉嶇姸鎬佹ā鍨嬨€? *
 * 瀵归綈 K2 ConstraintStorage + ConstraintSystemBuilder锛堝悎骞朵负鍗曚竴鎺ュ彛锛夈€? */
interface CfirConstraintSystem : CfirConstraintSystemMarker {

    /** 娉ㄥ唽绫诲瀷鍙橀噺 */
    fun registerTypeVariable(variable: CfirTypeVariable)

    /** 娣诲姞瀛愮被鍨嬬害鏉燂細sub <: super */
    fun addSubtypeConstraint(subType: ConeCangjieType, superType: ConeCangjieType, position: CfirConstraintPosition)

    /** 娣诲姞绛変环绾︽潫锛歭eft == right */
    fun addEqualityConstraint(left: ConeCangjieType, right: ConeCangjieType, position: CfirConstraintPosition)

    /** 鍥哄畾鎸囧畾绫诲瀷鍙橀噺 */
    fun fixVariable(variable: CfirTypeVariable)

    /** 鎸変緷璧栭『搴忓浐瀹氭墍鏈夋湭鍥哄畾鐨勭被鍨嬪彉閲?*/
    fun fixAllVariables()

    /** 浠庡浐瀹氱粨鏋滄瀯寤虹被鍨嬫浛鎹㈠櫒 */
    fun buildResultingSubstitutor(): CfirTypeSubstitutor

    /** 鏄惁瀛樺湪鎺ㄦ柇閿欒 */
    val hasErrors: Boolean

    /** 閿欒淇℃伅鍒楄〃 */
    val errors: List<String>

    /** 鎵€鏈夊凡娉ㄥ唽鐨勭被鍨嬪彉閲?*/
    val typeVariables: List<CfirTypeVariable>

    /** 鎵€鏈夊凡鏀堕泦鐨勭害鏉?*/
    val constraints: List<CfirConstraint>

    companion object {
        /** 鍒涘缓绌虹害鏉熺郴缁熷疄渚?*/
        fun create(): CfirConstraintSystem = CfirConstraintSystemImpl()
    }
}

