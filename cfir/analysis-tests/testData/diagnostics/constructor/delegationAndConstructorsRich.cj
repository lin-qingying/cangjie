// RUN_PIPELINE_TILL: FRONTEND
class Box {
    init(value: Int64) {}
}

class AmbiguousBox {
    init(value: Int64) {}
    init(value: Int64, extra!: Int64 = 0) {}
}

open class Base {
    init(value: Int64) {}
}

class MissingSuper <: Base {
    <!EXPLICIT_SUPER_CALL_REQUIRED!>init() {
    }<!>
}

class IllegalSuper <: Base {
    init() {
        let x = 1
        <!ILLEGAL_THIS_OR_SUPER_CALL!>super<!>(1)
    }
}

class Recursive {
    init() {
        <!RECURSIVE_CONSTRUCTOR_CALL!>this<!>()
    }
}

func testNoValueForConstructor(): Unit {
    Box<!NO_VALUE_FOR_PARAMETER!>()<!>
}

func testAmbiguousConstructorCall(): Unit {
    <!AMBIGUOUS_CONSTRUCTOR_CALL!>AmbiguousBox<!>(1)
}
