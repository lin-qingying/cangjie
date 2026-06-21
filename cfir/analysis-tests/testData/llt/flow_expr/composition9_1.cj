


func foo() {
    let h = <!UNRESOLVED_REFERENCE!>f<!> ~> <!UNRESOLVED_REFERENCE!>g<!>
    h(-1)
}
