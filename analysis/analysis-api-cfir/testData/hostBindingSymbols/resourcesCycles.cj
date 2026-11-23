package test.hosts

class ManagedResource <: Resource {
    public func isClosed(): Bool { true }
    public func close(): Unit {}
    public func value(): Int64 { 1 }
}

class Box<T> <: Resource {
    let item: T
    public init(item: T) { this.item = item }
    public func isClosed(): Bool { true }
    public func close(): Unit {}
    public func get(): T { item }
}

func useImplicitCycle(): Unit {
    try (a = <caret_implicitForward>b, b = <caret_implicitBackward>a) {}
}

func useExplicitCycle(): Unit {
    try (a: ManagedResource = <caret_explicitForward>b, b = <caret_explicitBackward>a) {}
}

func useMemberCycle(): Unit {
    try (a = <caret_memberForward>b.get(), b = Box(<caret_memberBackward>a)) {}
}

func useAnchoredMemberCycle(): Unit {
    try (a = <caret_anchoredForward>b.get(), b: Box<ManagedResource> = Box(<caret_anchoredBackward>a)) {}
}
