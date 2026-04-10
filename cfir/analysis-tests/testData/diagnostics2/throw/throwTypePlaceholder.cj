// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_ERRORS
class NotException {}

func badThrow(): Unit {
    <!THROW_EXPR_WITH_WRONG_TYPE!>/* suggested message: the object thrown must derive from `core.Exception`. source: cjc 1.0.1 actual output; DiagnosticSema.def:sema_throw_expr_with_wrong_type; note: code also accepts Error subtypes but message only mentions Exception */throw NotException()<!>
}
