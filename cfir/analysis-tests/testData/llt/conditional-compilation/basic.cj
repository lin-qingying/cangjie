// RUN_PIPELINE_TILL: FRONTEND
// WHEN_ENV: backend=cjnative,arch=x86_64,os=Windows,cjc_version=1.0.5,debug=false,test=false

@When[debug]
func removedByWhen() {
    removedReference()
}

@When[!debug]
func retainedByWhen() {
    <!UNRESOLVED_REFERENCE!>retainedReference<!>()
}

func parameterAndBlock(
    @When[debug] removedParameter: Int64,
    @When[!debug] retainedParameter: Int64,
): Unit {
    @When[debug]
    let removedLocal = removedLocalReference()
    @When[!debug]
    let retainedLocal = <!UNRESOLVED_REFERENCE!>retainedLocalReference<!>()
}
