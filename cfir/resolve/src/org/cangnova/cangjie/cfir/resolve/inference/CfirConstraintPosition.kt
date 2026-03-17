package org.cangnova.cangjie.cfir.resolve.inference

/**
 * 绾︽潫鏉ユ簮浣嶇疆锛岃褰曠害鏉熶骇鐢熺殑涓婁笅鏂囥€? *
 * 鐢ㄤ簬璇婃柇淇℃伅锛屽府鍔╁畾浣嶆帹鏂け璐ョ殑鍘熷洜銆? *
 * 瀵归綈 K2 ConstraintPosition锛堢畝鍖栵紝鍘绘帀 lambda/callable-ref 绛変綅缃級銆? */
sealed class CfirConstraintPosition {

    /** 鏉ヨ嚜鍑芥暟鍙傛暟浣嶇疆鐨勭害鏉?*/
    data class ArgumentPosition(val index: Int) : CfirConstraintPosition() {
        override fun toString(): String = "argument[$index]"
    }

    /** 鏉ヨ嚜鏈熸湜杩斿洖绫诲瀷鐨勭害鏉?*/
    data object ExpectedType : CfirConstraintPosition() {
        override fun toString(): String = "expected type"
    }

    /** 鏉ヨ嚜鍑芥暟杩斿洖绫诲瀷鐨勭害鏉?*/
    data object ReturnType : CfirConstraintPosition() {
        override fun toString(): String = "return type"
    }

    /** 鏉ヨ嚜绫诲瀷鍙傛暟涓婄晫鐨勭害鏉?*/
    data object UpperBound : CfirConstraintPosition() {
        override fun toString(): String = "upper bound"
    }
}

