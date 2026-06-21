

class A<T, R> {
    public static func foo(a: Option<T>, b: R) {}
    private static func foo(a: Option<T>, b: Bool) {}
}

main() {
    <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>A<Int64, Bool><!>.foo(1, true)
}
