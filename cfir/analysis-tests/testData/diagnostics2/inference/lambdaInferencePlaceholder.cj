// RUN_PIPELINE_TILL: FRONTEND
func applyFunc<T>(f: (T) -> T, x: T): T {
    return f(x)
}

func lambdaInferenceOk(): Int64 {
    return applyFunc({ a => a + 1 }, 10)
}

func lambdaInferenceFromArgument(): Unit {
    let value = applyFunc({ a => a }, true)
    let _ = value
}
