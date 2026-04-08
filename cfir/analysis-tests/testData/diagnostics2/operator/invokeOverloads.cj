class Handler {
    public operator func ()(a: Int64): Int64 {
        return a
    }

    public operator func ()(a: Int64, b: Int64): Int64 {
        return a + b
    }

    public operator func ()(s: Bool): Bool {
        return s
    }
}

func testCorrectOverloads(): Unit {
    let h = Handler()
    h(1)
    h(1, 2)
    h(true)
}

func testOverloadArgMismatch(): Unit {
    let h = Handler()
    h(<!ARGUMENT_TYPE_MISMATCH!>1.0<!>)
}

func testInvokeReturnInContext(): Unit {
    let h = Handler()
    let x: Int64 = h(1)
    let y: Bool = h(true)
    let z: Int64 <!PATTERN_INITIALIZER_TYPE_MISMATCH!>=<!> <!TYPE_MISMATCH!>h(true)<!>
}
