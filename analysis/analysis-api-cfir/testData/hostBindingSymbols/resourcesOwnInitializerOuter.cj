package test.hosts

class ManagedResource <: Resource {
    public func isClosed(): Bool { true }
    public func close(): Unit {}
    public func value(): Int64 { 1 }
}

func useOwnInitializerOuter(): Unit {
    let <caret_outerDeclaration>r = ManagedResource()
    try (r = <caret_selfInitializer>r) {
        println(<caret_resourceUse>r.value())
    }
    println(<caret_afterOuter>r.value())
}
