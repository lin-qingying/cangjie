

extend Int64 {
    static func foo<T>() {
        1
    }
}

main() {
    var a = Int64.foo<Int64>
    return a() - 1
}
