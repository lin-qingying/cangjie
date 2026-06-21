


main() {
    let a = true
    let b = false
    let d = Some(3)
    if (a || b && let Some(<!VAR_IN_OR_CONDITION!>c<!>) <- d) {
        0
    } else {
        1
    }
    if ((let Some(<!VAR_IN_OR_CONDITION!>c<!>) <- d) && a || b) {
        0
    }
}
