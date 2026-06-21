

class CA<T> {}

interface I2_1 {
    static func goo(): Unit {
        println("I2-1")
    }
}

interface I2_2 <: I2_1 {
    static func goo(): Unit {
        println("I2-2")
    }
}

interface I3 {
    static func goo(): Unit
}

extend<T> CA<T> <: I3 where T <: I2_1 & I2_2 {
    public static func goo(): Unit {
        T.goo()
        println("here")
    }
}

main() {
    CA<<!GENERIC_TYPE_INCONSISTENT!>Nothing<!>>.goo()
    0
}
