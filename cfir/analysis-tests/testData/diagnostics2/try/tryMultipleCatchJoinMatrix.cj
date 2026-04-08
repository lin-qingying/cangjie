// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_ERRORS
class FirstEx <: Exception {}
class SecondEx <: Exception {}

func mismatchAcrossMultipleCatches(flag: Int64): Int64 {
    return try {
        if (flag == 0) {
            throw FirstEx()
        }
        if (flag == 1) {
            throw SecondEx()
        }
        1
    } catch (e: FirstEx) {
        2
    } catch (e: SecondEx) {
        <!TRY_BRANCH_TYPE_MISMATCH!>/* suggested message: each catch branch result type must join with the accumulated try/catch result type. source: external/cangjie_compiler/src/Sema/TypeCheckExpr/TryExpr.cpp */false<!>
    }
}

func compatibleAcrossMultipleCatches(flag: Int64): Int64 {
    return try {
        if (flag == 0) {
            throw FirstEx()
        }
        if (flag == 1) {
            throw SecondEx()
        }
        1
    } catch (e: FirstEx) {
        2
    } catch (e: SecondEx) {
        3
    }
}
