

open class A {
    public var a: Int64
    public <!CLASS_UNINITIALIZED_FIELD!>init<!>(n: Int64) {
        if (n > 0) {
            a = n
        }
    }
}
