// RUN_PIPELINE_TILL: FRONTEND
func identity<T>(x: T): T {
    return x
}

func choose<T>(first: T, second: T): T {
    return first
}

func inferredFromReturn(): Int64 {
    return identity(42)
}

func conflictingReturnInference(): Unit {
    let value = <!NEW_INFERENCE_ERROR!>choose<!>(1, true)
    let _ = value
}
