


interface I {}
class Int1{}
extend Int1 <: I {}

extend<T> Option<T> <: I where T <: I {}

class Cl<U1, U2> where U1 <: I, U2 <: I {}

<!TYPEALIAS_UNUSED_TYPE_PARAMETERS!>type A<T1, T2, T3><!> = Cl<Option<T1>, Option<T1>> // ok

func f<X>(a: <!GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT!>A<!><X, X, X>) { // error
    A<<!GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT!>X<!>, X, X>() // error
    A<Int1, Int1, Int1>() // ok
}

main() {}
