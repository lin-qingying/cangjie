// RUN_PIPELINE_TILL: FRONTEND
func chooseGeneric<T>(first: T, second: T): T {
    return first
}

func emptyIntersectionLike(): Unit {
    let value = <!UNABLE_TO_INFER_GENERIC_FUNC!>chooseGeneric<!>(1, 1.0)
    let _ = value
}

func possibleEmptyIntersectionLike(): Unit {
    let value = <!UNABLE_TO_INFER_GENERIC_FUNC!>chooseGeneric<!>(true, 1)
    let _ = value
}
