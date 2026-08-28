



func foo(): Int32 {
    let arr1: Int32 = 0
    let arr2: Array<Int32> = [1, 2]
    for (a in <!EXPR_IN_FORIN_MUST_HAS_ITERATOR!>arr1<!>) {
    }
    for (a in arr2 where 10) {
    }
    return 0
}

