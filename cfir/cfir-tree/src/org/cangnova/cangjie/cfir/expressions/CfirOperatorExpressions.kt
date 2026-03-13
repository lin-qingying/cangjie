package org.cangjie.cfir.expressions

enum class CfirBinaryOpKind {
    AND,
    OR,
    COALESCING,
    PIPELINE,
}

enum class CfirComparisonOp {
    LT, GT, LE, GE, EQ, NE,
}