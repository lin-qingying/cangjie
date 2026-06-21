

extend Int64 {
    protected operator func ==(i: String): Bool {
        true
    }
}

main() {
    let i = 0
    if (i == "") {
        0
    } else {
        1
    }
}
