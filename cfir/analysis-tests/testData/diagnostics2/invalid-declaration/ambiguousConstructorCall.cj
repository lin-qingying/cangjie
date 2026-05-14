// RUN_PIPELINE_TILL: FRONTEND
class AmbiguousBox {
    init(value: Int64, left!: Int64 = 0) {}
    <!CONFLICTING_OVERLOADS!>init(value: Int64, right!: Int64 = 0)<!> {}
}

func testAmbiguousConstructorCall(): Unit {
    <!AMBIGUOUS_CONSTRUCTOR_CALL!>AmbiguousBox<!>(1)
}
