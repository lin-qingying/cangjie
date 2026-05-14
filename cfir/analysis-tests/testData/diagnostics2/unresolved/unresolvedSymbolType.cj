// RUN_PIPELINE_TILL: FRONTEND
func takesMissing(x: <!UNRESOLVED_REFERENCE!>MissingType<!>): Unit {
    let _ = x
}

func returnsMissing(): <!UNRESOLVED_REFERENCE!>MissingReturnType<!> {
    return 0
}
