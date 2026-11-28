package test.hosts

class Empty<T> <: Resource {
    public func isClosed(): Bool { true }
    public func close(): Unit {}
    public func echo(item: T): T { item }
}

func useNoExpectedTypeBackpropagation(): Unit {
    try (a: Empty<Int64> = <caret_toLater>b, b = <caret_uninferredConstructor>Empty()) {}
}
