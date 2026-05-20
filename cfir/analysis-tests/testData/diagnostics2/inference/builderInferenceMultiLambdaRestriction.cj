// RUN_PIPELINE_TILL: FRONTEND
func builderLike<T>(block1: () -> T, block2: () -> T): T {
    return block1()
}

func conflictingLambdaReturns(): Unit {
    let value = <!NEW_INFERENCE_ERROR!>builderLike<!>(<!NEW_INFERENCE_ERROR!>{ => 1 }<!>, <!NEW_INFERENCE_ERROR!>{ => true }<!>)
    let _ = value
}

func conflictingLambdaFamilies(): Unit {
    let value = <!NEW_INFERENCE_ERROR!>builderLike<!>(<!NEW_INFERENCE_ERROR!>{ => "x" }<!>, <!NEW_INFERENCE_ERROR!>{ => 1 }<!>)
    let _ = value
}
