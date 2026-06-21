

main() {
    let t = (0, 0)
    let x: String = <!TYPE_MISMATCH!>t[0]<!>
}
