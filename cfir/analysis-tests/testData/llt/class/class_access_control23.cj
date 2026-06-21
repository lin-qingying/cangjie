

interface I2 {
    static prop p3: Int64 {
        get() {
            26
        }
    }
}

interface I1 {
    static prop p3: Int64 {
        get() {
            16
        }
    }
}

open class S <: X {
    public static prop p3: Int64 {
        get() {
            6
        }
    }
}

open class X {
    public static prop p3: Int64 {
        get() {
            36
        }
    }
}

class A <: S & I1 & I2 {
    func check() {
        println(p3)
    }
}

main() {
    A().check()
    return 0
}
