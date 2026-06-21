

main() {
    let a: VArray<Int64, $5> = [1, 2, 3, 4, 5]

    let s: Int32 = <!TYPE_MISMATCH!>a.size<!> // error
    <!CANNOT_ASSIGN_TO_IMMUTABLE!>a.size = 1<!> // error
    <!CANNOT_ASSIGN_TO_IMMUTABLE!>a.size = s<!> // error
    a.<!UNRESOLVED_REFERENCE!>sss<!> // error
    0
}
