


main() {
    let err = 1 <!INVALID_BINARY_OPERATOR!>+<!> false
    let range11 = err..10 : 1
    let range12 = 0..err : 1
    let range13 = 0..10 : err
    let range14 = err..err : 1
    let range15 = 0..err : err
    let range16 = err..err : err

    let range21 = err..=10 : 2
    let range22 = 0..=err : 2
    let range23 = 0..=10 : err
    let range24 = err..=err : 2
    let range25 = 0..=err : err
    let range26 = err..=err : err

    range11 + false
    range11(true)
    0
}
