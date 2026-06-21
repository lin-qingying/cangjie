

interface A<T> {
    func add(a: T) {
        return a
    }
}

extend Int32 <: A<Int32> {}

main() {
    let a: Int32 = 1
    a.add(1)
    0
}
