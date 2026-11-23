package test.hosts

class ManagedResource <: Resource {
    public func isClosed(): Bool { false }
    public func close(): Unit {}
}

func useImplicitForwardResources(): Unit {
    try (first = <caret_forward>later, later = ManagedResource()) {
        let one = <caret_firstUse>first
        let two = <caret_laterUse>later
    }
}
