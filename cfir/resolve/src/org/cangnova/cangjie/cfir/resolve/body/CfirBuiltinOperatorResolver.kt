package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.name.Name

/**
 * 鍐呭缓鎿嶄綔绗﹁В鏋愬櫒銆? *
 * 褰?`transformFunctionCall` 鐨勮皟鐢ㄨВ鏋愯繑鍥?`NoCandidate` 鏃讹紝浣滀负鍥為€€璺緞锛? * 妫€鏌ヨ皟鐢ㄦ槸鍚︿负宸茬煡鐨勫唴寤烘搷浣滅锛堟帴鏀惰€呬负鍘熷绫诲瀷 + 鍑芥暟鍚嶄负鎿嶄綔绗﹀悕锛夛紝
 * 鑻ュ尮閰嶅垯鐩存帴杩斿洖鍐呭缓鎿嶄綔绗︾殑缁撴灉绫诲瀷銆? *
 * 瑕嗙洊鑼冨洿锛? * - 绠楁湳锛歱lus, minus, times, div, rem, pow
 * - 浣嶈繍绠楋細and, or, xor, shl, shr
 * - 姣旇緝锛歟qual, notEqual, less, greater, lessEqual, greaterEqual
 * - 涓€鍏冿細unaryMinus, unaryPlus, not, inv, inc, dec, postInc, postDec
 *
 * IdealInt/IdealFloat 鍜屾贩鍚堝搴﹁鍒欙細
 * - IdealInt op IdealInt 鈫?IdealInt
 * - IdealFloat op IdealFloat 鈫?IdealFloat
 * - IdealInt op 鍏蜂綋鏁存暟 鈫?鍏蜂綋鏁存暟锛堥殣寮忔嫇瀹斤級
 * - IdealFloat op 鍏蜂綋娴偣 鈫?鍏蜂綋娴偣
 * - 娣峰悎瀹藉害锛欼nt32 op Int64 鈫?Int64锛堝彇杈冨绫诲瀷锛? */
object CfirBuiltinOperatorResolver {

    /** 浜屽厓绠楁湳鎿嶄綔绗﹀悕绉?*/
    private val ARITHMETIC_OPS = setOf(
        "plus", "minus", "times", "div", "rem", "pow",
    )

    /** 浜屽厓浣嶆搷浣滅鍚嶇О */
    private val BITWISE_OPS = setOf("and", "or", "xor")

    /** 绉讳綅鎿嶄綔绗﹀悕绉?*/
    private val SHIFT_OPS = setOf("shl", "shr")

    /** 姣旇緝鎿嶄綔绗﹀悕绉帮紙瀵瑰簲 CfirComparisonOp锛?*/
    private val COMPARISON_OPS = setOf("equal", "notEqual", "less", "greater", "lessEqual", "greaterEqual")

    /** 涓€鍏冩暟鍊兼搷浣滅鍚嶇О */
    private val UNARY_NUMERIC_OPS = setOf(
        "unaryMinus", "unaryPlus", "inc", "dec", "postInc", "postDec",
    )

    /** 涓€鍏冩暣鏁版搷浣滅鍚嶇О */
    private val UNARY_INTEGER_OPS = setOf("inv")

