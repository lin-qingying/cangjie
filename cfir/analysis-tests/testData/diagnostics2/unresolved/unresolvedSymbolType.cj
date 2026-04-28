// RUN_PIPELINE_TILL: FRONTEND
func takesMissing(x: <!UNRESOLVED_REFERENCE!>MissingType<!>): Unit {
}

func returnsMissing(): <!UNRESOLVED_REFERENCE!>MissingReturnType<!> {
    return 0
}
