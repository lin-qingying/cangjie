

main() {
    let a1 = 1.0 <!INVALID_BINARY_OPERATOR!>**<!> Int32(-1.0)
    let a2: UInt64 = <!TYPE_MISMATCH!>1.0 ** -1.0<!>
    let c = Float16(0.0) <!INVALID_BINARY_OPERATOR!>**<!> 0.1
    let d = -1.0 <!INVALID_BINARY_OPERATOR!>**<!> r'c'
    // no need to test more primitive types
    let e1 = A() <!INVALID_BINARY_OPERATOR!>**<!> 0.0
    let e2 = 0.0 <!INVALID_BINARY_OPERATOR!>**<!> A()
    0
}

class A {}