    /**
     * 灏濊瘯瑙ｆ瀽鍐呭缓鎿嶄綔绗﹁皟鐢ㄣ€?     *
     * @param name 鍑芥暟鍚嶏紙濡?"plus"銆?unaryMinus"锛?     * @param receiverType 鎺ユ敹鑰呯被鍨嬶紙濡?Int64銆両dealInt锛?     * @param argumentTypes 鍙傛暟绫诲瀷鍒楄〃锛堜竴鍏冩搷浣滅涓虹┖鍒楄〃锛?     * @return 鎿嶄綔绗﹁繑鍥炵被鍨嬶紝鑻ラ潪鍐呭缓鎿嶄綔绗﹀垯杩斿洖 null
     */
    fun tryResolveBuiltinOperator(
        name: Name,
        receiverType: ConeCangjieType?,
        argumentTypes: List<ConeCangjieType>,
    ): ConeCangjieType? {
        if (receiverType == null) return null
        val opName = name.asString()

        return when {
            // 浜屽厓绠楁湳鎿嶄綔绗︼細鎺ユ敹鑰呭拰鍙傛暟鍧囦负鏁板€肩被鍨
            opName in ARITHMETIC_OPS && argumentTypes.size == 1 ->
                resolveArithmeticOp(receiverType, argumentTypes.first())

            // 浜屽厓浣嶆搷浣滅锛氭帴鏀惰€呭拰鍙傛暟鍧囦负鏁存暟绫诲瀷
            opName in BITWISE_OPS && argumentTypes.size == 1 ->
                resolveBitwiseOp(receiverType, argumentTypes.first())

            // 绉讳綅鎿嶄綔绗︼細鎺ユ敹鑰呬负鏁存暟绫诲瀷
            opName in SHIFT_OPS && argumentTypes.size == 1 ->
                resolveShiftOp(receiverType)

            // 姣旇緝鎿嶄綔绗︼細涓ょ鍙吋瀹圭殑绫诲瀷锛堥€氳繃鎷撳瑙勫垯锛
            opName in COMPARISON_OPS && argumentTypes.size == 1 ->
                resolveComparisonOp(receiverType, argumentTypes.first())

            // 涓€鍏冩暟鍊兼搷浣滅锛氭帴鏀惰€呬负鏁板€肩被鍨
            opName in UNARY_NUMERIC_OPS && argumentTypes.isEmpty() ->
                resolveUnaryNumericOp(receiverType)

            // 涓€鍏冩暣鏁版搷浣滅锛坕nv锛夛細鎺ユ敹鑰呬负鏁存暟绫诲瀷
            opName in UNARY_INTEGER_OPS && argumentTypes.isEmpty() ->
                resolveUnaryIntegerOp(receiverType)

            // not 鎿嶄綔绗︼細鎺ユ敹鑰呬负 Bool 绫诲瀷
            opName == "not" && argumentTypes.isEmpty() ->
                resolveNotOp(receiverType)

            else -> null
        }
    }

    /** 绠楁湳鎿嶄綔绗﹁В鏋愶細鏁板€肩被鍨嬩簩鍏冭繍绠?*/
    private fun resolveArithmeticOp(
        receiverType: ConeCangjieType,
        argType: ConeCangjieType,
    ): ConeCangjieType? {
        if (!receiverType.isNumericType || !argType.isNumericType) return null
        return resolveNumericPromotion(receiverType, argType)
    }

    /** 浣嶆搷浣滅瑙ｆ瀽锛氭暣鏁扮被鍨嬩簩鍏冭繍绠?*/
    private fun resolveBitwiseOp(
        receiverType: ConeCangjieType,
        argType: ConeCangjieType,
    ): ConeCangjieType? {
        if (!receiverType.isIntegerType || !argType.isIntegerType) return null
        return resolveNumericPromotion(receiverType, argType)
    }

    /** 绉讳綅鎿嶄綔绗﹁В鏋愶細鎺ユ敹鑰呬负鏁存暟绫诲瀷锛岃繑鍥炴帴鏀惰€呯被鍨?*/
    private fun resolveShiftOp(receiverType: ConeCangjieType): ConeCangjieType? {
        if (!receiverType.isIntegerType) return null
        return receiverType
    }

    /** 涓€鍏冩暟鍊兼搷浣滅瑙ｆ瀽 */
    private fun resolveUnaryNumericOp(receiverType: ConeCangjieType): ConeCangjieType? {
        if (!receiverType.isNumericType) return null
        return receiverType
    }

    /** 涓€鍏冩暣鏁版搷浣滅锛坕nv锛夎В鏋?*/
    private fun resolveUnaryIntegerOp(receiverType: ConeCangjieType): ConeCangjieType? {
        if (!receiverType.isIntegerType) return null
        return receiverType
    }

