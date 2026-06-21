

class C {
    static var i = 1
}

class A<T> where T <: C {}

extend<T> A<T> {
    func f() {
        return T.i
    }
}

main() {
    return A<C>().f() - 1
}
