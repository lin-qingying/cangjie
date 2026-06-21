

enum A<T> {
    a(T)

    public func test(x: A<T>): A<T> {
        x
    }
    public func foo() {
        0
    }
    public func test2() {
        this.foo()
        foo()
    }
}

func test<T>() {
    var a: Option<Int64> = 1
}

enum B {
    b

    public func foo() {
        0
    }
    public func test() {
        this.foo()
        foo()
    }
}

main(): Int64 {
    var i = A<Int64>.a(1)
    var x: A<Int64> = i.test(i)
    test<Int64>()
    return b.test()
}
