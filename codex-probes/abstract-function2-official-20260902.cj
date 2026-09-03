interface I {
    static func f2(a!: Int64, b!: Int64): Int64
}

class A <: I {}

interface I2 <: I {}

class B {
    static func a(): Unit
}
