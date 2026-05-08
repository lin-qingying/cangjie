// RUN_PIPELINE_TILL: FRONTEND
func choose(value: Int64, left!: Int64 = 0): Int64 {
    return value + left
}

func choose(value: Int64, right!: Bool = false): Int64 {
    if (right) {
        return value + 1
    }
    return value
}

func chooseWithNamedArguments(): Unit {
    choose(1, left: 2)
    choose(1, right: true)
}

func chooseAmbiguously(): Unit {
    let _ = <!AMBIGUOUS_FUNCTION_CALL!>choose<!>(1)
}
