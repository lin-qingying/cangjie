

func f(): Float64 {
    var a: Int64 = 1
    <!RETURN_TYPE_MISMATCH!>a<!>
}

func <!CONFLICTING_OVERLOADS!>f<!>(): Int64 {
    <!RETURN_TYPE_MISMATCH!>while (true) {}<!>
}

func foo() {
    return true
}

func bar() {
    return 1
}
