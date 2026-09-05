

main() {
    let err = 1 <!INVALID_BINARY_OPERATOR!>+<!> false
    let x = false
    <!CANNOT_ASSIGN_TO_IMMUTABLE, TYPE_MISMATCH!>x++<!> + 1
    <!CANNOT_ASSIGN_TO_IMMUTABLE, TYPE_MISMATCH!>x--<!> * 2

    var y = err
    y++
    y--

    let z = err
    <!CANNOT_ASSIGN_TO_IMMUTABLE!>z++<!>
    <!CANNOT_ASSIGN_TO_IMMUTABLE!>z--<!>

    0
}
