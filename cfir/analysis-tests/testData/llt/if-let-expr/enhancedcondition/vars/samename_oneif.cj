

main() {
    let a = Some(1)
    if (let b <- a && let <!REDECLARATION!>b<!> <- Some(a)) {
        1
    } else {
        0
    }
}
