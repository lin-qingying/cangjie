// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_ERRORS
enum Token {
    A
}

func throwInt(): Unit {
    <!THROW_EXPR_WITH_WRONG_TYPE!>/* suggested message: the object thrown must derive from `core.Exception`. source: cjc 1.0.1 actual output; DiagnosticSema.def:sema_throw_expr_with_wrong_type; note: same error for all non-Exception/Error types */throw 1<!>
}

func throwEnumValue(): Unit {
    <!THROW_EXPR_WITH_WRONG_TYPE!>/* suggested message: the object thrown must derive from `core.Exception`. source: cjc 1.0.1 actual output; DiagnosticSema.def:sema_throw_expr_with_wrong_type; note: same error for all non-Exception/Error types */throw Token.A<!>
}
