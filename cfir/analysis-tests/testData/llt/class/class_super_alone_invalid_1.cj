

open class C0 {}

open class C1 <: C0 {
    public var x = <!ILLEGAL_SUPER_ALONE!>super<!>
}

main(): Int64 {
    return 1
}
