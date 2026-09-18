



class X {}

interface Y {}

enum R<T> where T <: X & Y {
    e
}

var a = R<<!GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT!>X<!>>.e
var b = R<<!GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT!>Y<!>>.e

main(): Int64 {
    return 0
}

