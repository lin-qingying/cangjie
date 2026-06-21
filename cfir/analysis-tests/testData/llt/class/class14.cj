

open class A {
    public static func foo1(a: Int32, b!: Int64 = 0): Int32 {
        return a
    }
}

class B <: A {
    public static func foo1(a: Int32, b!: Int64 = 0): Int32 {
        return a
    }
}

abstract class A1 {
    public static func foo1(a: Int32, b!: Int64 = 0): Int32 {
        return a
    }
}

class B1 <: A1 {
    public static func foo1(a: Int32, b!: Int64 = 0): Int32 {
        return a
    }
}
