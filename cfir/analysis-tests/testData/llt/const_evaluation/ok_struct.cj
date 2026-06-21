


struct S {
    var x: Int64
    const init(x: Int64) {
        this.x = x
        this.x++
    }
}

main() {
    const s = S(-1)
    s.x
}
