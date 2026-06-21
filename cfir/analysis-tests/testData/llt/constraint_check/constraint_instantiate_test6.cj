


class X {}

interface Y {}

interface R<T> where T <: X & Y {}

func test(a: <!GENERIC_ARGUMENT_NO_MATCH!>R<X><!>, b: <!GENERIC_ARGUMENT_NO_MATCH!>R<Y><!>) {}

main(): Int64 {
    return 0
}
