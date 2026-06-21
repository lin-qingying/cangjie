

class Test {
    public init() {
        this(true)
    }

    public <!RECURSIVE_CONSTRUCTOR_CALL!>init<!>(a: Bool) {
        this(true)
    }
}

main(): Int64 {
    var test: Test = Test()
    return 0
}
