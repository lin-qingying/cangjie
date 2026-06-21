

func f(c: Bool) {
    let a: Int64
    if (c) {
        a = Some(1) ?? throw Exception()
    } else {
        a = 2
    }
    return a
}

main() {
    f(true) - 1
}
