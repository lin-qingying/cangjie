// RUN_PIPELINE_TILL: FRONTEND
func matchGeneric<T>(value: T): Int64 {
    return match (value) {
        case <!NOT_OVERLOAD_IN_MATCH!>1<!> => 1
        case _ => 0
    }
}
