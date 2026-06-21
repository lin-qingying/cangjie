


class A<T> {
    func foo(a: Int64, b: Int64) {
        for (i in a..=b) {
            println(i)
        }
    }
}

main() {
    A<Int64>().foo(3, 7)
}
