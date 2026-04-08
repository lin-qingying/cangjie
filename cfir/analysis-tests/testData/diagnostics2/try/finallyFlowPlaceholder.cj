// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_ERRORS
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
        <!FINALLY_BLOCK_MUST_NOT_PRODUCE_RESULT!>/* suggested message: finally block should not participate in try expression result typing. source: external/cangjie_compiler/src/Sema/TypeCheckExpr/TryExpr.cpp */false<!>
    }
}
