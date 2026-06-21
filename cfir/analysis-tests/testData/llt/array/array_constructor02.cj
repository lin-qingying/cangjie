

main(): Int64 {
    let arr0 = Array(3, <!NAMED_PARAMETER_NOT_FOUND!>ite<!>: 3)
    let arr1: Array<Rune> = <!UNABLE_TO_INFER_GENERIC_FUNC!>Array(3, repeat: 3)<!>
    let arr2: Array<Rune> = <!TYPE_MISMATCH!>Array<Int64>(3, repeat: 3)<!>
    return 0
}
