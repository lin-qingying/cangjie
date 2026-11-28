package test.hosts

class Box<T> <: Resource {
    let item: T
    public init(item: T) { this.item = item }
    public func isClosed(): Bool { true }
    public func close(): Unit {}
    public func get(): T { item }
}

func useGenericForwardMismatch(): Unit {
    try (a: Box<String> = <caret_toLater>b, b = Box(1)) {}
}
