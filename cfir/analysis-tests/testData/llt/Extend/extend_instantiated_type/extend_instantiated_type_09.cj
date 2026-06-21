

class B<T> {}

class A<T> {}

extend<T> A<B<T>> {
    static func test(a: B<T>) {
        11
    }
}

extend A<B<Int64>> {
    static func foo() {
        return test(B<Int64>())
    }
}

main() {
    let v = A<B<Int64>>.foo()
    if (v != 11) {
        return 1
    }
    return 0
}
