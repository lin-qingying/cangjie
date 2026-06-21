

class A {
    public func test() {
        this
    }
}

func f(a: ??A) {}

main() {
    let a = A()
    f(a.test())
}
