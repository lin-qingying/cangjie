

func test<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>X<!>, Y>() where X <: Y {}

class C<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>X<!>, Y> where X <: Y {}

struct R<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>X<!>, Y> where X <: Y {}

main() {
    var i = test<Int8, Int8>()
    var c = C<Int8, Int8>()
    var r = R<Int8, Int8>()
    return 0
}
