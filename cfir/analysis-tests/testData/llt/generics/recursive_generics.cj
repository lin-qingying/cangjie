


class A<<!GENERIC_PARAM_DIRECTLY_RECURSIVE!>K<!>, U> where U <: K, K <: U {
    func foo(x: Box<U>): Box<K> {
        x as Box<K> ?? throw Exception()
    }
}

func foo<<!GENERIC_PARAM_DIRECTLY_RECURSIVE!>K<!>, U>(x: Box<U>): Box<K> where U <: K, K <: U {
    x as Box<K> ?? throw Exception()
}

class B<<!GENERIC_PARAM_DIRECTLY_RECURSIVE!>K<!>, U, X, Y, Z> where K <: U, U <: X, X <: Y, Y <: Z, Z <: U {
    func foo(x: Box<Z>): Box<K> {
        x as Box<K> ?? throw Exception()
    }
}

main(): Int64 {
    0
}
