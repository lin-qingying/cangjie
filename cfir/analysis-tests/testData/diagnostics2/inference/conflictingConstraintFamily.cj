// RUN_PIPELINE_TILL: FRONTEND
interface Named {}
interface Indexed {}

class NamedOnly <: Named {}
class IndexedOnly <: Indexed {}

func chooseBounded<T>(first: T, second: T): T where T <: Named & Indexed {
    return first
}

func conflictingUpperBounds(): Unit {
    let value = <!NEW_INFERENCE_ERROR!>chooseBounded<!>(NamedOnly(), IndexedOnly())
    let _ = value
}
