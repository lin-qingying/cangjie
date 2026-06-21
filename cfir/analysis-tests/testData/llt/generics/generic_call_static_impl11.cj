

class A<T> {
    static func test(): Unit {}
}

func test(a: () -> Unit) {}

main() {
    test(<!GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT!>A<!>.test)
}
