

class A {
    public mut static prop a: Int64 {
        get() {
            return 4
        }
        set(_) {
            return ()
        }
    }
    public mut prop b: Int64 {
        get() {
            return a
        }
        set(_) {
            return ()
        }
    }
}

main() {
    var a = A()
    A.a = 1
    a.b = 2
    if (a.b == 4) {
        print("${a.b}")
        return 0
    }
    return 1
}

