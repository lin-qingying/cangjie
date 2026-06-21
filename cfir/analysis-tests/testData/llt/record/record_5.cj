

struct Base12 {
    public var d: Int64 = 1
    public prop a: Int64 {
        get() {
            return this.d
        }
    }
}

main() {
    var x = Base12()
    if (x.a == 1) {
        print("${x.a}")
        return 0
    }
    return 1
}
