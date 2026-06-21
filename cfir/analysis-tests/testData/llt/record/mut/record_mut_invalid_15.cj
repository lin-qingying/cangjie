

main() {
    return 2
}

struct R <: I {
    public var a: Int32 = 8

    public func c(b: Int32): Int32 {
        let a = <!IMMUTABLE_FUNCTION_CANNOT_ACCESS_MUTABLE_FUNCTION!>cc<!>
        return 0
    }
}

interface I {
    mut func cc() {
    }
}
