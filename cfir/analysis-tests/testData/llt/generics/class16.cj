

class C<T> {
    public static func foo<V>() {
    }
}

main() {
    var c = <!GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT!>C<!>.foo<Int>
}
