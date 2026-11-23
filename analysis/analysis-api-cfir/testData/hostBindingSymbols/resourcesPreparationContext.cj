package test.hosts

class Box<T> <: Resource {
    let item: T
    public init(item: T) { this.item = item }
    public func isClosed(): Bool { true }
    public func close(): Unit {}
    public func get(): T { item }
}

func usePreparationContext<<caret_typeParameter>T>(<caret_parameter>item: T): Unit {
    try (a = Box({ =>
        let <caret_lambdaDeclaration>item: String = "lambda"
        println(<caret_lambdaUse>item)
        <caret_toLater>b.get()
    }), b = Box(<caret_initializerParameter>item)) {
        let <caret_valueDeclaration>value: T = <caret_firstUse>a.get()()
        let <caret_otherDeclaration>other: T = <caret_laterUse>b.get()
        Box(value).get()
        Box(other).get()
        let <caret_bodyDeclaration>item: String = "body"
        println(<caret_bodyUse>item)
    }
}
