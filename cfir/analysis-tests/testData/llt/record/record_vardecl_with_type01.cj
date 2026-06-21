

struct RA {
    public let a: Int64 = 10
    public init(a: Int64) {
        <!CANNOT_ASSIGN_TO_IMMUTABLE!>this.a<!> = a
    }
}

main() {
    var ra = RA(5)
    if (ra.a == 5) {
        return 0
    }
    return 1
}
