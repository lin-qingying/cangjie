

struct Data {
    public let a = 1
    public let b = 2
    public init(x: Int64, y: Int64) {
        <!CANNOT_ASSIGN_TO_IMMUTABLE!>a<!> = x
        <!CANNOT_ASSIGN_TO_IMMUTABLE!>b<!> = y
    }
}

main(): Int64 {
    return <!UNRESOLVED_REFERENCE!>run<!>()
}
