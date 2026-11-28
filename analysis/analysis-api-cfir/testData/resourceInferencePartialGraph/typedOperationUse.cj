package test.resourcepartial

class Box2<T> <: Resource {
    let left: T
    let right: T
    public init(left: T, right: T) { this.left = left; this.right = right }
    public func get(): T { left }
    public func isClosed(): Bool { true }
    public func close(): Unit {}
}

func acceptInt(value: Int64): Unit { println(value) }

main() {
    let <caret_operationDeclaration>operation: (Int64, Int64) -> Int64 = { <caret_leftParameter>left, <caret_rightParameter>right =>
        try (a = <caret_resourceReference>b, b = <caret_resourceConstructor>Box2(<caret_initializerLeft>left, <caret_initializerRight>right)) {
            acceptInt(<caret_resourceUse>a.<caret_resourceGet>get())
        }
        <caret_returnLeft>left
    }
    let <caret_resultDeclaration>result: Int64 = <caret_operationUse>operation(1, 2)
    println(result)
}
