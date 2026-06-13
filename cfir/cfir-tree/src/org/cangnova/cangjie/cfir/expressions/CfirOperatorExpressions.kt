package org.cangnova.cangjie.cfir.expressions

enum class CfirBinaryOpKind(val symbol: String) {
    AND("&&"),
    OR("||"),
    COALESCING("??"),
    PIPELINE("|>"),
    COMPOSITION("~>"),
}

enum class CfirComparisonOp(val symbol: String) {
    LT("<"), GT(">"), LE("<="), GE(">="), EQ("=="), NE("!="),
}
