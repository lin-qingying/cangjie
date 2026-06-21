

let val = 11

interface I<T> {
    func test() {
        return val
    }
}

interface II {}

open class A<T> <: I<T> {}

extend<T> A<T> <: II where T <: II {}

class B<T> <: A<T> {}

main() {
    let b = B<Int64>()
    return b.test() - val
}
