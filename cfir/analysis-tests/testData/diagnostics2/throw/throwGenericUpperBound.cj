// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_ERRORS
func throwExceptionBound<T>(value: T): Unit where T <: Exception {
    throw value
}

func throwErrorBound<T>(value: T): Unit where T <: Error {
    throw value
}

func throwUnknownBound<T>(value: T): Unit {
    <!THROW_EXPR_WITH_WRONG_TYPE!>/* suggested message: throw expression must be provably an Exception or Error subtype. source: external/cangjie_compiler/src/Sema/TypeCheckExpr/ThrowExpr.cpp */throw value<!>
}
