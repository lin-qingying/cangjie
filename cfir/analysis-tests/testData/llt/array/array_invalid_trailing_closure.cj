


main(): Int64 {
    let arr1 = Array<Int64><!WRONG_NUMBER_OF_ARGUMENTS!>() {i => i}<!>

    let arr2 = Array<Int64>(1) {<!UNRESOLVED_REFERENCE!>i<!>}

    let arr3 = Array<Int64><!WRONG_NUMBER_OF_ARGUMENTS!>(1, 2) {i => i}<!>
    return 0
}
