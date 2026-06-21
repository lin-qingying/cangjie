

open class C0 {}

open class C1 <: C0 {
    public var x = <!ILLEGAL_THIS_OR_SUPER_CALL!>super<!>
}

main(): Int64 {
    return 1
}
