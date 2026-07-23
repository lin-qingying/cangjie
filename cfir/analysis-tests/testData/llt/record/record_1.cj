

struct B {
    public let c: Int64
    public let a: Int64 = 1
    public init(c: Int64, a: Int64) {
        this.c = c
        this.<!CANNOT_ASSIGN_TO_IMMUTABLE!>a<!> = a
    }
}

main(): Int64 {
    return 0
}
