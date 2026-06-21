

struct A<T> {}

extend<T> A<T> {
    public static prop p: Int64 {
        get() {
            return 0
        }
    }
}

main(): Int64 {
    return <!GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT!>A<!>.p
}
