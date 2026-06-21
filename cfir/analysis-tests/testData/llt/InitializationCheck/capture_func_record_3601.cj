

main(): Int64 {
    return 0
}

struct Aoo {
    public var a: Int32 = 1
    public let b: Int32 = a
    public var c = if (true) {
        func foo2() {
            <!USED_BEFORE_INITIALIZATION!>a<!>
        }
    }
    public func foo3() {
        a
    }
}
