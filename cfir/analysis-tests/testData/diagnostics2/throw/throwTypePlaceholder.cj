// RUN_PIPELINE_TILL: FRONTEND
class NotException {}

func badThrow(): Unit {
    <!THROW_EXPR_WITH_WRONG_TYPE!>throw<!> NotException()
}
