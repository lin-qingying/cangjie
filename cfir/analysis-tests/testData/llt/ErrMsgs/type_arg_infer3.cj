


// test err msg for arg/param and ret/expected type mismatch
func f<T>(a: A<T>) {}

func g<T>(a: A<T>): A<T> {
    a
}

class A<T> {}

class B<T> {}

main() {
    <!UNABLE_TO_INFER_GENERIC_FUNC!>f(<!TYPE_MISMATCH!>B<Int>()<!>)<!>
    let s: B<Int> = <!TYPE_MISMATCH!>g(A<Int>())<!>
}
