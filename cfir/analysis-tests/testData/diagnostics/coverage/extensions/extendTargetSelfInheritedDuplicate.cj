// RUN_PIPELINE_TILL: FRONTEND
interface Root {}
interface Leaf <: Root {}

open class Base <: Leaf {}
class Host <: Base {}

extend Host <: <!EXTEND_DUPLICATE_INTERFACE!>Root<!> {}
extend Host <: <!EXTEND_DUPLICATE_INTERFACE!>Leaf<!> {}
