

class A<T> {
    private func foo(): Int64 {
        return 1
    }
}

extend<T> A<T> {
    func foo(): Int64 {
        return 2
    }
}

main(): Int64 {
    let a = A<Int16>()
    return a.foo() - 2
}
