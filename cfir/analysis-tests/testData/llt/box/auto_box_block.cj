

main() {
    var a = if (true) {}
    else {}
    var b = if (true) {
        let x = 1
    } else {}
    var c = if (true) {
        ()
    } else {}
    if (a is Unit && b is Unit && c is Unit) {
        return 0
    }
    return 1
}
