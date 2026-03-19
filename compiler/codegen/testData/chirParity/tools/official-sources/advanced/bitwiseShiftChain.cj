// RUN_PIPELINE_TILL: FRONTEND
main(): Int32 {
    let exprAnd: Int32 = 6 & 3
    let exprXor: Int32 = exprAnd ^ 1
    let exprShl: Int32 = exprXor << 1
    let exprLshr: Int32 = exprShl >> 1
    let exprOr: Int32 = exprLshr | 8
    let exprAshr: Int32 = -8 >> 1
    return exprOr + exprAshr
}


