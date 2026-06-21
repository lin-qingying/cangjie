

func f(x: (String) -> Unit) {
    print(1)
    x("")
}

func f(x: (Bool) -> Unit) {
    print(2)
    x(false)
}

func test<T>(_: T) {
    print(3)
}

func test(_: Int64) {
    print(4)
}

main() {
    f(
        {
            param =>
            test(param)
            let _: String = param
        }
    )
    f(
        {
            param =>
            test(0)
            let _: Bool = param
        }
    )
    println()
}
