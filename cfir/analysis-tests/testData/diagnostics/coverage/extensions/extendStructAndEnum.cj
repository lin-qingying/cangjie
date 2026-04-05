// RUN_PIPELINE_TILL: FRONTEND
interface Marker {}

struct Record {}

enum Choice {
    | A
}

extend Record <: Marker {}
extend Choice <: Marker {}
