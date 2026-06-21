

class A {
    public var a: Int64 = 1
}

main() {
    <!GENERIC_ARGUMENT_NO_MATCH!>A().a<Bool><!>
    let a1: ?A = A()
    let r1 = <!GENERIC_ARGUMENT_NO_MATCH!>a1?.a<Bool><!>
    return 0
}
