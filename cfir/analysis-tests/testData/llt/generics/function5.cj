


func Foo<T, U>(a: T, b: U): T {
    return a
}

func Bar<T>(a: T) {
    var b: Int32 = 1
    var c = Foo<T, Int32>(a, b)
    return c
}

main(): Int64 {
    var a: Int32 = 1
    var b = Bar<Int32>(a)
    if (b == 1) {
        return 0
    } else {
        return 1
    }
}
