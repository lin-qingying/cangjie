


class A <: Resource {
    public func isClosed() {
        throw Exception("isClosed")
    }
    public func close() {
        throw Exception("close")
    }
}

main() {
    try (a = A()) {
        throw Exception("try")
    }
}
