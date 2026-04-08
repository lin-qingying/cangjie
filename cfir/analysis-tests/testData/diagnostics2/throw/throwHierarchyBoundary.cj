// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_ERRORS
open class BaseEx <: Exception {}
class ChildEx <: BaseEx {}
open class BaseErr <: Error {}
class ChildErr <: BaseErr {}
class Plain {}

func throwChildException(): Unit {
    throw ChildEx()
}

func throwChildError(): Unit {
    throw ChildErr()
}

func throwPlainObject(): Unit {
    <!THROW_EXPR_WITH_WRONG_TYPE!>/* suggested message: throw expression must belong to the Exception or Error hierarchy. source: external/cangjie_compiler/src/Sema/TypeCheckExpr/ThrowExpr.cpp */throw Plain()<!>
}
