

interface C {
    static func <!UNABLE_TO_INFER_RETURN_TYPE!>foo<!>() {
        goo()
    }
    static func goo() {
        foo()
    }
}
