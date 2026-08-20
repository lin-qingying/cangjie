// RUN_PIPELINE_TILL: FRONTEND
func mismatchCatchResult(flag: Bool): Int64 {
    return try {
        if (flag){ 1} else {2}
    } catch (e: Exception) {
        <!TYPE_MISMATCH!>false<!>
    }
}

func compatibleCatchResult(flag: Bool): Int64 {
    return try {
        if (flag) {1} else{ 2}
    } catch (e: Exception) {
        0
    }
}
