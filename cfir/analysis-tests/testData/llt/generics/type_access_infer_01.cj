

class A<T> {
    static var c = 5
}

main() {
    try {
        <!GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT!>A<!>.c >>= 0
    } catch (d: NoneValueException) {}
}
