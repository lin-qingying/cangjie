

open class Boo<T1> {
    public static func foo<V1>(x: T1, y: V1) {
        return 1
    }
}

class Coo <: Boo<Int32> {
    public static redef func foo<V2>(x: Int32, y: V2) {
        return 2
    }
}

main() {
    var c = Coo.foo<Int64>(0, 0)
    if (c != 2) {
        return 1
    }
    return 0
}