    /**
     * 姣旇緝鎿嶄綔绗﹁В鏋愶細浠绘剰鍙吋瀹圭殑绫诲瀷閮借繑鍥?Bool銆?     *
     * - 涓ょ鍧囦负鏁板€肩被鍨?鈫?Bool锛堥€氳繃鎷撳瑙勫垯鍒ゆ柇鍏煎鎬э級
     * - 涓ょ鍧囦负 Bool 鈫?Bool锛堜粎 equal/notEqual锛?     * - 涓ょ鍧囦负 Rune 鈫?Bool
     * - 涓ょ鍧囦负 String 鈫?Bool
     * - 鍏朵粬 鈫?null锛堥潪鍐呭缓姣旇緝锛?     */
    private fun resolveComparisonOp(
        receiverType: ConeCangjieType,
        argType: ConeCangjieType,
    ): ConeCangjieType? {
        // 涓ょ鍧囦负鏁板€肩被鍨嬶紙鏁存暟鎴栨诞鐐癸紝鍚 IdealInt/IdealFloat锛
        if (receiverType.isNumericType && argType.isNumericType) {
            return ConePrimitiveType.BOOLEAN
        }

        // 涓ょ鍧囦负 Bool锛堜粎鏀寔 equal/notEqual锛
        if (receiverType.isBoolean && argType.isBoolean) {
            return ConePrimitiveType.BOOLEAN
        }

        // 涓ょ鍧囦负 Rune
        if (receiverType.isRune && argType.isRune) {
            return ConePrimitiveType.BOOLEAN
        }

        // 涓ょ鍧囦负 String
        if (receiverType.isString && argType.isString) {
            return ConePrimitiveType.BOOLEAN
        }

        // 闈炲唴寤烘瘮杈
        return null
    }

    private fun resolveNotOp(receiverType: ConeCangjieType): ConeCangjieType? {
        if (!receiverType.isBoolean) return null
        return ConePrimitiveType.BOOLEAN
    }

    /**
     * 鏁板€肩被鍨嬫彁鍗囧拰鎷撳瑙勫垯銆?     *
     * IdealType 瑙勫垯锛堜繚鎸佷笉鍙橈級锛?     * - IdealInt op IdealInt 鈫?IdealInt
     * - IdealFloat op IdealFloat 鈫?IdealFloat
     * - IdealInt op 鍏蜂綋鏁存暟 鈫?鍏蜂綋鏁存暟
     * - IdealFloat op 鍏蜂綋娴偣 鈫?鍏蜂綋娴偣
     *
     * 娣峰悎瀹藉害瑙勫垯锛堟柊澧烇級锛?     * - Int32 op Int64 鈫?Int64锛堝彇杈冨绫诲瀷锛?     * - 涓嶅吋瀹规椂杩斿洖鎺ユ敹鑰呯被鍨?     */
    private fun resolveNumericPromotion(
        receiverType: ConeCangjieType,
        argType: ConeCangjieType,
    ): ConeCangjieType {
        val receiverIsIdeal = receiverType.isIdealType
        val argIsIdeal = argType.isIdealType

        return when {
            // IdealType 瑙勫垯锛堜繚鎸佷笉鍙橈級
            receiverIsIdeal && argIsIdeal -> receiverType
            receiverIsIdeal -> argType
            argIsIdeal -> receiverType
            // 鏂板锛氭贩鍚堝搴﹁鍒欙紝鍙栧悓鏃忎腑杈冨鐨勭被鍨
            receiverType is ConePrimitiveType && argType is ConePrimitiveType -> {
                val wider = ConeNumericWidening.widerOf(receiverType.kind, argType.kind)
                if (wider != null) ConePrimitiveType(wider) else receiverType
            }
            // 闈炲師濮嬫暟鍊肩被鍨嬭繑鍥炴帴鏀惰€呯被鍨
            else -> receiverType
        }
    }
}

