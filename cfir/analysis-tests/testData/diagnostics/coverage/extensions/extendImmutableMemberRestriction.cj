// RUN_PIPELINE_TILL: FRONTEND
interface Marker {}

struct Record {}

enum Choice {
    | A
}

extend Record <: Marker {
    mut prop state: Int64 {
        get() {
            1
        }
        set(value) {
        }
    }

    operator func [](index: Int64, value!: Int64): Unit {
        return
    }
}

extend Choice <: Marker {
    <!EXTEND_IMMUTABLE_MUT_PROPERTY!>mut<!> prop state: Int64 {
        get() {
            1
        }
        set(value) {
        }
    }

    operator func [](index: Int64, value!: Int64): Unit {
        return
    }
}
