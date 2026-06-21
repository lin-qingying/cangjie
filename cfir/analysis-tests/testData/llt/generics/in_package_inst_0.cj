

interface IA<T> {
    func goo(): Unit
}

abstract class A<T> <: IA<T> {
    public func foo() {
        println("foo")
    }

    public open func goo() {
        println("goo")
    }

    public func bar() {
        println("bar")
    }
}

class B <: A<B> {}

func test<T>(t: T) where T <: IA<T> {
    t.goo()
}

main() {
    var b = B()
    test(b)
}
