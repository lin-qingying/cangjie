


func f<T>(_: T, _: T): Unit where T <: Equatable<T> {}

class A <: Equatable<A> {
    public operator func !=(_: A) {
        false
    }
    public operator func ==(_: A) {
        true
    }
}

main() {
    f(1, Some(1))
    f(Some(1), 1)
    f(A(), Some(A()))
    f(Some(A()), A())
}
