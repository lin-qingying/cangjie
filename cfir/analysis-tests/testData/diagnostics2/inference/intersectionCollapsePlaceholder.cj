// RUN_PIPELINE_TILL: FRONTEND
func chooseGeneric<T>(first: T, second: T): T {
    return first
}

func emptyIntersectionLike(): Unit {
    let value = <!NEW_INFERENCE_ERROR!>chooseGeneric<!>(1, 1.0)
    let _ = value
}

func possibleEmptyIntersectionLike(): Unit {
    let value = <!NEW_INFERENCE_ERROR!>chooseGeneric<!>(true, 1)
    let _ = value
}
