

open class A {
    public var a: Int64
    public init(n: Int64) {
        a = n
    }
}

<!EXPLICIT_SUPER_CALL_REQUIRED!>class B <: A {}<!>
