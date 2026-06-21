

interface I<T> {
    func foo(a: T): T {
        return a
    }
}

class B <: I<Int32> {}

main() {
    var obj = B()
    if (obj.foo(1) == 1) {
        return 0
    }
    return 1
}
