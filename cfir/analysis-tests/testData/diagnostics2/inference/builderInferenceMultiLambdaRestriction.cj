// RUN_PIPELINE_TILL: FRONTEND
func builderLike<T>(block1: () -> T, block2: () -> T): T {
    return block1()
}

func conflictingLambdaReturns(): Unit {
    let value = <!NEW_INFERENCE_ERROR!>builderLike<!>({ => 1 }, { => true })
    let _ = value
}

func conflictingLambdaFamilies(): Unit {
    let value = <!NEW_INFERENCE_ERROR!>builderLike<!>({ => "x" }, { => 1 })
    let _ = value
}
