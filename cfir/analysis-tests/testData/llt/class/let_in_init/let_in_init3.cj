

class A {
    <!TYPE_UNINITIALIZED_STATIC_FIELD!>public static let b: Float32<!>
    public init(bb: Float32) {
        <!CANNOT_ASSIGN_TO_IMMUTABLE!>A.b = bb<!>
    }
}

main() {
    let y: Float32 = 9.0
    var a = A(y)
    return 0
}
