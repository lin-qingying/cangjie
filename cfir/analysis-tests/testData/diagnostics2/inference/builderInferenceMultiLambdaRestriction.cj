// RUN_PIPELINE_TILL: FRONTEND
func builderLike<T>(block1: () -> T, block2: () -> T): T {
    return block1()
}

func conflictingLambdaReturns(): Unit {
    let value = <!UNABLE_TO_INFER_GENERIC_FUNC!>builderLike<!>(<!UNABLE_TO_INFER_GENERIC_FUNC!>{ => 1 }<!>, <!UNABLE_TO_INFER_GENERIC_FUNC!>{ => true }<!>)
    let _ = value
}

func conflictingLambdaFamilies(): Unit {
    let value = <!UNABLE_TO_INFER_GENERIC_FUNC!>builderLike<!>(<!UNABLE_TO_INFER_GENERIC_FUNC!>{ => "x" }<!>, <!UNABLE_TO_INFER_GENERIC_FUNC!>{ => 1 }<!>)
    let _ = value
}
