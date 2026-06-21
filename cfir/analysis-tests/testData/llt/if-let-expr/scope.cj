

main() {
    let aa: Option<Bool> = None
    if (let Some(xx) <- aa) {
        println("11")
    } else if (!<!UNRESOLVED_REFERENCE!>xx<!>) {
        println("22")
    } else {
        println("33")
    }
}
