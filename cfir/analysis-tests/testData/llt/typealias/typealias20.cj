


type li = Array<Rune>

extend<T> Array<T> {
    public static func a() {
        1
    }
}

main(): Int64 {
    var b: Int64 = li.a()
    if (b != 1) {
        return 1
    }
    return 0
}
