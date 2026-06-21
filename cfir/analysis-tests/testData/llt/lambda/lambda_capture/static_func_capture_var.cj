

class C {
    public static var a = 0
    public var b = 0
    public static func foo() {
        a++
        return a
    }
    public func f() {
        b++
        print(b.toString())
        return b
    }
}

main(): Int64 {
    var f = C.foo
    var g = C().f
    func h(h: () -> Int64) {
        print(h().toString())
    }
    g()
    h(C.foo)
    return f() - 2
}
