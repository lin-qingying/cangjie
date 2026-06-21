

main() {
    let a = Some(1)
    if (let Some(b) <- a) {
        let <!REDECLARATION!>b<!> = <!UNRESOLVED_REFERENCE!>b<!>
    } else {
        0
    }
}
