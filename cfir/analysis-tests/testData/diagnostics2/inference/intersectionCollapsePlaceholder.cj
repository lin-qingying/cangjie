// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_ERRORS
func chooseGeneric<T>(first: T, second: T): T {
    return first
}

func emptyIntersectionLike(): Unit {
    let value = <!INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION!>/* suggested message: inferred type variable collapses into an empty intersection. source: external/cangjie_compiler/src/Sema/TypeArgumentInference.cpp */chooseGeneric(1, 1.0)<!>
    println(value)
}

func possibleEmptyIntersectionLike(): Unit {
    let value = <!INFERRED_TYPE_VARIABLE_INTO_POSSIBLE_EMPTY_INTERSECTION!>/* suggested message: inferred type variable may collapse into a possible empty intersection. source: external/cangjie_compiler/src/Sema/TypeArgumentInference.cpp */chooseGeneric(true, 1)<!>
    println(value)
}
