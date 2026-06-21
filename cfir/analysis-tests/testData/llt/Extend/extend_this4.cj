

class A {}

class B<T> {}

extend A {
    public func foo() {
        let a: A = this
    }
}

extend<T> B<T> {
    public func foo() {
        let b: B<T> = this
    }
}

main() {
    return 0
}
