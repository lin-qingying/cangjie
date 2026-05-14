// RUN_PIPELINE_TILL: FRONTEND
open class BaseEx <: Exception {}
class ChildEx <: BaseEx {}
class Plain {}

func throwChildException(): Unit {
    throw ChildEx()
}

func throwErrorValue(error: Error): Unit {
    throw error
}

func throwPlainObject(): Unit {
    <!THROW_EXPR_WITH_WRONG_TYPE!>throw<!> Plain()
}
