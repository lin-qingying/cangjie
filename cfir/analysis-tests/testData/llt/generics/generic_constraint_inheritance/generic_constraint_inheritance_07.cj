

interface A {}

interface B <: A {}

interface C <: B {}

interface I1 {
    static func test<T>(): Unit where T <: A & B {}
}

class D <: I1 {
    public static func test<T>(): Unit where T <: <!GENERIC_CONSTRAINT_NOT_LOOSER!>C<!> {}
}

class E <: I1 {
    public static func test<T>(): Unit where T <: B {}
}

class F <: I1 {
    public static func test<T>(): Unit where T <: A {}
}

main() {}
