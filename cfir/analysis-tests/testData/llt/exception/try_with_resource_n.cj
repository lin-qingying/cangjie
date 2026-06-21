


class A {}

main() {
    try (<!TYPE_MISMATCH!>input = A()<!>) {
    } catch (e: Exception) {
        return 1
    }
    try (<!TYPE_MISMATCH!>aaa = ()<!>) {
        aaa;
        ()
    }
    return 0
}
