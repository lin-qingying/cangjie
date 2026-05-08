// RUN_PIPELINE_TILL: FRONTEND
func tryFinallyTypeStable(flag: Bool): Int64 {
    try {
        if (flag) {
            return 1
        }
    } finally {
        let cleanup = 0
    }
    return 2
}

func finallyIllegalResultUse(): Int64 {
    return try {
        1
    } catch (e: Exception) {
        2
    } finally {
        <!UNUSED_EXPRESSION!>false<!>
    }
}
