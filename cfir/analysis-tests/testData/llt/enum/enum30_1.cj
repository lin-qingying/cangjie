

enum Foo {
    A | B | C

    public func f1(v: Foo) {
        <!CANNOT_ASSIGN_TO_IMMUTABLE!>v<!> = B
        <!CANNOT_ASSIGN_TO_IMMUTABLE!>A<!> = B
    }
}

main(): Int64 {
    0
}
