package org.cangnova.cangjie.cfir.expressions

enum class CfirBinaryOpKind(val symbol: String) {
    AND("&&"),
    OR("||"),
    COALESCING("??"),
    PIPELINE("|>"),
}

enum class CfirComparisonOp(val symbol: String) {
    LT("<"), GT(">"), LE("<="), GE(">="), EQ("=="), NE("!="),
}