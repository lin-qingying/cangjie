func loopControl(): Unit {
    var i = 0
    while (i < 100) {
        i = i + 1
        if (i > 50) {
            continue
        }
        if (i > 90) {
            break
        }
    }
}
