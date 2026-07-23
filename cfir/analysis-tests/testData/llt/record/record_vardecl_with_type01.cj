

struct RA {
    public let a: Int64 = 10
    public init(a: Int64) {
        this.<!CANNOT_ASSIGN_TO_IMMUTABLE!>a<!> = a
    }
}

main() {
    var ra = RA(5)
    if (ra.a == 5) {
        return 0
    }
    return 1
}
