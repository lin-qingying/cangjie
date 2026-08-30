

class A {
    public static let <!TYPE_UNINITIALIZED_STATIC_FIELD!>b<!>: Float32
    public init(bb: Float32) {
        A.<!CANNOT_ASSIGN_TO_IMMUTABLE!>b<!> = bb
    }
}

main() {
    let y: Float32 = 9.0
    var a = A(y)
    return 0
}
