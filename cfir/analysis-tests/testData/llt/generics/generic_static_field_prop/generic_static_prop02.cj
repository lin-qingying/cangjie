

class Class {
    static var b: Int64 = 1
}

struct A<T> where T <: Class {
    static prop c: Int64 { // error
        get() {
            <!STATIC_VARIABLE_USE_GENERIC_PARAMETER!>T<!>.b // error
        }
    }
    mut static prop d: Int64 { // error
        get() {
            <!STATIC_VARIABLE_USE_GENERIC_PARAMETER!>T<!>.b // error
        }
        set(_) {
            <!STATIC_VARIABLE_USE_GENERIC_PARAMETER!>T<!>.b // error
        }
    }
}

main() {}
