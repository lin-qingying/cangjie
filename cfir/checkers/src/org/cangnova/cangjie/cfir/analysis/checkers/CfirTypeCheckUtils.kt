package org.cangnova.cangjie.cfir.analysis.checkers

import org.cangnova.cangjie.cfir.types.ConeCangjieType
import org.cangnova.cangjie.cfir.types.ConeSubtypeChecker
import org.cangnova.cangjie.cfir.types.ConeTypeContext

/**
 * 绫诲瀷妫€鏌ュ伐鍏风被銆? *
 * 鎻愪緵瀛愮被鍨嬪垽瀹氬拰绫诲瀷娓叉煋鑳藉姏锛屼緵鍚?checker 鍏辩敤銆? * 浣跨敤 [BasicConeTypeContext] 閬垮厤 checkers鈫抮esolve 寰幆渚濊禆銆? */
object CfirTypeCheckUtils {
    private val subtypeChecker = ConeSubtypeChecker(BasicConeTypeContext)

    /** 鍒ゆ柇 [subType] 鏄惁涓?[superType] 鐨勫瓙绫诲瀷 */
    fun isSubtypeOf(subType: ConeCangjieType, superType: ConeCangjieType): Boolean =
        subtypeChecker.isSubtypeOf(subType, superType)

    /** 娓叉煋绫诲瀷涓哄彲璇诲瓧绗︿覆 */
    fun renderType(type: ConeCangjieType): String = type.toString()

    /**
     * 鍩虹绫诲瀷涓婁笅鏂囥€?     *
     * 浠呮敮鎸佸唴寤鸿鍒欙紙IdealType/Nothing/Any/鐩哥瓑/鍘熷绫诲瀷锛夛紝
     * 涓嶆彁渚涜秴绫诲瀷鍥鹃亶鍘嗐€傜被缁ф壙鍏崇郴鐢卞悗缁寮鸿ˉ榻愩€?     */
    private object BasicConeTypeContext : ConeTypeContext {
        override fun supertypes(type: ConeCangjieType): Collection<ConeCangjieType> = emptyList()
        override fun isSameTypeConstructor(a: ConeCangjieType, b: ConeCangjieType): Boolean = a == b
    }
}

