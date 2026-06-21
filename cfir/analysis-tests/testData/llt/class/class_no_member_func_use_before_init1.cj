

struct GenRec {
    public var b: Int32 = 1
    public init(str: String) {
        this.b = foo()
    }
    private func foo(): Int32 {
        var a = b + 1
        return a
    }
}

main(): Int64 {
    var rec = GenRec("hi")
    var res = rec.b
    print(res.toString())
    if (res == 2) {
        return 0
    }
    return 1
}
