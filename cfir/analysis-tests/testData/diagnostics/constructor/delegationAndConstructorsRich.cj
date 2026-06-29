// RUN_PIPELINE_TILL: FRONTEND
class Box {
    init(value: Int64) {
        let _ = value
    }
}

class AmbiguousBox {
    init(value: Int64, left!: Int64 = 0) {}
    <!CONFLICTING_OVERLOADS!>init<!>(value: Int64, right!: Int64 = 0) {}
}

open class Base {
    init(value: Int64) {
        let _ = value
    }
}

class MissingSuper <: Base {
    <!NO_NON_PARAM_CONSTRUCTOR_IN_SUPER_CLASS!>init<!>() {
    }
}

class IllegalSuper <: Base {
    init() {
        let _ = 1
        <!ILLEGAL_PLACE_OF_CALLING_THIS_OR_SUPER!>super<!>(1)
    }
}

class Recursive {
    <!RECURSIVE_CONSTRUCTOR_CALL!>init<!>() {
        this()
    }
}

func testNoValueForConstructor(): Unit {
    Box<!NO_VALUE_FOR_PARAMETER!>()<!>
}

func testAmbiguousConstructorCall(): Unit {
    <!AMBIGUOUS_CONSTRUCTOR_CALL!>AmbiguousBox<!>(1)
}
