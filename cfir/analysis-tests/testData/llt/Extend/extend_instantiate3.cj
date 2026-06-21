

interface A<T> {
    func add(a: T) {
        return a
    }
}

class C {}

extend C <: A<Int32> {}

main() {
    let c: C = C()
    c.add(1)
    0
}
