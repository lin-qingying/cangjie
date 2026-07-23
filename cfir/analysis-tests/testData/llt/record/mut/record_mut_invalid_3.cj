


struct R1<T> {
    public var <!CLASS_UNINITIALIZED_FIELD!>a<!>: T

    public func foo1(x: T) {
        this.<!CANNOT_MODIFY_VAR!>a<!> = x
    }

    public func foo2(x: T) {
        <!CANNOT_MODIFY_VAR!>a<!> = x
    }
}

main() {
    return 1
}
