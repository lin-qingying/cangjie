// RUN_PIPELINE_TILL: FRONTEND
foreign func cFunc(value: Int64): Unit

func passLiteral(): Unit {
    unsafe {
        cFunc(inout <!INOUT_MUST_BE_VAR_VARIABLE!>1<!>)
    }
}

func passExpression(): Unit {
    var x = 1
    unsafe {
        cFunc(inout <!INOUT_MUST_BE_VAR_VARIABLE!>(x + 1)<!>)
    }
}
