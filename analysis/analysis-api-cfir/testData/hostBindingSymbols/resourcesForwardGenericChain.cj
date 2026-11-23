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

func useForwardGenericChain(): Unit {
    try (a = <caret_toLater>b, b = Box(<caret_toLast>c.value()), c = ManagedResource()) {
        let one: Int64 = <caret_firstUse>a.<caret_firstGet>get()
        let two: Int64 = <caret_laterUse>b.get()
        let three: Int64 = <caret_lastUse>c.value()
        println(one + two + three)
    }
}
