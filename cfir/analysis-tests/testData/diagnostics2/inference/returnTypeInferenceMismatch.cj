// RUN_PIPELINE_TILL: FRONTEND
func produce<T>(value: T): () -> T {
    return { => value }
}

func expectInt(block: () -> Int64): Int64 {
    return block()
}

func badReturnInference(): Int64 {
    return expectInt(<!ARGUMENT_TYPE_MISMATCH!>produce(true)<!>)
}
