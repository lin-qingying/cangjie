

interface I<T> {}

class A<T> <: I<T> {}

class B {
    func test<T>(a: Option<I<T>>): This {
        this
    }
}

main() {
    let ins = B()
    let a = A<Int64>()
    ins.test<Int64>(a)
    0
}
