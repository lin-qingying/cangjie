

interface A {}

interface B <: A {}

interface C <: B {}

interface I1 {
    static func test<T>(): Unit where T <: B {}
}

class D <: I1 {
    public static func test<T>(): Unit where <!GENERIC_CONSTRAINT_NOT_LOOSER!>T <: A & C<!> {}
}

class E <: I1 {
    public static func test<T>(): Unit where <!GENERIC_CONSTRAINT_NOT_LOOSER!>T <: B & C<!> {}
}

class F <: I1 {
    public static func test<T>(): Unit where T <: A & B {}
}

main() {}
