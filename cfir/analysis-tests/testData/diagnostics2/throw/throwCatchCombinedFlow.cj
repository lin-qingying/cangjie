// RUN_PIPELINE_TILL: FRONTEND
class DomainEx <: Exception {}

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

func throwAndCatchError(flag: Bool, error: Error): Int64 {
    return try {
        if (flag) {
            throw error
        }
        1
    } catch (e: Error) {
        2
    }
}

func throwIllegalAndCatch(flag: Bool): Int64 {
    return try {
        if (flag) {
            <!THROW_EXPR_WITH_WRONG_TYPE!>throw<!> 0
        }
        1
    } catch (e: Exception) {
        2
    }
}
