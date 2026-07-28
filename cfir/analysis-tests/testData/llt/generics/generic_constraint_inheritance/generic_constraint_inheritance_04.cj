

open class A {}

open class B <: A {}

class C <: B {}

interface I {
    static func test<T>(): Unit where T <: B & Equatable<T> {}
}

class D <: I {
    public static func test<T>(): Unit where <!GENERIC_CONSTRAINT_NOT_LOOSER!>T <: C & Equatable<T><!> {}
}

class E <: I {
    public static func test<T>(): Unit where T <: B {}
}

class F <: I {
    public static func test<T>(): Unit where T <: A & Equatable<T> {}
}

main() {}
