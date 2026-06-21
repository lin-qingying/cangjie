

class A<T> {
    static var a: Int64 = 1
}

main() {
    A<Int32>.a = 2
    return A<Int64>.a
}
