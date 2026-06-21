

func foo<T>(a: T): T {
    return a
}

main() {
    var f = foo<Int32>
    var a = f(1)
    if (a == 1) {
        return 0
    } else {
        return 1
    }
}
