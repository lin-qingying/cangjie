

class C {
    static var i = 1
}

class A<T> where T <: C {}

extend<R> A<R> {
    func f() {
        return R.i
    }
}

main() {
    return A<C>().f() - 1
}
