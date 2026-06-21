

extend<T> Array<T> {
    public static prop ns: Int64 {
        get() {
            return 0
        }
    }
}

main(): Int64 {
    return <!GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT!>Array<!>.ns
}
