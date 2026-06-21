


open class C1 {}

extend C1 {
    public prop foo: Int64 {
        get() {
            return 1
        }
    }
}

class C2 <: C1 {
    public prop <!EXTEND_FUNCTION_CANNOT_OVERRIDDEN!>foo<!>: Int64 {
        get() {
            return 2
        }
    }
}

main() {
    return -1
}
