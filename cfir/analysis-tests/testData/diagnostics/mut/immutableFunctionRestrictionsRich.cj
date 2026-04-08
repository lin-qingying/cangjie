// RUN_PIPELINE_TILL: FRONTEND
struct Counter {
    var value: Int64 = 0

    mut func inc(): Unit {
        value = value + 1
    }

    func readOnlyAssign(): Unit {
        <!CANNOT_MODIFY_VAR!>value<!> = 1
    }

    func readOnlyCall(): Unit {
        <!IMMUTABLE_FUNCTION_CANNOT_ACCESS_MUTABLE_FUNCTION!>inc<!>()
    }

    mut func allowed(): Unit {
        value = value + 1
        inc()
    }
}
