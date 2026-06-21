

abstract class A {
    public operator func [](x: Float32, value!: Int64): Unit
}

class B <: A {
    var a: Int64 = 0
    public operator func [](x: Float32, value!: Int64): Unit {
        a = value
    }
    operator func [](x: Float32): Int64 {
        a
    }
}

main(): Int64 {
    var b = B()
    b[1.0] = 2
    return b[2.0] - 2
}
