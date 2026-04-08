// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_ERRORS
func produce<T>(value: T): () -> T {
    return { => value }
}

func expectInt(block: () -> Int64): Int64 {
    return block()
}

func badReturnInference(): Int64 {
    return <!NEW_INFERENCE_ERROR!>/* suggested message: inferred return type cannot satisfy the expected return context. source: external/cangjie_compiler/src/Sema/TypeArgumentInference.cpp */expectInt(produce(true))<!>
}
