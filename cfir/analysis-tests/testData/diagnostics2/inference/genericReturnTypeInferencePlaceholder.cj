// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_ERRORS
func identity<T>(x: T): T {
    return x
}

func choose<T>(first: T, second: T): T {
    return first
}

func inferredFromReturn(): Int64 {
    return identity(42)
}

func conflictingReturnInference(): Unit {
    let value = <!NEW_INFERENCE_ERROR!>/* suggested message: inference error: incompatible constraints for type variable. source: manual_source_zh_cn_generic_overview; external/cangjie_compiler/src/Sema/TypeArgumentInference.cpp */choose(1, true)<!>
    println(value)
}
