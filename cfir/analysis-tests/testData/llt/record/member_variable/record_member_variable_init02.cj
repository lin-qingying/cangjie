

struct A {
    public static let a: Int64 = A.<!USED_BEFORE_INITIALIZATION!>b<!>
    public static let b: Int64 = A.a
}
