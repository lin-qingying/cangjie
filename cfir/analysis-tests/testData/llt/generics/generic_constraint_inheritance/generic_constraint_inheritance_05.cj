

open class A {}

open class B <: A {}

class C <: B {}

interface I<X> {
    static func test<T>(): Unit where T <: B & Equatable<X> {}
}

class D<X> <: I<X> {
    public static func test<T>(): Unit where T <: <!GENERIC_CONSTRAINT_NOT_LOOSER!>C & Equatable<X><!> {}
}

class E<X> <: I<X> {
    public static func test<T>(): Unit where T <: B {}
}

class F<X> <: I<X> {
    public static func test<T>(): Unit where T <: A & Equatable<X> {}
}

main() {}
