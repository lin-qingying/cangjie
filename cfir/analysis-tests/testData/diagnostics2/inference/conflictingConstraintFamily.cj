// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_ERRORS
interface Named {}
interface Indexed {}

class NamedOnly <: Named {}
class IndexedOnly <: Indexed {}

func chooseBounded<T>(first: T, second: T): T where T <: Named & Indexed {
    return first
}

func conflictingUpperBounds(): Unit {
    let value = <!NEW_INFERENCE_ERROR!>/* suggested message: conflicting bounds for the inferred type variable cannot be solved. source: external/cangjie_compiler/src/Sema/TypeArgumentInference.cpp */chooseBounded(NamedOnly(), IndexedOnly())<!>
    println(value)
}
