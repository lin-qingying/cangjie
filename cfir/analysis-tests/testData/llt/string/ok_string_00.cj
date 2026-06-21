


func test(b: Bool) {
    if (b) {
        0
    } else {
        1
    }
}

main() {
    let s01 = ''
    let s02 = ""
    let s03: String = ''
    let s04: String = ""
    let s11 = '🐘'
    let s12 = "🐒"
    let s13: String = '🐅'
    let s14: String = "🦏"
    let s21 = '🐘象'
    let s22 = "🐒猴"
    let s23: String = '🐅虎'
    let s24: String = "🦏犀牛"

    test(
        s01 is String && s02 is String && s03 is String && s04 is String && s11 is String && s12 is String &&
            s13 is String && s14 is String && s21 is String && s22 is String && s23 is String && s24 is String)
}
