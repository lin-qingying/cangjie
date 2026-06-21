

interface I3<T> {
    func foo(a: T): T {
        return a
    }
}

class C<T, V> <: I3<T> & I3<V> {}

class D <: I3<Int32> & I3<Rune> {}

main() {
    let c = C<Int32, String>()
    if (c.foo(11) != 11) {
        return 1
    }
    if (c.foo("str") != "str") {
        return 2
    }

    let d = D()
    if (d.foo(11) != 11) {
        return 3
    }
    if (d.foo(r'c') != r'c') {
        return 4
    }
    return 0
}
