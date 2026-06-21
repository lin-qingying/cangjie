

open class A {
    public var a: Int64
    <!CLASS_UNINITIALIZED_FIELD!>public init(n: Int64)<!> {
        if (n > 0) {
            a = n
        }
    }
}
