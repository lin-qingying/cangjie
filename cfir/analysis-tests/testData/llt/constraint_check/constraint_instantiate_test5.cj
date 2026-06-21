


class X {}

interface Y {}

class R<T> where T <: X & Y {}

var a = <!GENERIC_ARGUMENT_NO_MATCH!>R<X>()<!>
var b = <!GENERIC_ARGUMENT_NO_MATCH!>R<Y>()<!>

main(): Int64 {
    return 0
}
