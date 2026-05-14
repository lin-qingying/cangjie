// RUN_PIPELINE_TILL: FRONTEND
class PlainClass {}
class GoodException <: Exception {}

func throwException(): Unit {
    throw GoodException()
}

func throwError(value: Error): Unit {
    throw value
}

func throwPlainClass(): Unit {
    <!THROW_EXPR_WITH_WRONG_TYPE!>throw<!> PlainClass()
}
