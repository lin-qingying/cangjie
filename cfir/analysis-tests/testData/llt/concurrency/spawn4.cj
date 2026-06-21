


class MyClass {
    public let value: Int64
    public init(x: Int64) {
        value = x
    }
}

main(): Int64 {
    let fut: Future<MyClass> = spawn {
        =>
        var myClass: MyClass = MyClass(3)
        return myClass
    }
    return 0
}
