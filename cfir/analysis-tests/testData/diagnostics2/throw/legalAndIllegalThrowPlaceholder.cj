// RUN_PIPELINE_TILL: FRONTEND
class NotException {}
class GoodException <: Exception {}

func legalThrow(): Unit {
    throw GoodException()
}

func illegalThrowPrimitive(): Unit {
    <!THROW_EXPR_WITH_WRONG_TYPE!>throw<!> 1
}

func illegalThrowClass(): Unit {
    <!THROW_EXPR_WITH_WRONG_TYPE!>throw<!> NotException()
}
