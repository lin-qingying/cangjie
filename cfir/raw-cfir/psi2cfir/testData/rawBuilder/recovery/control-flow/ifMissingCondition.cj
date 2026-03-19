// RUN_PIPELINE_TILL: FRONTEND
func ifMissingCondition(): Int64 {
    return if () {
        1
    } else {
        2
    }
}

