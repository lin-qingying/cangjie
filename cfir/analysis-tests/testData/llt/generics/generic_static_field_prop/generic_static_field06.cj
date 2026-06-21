

struct Soo<T> {
    public static func foo<V>(): Option<V> {
        return Option<V>.None
    }
    public static var a: Option<String> = <!STATIC_VARIABLE_USE_GENERIC_PARAMETER!>foo<String>()<!>
}

class Coo<T> {
    public static func foo<V>(): Option<V> {
        return Option<V>.None
    }
    public static var a: Option<String> = <!STATIC_VARIABLE_USE_GENERIC_PARAMETER!>foo<String>()<!>
}

main() {
    0
}
