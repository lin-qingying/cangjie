


class A {
    public var value1: (String) -> Unit
    public var value2: (String) -> Unit
    public init(
        value1!: (String) -> Unit,
        value2!: (String) -> Unit
    ) {
        this.value1 = value1
        this.value2 = value2
    }
}

main() {
    func test(value: String) {
        print("test")
    }

    A(
        value1: {
            param => test(param)
        },
        value2: {
            param => test(param)
        }
    )

    return 0
}
