package test.hosts

class ManagedResource <: Resource {
    public func isClosed(): Bool { false }
    public func close(): Unit {}
}

func useResources(): Unit {
    try (first = ManagedResource(), second = <caret_following>first) {
        let one = <caret_firstUse>first
        let two = <caret_secondUse>second
    }
}
