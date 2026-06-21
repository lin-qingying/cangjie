

extend Int64 {
    protected prop flag: Bool {
        get() {
            true
        }
    }
}

main() {
    let i = 0
    if (i.flag) {
        0
    } else {
        1
    }
}
