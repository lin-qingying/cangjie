

main(): Int64 {
    return 0
}

open class A<T> {}

extend<T> A<T> {
    public func foo() {
        0
    }
}

class B<T> <: A<T> {
    public func <!EXTEND_FUNCTION_CANNOT_OVERRIDDEN!>foo<!>() {
        1
    }
    public operator func ==(right: B<T>): Bool {
        return true
    }
}
