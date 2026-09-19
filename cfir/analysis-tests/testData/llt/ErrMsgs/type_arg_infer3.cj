


// test err msg for arg/param and ret/expected type mismatch
func f<T>(a: A<T>) {}

func g<T>(a: A<T>): A<T> {
    a
}

class A<T> {}

class B<T> {}

main() {
    <!UNABLE_TO_INFER_GENERIC_FUNC!>f<!>(B<Int>())
    let s: B<Int> = <!UNABLE_TO_INFER_GENERIC_FUNC!>g<!>(A<Int>())
}
