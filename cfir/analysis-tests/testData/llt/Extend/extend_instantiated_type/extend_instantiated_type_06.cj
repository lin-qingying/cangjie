

class A<T, K> where K <: ToString {}

extend<T> A<Int64, T> <: ToString {
    public func toString() {
        return "A<Int64, T>"
    }
}

main() {
    let a1 = A<Int64, Rune>()
    let t: ToString = a1
    if (t.toString() != "A<Int64, T>") {
        return 1
    }
    return 0
}
