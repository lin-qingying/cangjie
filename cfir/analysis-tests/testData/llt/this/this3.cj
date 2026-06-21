


interface I {}

open class Base <: I {
    public static func f1(): Unit {
        println("Base f1")
    }
    public static func g1(): Unit {
        f1()
    }
}

class Sub <: Base {
    public static func f1(): Unit {
        println("Sub f1")
    }
}

main() {
    Sub.g1()
}
