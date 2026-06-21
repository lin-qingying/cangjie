

// DIAGNOSTICS: -UNUSED_VARIABLE

class ClassC<T> {}

struct A<T> {
    static var a: <!STATIC_VARIABLE_USE_GENERIC_PARAMETER!>ClassC<T><!> = <!STATIC_VARIABLE_USE_GENERIC_PARAMETER!>ClassC<T>()<!>
}

main() {}
