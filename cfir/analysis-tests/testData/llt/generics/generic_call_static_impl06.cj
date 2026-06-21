

interface I {
    static func f1(): Unit
}

abstract class <!INHERIT_ABSTRACT_CLASS_STATIC_UNIMPLEMENT_FUNC!>A<!> <: I {}

func f<T>(): Unit where T <: A {
    T.f1()
}

main() {
    f<A>()
}
