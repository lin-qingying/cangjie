

open class A {
    public static var x: Int64 = 5
    public mut static prop v: Int64 {
        get() {
            return x
        }
        set(v) {
            x = v
        }
    }
}

class B <: A {
    public mut static redef prop v: Int64 {
        set(x) {
        }
    }
}

class C <: A {
    public mut static redef prop v: Int64 {
        get() {
            x + 1
        }
    }
}

main(): Int64 {
    C.v = 2
    if (A.v == 2 && B.v == 2 && C.v == 3) {
        print("${A.v}\n")
        print("${B.v}\n")
        print("${C.v}\n")
        return 0
    }
    return 1
}
