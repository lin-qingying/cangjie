

class C <: A {
    public init() {
        a = 0
        b = 0
    }
    public init(c: Int32) {
        a = 0
        b = 0
    }
}

open class A {
    public var a: Int32 = 1
    <!CLASS_UNINITIALIZED_FIELD!>public let b: Int32<!>
}

main(): Int64 {
    return 0
}
