


func foo(): Int32 {
    let arr1: Int32 = 0
    let arr2: Array<Int32> = [1, 2]
    for (a in <!EXPR_IN_FORIN_MUST_HAS_ITERATOR!>a<!>rr1) {
    }
    for (a in arr2 where <!CANNOT_CONVERT_LITERAL!>1<!>0) {
    }
    return 0
}
