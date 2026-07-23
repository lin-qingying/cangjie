

struct A {
    public let b: Int64 = 1
    public init() {
        <!CANNOT_ASSIGN_TO_IMMUTABLE!>b<!> = 1
        this.<!CANNOT_ASSIGN_TO_IMMUTABLE!>b<!> = 2
    }
}

main(): Int64 {
    0
}
