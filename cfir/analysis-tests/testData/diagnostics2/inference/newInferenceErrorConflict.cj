// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_ERRORS
func choose<T>(first: T, second: T): T {
    return first
}

func conflictingPrimitiveAndBool(): Unit {
    let value = <!NEW_INFERENCE_ERROR!>/* suggested message: inference error: incompatible constraints for type variable. source: external/cangjie_compiler/src/Sema/TypeArgumentInference.cpp */choose(1, true)<!>
    println(value)
}

func conflictingStringAndInt(): Unit {
    let value = <!NEW_INFERENCE_ERROR!>/* suggested message: inference error: incompatible constraints for type variable. source: external/cangjie_compiler/src/Sema/TypeArgumentInference.cpp */choose("x", 1)<!>
    println(value)
}
