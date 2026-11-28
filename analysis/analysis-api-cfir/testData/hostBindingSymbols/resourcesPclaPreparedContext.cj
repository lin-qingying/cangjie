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

func acceptManaged(value: ManagedResource): Unit { println(value.value()) }

main() {
    let <caret_operationDeclaration>operation = { <caret_parameter>item =>
        acceptManaged(<caret_beforeParameter>item)
        try (a = Box({ =>
            let <caret_lambdaDeclaration>item: String = "shadow"
            println(<caret_lambdaUse>item)
            <caret_forward>b.get()
        }), b = Box(<caret_initializerParameter>item)) {
            acceptManaged(<caret_firstUse>a.get()())
            acceptManaged(<caret_laterUse>b.get())
            let <caret_bodyDeclaration>item: String = "body"
            println(<caret_bodyUse>item)
        }
        acceptManaged(<caret_afterParameter>item)
        <caret_returnParameter>item
    }
    let <caret_resultDeclaration>result: ManagedResource = <caret_operationUse>operation(ManagedResource())
    acceptManaged(result)
}
