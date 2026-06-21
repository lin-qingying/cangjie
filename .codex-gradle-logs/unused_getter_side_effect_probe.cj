struct B {
    public var b = 3
}

class A {
    public mut prop a: B {
        get() {
            print("getter\n")
            return B()
        }
        set(_) {
            return ()
        }
    }
}

main() {
    var a = A()
    a.a.b
    return 0
}
