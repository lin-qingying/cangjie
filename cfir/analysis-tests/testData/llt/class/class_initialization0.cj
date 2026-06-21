

class A <: B {
    public var a = <!USED_BEFORE_INITIALIZATION!>b<!>
}

open class B {
    public var b: Int64 = 1
}

main(): Int64 {
    return 0
}
