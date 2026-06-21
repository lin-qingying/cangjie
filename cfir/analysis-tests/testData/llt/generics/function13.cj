

func foo<T, U>(a: T, b: U): T {
    func bar<P>(a: P) {
        return a
    }
    return bar<T>(a)
}

main(): Int64 {
    var a: Bool = false
    var b: Int32 = 1
    var res = foo<Bool, Int32>(a, b)

    if (res != false) {
        return 1
    }
    return 0
}
