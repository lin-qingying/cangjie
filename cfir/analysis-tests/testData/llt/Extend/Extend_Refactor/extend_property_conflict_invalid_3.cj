


interface I1 {
    prop foo: Int64 {
        get() {
            return 1
        }
    }
}

interface I2 {
    prop foo: Int64 {
        get() {
            return 2
        }
    }
}

class C1 {}

extend C1 {
    public func <!EXTEND_MEMBER_CANNOT_SHADOW!>foo<!>(): Int64 {
        3
    }
}

<!INTERFACE_MEMBER_MUST_BE_IMPLEMENTED!>e<!>xtend C1 <: I1 {}

<!INTERFACE_MEMBER_MUST_BE_IMPLEMENTED!>e<!>xtend C1 <: I2 {}

main() {
    return -1
}
