// RUN_PIPELINE_TILL: FRONTEND
open class Base {
    private func hidden(): Int64 {
        return 1
    }
}

class Child <: Base {
    <!CANNOT_OVERRIDE_INVISIBLE_MEMBER!>override<!> func hidden(): Int64 {
        return 2
    }

    <!NOTHING_TO_OVERRIDE!>override<!> func missing(): Int64 {
        return 0
    }
}
