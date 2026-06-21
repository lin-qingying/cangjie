

func f() {
    var a = <!UNRESOLVED_REFERENCE!>a<!>
    var b: (Unit) -> Int32 = <!TYPE_MISMATCH!>{=> 1}<!>
    var c: (Unit) -> Int32 = <!TYPE_MISMATCH!>1<!>
}
