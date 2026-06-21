

open class A {
    public mut open prop v: Int64 {
        get() {
            return 2
        }
        set(v) {}
    }
}

class B <: A {
    public mut override prop v: Int64 {
        set(v) {
        }
    }
    public func f() {
        return v + 1
    }
}

main(): Int64 {
    if (A().v == 2 && B().v == 2 && B().f() == 3) {
        print("${A().v}\n")
        print("${B().v}\n")
        return 0
    }
    return 1
}
