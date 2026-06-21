

class A {
    public var x: Int64 = 2
    public mut prop b: Int64 {
        get() {
            return x
        }
        set(v) {
            x = v
            return ()
        }
    }
}

main() {
    var a = A()
    a.b += 5
    a.b *= 5
    if (a.b == 35) {
        print("${a.b}\n")
        return 0
    }
    return 1
}

