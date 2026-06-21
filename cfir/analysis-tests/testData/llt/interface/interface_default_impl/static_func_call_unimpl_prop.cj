

interface I {
    mut static prop a: Int64
    static func fset(): Unit {
        a = 1
    }
    static func fget(): Int64 {
        a
    }
}

interface I2 <: I {
    mut static prop a: Int64 {
        get() {
            print("1")
            0
        }
        set(x) {
            print("2")
        }
    }
}

main() {
    I2.fset()
    I2.fget()
    0
}
