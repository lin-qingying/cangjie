// RUN_PIPELINE_TILL: FRONTEND
func builderLike<T>(block1: () -> T, block2: () -> T): T {
    return block1()
}

func conflictingLambdaReturns(): Unit {
    let value = builderLike({ => 1 }, <!BUILDER_INFERENCE_MULTI_LAMBDA_RESTRICTION!>{ => true }<!>)
    let _ = value
}

func conflictingLambdaFamilies(): Unit {
    let value = builderLike({ => "x" }, <!BUILDER_INFERENCE_MULTI_LAMBDA_RESTRICTION!>{ => 1 }<!>)
    let _ = value
}
