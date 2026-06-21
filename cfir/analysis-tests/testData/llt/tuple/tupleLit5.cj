

interface A<T> {
    operator func [](b: (A<T>, A<T>)): Bool {
        true
    }
    operator func +(b: (A<T>, A<T>)): Bool {
        true
    }
}

class A_B<T> <: A<T> {}

main(): Int64 {
    var a: (A<Int32>, A<Int32>) = (A_B<Int32>(), A_B<Int32>())
    var b = A_B<Int32>()
    if (!(b[a])) {
        return 1
    }
    if (!(b + a)) {
        return 13
    }
    return 0
}
