

class A<T> {}

extend<T> A<T> {
    public func foo() {}
}

main() {
    let a: A<Int32> = A<Int32>()
    a.foo()
    0
}
