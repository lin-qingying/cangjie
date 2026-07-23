

class A {
    public let a: Int32 = 0
    public A(aa: Int32) {
        this.<!CANNOT_ASSIGN_TO_IMMUTABLE!>a<!> = aa
    }
}

main() {
    let x: Int32 = 10
    var a = A(x)
    return 0
}
