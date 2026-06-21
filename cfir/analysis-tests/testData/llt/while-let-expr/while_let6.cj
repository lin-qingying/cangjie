


main() {
    var p = Some(1)
    var a = 1
    while (let Some(i) <- p) {
        a -= i
        p = None
    }
    a
}
