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

func useForwardCapture(): Unit {
    try (a = Box({ => <caret_capture>b }), b = ManagedResource()) {
        let <caret_valueDeclaration>value: ManagedResource = <caret_firstUse>a.get()()
        println(value.value())
        println(<caret_laterUse>b.value())
    }
}
