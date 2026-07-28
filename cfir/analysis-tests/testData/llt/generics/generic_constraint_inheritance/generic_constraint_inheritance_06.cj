

open class A {}

open class B <: A {}

class C <: B {}

interface I<X> {
    static func test<T>(): Unit where T <: B & Equatable<X> {}
}

class D <: I<B> {
    public static func test<T>(): Unit where <!GENERIC_CONSTRAINT_NOT_LOOSER!>T <: C & Equatable<B><!> {}
}

class E <: I<B> {
    public static func test<T>(): Unit where <!GENERIC_CONSTRAINT_NOT_LOOSER!>T <: Equatable<C><!> {}
}

class F <: I<B> {
    public static func test<T>(): Unit where T <: A & Equatable<B> {}
}

main() {}
