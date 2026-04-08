// RUN_PIPELINE_TILL: FRONTEND
func positional(a: Int64, b: Int64): Int64 {
    return a + b
}

func positionalDefault(a: Int64 = 0, b: Int64 = 0): Int64 {
    return a + b
}

func named(top!: Int64, left!: Int64): Int64 {
    return top + left
}

func mixedFriendly(top!: Int64, plain: Int64): Int64 {
    return top + plain
}

func testMissingValue(): Unit {
    positional<!NO_VALUE_FOR_PARAMETER!>(1)<!>
}

func testTooManyArguments(): Unit {
    positional(1, 2, <!TOO_MANY_ARGUMENTS!>3<!>)
}

func testNamedArgumentsNotAllowed(): Unit {
    positionalDefault(<!NAMED_ARGUMENTS_NOT_ALLOWED!>a<!>: 1, <!NAMED_ARGUMENTS_NOT_ALLOWED!>b<!>: 2)
}

func testNamedParameterNotFound(): Unit {
    named(top: 1, <!NAMED_PARAMETER_NOT_FOUND!>right<!>: 2, left: 3)
}

func testArgumentPassedTwice(): Unit {
    named(top: 1, <!ARGUMENT_PASSED_TWICE!>top<!>: 2, left: 3)
}

func testMixingNamedAndPositionalArguments(): Unit {
    mixedFriendly(top: 1, <!MIXING_NAMED_AND_POSITIONAL_ARGUMENTS!>2<!>)
}

func testNeedNamedArgument(): Unit {
    named(<!NEED_NAMED_ARGUMENT!>1<!>, left: 2)
}

func testNamedHappyPath(): Unit {
    named(top: 1, left: 2)
}
