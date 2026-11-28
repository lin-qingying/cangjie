package test.hosts

class Box<T> <: Resource {
    let item: T
    public init(item: T) { this.item = item }
    public func isClosed(): Bool { true }
    public func close(): Unit {}
    public func get(): T { item }
}

class Empty<T> <: Resource {
    public func isClosed(): Bool { true }
    public func close(): Unit {}
    public func echo(item: T): T { item }
}

func useConcreteGenericContext(): Unit {
    try (box = Box(1), empty: Empty<Int64> = Empty()) {
        let value: Int64 = <caret_concreteBox>box.get()
        println(<caret_concreteEmpty>empty.echo(value))
    }
}

func useGenericContext<<caret_typeParameter>T>(<caret_parameter>item: T): Unit {
    try (box = Box(<caret_initializerParameter>item), copy = <caret_toBox>box, empty: Empty<T> = Empty()) {
        let <caret_valueDeclaration>value: T = <caret_genericCopy>copy.get()
        <caret_genericEmpty>empty.echo(value)
    }
}
