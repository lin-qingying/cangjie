

class X {}

open class A<T> {}

extend<T> A<T> {
    func foo() {
        123
    }
}

extend<T> A<T> where T <: X {
    func coo() {
        234
    }
}

class B <: A<X> {}

main() {
    let b = B()
    if (b.foo() != 123) {
        return 1
    }
    if (b.coo() != 234) {
        return 2
    }
    return 0
}
