

class Class {
    static var b: Int64 = 1
}

interface A<T> where T <: Class {
    static prop c: Int64 {
        get() {
            <!STATIC_VARIABLE_USE_GENERIC_PARAMETER!>T<!>.b // error
        }
    }
    mut static prop d: Int64 {
        get() {
            <!STATIC_VARIABLE_USE_GENERIC_PARAMETER!>T<!>.b // error
        }
        set(_) {
            <!STATIC_VARIABLE_USE_GENERIC_PARAMETER!>T<!>.b // error
        }
    }
}

main() {}
