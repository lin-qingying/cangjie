


class C {
    static var b: Int64 = 1
}

struct A<T> {
    static var b: Int64 = <!STATIC_VARIABLE_USE_GENERIC_PARAMETER!>T<!>.b // error
}

main() {}
