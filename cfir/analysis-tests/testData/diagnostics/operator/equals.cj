
// LANGUAGE: +InvalidBinaryOperatorDiagnostics

class B {}

class C {
    public operator func ==(other: C): Bool {
        return true
    }
}

func name1(): Unit {
    let a = C()
    let b = C()

    a == b
}

func name(): Unit {
    let a = B()
    let b = B()

    a <!INVALID_BINARY_OPERATOR!>==<!> b
}
