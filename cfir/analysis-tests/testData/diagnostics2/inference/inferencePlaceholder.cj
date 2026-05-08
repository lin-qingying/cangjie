// RUN_PIPELINE_TILL: FRONTEND
func chooseGeneric<T>(first: T, second: T): T {
    return first
}

func builderLike<T>(block1: () -> T, block2: () -> T): T {
    return block1()
}

func placeholders(): Unit {
    let _ = <!NEW_INFERENCE_ERROR!>chooseGeneric<!>(1, true)

    let _ = builderLike({ => 1 }, <!BUILDER_INFERENCE_MULTI_LAMBDA_RESTRICTION!>{ => true }<!>)

    let _ = <!NEW_INFERENCE_ERROR!>chooseGeneric<!>(1, 1.0)

    let _ = <!NEW_INFERENCE_ERROR!>chooseGeneric<!>(true, 1)
}
