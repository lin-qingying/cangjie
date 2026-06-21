

// DIAGNOSTICS: -UNUSED_VARIABLE

class C {
    static var b: Int64 = 1
}

class A<T> where T <: C {
    static var b: Int64 = <!STATIC_VARIABLE_USE_GENERIC_PARAMETER!>T<!>.b // error
}

main() {}
