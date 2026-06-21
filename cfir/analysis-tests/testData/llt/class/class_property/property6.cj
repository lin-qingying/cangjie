

class B {
    public var b = 1
}

class A {
    public mut prop a: B {
        get() {
            return B()
        }
        set(_) {
            return ()
        }
    }
    public func f() {
        return B()
    }
}

main() {
    var a = A()
    if (a.a.b == 1) {
        print("${a.a.b}\n")
        return 0
    }
    a.a = B()
    a.f()
    return 1
}

