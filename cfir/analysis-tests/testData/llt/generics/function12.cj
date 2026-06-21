

var num = 3

func g<A>(a: A): A {
    var b: A
    if (num > 0) {
        num--
        b = g<A>(a)
    } else {
        return a
    }
    return b
}

main(): Int64 {
    var res = g<Int32>(123)
    if (res != 123) {
        return 1
    }
    return 0
}
