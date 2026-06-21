


class A <: Exception {}

main() {
    try {
        throw A()
    } catch (e: <!CATCH_TYPE_MUST_EXTEND_EXCEPTION!>A | Error<!>) {
        println(e)
    }
}
