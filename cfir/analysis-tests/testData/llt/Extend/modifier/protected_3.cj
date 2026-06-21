

extend String {
    protected prop flag: Bool {
        get() {
            true
        }
    }
}

main() {
    let s = ""
    if (s.flag) {
        0
    } else {
        1
    }
}
