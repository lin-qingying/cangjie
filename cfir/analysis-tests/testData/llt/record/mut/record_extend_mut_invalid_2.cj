


struct R1 {
    public var a: Int64 = 0
}

extend R1 {
    public func foo1() {
        this.<!CANNOT_MODIFY_VAR!>a<!> = 1
    }

    public func foo2() {
        <!CANNOT_MODIFY_VAR!>a<!> = 1
    }

    public func foo3() {
        this.<!CANNOT_MODIFY_VAR!>a<!> = this.a + 1
    }
}

main() {
    return 1
}
