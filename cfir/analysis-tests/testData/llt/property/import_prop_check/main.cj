// FILE: pkg.cj


package b

public struct A {
    private var val = 1
    public mut prop a: Int64 {
        get() {
            return val
        }
        set(v) {
            val = v
        }
    }
}

// FILE: main.cj



internal import b.*

main() {
    var a = A()
    var res = a.a // 1
    a.a = 2
    res += a.a
    if (res != 3) {
        return 1
    }
    return 0
}
