

class B {}

enum A<T> {
    a(?T)
}

main(): Int64 {
    var x = a<B>(B())
    return 0
}
