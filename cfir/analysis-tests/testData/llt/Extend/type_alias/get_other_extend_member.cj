

class A {}

type a = A

type b = A

extend a {
    public func ggg() {
        2
    }
}

extend b {
    public func h() {
        ggg()
    }
}

main() {
    var x = a()
    if (x.h() != 2) {
        return 1
    }
    return 0
}
