// RUN_PIPELINE_TILL: FRONTEND
func choose<T>(first: T, second: T): T {
    return first
}

func conflictingPrimitiveAndBool(): Unit {
    let value = <!UNABLE_TO_INFER_GENERIC_FUNC!>choose<!>(1, true)
    let _ = value
}

func conflictingStringAndInt(): Unit {
    let value = <!UNABLE_TO_INFER_GENERIC_FUNC!>choose<!>("x", 1)
    let _ = value
}
