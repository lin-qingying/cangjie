

interface EQ {}

class S <: EQ {}

class A<T> {
    static func ad(a: T) {}
}

extend<T> A<T> where T <: EQ {
    static func add(a: T) {}
}

main() {
    let s: S = S()
    A.add(s)
    A.ad(s)
    0
}
