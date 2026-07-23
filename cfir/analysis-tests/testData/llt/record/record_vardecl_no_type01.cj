

struct RA {
    public let a = 10
    public init(a: Int64) {
        this.<!CANNOT_ASSIGN_TO_IMMUTABLE!>a<!> = a
    }
}

main() {
    return 0
}
