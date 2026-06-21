

struct R<T> {
    public func foo1(a: T): Int64 {
        return 1
    }
}

extend<T> R<T> {
    public func foo2(b: T): Int64 {
        foo1(b)
        return 2
    }
}

main() {
    var obj = R<Int64>()
    return obj.foo2(1) - 2
}
