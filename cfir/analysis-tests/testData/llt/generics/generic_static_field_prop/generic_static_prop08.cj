

struct Soo<T> {
    public static prop foo: Int64 {
        get() {
            1
        }
    }
    public static var a = <!STATIC_VARIABLE_USE_GENERIC_PARAMETER!>foo<!>
}

class Coo<T> {
    public static prop foo: Int64 {
        get() {
            1
        }
    }
    public static var a = <!STATIC_VARIABLE_USE_GENERIC_PARAMETER!>foo<!>
}

main() {
    0
}
