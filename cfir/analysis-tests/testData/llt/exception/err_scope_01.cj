


class A <: ToString & Resource {
    public func toString() {
        "A"
    }
    public func close() {}
    public func isClosed() {
        false
    }
}

main() {
    try (x = A()) {
        let <!REDECLARATION!>x<!> = 0
        println(x)
    }
}

