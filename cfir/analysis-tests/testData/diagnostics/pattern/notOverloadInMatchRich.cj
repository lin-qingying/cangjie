// RUN_PIPELINE_TILL: FRONTEND
func matchGeneric<T>(value: T): Int64 {
    return match (value) {
        case <!CANNOT_CONVERT_LITERAL!>1<!> => 1
        case _ => 0
    }
}
