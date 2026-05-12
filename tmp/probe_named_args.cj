func positional(a: Int64, b: Int64): Int64 {
    return a + b
}

func named(top!: Int64, left!: Int64): Int64 {
    return top + left
}

func mixedFriendly(plain: Int64, top!: Int64): Int64 {
    return top + plain
}

func testNamedArgumentsNotAllowed(): Unit {
    positional(a: 1, b: 2)
}

func testNamedParameterNotFound(): Unit {
    named(top: 1, right: 2)
}

func testArgumentPassedTwice(): Unit {
    named(top: 1, top: 2)
}

func testMixingNamedAndPositionalArguments(): Unit {
    mixedFriendly(top: 1, 2)
}

func testNeedNamedArgument(): Unit {
    named(1, left: 2)
}
