

func foo() {}
main() {
    let a = foo 
    let b = foo 
    let c = (1,2)
    let d = (3,4)
    a <!INVALID_BINARY_OPERATOR!>==<!> b 
    c <!INVALID_BINARY_OPERATOR!>+<!> d 
    0
}
