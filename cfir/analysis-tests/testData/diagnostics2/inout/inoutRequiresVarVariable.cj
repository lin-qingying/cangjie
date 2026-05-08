// RUN_PIPELINE_TILL: FRONTEND
foreign func cFunc(value: Int64): Unit

func passLet(): Unit {
    let x = 1
    unsafe {
        cFunc(inout <!INOUT_MUST_BE_VAR_VARIABLE!>x<!>)
    }
}

func passVar(): Unit {
    var x = 1
    unsafe {
        cFunc(inout x)
    }
}
