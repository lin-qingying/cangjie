

interface I<T> {
    func test(v: T): Unit {
        let x = A<T>(v)
        let y = x.goo()
        x.foo(y)
    }
}

class A<T> <: I<T> {
    let v: T
    init(a: T) {
        v = a
    }
    func foo(a: T): Unit {}
    func goo(): T {
        v
    }
}

main(): Int64 {
    let ins = A<Int64>(1)
    ins.test(2)
    0
}
