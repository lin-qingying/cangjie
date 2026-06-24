

main(): Int64 {
    var a = B<Int32>().<!AMBIGUOUS_FUNCTION_CALL!>foo<!>()
    return 0
}

open class A<T> {}

extend<T> A<T> {
    public func foo() {
        0
    }
}

class B<T> <: A<T> {
    public override func <!EXTEND_FUNCTION_CANNOT_OVERRIDDEN!>foo<!>() {
        1
    }
}
