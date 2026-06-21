

class A <: Resource {
    public func isClosed() {
        print("A!");
        true
    }
    public func close() {}
}

const a: Unit = <!EXPECT_CONST!>try (a: A = A()) {
    0
}<!>

main() {
    print("success!\n")
    return a
}
