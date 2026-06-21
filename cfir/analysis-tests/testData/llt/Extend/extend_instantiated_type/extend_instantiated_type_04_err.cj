

class A<T> {}

extend<T> A<T> {}

extend A<Int64> {
    func test() {
        println("A int64")
    }
}

main() {
    let a1 = A<Int64>()
    let a2 = A<Rune>()
    a1.test()
    a2.<!NOT_MEMBER_OF!>test<!>()
}
