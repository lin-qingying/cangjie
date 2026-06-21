

main() {
    var b = R<!TOO_MANY_ARGUMENTS!>(a: 6)<!>
    b.c(7)
    return Int64(b.a - 7)
}

struct R {
    public var a: Int32 = 8
    public mut func cc() {
    }
}

extend R {
    public func c(b: Int32): Int32 {
        <!IMMUTABLE_FUNCTION_CANNOT_ACCESS_MUTABLE_FUNCTION!>cc<!>()
        return 0
    }
}
