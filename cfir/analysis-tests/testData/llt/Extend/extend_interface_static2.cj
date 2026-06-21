

interface A<T> {
    static func add(a: T): T {
        return a
    }
}

class C {}

extend C <: A<Int64> {}

main() {
    let b: Int64 = 1
    return C.add(b) - 1
}
