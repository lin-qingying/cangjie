// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_ERRORS
func applyFunc<T>(f: (T) -> T, x: T): T {
    return f(x)
}

func lambdaInferenceOk(): Int64 {
    return applyFunc({ a => a + 1 }, 10)
}

func lambdaInferenceConflict(): Unit {
    let value = <!BUILDER_INFERENCE_MULTI_LAMBDA_RESTRICTION!>/* suggested message: lambda inference still needs a dedicated producer-parity case for this unresolved shape. source: manual_source_zh_cn_function_call_desugar; external/cangjie_compiler/src/Sema/TypeArgumentInference.cpp */applyFunc({ a => a }, true)<!>
    println(value)
}
