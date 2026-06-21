

class A {}
// test tracking nested constraints
func f<T>() where T <: A {
    func g<R>(): R where R <: <!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!> & A {
        let v: Int64 = <!UNABLE_TO_INFER_GENERIC_FUNC!>g()<!>
        return g<R>()
    }
}

main() {
}
