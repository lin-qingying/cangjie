

struct R<T> {}

extend<T> R<T> {
    public static func f(): Int64 {
        g<Rune>()
    }
    public static func g<X>(): Int64 {
        1
    }
}

main(): Int64 {
    return R<Int8>.f() - 1
}
