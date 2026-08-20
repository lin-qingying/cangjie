// RUN_PIPELINE_TILL: FRONTEND
func takesMissing(x: <!UNDECLARED_TYPE_NAME!>MissingType<!>): Unit {
    let _ = x
}

func returnsMissing(): <!UNDECLARED_TYPE_NAME!>MissingReturnType<!> {
    return 0
}
