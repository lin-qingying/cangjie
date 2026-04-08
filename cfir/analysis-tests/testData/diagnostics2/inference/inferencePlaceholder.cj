// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_ERRORS
func chooseGeneric<T>(first: T, second: T): T {
    return first
}

func builderLike<T>(block1: () -> T, block2: () -> T): T {
    return block1()
}

func placeholders(): Unit {
    let _ = <!NEW_INFERENCE_ERROR!>/* suggested message: inference error: incompatible constraints for type variable. source: manual_source_zh_cn_generic_overview; external/cangjie_compiler/src/Sema/TypeArgumentInference.cpp */chooseGeneric(1, true)<!>

    let _ = <!BUILDER_INFERENCE_MULTI_LAMBDA_RESTRICTION!>/* suggested message: builder inference cannot resolve multiple lambda inputs for the same type variable. source: manual_source_zh_cn_function_call_desugar; external/cangjie_compiler/src/Sema/TypeArgumentInference.cpp */builderLike({ => 1 }, { => true })<!>

    let _ = <!INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION!>/* suggested message: inferred type variable collapses into an empty intersection. source: manual_source_zh_cn_generic_overview; external/cangjie_compiler/src/Sema/TypeArgumentInference.cpp */chooseGeneric(1, 1.0)<!>

    let _ = <!INFERRED_TYPE_VARIABLE_INTO_POSSIBLE_EMPTY_INTERSECTION!>/* suggested message: inferred type variable may collapse into a possible empty intersection. source: manual_source_zh_cn_generic_overview; external/cangjie_compiler/src/Sema/TypeArgumentInference.cpp */chooseGeneric(true, 1)<!>
}
