

class Class {
    static var b: Int64 = 1
}

enum A<T> where T <: Class {
    | XX

    static prop c: Int64 {
        get() {
            <!STATIC_VARIABLE_USE_GENERIC_PARAMETER!>T<!>.b // error
        }
    }
}

main() {}
