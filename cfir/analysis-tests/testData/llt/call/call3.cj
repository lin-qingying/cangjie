

open class A<T> {
    public static func add(a: A<T>, b: A<T>): Int64 {
        1
    }
}

main() {
    var a = A<Int64>()
    var b = A<Int64>()
    if (A.add(a, b) != 1) {
        return 1
    }
    return 0
}
