

struct A {
    public let b: Int64 = 1
    public init() {
        <!CANNOT_ASSIGN_TO_IMMUTABLE!>b<!> = 1
        <!CANNOT_ASSIGN_TO_IMMUTABLE!>this.b<!> = 2
    }
}

main(): Int64 {
    0
}
