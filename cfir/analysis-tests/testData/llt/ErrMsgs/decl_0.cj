


interface I {}

class C<T> where T <: I {}

func foo(x: C<<!GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT!>U<!>nit>) {}

main() {
    let v: C<<!GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT!>U<!>nit> = 1
    foo(1)
    ()
}
