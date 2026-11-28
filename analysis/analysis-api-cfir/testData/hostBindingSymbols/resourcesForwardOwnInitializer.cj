package test.hosts

class Box<T> <: Resource {
    let item: T
    public init(item: T) { this.item = item }
    public func isClosed(): Bool { true }
    public func close(): Unit {}
    public func get(): T { item }
}

func useForwardOwnInitializer(): Unit {
    let <caret_outerDeclaration>r: Int64 = 7
    try (a = <caret_toLater>r, r = Box(<caret_selfInitializer>r)) {
        let one: Int64 = <caret_firstUse>a.get()
        let two: Int64 = <caret_itemUse>r.get()
        println(one + two)
    }
    println(<caret_afterOuter>r)
}
