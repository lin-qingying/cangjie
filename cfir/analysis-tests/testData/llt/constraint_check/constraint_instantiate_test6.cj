



class X {}

interface Y {}

interface R<T> where T <: X & Y {}

func test(a: R<<!GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT!>X<!>>, b: R<<!GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT!>Y<!>>) {}

main(): Int64 {
    return 0
}

