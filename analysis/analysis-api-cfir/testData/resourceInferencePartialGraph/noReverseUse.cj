package test.resourcepartial

class Empty<T> <: Resource {
    public func isClosed(): Bool { true }
    public func close(): Unit {}
}

func acceptInt(value: Int64): Unit { println(value) }

main() {
    let <caret_operationDeclaration>operation: (Int64, Int64) -> Int64 = { <caret_leftParameter>left, <caret_rightParameter>right =>
        try (a: Empty<Int64> = <caret_resourceReference>b, b = <caret_uninferredConstructor>Empty()) {
            acceptInt(<caret_bodyLeft>left)
        }
        <caret_returnLeft>left
    }
    let <caret_resultDeclaration>result: Int64 = <caret_operationUse>operation(1, 2)
    println(result)
}
