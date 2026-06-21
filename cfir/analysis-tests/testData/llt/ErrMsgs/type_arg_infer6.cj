


// ensure no temporary type var names like 'T-0' exist in constraints
func f<T>(x: T) where T <: Comparable<T> {
}

class C {}

main() {
    <!UNABLE_TO_INFER_GENERIC_FUNC!>f(C())<!>
}
