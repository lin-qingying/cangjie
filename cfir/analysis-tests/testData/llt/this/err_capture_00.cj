


struct S {
    mut func f() {
        false
    }
    S() {
        let g = {=> <!ILLEGAL_CAPTURE_THIS!>f<!>()}
        print("${g()}")
    }
}

main() {}
