

open class Parent {
    public static func Check<T1, T2>(arg: T1): Int64 {
        0
    }
}

open class Sub<T> <: Parent {
    public static func Check(arg: T): Int64 {
        1
    }
}

main(ina: Array<String>) {
    if (Sub<Int32>.Check(Int32(1)) != 1) {
        return 1
    }
    if (Parent.Check<Parent, Parent>(Parent()) != 0) {
        return 2
    }
    0
}
