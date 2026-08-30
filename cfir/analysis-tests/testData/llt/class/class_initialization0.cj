

class A <: B {
    public var a = <!ILLEGAL_USAGE_OF_SUPER_MEMBER!>b<!>
}

open class B {
    public var b: Int64 = 1
}

main(): Int64 {
    return 0
}
