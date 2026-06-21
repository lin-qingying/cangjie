

class A {
    public var v = 1
}

type a = A

extend a {
    public func f() {
        this.v + v
    }
}

main() {
    var x = a()
    if (x.f() != 2) {
        return 1
    }
    return 0
}
