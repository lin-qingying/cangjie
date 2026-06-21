

open class Base {}

class Sub <: Base {}

class A<T> {
    var v: Int64
    init(x: Sub, y: Sub) {
        v = 1
    } // The closest matching rule chooses this one
    init(x: Base, y: Base) {
        v = 2
    }
}

main() {
    let a = A<Unit>(Sub(), Sub())
    return a.v - 1
}
