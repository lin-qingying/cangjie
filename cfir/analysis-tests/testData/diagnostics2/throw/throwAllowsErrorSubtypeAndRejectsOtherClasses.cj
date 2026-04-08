// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_ERRORS
class PlainClass {}
class GoodException <: Exception {}
class GoodError <: Error {}

func throwException(): Unit {
    throw GoodException()
}

func throwError(): Unit {
    throw GoodError()
}

func throwPlainClass(): Unit {
    <!THROW_EXPR_WITH_WRONG_TYPE!>/* suggested message: throw expression must have Exception or Error subtype. source: external/cangjie_compiler/src/Sema/TypeCheckExpr/ThrowExpr.cpp */throw PlainClass()<!>
}
