

interface I1 {
    mut static prop a: Int64
    static func f(): Unit
}

interface I2 <: I1 {}

abstract class I3 <: I2 {
    public mut static prop a: Int64 {
        get() {
            0
        }
        set(v) {
            println("set T.a")
        }
    }
    public static func f(): Unit {
        println("call T.f")
    }
}

func f<T>(): Unit where T <: I2 {
    T.a = 1
    T.f()
}

main() {
    f<I3>()
}
