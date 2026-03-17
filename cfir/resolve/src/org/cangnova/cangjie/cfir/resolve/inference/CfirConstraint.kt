package org.cangnova.cangjie.cfir.resolve.inference

import org.cangnova.cangjie.cfir.types.ConeCangjieType

/**
 * 绫诲瀷鎺ㄦ柇绾︽潫銆? *
 * 绾︽潫绯荤粺涓殑鍗曚釜绾︽潫锛屾弿杩扮被鍨嬪彉閲忎笌鍏蜂綋绫诲瀷涔嬮棿鐨勫叧绯汇€? *
 * 瀵归綈 K2 Constraint锛堢畝鍖栦负 2 绉嶏紝鍘绘帀 TypeInEquality 绛夛級銆? */
sealed class CfirConstraint {
    /** 绾︽潫鏉ユ簮浣嶇疆 */
    abstract val position: CfirConstraintPosition
}

/**
 * 瀛愮被鍨嬬害鏉燂細[subType] <: [superType]銆? *
 * 琛ㄧず subType 蹇呴』鏄?superType 鐨勫瓙绫诲瀷銆? * 鑻?subType 鎴?superType 涓寘鍚被鍨嬪彉閲忥紝绾︽潫绯荤粺灏嗘嵁姝ゆ敹闆嗕笂鐣?涓嬬晫銆? */
class CfirSubtypeConstraint(
    val subType: ConeCangjieType,
    val superType: ConeCangjieType,
    override val position: CfirConstraintPosition,
) : CfirConstraint() {
    override fun toString(): String = "$subType <: $superType @ $position"
}

/**
 * 绛変环绾︽潫锛歔left] == [right]銆? *
 * 琛ㄧず涓や釜绫诲瀷蹇呴』瀹屽叏鐩哥瓑銆? * 閫氬父鏉ヨ嚜绫诲瀷鍙傛暟鍑虹幇鍦ㄤ笉鍙樹綅缃殑鍦烘櫙锛堝鏁扮粍鍏冪礌绫诲瀷锛夈€? */
class CfirEqualityConstraint(
    val left: ConeCangjieType,
    val right: ConeCangjieType,
    override val position: CfirConstraintPosition,
) : CfirConstraint() {
    override fun toString(): String = "$left == $right @ $position"
}

