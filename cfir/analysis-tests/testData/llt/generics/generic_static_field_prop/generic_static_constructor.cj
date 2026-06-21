

class C {
    static func test() {
        0
    }
}

class A<T> where T <: C {
    static init() {
        <!STATIC_VARIABLE_USE_GENERIC_PARAMETER!>T<!>.test()
    }
}

main() {}
