

class A {}
// test tracking nested constraints
func f<T>() where T <: A {
    func g<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>R<!>>(): R where R <: T & A {
        let v: Int64 = <!UNABLE_TO_INFER_GENERIC_FUNC!>g<!>()
        return g<R>()
    }
}

main() {
}
