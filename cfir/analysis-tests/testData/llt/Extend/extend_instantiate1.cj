

class A<T> {
    func foo(a: T): Unit {
    }
}

extend<T> A<T> {
    func add(a: T): T {
        foo(a)
        return a
    }
}

main() {
    let a = A<Int32>()
    a.add(1)
    0
}
