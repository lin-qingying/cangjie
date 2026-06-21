


func f(x: Int64): Any {
    if (x == 0) {
    } else if (x == 1) {
        true
    } else {
        throw Exception("")
    }
}

main() {
    let _ = f(0)
}
