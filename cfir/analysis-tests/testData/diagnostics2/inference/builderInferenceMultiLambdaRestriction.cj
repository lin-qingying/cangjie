// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_ERRORS
func builderLike<T>(block1: () -> T, block2: () -> T): T {
    return block1()
}

func conflictingLambdaReturns(): Unit {
    let value = <!BUILDER_INFERENCE_MULTI_LAMBDA_RESTRICTION!>/* suggested message: builder inference cannot resolve multiple lambda inputs for the same type variable. source: external/cangjie_compiler/src/Sema/TypeArgumentInference.cpp */builderLike({ => 1 }, { => true })<!>
    println(value)
}

func conflictingLambdaFamilies(): Unit {
    let value = <!BUILDER_INFERENCE_MULTI_LAMBDA_RESTRICTION!>/* suggested message: builder inference cannot resolve multiple lambda inputs for the same type variable. source: external/cangjie_compiler/src/Sema/TypeArgumentInference.cpp */builderLike({ => "x" }, { => 1 })<!>
    println(value)
}
