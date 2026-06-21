


struct R1<T> {
    <!CLASS_UNINITIALIZED_FIELD!>public var a: T<!>

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
