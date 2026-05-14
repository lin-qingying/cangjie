// RUN_PIPELINE_TILL: FRONTEND
func throwExceptionBound<T>(value: T): Unit where T <: Exception {
    throw value
}

func throwErrorBound<T>(value: T): Unit where T <: Error {
    throw value
}

func throwUnknownBound<T>(value: T): Unit {
    <!THROW_EXPR_WITH_WRONG_TYPE!>throw<!> value
}
