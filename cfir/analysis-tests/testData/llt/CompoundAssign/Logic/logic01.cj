


main(): Int64 {
    var a = true
    var b = false
    a &&= (1 > 2)
    b ||= (1 + 1 == 2)
    if (!a && b) {
        return 0
    }
    return 1
}
