

func f(): Float64 {
    var a: Int64 = 1
    <!TYPE_MISMATCH!>a<!>
}

func <!CONFLICTING_OVERLOADS!>f<!>(): Int64 {
    <!TYPE_MISMATCH!>while (true) {}<!>
}

func foo() {
    return true
}

func bar() {
    return 1
}
