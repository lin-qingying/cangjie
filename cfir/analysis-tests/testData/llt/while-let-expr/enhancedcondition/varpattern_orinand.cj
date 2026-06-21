

main() {
    let a = true
    let b = false
    let d = Some(3)
    while (<!VAR_IN_OR_CONDITION!>a || b && let Some(c) <- d<!>) {
        0
    }
    while (<!VAR_IN_OR_CONDITION!>(let Some(c) <- d) && a || b<!>) {
        0
    }
}
