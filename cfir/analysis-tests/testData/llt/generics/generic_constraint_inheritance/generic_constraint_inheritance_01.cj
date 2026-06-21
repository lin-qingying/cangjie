

open class A {}

open class B <: A {}

open class C <: B {}

open class Base {
    static func f<T>(a: T): Unit where T <: B {}
    static func g<T>(): Unit where T <: B {}
    static func p<T>(): Unit {}
}

class D <: Base {
    static redef func f<T>(a: T): Unit where T <: <!GENERIC_CONSTRAINT_NOT_LOOSER!>C<!> {} // Error，stricter constraint
    static redef func g<T>(): Unit where T <: <!GENERIC_CONSTRAINT_NOT_LOOSER!>C<!> {} // Error，stricter constraint
    static redef func p<T>(): Unit where T <: <!GENERIC_CONSTRAINT_NOT_LOOSER!>C<!> {} // Error，stricter constraint
}

class E <: Base {
    static redef func f<T>(a: T): Unit where T <: A {} // OK, looser constraint
    static redef func g<T>(): Unit where T <: A {} // OK, looser constraint
}

class F <: Base {
    static redef func f<T>(a: T): Unit where T <: B {} // OK，same constraint
    static redef func g<T>(): Unit where T <: B {} // OK，same constraint
}

main() {}
