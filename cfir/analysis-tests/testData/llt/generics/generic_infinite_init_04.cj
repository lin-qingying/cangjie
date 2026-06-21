


// X -> X, Y -> E<X>
// X -> Y, Y -> E<X>  cause infinite substitution
// X -> E<X>, Y -> E<X>  cause infinite substitution

enum E<X> {
    e(X)
}

struct A<X, Y> {
    public operator func [](b: A<X, E<X>>): Bool {
        true
    }
    public operator func +(b: <!GENERIC_INFINITE_INSTANTIATION!>A<!><Y, E<X>>): Bool {
        true
    }
}

main(): Int64 {
    var a: A<Int32, Int32>
    return 0
}
