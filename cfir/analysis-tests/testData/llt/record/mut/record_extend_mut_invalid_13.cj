

main() {
    var b = R<!WRONG_NUMBER_OF_ARGUMENTS!>(a: 6)<!>
    b.c(7)
    return Int64(b.a - 7)
}

struct R {
    public var a: Int32 = 8
    public func cc() {
        <!UNRESOLVED_REFERENCE!>c<!>(1)
    }
}

extend R {
    public mut func c(b: Int32): Int32 {
        return 0
    }
}
