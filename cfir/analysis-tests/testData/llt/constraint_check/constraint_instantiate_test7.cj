


class X {}

interface Y {}

enum R<T> where T <: X & Y {
    e
}

var a = <!GENERIC_ARGUMENT_NO_MATCH!>R<X><!>.e
var b = <!GENERIC_ARGUMENT_NO_MATCH!>R<Y><!>.e

main(): Int64 {
    return 0
}
