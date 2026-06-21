

open class AAA <: Resource {
    public func close(a: Bool): Unit {}
    public func close(): Unit {}
    public func isClosed(): Bool {
        return true
    }
}

class BBB <: AAA {
    public func close(a: Int64): Unit {}
}

main() {
    try (aaa = AAA()) {
        println("try")
    } finally {
        println("finally")
    }
    try (bbb = BBB()) {
        println("try")
    } finally {
        println("finally")
    }
}
