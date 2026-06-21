

interface I {}

extend Int32 <: I {}

main(): Int64 {
    if (Coo.foo(1, 2) != 2 || Boo<Int32>.foo(3, 4) != 4) {
        return 1
    }
    return 0
}

open class Boo<T> {
    public static func foo<V>(x: T, y: V) where V <: I {
        return y
    }
}

class Coo <: Boo<Int32> {
    public static redef func foo<V>(x: Int32, y: V) where V <: I {
        return y
    }
}
