package test.hosts

class Box<T> <: Resource {
    let item: T
    public init(item: T) { this.item = item }
    public func isClosed(): Bool { true }
    public func close(): Unit {}
    public func get(): T { item }
}

func acceptInt(value: Int64): Unit { println(value) }

main() {
    let <caret_operationDeclaration>operation = { <caret_parameter>item =>
        try (a = <caret_forward>b, b = Box(<caret_initializerParameter>item)) {
            acceptInt(<caret_resourceUse>a.<caret_resourceGet>get())
        }
        <caret_returnParameter>item
    }
    let <caret_resultDeclaration>result: Int64 = <caret_operationUse>operation(1)
    println(result)
}
