

public func foo() {
    while (let a <- 0) {
        let <!REDECLARATION!>a<!> = 1
    }
}

main() {
    foo()
    0
}
