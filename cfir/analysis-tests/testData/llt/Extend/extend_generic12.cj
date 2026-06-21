

main(): Int64 {
    A<Int64>().<!NOT_MEMBER_OF!>f<!>
    if (A<Int64>().<!NOT_MEMBER_OF!>a<!> != 1) {
        return 1
    }
    C<Int64>().a.<!NOT_MEMBER_OF!>a<!>
    C<Int64>().a.<!NOT_MEMBER_OF!>f<!>
    return 0
}

class A<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>> where T <: Int64 {}

extend<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>> A<T> where T <: String {
    public prop a: Int64 {
        get() {
            return 1
        }
    }
    public func f() {
        2
    }
}

class C<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>> where T <: Int64 {
    public var a: A<T> = A<T>()
}
