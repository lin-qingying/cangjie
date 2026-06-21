

enum E<T0, T1> {
    EE
}

class A<T> {
    static var a = 1
}

main() {
    A<Option<<!GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT!>E<!>>>.a <<= 2
    return 1
}
