// RUN_PIPELINE_TILL: FRONTEND
func choose<T>(first: T, second: T): T {
    return first
}

func conflictingPrimitiveAndBool(): Unit {
    let value = <!NEW_INFERENCE_ERROR!>choose<!>(1, true)
    let _ = value
}

func conflictingStringAndInt(): Unit {
    let value = <!NEW_INFERENCE_ERROR!>choose<!>("x", 1)
    let _ = value
}
