

class A {
    public var test: Any = "any"
    public var value: Int64 = 0
    public init() {
        super()
    }
}

main() {
    var testA: Option<A> = A()
    if (let Some(v: String) <- testA?.test) {
        println("1")
    }
    while (let Some(v: String) <- testA?.test) {
        println("2")
        break
    }
    0
}
