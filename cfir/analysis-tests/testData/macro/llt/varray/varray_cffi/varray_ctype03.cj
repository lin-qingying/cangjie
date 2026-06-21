


struct A {}

foreign func c_f1(a: <!INVALID_CFUNC_PARAMETER_TYPE!>VArray<A, $3><!>): Unit // error

@C
func c_f2(a: <!INVALID_CFUNC_PARAMETER_TYPE!>VArray<A, $3><!>): Unit {} // error

var c_f3: CFunc<((<!INVALID_CFUNC_PARAMETER_TYPE!>VArray<A, $3><!>) -> Unit)> = {a => ()} //error

main() {}
