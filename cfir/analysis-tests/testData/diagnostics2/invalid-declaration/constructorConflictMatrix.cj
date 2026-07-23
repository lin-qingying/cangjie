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

func testNoValueForConstructor(): Unit {
    Box<!WRONG_NUMBER_OF_ARGUMENTS!>()<!>
}

func testAmbiguousConstructorCall(): Unit {
    <!AMBIGUOUS_CONSTRUCTOR_CALL!>AmbiguousBox<!>(1)
}
