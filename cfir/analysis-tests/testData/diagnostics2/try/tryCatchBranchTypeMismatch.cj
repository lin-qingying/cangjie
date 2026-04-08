// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_ERRORS
func mismatchCatchResult(flag: Bool): Int64 {
    return try {
        if (flag) 1 else 2
    } catch (e: Exception) {
        <!TRY_BRANCH_TYPE_MISMATCH!>/* suggested message: catch branch result type must join with the try branch result type. source: external/cangjie_compiler/src/Sema/TypeCheckExpr/TryExpr.cpp */false<!>
    }
}

func compatibleCatchResult(flag: Bool): Int64 {
    return try {
        if (flag) 1 else 2
    } catch (e: Exception) {
        0
    }
}
