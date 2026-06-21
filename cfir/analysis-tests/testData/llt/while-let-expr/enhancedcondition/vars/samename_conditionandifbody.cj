

main() {
    let a = Some(1)
    while (let Some(b) <- a) {
        let <!REDECLARATION!>b<!> = <!UNRESOLVED_REFERENCE!>b<!>
        break
    }
}
