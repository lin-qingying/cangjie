// RUN_PIPELINE_TILL: FRONTEND
func chooseGeneric<T>(first: T, second: T): T {
    return first
}

func builderLike<T>(block1: () -> T, block2: () -> T): T {
    return block1()
}

func placeholders(): Unit {
    let _ = <!UNABLE_TO_INFER_GENERIC_FUNC!>chooseGeneric<!>(1, true)

    let _ = <!UNABLE_TO_INFER_GENERIC_FUNC!>builderLike<!>(<!UNABLE_TO_INFER_GENERIC_FUNC!>{ => 1 }<!>, <!UNABLE_TO_INFER_GENERIC_FUNC!>{ => true }<!>)

    let _ = <!UNABLE_TO_INFER_GENERIC_FUNC!>chooseGeneric<!>(1, 1.0)

    let _ = <!UNABLE_TO_INFER_GENERIC_FUNC!>chooseGeneric<!>(true, 1)
}
