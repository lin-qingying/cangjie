// WITH_EXTRA_CHECKERS

class A<T> {
    init(a: Array<T>) {}
    init(a: Collection<T>) {
        match (a) {
            case v: Array<T> => println("array")
            case _ => ()
        }
    }
}

extend<T> Array<T> {
    func test(): A<Array<T>> {
        return A<Array<T>>([this])
    }
}

main() {
    var <!UNUSED_VARIABLE!>a<!>: A<Int64>
    0
}
