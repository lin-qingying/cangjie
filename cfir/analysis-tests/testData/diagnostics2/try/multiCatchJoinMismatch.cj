// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_ERRORS
class ExA <: Exception {}
class ExB <: Exception {}

func multiCatchJoinMismatch(flag: Bool): Int64 {
    return try {
        if (flag) {
            throw ExA()
        }
        1
    } catch (e: ExA) {
        2
    } catch (e: ExB) {
        <!TRY_BRANCH_TYPE_MISMATCH!>/* suggested message: each catch branch must join with the accumulated try/catch result type. source: external/cangjie_compiler/src/Sema/TypeCheckExpr/TryExpr.cpp */false<!>
    }
}
