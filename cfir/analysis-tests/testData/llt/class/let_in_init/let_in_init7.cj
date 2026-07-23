

class A {
    public static let b: Float32 = 0.0
    public A(bb: Float32) {
        A.<!CANNOT_ASSIGN_TO_IMMUTABLE!>b<!> = bb
    }
}

main() {
    let y: Float32 = 9.0
    var a = A(y)
    return 0
}
