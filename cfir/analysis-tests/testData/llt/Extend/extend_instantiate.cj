

class A<T> {}

extend<T> A<T> {
    func add(a: T): T {
        return a
    }
    public func add(): Unit {}
}

main() {
    let a = A<Int32>()
    a.add(1)
    a.add()
    0
}
