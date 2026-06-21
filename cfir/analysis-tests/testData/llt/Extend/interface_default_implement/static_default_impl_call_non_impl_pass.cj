

public interface I<T> where T <: I<T> {
    static func foo1(): Unit
    static func foo2() {
        T.foo1()
    }
}

main() {
    return 0
}
