


class A<T> {
    public A(let b!: Int32 = 0) {
    }
}

interface I<T> {
    static func foo<T>() {
        1
    }
}

extend<T> A<T> <: I<T> {
    public static func me<T>(a!: () -> Int64 = foo<T>) {
        a()
    }
}

main(): Int64 {
    return A<Int32>.me<Int64>() - 1
}
