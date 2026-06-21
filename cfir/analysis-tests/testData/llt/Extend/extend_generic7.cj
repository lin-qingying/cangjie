

interface EQ {}

class S <: EQ {}

class A<T> {}

extend<T> A<T> where T <: EQ {
    static func add() {}
}

main() {
    A<S>.add()
    A<Int32>.<!NOT_MEMBER_OF!>add<!>()
    0
}
