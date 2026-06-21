

class C1 {
    public var x = 0
    public func foo(): This {
        x = 1
        return this
    }
}

main() {
    var obj = C1()
    if (obj.foo().x == 1) {
        return 0
    }
    return 1
}
