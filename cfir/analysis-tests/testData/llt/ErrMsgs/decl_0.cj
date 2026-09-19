


interface I {}

class C<T> where T <: I {}

func foo(x: C<<!GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT!>Unit<!>>) {}

main() {
    let v: C<<!GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT!>Unit<!>> = 1
    foo(1)
    ()
}
