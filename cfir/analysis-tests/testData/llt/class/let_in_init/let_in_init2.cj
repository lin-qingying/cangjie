

class A {
    public let a: Int32 = 0
    public init(aa: Int32) {
        <!CANNOT_ASSIGN_TO_IMMUTABLE!>this.a<!> = aa
    }
}

main() {
    let x: Int32 = 10
    var a = A(x)
    return 0
}
