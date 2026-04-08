// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_ERRORS
class DomainEx <: Exception {}
class DomainError <: Error {}

func throwAndCatchException(flag: Bool): Int64 {
    return try {
        if (flag) {
            throw DomainEx()
        }
        1
    } catch (e: DomainEx) {
        2
    }
}

func throwAndCatchError(flag: Bool): Int64 {
    return try {
        if (flag) {
            throw DomainError()
        }
        1
    } catch (e: DomainError) {
        2
    }
}

func throwIllegalAndCatch(flag: Bool): Int64 {
    return try {
        if (flag) {
            <!THROW_EXPR_WITH_WRONG_TYPE!>/* suggested message: throw expression must have Exception or Error subtype. source: external/cangjie_compiler/src/Sema/TypeCheckExpr/ThrowExpr.cpp */throw 0<!>
        }
        1
    } catch (e: Exception) {
        2
    }
}
