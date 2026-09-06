


interface I1 {
    prop foo: Int64 {
        get() {
            return 1
        }
    }
}

interface I2 {
    prop foo: Int32 {
        get() {
            return 2
        }
    }
}

class C1 {}

<!INHERIT_MEMBER_TYPE_INCONSISTENT!>extend<!> C1 <: I1 & I2 {
    public prop <!PROPERTY_OVERRIDE_IMPLEMENT_TYPE_DIFF!>foo<!>: Int64 {
        get() {
            return 2
        }
    }
}

main() {
    return -1
}
