

extend String {
    protected operator func ==(i: Int64): Bool {
        true
    }
}

main() {
    let s = ""
    if (s == 0) {
        0
    } else {
        1
    }
}
